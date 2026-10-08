package com.bsball.service;

import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.stats.earnedrun.UnearnedRunReason;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.stats.earnedrun.EarnedRunDecision;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionEngine;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.EarnedRunStatus;
import com.bsball.stats.earnedrun.ScoringRunner;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EarnedRunReconstructionService {
    private final EarnedRunPlayRepository playRepository;
    private final EarnedRunDecisionRepository decisionRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final EarnedRunReconstructionEngine engine;

    public EarnedRunReconstructionService(
            EarnedRunPlayRepository playRepository,
            EarnedRunDecisionRepository decisionRepository,
            GamePlayerStatRepository gamePlayerStatRepository) {
        this.playRepository = playRepository;
        this.decisionRepository = decisionRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
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
        List<EarnedRunDecisionEntity> oldDecisions = findExistingDecisions(
                tenantId, gameId, existing);
        deleteExistingDecisions(tenantId, gameId, existing);
        playRepository.deleteByTenantIdAndGameIdAndInningAndHalf(
                tenantId, gameId, inning, half);
        persistReconstruction(tenantId, gameId, inning, half, operatorId, plays, result);
        applyPitcherStatDeltas(tenantId, gameId, fieldingTeamId, oldDecisions, result.decisions());
        return result;
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
        EarnedRunStatus previous = EarnedRunStatus.valueOf(entity.getEarnedStatus());
        if (previous != next) {
            adjustPitcherStat(tenantId, gameId, entity.getResponsiblePitcherId(), previous, -1);
            adjustPitcherStat(tenantId, gameId, entity.getResponsiblePitcherId(), next, 1);
        }
        entity.setEarnedStatus(next.name());
        entity.setUnearnedReason(next == EarnedRunStatus.UNEARNED ? unearnedReason : null);
        entity.setOverriddenBy(operatorId);
        entity.setOverriddenAt(LocalDateTime.now());
        return decisionRepository.save(entity);
    }

    private void adjustPitcherStat(long tenantId, long gameId, long pitcherId,
            EarnedRunStatus status, int direction) {
        GamePlayerStat stat = gamePlayerStatRepository.findByGameId(gameId).stream()
                .filter(row -> Objects.equals(row.getTenantId(), tenantId))
                .filter(row -> Objects.equals(row.getPlayerId(), pitcherId))
                .filter(row -> Integer.valueOf(1).equals(row.getIsPitcher()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing pitcher stat row for responsible pitcher " + pitcherId));
        stat.setPitchR(Math.max(0, valueOrZero(stat.getPitchR()) + direction));
        if (status == EarnedRunStatus.EARNED) stat.setEr(Math.max(0, valueOrZero(stat.getEr()) + direction));
        if (status == EarnedRunStatus.UNEARNED) stat.setUnearnedR(Math.max(0, valueOrZero(stat.getUnearnedR()) + direction));
        if (status == EarnedRunStatus.PENDING) stat.setPendingR(Math.max(0, valueOrZero(stat.getPendingR()) + direction));
        gamePlayerStatRepository.save(stat);
    }

    private List<EarnedRunDecisionEntity> findExistingDecisions(
            long tenantId, long gameId, List<EarnedRunPlayEntity> existing) {
        List<Long> playIds = existing.stream().map(EarnedRunPlayEntity::getId).toList();
        if (playIds.isEmpty()) {
            return List.of();
        }
        return decisionRepository.findByTenantIdAndGameIdAndPlayIdIn(
                tenantId, gameId, playIds);
    }

    private void applyPitcherStatDeltas(
            long tenantId,
            long gameId,
            Long fieldingTeamId,
            List<EarnedRunDecisionEntity> oldDecisions,
            List<EarnedRunDecision> newDecisions) {
        Map<Long, int[]> deltas = new HashMap<>();
        for (EarnedRunDecisionEntity decision : oldDecisions) {
            addDelta(deltas, decision.getResponsiblePitcherId(), -1,
                    "EARNED".equals(decision.getEarnedStatus()) ? -1 : 0,
                    "UNEARNED".equals(decision.getEarnedStatus()) ? -1 : 0,
                    "PENDING".equals(decision.getEarnedStatus()) ? -1 : 0);
        }
        for (EarnedRunDecision decision : newDecisions) {
            addDelta(deltas, decision.responsiblePitcherId(), 1,
                    decision.status() == EarnedRunStatus.EARNED ? 1 : 0,
                    decision.status() == EarnedRunStatus.UNEARNED ? 1 : 0,
                    decision.status() == EarnedRunStatus.PENDING ? 1 : 0);
        }
        if (deltas.isEmpty()) {
            return;
        }

        Map<Long, GamePlayerStat> pitcherStats = new HashMap<>();
        for (GamePlayerStat stat : gamePlayerStatRepository.findByGameId(gameId)) {
            if (stat.getTenantId() != null
                    && stat.getTenantId() == tenantId
                    && stat.getPlayerId() != null
                    && Integer.valueOf(1).equals(stat.getIsPitcher())) {
                pitcherStats.put(stat.getPlayerId(), stat);
            }
        }
        for (Map.Entry<Long, int[]> entry : deltas.entrySet()) {
            GamePlayerStat stat = pitcherStats.get(entry.getKey());
            if (stat == null) {
                stat = createPitcherStatRow(tenantId, gameId, fieldingTeamId, entry.getKey());
                pitcherStats.put(entry.getKey(), stat);
            }
            int[] delta = entry.getValue();
            stat.setPitchR(Math.max(0, valueOrZero(stat.getPitchR()) + delta[0]));
            stat.setEr(Math.max(0, valueOrZero(stat.getEr()) + delta[1]));
            stat.setUnearnedR(Math.max(0, valueOrZero(stat.getUnearnedR()) + delta[2]));
            stat.setPendingR(Math.max(0, valueOrZero(stat.getPendingR()) + delta[3]));
            gamePlayerStatRepository.save(stat);
        }
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

    private void addDelta(
            Map<Long, int[]> deltas,
            Long pitcherId,
            int runDelta,
            int earnedDelta,
            int unearnedDelta,
            int pendingDelta) {
        int[] value = deltas.computeIfAbsent(pitcherId, ignored -> new int[4]);
        value[0] += runDelta;
        value[1] += earnedDelta;
        value[2] += unearnedDelta;
        value[3] += pendingDelta;
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
        entity.setRunnerOrigin(runner.origin().name());
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
}
