package com.bsball.service;

import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.Player;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.stats.earnedrun.EarnedRunDecision;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionEngine;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.EarnedRunStatus;
import com.bsball.stats.earnedrun.ScoringRunner;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EarnedRunReconstructionService {
    private final EarnedRunPlayRepository playRepository;
    private final EarnedRunDecisionRepository decisionRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final EarnedRunReconstructionEngine engine;
    private final PlayerRepository playerRepository;

    public EarnedRunReconstructionService(
            EarnedRunPlayRepository playRepository,
            EarnedRunDecisionRepository decisionRepository,
            GamePlayerStatRepository gamePlayerStatRepository,
            PlayerRepository playerRepository) {
        this.playRepository = playRepository;
        this.decisionRepository = decisionRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.playerRepository = playerRepository;
        this.engine = new EarnedRunReconstructionEngine();
    }

    /** Replaces and reconstructs all earned-run events for one half-inning. */
    @Transactional
    public EarnedRunReconstructionResult replaceHalfInning(
            long tenantId,
            long gameId,
            int inning,
            String half,
            long operatorId,
            List<EarnedRunPlay> plays,
            Long fieldingTeamId) {
        validateIdentity(tenantId, gameId, inning, half, operatorId);
        EarnedRunReconstructionResult result = engine.reconstruct(plays);
        List<EarnedRunPlayEntity> existing = playRepository
                .findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                        tenantId, gameId, inning, half);
        deleteExistingDecisions(tenantId, gameId, existing);
        playRepository.deleteByTenantIdAndGameIdAndInningAndHalf(
                tenantId, gameId, inning, half);
        persistReconstruction(tenantId, gameId, inning, half, operatorId, plays, result);
        Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> absoluteTotals =
                recomputePitcherStats(tenantId, gameId, fieldingTeamId);
        return result.withPitcherAbsoluteTotals(absoluteTotals);
    }

    /** 该场是否已有 ER 引擎事件；按 gameId 全局唯一判断，避免换赛事/租户后守卫失效。 */
    public boolean hasEngineData(long gameId) {
        return gameId > 0 && playRepository.existsByGameId(gameId);
    }

    /** 该场是否仍有以该投手为责任投手的自责分判定；删除投手统计行前的守卫。 */
    public boolean hasResponsiblePitcherDecisions(long gameId, long pitcherId) {
        return gameId > 0 && pitcherId > 0
                && decisionRepository.existsByGameIdAndResponsiblePitcherId(
                        Long.valueOf(gameId), Long.valueOf(pitcherId));
    }

    @Transactional
    public EarnedRunDecisionEntity overrideDecision(
            long tenantId, long gameId, long decisionId, long operatorId,
            String earnedStatus, String unearnedReason) {
        if (tenantId <= 0 || gameId <= 0 || decisionId <= 0 || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, decision and operator must be positive");
        }
        EarnedRunStatus next = EarnedRunStatus.valueOf(Objects.requireNonNull(earnedStatus).toUpperCase());
        if (next == EarnedRunStatus.PENDING) {
            throw new IllegalArgumentException("Manual review must resolve PENDING to EARNED or UNEARNED");
        }
        if (next == EarnedRunStatus.UNEARNED && (unearnedReason == null || unearnedReason.isBlank())) {
            throw new IllegalArgumentException("UNEARNED requires an unearned reason");
        }
        EarnedRunDecisionEntity entity = decisionRepository
                .findByIdAndTenantIdAndGameId(decisionId, tenantId, gameId)
                .orElseThrow(() -> new IllegalArgumentException("Earned-run decision not found"));
        entity.setEarnedStatus(next.name());
        entity.setUnearnedReason(next == EarnedRunStatus.UNEARNED ? unearnedReason : null);
        entity.setOverriddenBy(operatorId);
        entity.setOverriddenAt(LocalDateTime.now());
        EarnedRunDecisionEntity saved = decisionRepository.save(entity);
        // 覆核只改单条判定，但仍按该场全量判定绝对重算，避免增量误差残留。
        recomputePitcherStats(tenantId, gameId, null);
        return saved;
    }

    /**
     * 以该场全部逐分判定为唯一事实来源，绝对覆盖每名责任投手的 R/ER/UER/PENDING。
     * 增量维护在漏记、重复重建或人工覆核后无法自愈，因此每次都做全量重算。
     */
    private Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> recomputePitcherStats(
            long tenantId, long gameId, Long fallbackTeamId) {
        Map<Long, GamePlayerStat> pitcherStats = new HashMap<>();
        Map<Long, Long> teamByPlayer = new HashMap<>();
        for (GamePlayerStat stat : gamePlayerStatRepository.findByGameId(gameId)) {
            if (stat.getTenantId() == null || stat.getTenantId() != tenantId
                    || stat.getPlayerId() == null) {
                continue;
            }
            teamByPlayer.putIfAbsent(stat.getPlayerId(), stat.getTeamId());
            if (Integer.valueOf(1).equals(stat.getIsPitcher())) {
                pitcherStats.put(stat.getPlayerId(), stat);
            }
        }

        Map<Long, int[]> totals = new HashMap<>();
        for (EarnedRunDecisionEntity decision : decisionRepository
                .findByTenantIdAndGameId(tenantId, gameId)) {
            Long pitcherId = decision.getResponsiblePitcherId();
            if (pitcherId == null) {
                continue;
            }
            int[] value = totals.computeIfAbsent(pitcherId, ignored -> new int[4]);
            value[0] += 1;
            String status = decision.getEarnedStatus();
            if (EarnedRunStatus.EARNED.name().equals(status)) {
                value[1] += 1;
            } else if (EarnedRunStatus.UNEARNED.name().equals(status)) {
                value[2] += 1;
            } else if (EarnedRunStatus.PENDING.name().equals(status)) {
                value[3] += 1;
            }
        }

        for (Map.Entry<Long, int[]> entry : totals.entrySet()) {
            GamePlayerStat stat = pitcherStats.get(entry.getKey());
            if (stat == null) {
                Long teamId = teamByPlayer.get(entry.getKey());
                if (teamId == null) {
                    teamId = fallbackTeamId;
                }
                if (teamId == null) {
                    throw new IllegalStateException(
                            "Missing pitcher stat row for responsible pitcher " + entry.getKey());
                }
                stat = createPitcherStatRow(tenantId, gameId, teamId, entry.getKey());
                pitcherStats.put(entry.getKey(), stat);
            }
            int[] total = entry.getValue();
            stat.setPitchR(total[0]);
            stat.setEr(total[1]);
            stat.setUnearnedR(total[2]);
            stat.setPendingR(total[3]);
            gamePlayerStatRepository.save(stat);
        }

        // 返回全投手绝对值（含本次未触及的投手），供前端幂等覆盖展示。
        Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> absoluteTotals = new HashMap<>();
        for (Map.Entry<Long, GamePlayerStat> entry : pitcherStats.entrySet()) {
            GamePlayerStat stat = entry.getValue();
            absoluteTotals.put(entry.getKey(), new EarnedRunReconstructionResult.PitcherRunTotals(
                    valueOrZero(stat.getPitchR()),
                    valueOrZero(stat.getEr()),
                    valueOrZero(stat.getUnearnedR()),
                    valueOrZero(stat.getPendingR())));
        }
        return absoluteTotals;
    }

    /**
     * 责任投手缺少投手统计行时补建：teamId 取该半局守备方（上半局主队、下半局客队）。
     * 中途登场或“仅守备”投手在落库前先被引擎记账时不至于整半局重建失败（#203）。
     */
    private GamePlayerStat createPitcherStatRow(
            long tenantId, long gameId, Long fieldingTeamId, long pitcherId) {
        GamePlayerStat stat = new GamePlayerStat();
        stat.setTenantId(tenantId);
        stat.setGameId(gameId);
        stat.setTeamId(fieldingTeamId);
        stat.setPlayerId(pitcherId);
        stat.setIsPitcher(1);
        stat.setPosition("P");
        stat.setPitchR(0);
        stat.setEr(0);
        stat.setUnearnedR(0);
        stat.setPendingR(0);
        return stat;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private void persistReconstruction(
            long tenantId,
            long gameId,
            int inning,
            String half,
            long operatorId,
            List<EarnedRunPlay> plays,
            EarnedRunReconstructionResult result) {
        Map<Long, EarnedRunPlayEntity> savedPlays = new HashMap<>();
        for (EarnedRunPlay play : plays) {
            EarnedRunPlayEntity saved = playRepository.save(toEntity(
                    tenantId, gameId, inning, half, play));
            savedPlays.put(play.playId(), saved);
        }
        Map<String, ScoringRunner> runners = indexRunners(plays);
        for (EarnedRunDecision decision : result.decisions()) {
            ScoringRunner runner = runners.get(decisionKey(decision.playId(), decision.runnerId()));
            EarnedRunPlayEntity play = savedPlays.get(decision.playId());
            decisionRepository.save(toEntity(
                    tenantId, gameId, operatorId, play.getId(), runner, decision));
        }
    }

    private EarnedRunPlayEntity toEntity(
            long tenantId, long gameId, int inning, String half, EarnedRunPlay play) {
        EarnedRunPlayEntity entity = new EarnedRunPlayEntity();
        entity.setTenantId(tenantId);
        entity.setGameId(gameId);
        entity.setInning(inning);
        entity.setHalf(half);
        entity.setSequence(play.sequence());
        entity.setSourceEventId(String.valueOf(play.playId()));
        entity.setActualOutsAdded(play.actualOutsAdded());
        entity.setReconstructedOutsAdded(play.reconstructedOutsAdded());
        entity.setRulingPending(play.rulingPending());
        return entity;
    }

    private EarnedRunDecisionEntity toEntity(
            long tenantId,
            long gameId,
            long operatorId,
            long playId,
            ScoringRunner runner,
            EarnedRunDecision decision) {
        EarnedRunDecisionEntity entity = new EarnedRunDecisionEntity();
        entity.setTenantId(tenantId);
        entity.setGameId(gameId);
        entity.setPlayId(playId);
        entity.setRunnerId(decision.runnerId());
        entity.setResponsiblePitcherId(decision.responsiblePitcherId());
        // 引擎当前只产出责任投手；上垒投手先按责任投手回填，待逐球上垒投手接入后替换。
        entity.setReachedBasePitcherId(decision.responsiblePitcherId());
        entity.setRunnerOrigin(runner.origin().name());
        entity.setWouldScoreWithoutMisplay(runner.wouldScoreWithoutMisplay());
        entity.setEarnedStatus(decision.status().name());
        entity.setUnearnedReason(decision.reason() == null ? null : decision.reason().name());
        if (runner.overrideStatus() != null) {
            entity.setOverriddenBy(operatorId);
            entity.setOverriddenAt(LocalDateTime.now());
        }
        return entity;
    }

    private Map<String, ScoringRunner> indexRunners(List<EarnedRunPlay> plays) {
        Map<String, ScoringRunner> runners = new HashMap<>();
        for (EarnedRunPlay play : plays) {
            for (ScoringRunner runner : play.scoringRunners()) {
                runners.put(decisionKey(play.playId(), runner.runnerId()), runner);
            }
        }
        return runners;
    }

    private String decisionKey(long playId, long runnerId) {
        return playId + ":" + runnerId;
    }

    private void deleteExistingDecisions(
            long tenantId, long gameId, List<EarnedRunPlayEntity> existing) {
        List<Long> playIds = existing.stream().map(EarnedRunPlayEntity::getId).toList();
        if (!playIds.isEmpty()) {
            decisionRepository.deleteByTenantIdAndGameIdAndPlayIdIn(tenantId, gameId, playIds);
        }
    }

    private void validateIdentity(
            long tenantId, long gameId, int inning, String half, long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || inning <= 0 || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, inning and operator must be positive");
        }
        if (!"top".equals(half) && !"bottom".equals(half)) {
            throw new IllegalArgumentException("Half must be top or bottom");
        }
    }

    // ─── 判定清單（供覆寫 UI） ─────────────────────────────────────────────────

    /** 回傳該場所有 ER 判定行，含跑者/投手名稱及半局資訊。 */
    public List<EarnedRunDecisionListDTO> listDecisions(long tenantId, long gameId) {
        Map<Long, EarnedRunPlayEntity> playById = playRepository
                .findByTenantIdAndGameId(tenantId, gameId).stream()
                .collect(Collectors.toMap(EarnedRunPlayEntity::getId, p -> p, (a, b) -> a));

        return decisionRepository.findByTenantIdAndGameId(tenantId, gameId).stream()
                .map(d -> {
                    EarnedRunPlayEntity play = playById.get(d.getPlayId());
                    String runnerName = resolvePlayerName(d.getRunnerId());
                    String pitcherName = resolvePlayerName(d.getResponsiblePitcherId());
                    return new EarnedRunDecisionListDTO(
                            d.getId(),
                            d.getPlayId(),
                            play != null ? play.getInning() : null,
                            play != null ? play.getHalf() : null,
                            play != null ? play.getSequence() : null,
                            d.getRunnerId(),
                            runnerName,
                            d.getResponsiblePitcherId(),
                            pitcherName,
                            d.getReachedBasePitcherId(),
                            resolvePlayerName(d.getReachedBasePitcherId()),
                            d.getEarnedStatus(),
                            d.getUnearnedReason(),
                            d.getRunnerOrigin(),
                            d.getOverriddenBy(),
                            d.getOverriddenAt());
                })
                .sorted((a, b) -> {
                    int ia = a.inning() != null ? a.inning() : 0;
                    int ib = b.inning() != null ? b.inning() : 0;
                    if (ia != ib) return Integer.compare(ia, ib);
                    int ha = "top".equals(a.half()) ? 0 : 1;
                    int hb = "top".equals(b.half()) ? 0 : 1;
                    if (ha != hb) return Integer.compare(ha, hb);
                    int sa = a.sequence() != null ? a.sequence() : 0;
                    int sb = b.sequence() != null ? b.sequence() : 0;
                    return Integer.compare(sa, sb);
                })
                .toList();
    }

    private String resolvePlayerName(Long playerId) {
        if (playerId == null) return null;
        return playerRepository.findById(playerId)
                .map(Player::getName)
                .orElse("#" + playerId);
    }

    // ─── §11 結算驗證 ───────────────────────────────────────────────────────────

    /**
     * 比賽結束時執行，回傳違規訊息列表（空 = 全部通過）。
     * 規範 §11：R=ER+UER+PENDING、ER≤R、UER≥0、不重複判定、責任投手非空。
     */
    public List<String> validateSettlement(long tenantId, long gameId) {
        List<String> violations = new ArrayList<>();

        // 1-3: per-pitcher aggregated checks
        for (GamePlayerStat p : gamePlayerStatRepository.findByGameId(gameId)) {
            if (p.getTenantId() == null || p.getTenantId() != tenantId) continue;
            if (!Integer.valueOf(1).equals(p.getIsPitcher())) continue;
            long pid = p.getPlayerId() != null ? p.getPlayerId() : 0;
            int r = nz(p.getPitchR());
            int er = nz(p.getEr());
            int uer = nz(p.getUnearnedR());
            int pr = nz(p.getPendingR());
            if (er + uer + pr != r) {
                violations.add(String.format(
                        "\u00a711.1 投手 %d: R(%d) \u2260 ER+UER+PENDING(%d+%d+%d=%d)",
                        pid, r, er, uer, pr, er + uer + pr));
            }
            if (er > r) {
                violations.add(String.format("\u00a711.2 投手 %d: ER(%d) > R(%d)", pid, er, r));
            }
            if (uer < 0) {
                violations.add(String.format("\u00a711.3 投手 %d: UER(%d) < 0", pid, uer));
            }
        }

        // 4-5: per-decision checks
        List<EarnedRunDecisionEntity> decisions =
                decisionRepository.findByTenantIdAndGameId(tenantId, gameId);
        Set<String> seen = new HashSet<>();
        for (EarnedRunDecisionEntity d : decisions) {
            // \u00a711.4 同一得分（playId+runnerId）不得重複判定
            String key = d.getPlayId() + ":" + d.getRunnerId();
            if (!seen.add(key)) {
                violations.add(String.format(
                        "\u00a711.4 重複判定: decisionId=%d playId=%d runnerId=%d",
                        d.getId(), d.getPlayId(), d.getRunnerId()));
            }
            // \u00a711.5 每筆判定須有責任投手（PENDING 除外：引擎尚未指派）
            if (d.getResponsiblePitcherId() == null
                    && !EarnedRunStatus.PENDING.name().equals(d.getEarnedStatus())) {
                violations.add(String.format(
                        "\u00a711.5 缺少責任投手: decisionId=%d status=%s",
                        d.getId(), d.getEarnedStatus()));
            }
        }

        return violations;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
