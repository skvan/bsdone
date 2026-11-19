package com.bsball.service;

import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.dto.PitcherRunTotalsDTO;
import com.bsball.model.dto.IdentityTransferPreviewDTO;
import com.bsball.model.dto.IdentityTransferResultDTO;
import com.bsball.model.dto.PitcherTransferResultDTO;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.ErDecisionAdjustment;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GameIdentityTransfer;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.ErDecisionAdjustmentRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GameIdentityTransferRepository;
import com.bsball.repository.GamePitcherTransferRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameRepository;
import com.bsball.repository.GameSnapshotRepository;
import com.bsball.repository.PitcherRunOverrideRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.stats.earnedrun.EarnedRunDecision;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionEngine;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.EarnedRunStatus;
import com.bsball.stats.earnedrun.ScoringRunner;
import com.bsball.utils.GamePlayerStatZeros;
import com.bsball.utils.LiveSnapshotIdentityTransfer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EarnedRunReconstructionService {
    private static final String OVERRIDE_APPROVED = "APPROVED";
    private static final String OVERRIDE_REVOKED = "REVOKED";
    private static final String OVERRIDE_INVALIDATED = "INVALIDATED";
    private static final String TRANSFER_STATUS_TRANSFERRED = "TRANSFERRED";
    private static final String TRANSFER_STATUS_DELETED = "DELETED";
    private static final String SCOPE_STATS = "STATS";
    private static final String SCOPE_FIELDING = "FIELDING";
    private static final String SCOPE_ER_DECISIONS = "ER_DECISIONS";
    private static final String SCOPE_LIVE_RECORDS = "LIVE_RECORDS";
    private static final Set<String> DEFAULT_IDENTITY_SCOPES =
            Set.of(SCOPE_STATS, SCOPE_FIELDING, SCOPE_ER_DECISIONS);
    private static final Set<String> ALL_IDENTITY_SCOPES =
            Set.of(SCOPE_STATS, SCOPE_FIELDING, SCOPE_ER_DECISIONS, SCOPE_LIVE_RECORDS);
    static final String ADJUSTMENT_ACTIVE = "ACTIVE";
    static final String ADJUSTMENT_SUPERSEDED = "SUPERSEDED";
    static final String ADJUSTMENT_REVOKED = "REVOKED";
    static final String ADJUSTMENT_REASSIGN = "REASSIGN";
    static final String ADJUSTMENT_VOID = "VOID";

    private final EarnedRunPlayRepository playRepository;
    private final EarnedRunDecisionRepository decisionRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final PitcherRunOverrideRepository pitcherRunOverrideRepository;
    private final GameFieldingPosStatRepository gameFieldingPosStatRepository;
    private final GameFieldingAdjustmentRepository gameFieldingAdjustmentRepository;
    private final GamePitcherTransferRepository gamePitcherTransferRepository;
    private final GameIdentityTransferRepository gameIdentityTransferRepository;
    private final ErDecisionAdjustmentRepository erDecisionAdjustmentRepository;
    private final GameRepository gameRepository;
    private final GameSnapshotRepository gameSnapshotRepository;
    private final EarnedRunReconstructionEngine engine;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;
    private final ObjectMapper auditMapper;

    public EarnedRunReconstructionService(
            EarnedRunPlayRepository playRepository,
            EarnedRunDecisionRepository decisionRepository,
            GamePlayerStatRepository gamePlayerStatRepository,
            PitcherRunOverrideRepository pitcherRunOverrideRepository,
            GameFieldingPosStatRepository gameFieldingPosStatRepository,
            GameFieldingAdjustmentRepository gameFieldingAdjustmentRepository,
            GamePitcherTransferRepository gamePitcherTransferRepository,
            GameIdentityTransferRepository gameIdentityTransferRepository,
            ErDecisionAdjustmentRepository erDecisionAdjustmentRepository,
            GameRepository gameRepository,
            GameSnapshotRepository gameSnapshotRepository,
            PlayerRepository playerRepository,
            ObjectMapper objectMapper) {
        this.playRepository = playRepository;
        this.decisionRepository = decisionRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.pitcherRunOverrideRepository = pitcherRunOverrideRepository;
        this.gameFieldingPosStatRepository = gameFieldingPosStatRepository;
        this.gameFieldingAdjustmentRepository = gameFieldingAdjustmentRepository;
        this.gamePitcherTransferRepository = gamePitcherTransferRepository;
        this.gameIdentityTransferRepository = gameIdentityTransferRepository;
        this.erDecisionAdjustmentRepository = erDecisionAdjustmentRepository;
        this.gameRepository = gameRepository;
        this.gameSnapshotRepository = gameSnapshotRepository;
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
        // 审计快照包含 BaseEntity 的 LocalDateTime；项目 ObjectMapper 未注册 JavaTimeModule，使用副本注册。
        this.auditMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        this.engine = new EarnedRunReconstructionEngine();
    }

    /**
     * 快照正文按需读取（2026-11-15 OOM 修复，Issue #686）：
     * 大字段不随 Game 实体加载，只在核对/改写现场记录时单独查询。
     */
    private String liveSnapshotOf(Game game) {
        return game.getId() == null ? null
                : this.gameSnapshotRepository.findSnapshotJsonById(game.getId()).orElse(null);
    }

    private record IntStatField(
            Function<GamePlayerStat, Integer> getter,
            BiConsumer<GamePlayerStat, Integer> setter) {}

    private record DoubleStatField(
            Function<GamePlayerStat, Double> getter,
            BiConsumer<GamePlayerStat, Double> setter) {}

    private record DecisionKey(String sourceEventId, Long runnerId) {}

    private record DecisionView(
            EarnedRunDecisionEntity decision,
            EarnedRunPlayEntity play,
            Long effectivePitcherId,
            boolean voided,
            ErDecisionAdjustment adjustment) {}

    private static final List<IntStatField> PITCHER_TRANSFER_INT_FIELDS = List.of(
            new IntStatField(GamePlayerStat::getNp, GamePlayerStat::setNp),
            new IntStatField(GamePlayerStat::getOutsRecorded, GamePlayerStat::setOutsRecorded),
            new IntStatField(GamePlayerStat::getPitchH, GamePlayerStat::setPitchH),
            new IntStatField(GamePlayerStat::getPitchDoubles, GamePlayerStat::setPitchDoubles),
            new IntStatField(GamePlayerStat::getPitchTriples, GamePlayerStat::setPitchTriples),
            new IntStatField(GamePlayerStat::getPitchBbHp, GamePlayerStat::setPitchBbHp),
            new IntStatField(GamePlayerStat::getPitchSo, GamePlayerStat::setPitchSo),
            new IntStatField(GamePlayerStat::getPitchHr, GamePlayerStat::setPitchHr),
            new IntStatField(GamePlayerStat::getPitchInsideParkHr, GamePlayerStat::setPitchInsideParkHr),
            new IntStatField(GamePlayerStat::getPitchSh, GamePlayerStat::setPitchSh),
            new IntStatField(GamePlayerStat::getPitchSf, GamePlayerStat::setPitchSf),
            new IntStatField(GamePlayerStat::getPitchBf, GamePlayerStat::setPitchBf),
            new IntStatField(GamePlayerStat::getPitchPa, GamePlayerStat::setPitchPa),
            new IntStatField(GamePlayerStat::getPitchAb, GamePlayerStat::setPitchAb),
            new IntStatField(GamePlayerStat::getPitchBb, GamePlayerStat::setPitchBb),
            new IntStatField(GamePlayerStat::getPitchIbb, GamePlayerStat::setPitchIbb),
            new IntStatField(GamePlayerStat::getPitchHbp, GamePlayerStat::setPitchHbp),
            new IntStatField(GamePlayerStat::getPitchIo, GamePlayerStat::setPitchIo),
            new IntStatField(GamePlayerStat::getPickoffAttempts, GamePlayerStat::setPickoffAttempts),
            new IntStatField(GamePlayerStat::getWp, GamePlayerStat::setWp),
            new IntStatField(GamePlayerStat::getBk, GamePlayerStat::setBk),
            new IntStatField(GamePlayerStat::getGo, GamePlayerStat::setGo),
            new IntStatField(GamePlayerStat::getFo, GamePlayerStat::setFo),
            new IntStatField(GamePlayerStat::getGs, GamePlayerStat::setGs),
            new IntStatField(GamePlayerStat::getSvo, GamePlayerStat::setSvo),
            new IntStatField(GamePlayerStat::getCg, GamePlayerStat::setCg),
            new IntStatField(GamePlayerStat::getPg, GamePlayerStat::setPg),
            new IntStatField(GamePlayerStat::getW, GamePlayerStat::setW),
            new IntStatField(GamePlayerStat::getL, GamePlayerStat::setL),
            new IntStatField(GamePlayerStat::getSv, GamePlayerStat::setSv),
            new IntStatField(GamePlayerStat::getHld, GamePlayerStat::setHld),
            new IntStatField(GamePlayerStat::getPo, GamePlayerStat::setPo),
            new IntStatField(GamePlayerStat::getA, GamePlayerStat::setA),
            new IntStatField(GamePlayerStat::getE, GamePlayerStat::setE),
            new IntStatField(GamePlayerStat::getTc, GamePlayerStat::setTc),
            new IntStatField(GamePlayerStat::getFieldingGs, GamePlayerStat::setFieldingGs),
            new IntStatField(GamePlayerStat::getDp, GamePlayerStat::setDp),
            new IntStatField(GamePlayerStat::getPb, GamePlayerStat::setPb),
            new IntStatField(GamePlayerStat::getCatcherSb, GamePlayerStat::setCatcherSb),
            new IntStatField(GamePlayerStat::getCatcherCs, GamePlayerStat::setCatcherCs));

    private static final List<DoubleStatField> PITCHER_TRANSFER_DOUBLE_FIELDS = List.of(
            new DoubleStatField(GamePlayerStat::getIp, GamePlayerStat::setIp),
            new DoubleStatField(GamePlayerStat::getDefInn, GamePlayerStat::setDefInn));

    private static final List<IntStatField> IDENTITY_TRANSFER_INT_FIELDS =
            buildIdentityTransferIntFields();

    private static List<IntStatField> buildIdentityTransferIntFields() {
        List<IntStatField> fields = new ArrayList<>();
        fields.add(new IntStatField(GamePlayerStat::getPa, GamePlayerStat::setPa));
        fields.add(new IntStatField(GamePlayerStat::getAb, GamePlayerStat::setAb));
        fields.add(new IntStatField(GamePlayerStat::getR, GamePlayerStat::setR));
        fields.add(new IntStatField(GamePlayerStat::getH, GamePlayerStat::setH));
        fields.add(new IntStatField(GamePlayerStat::getE, GamePlayerStat::setE));
        fields.add(new IntStatField(GamePlayerStat::getBbHp, GamePlayerStat::setBbHp));
        fields.add(new IntStatField(GamePlayerStat::getSb, GamePlayerStat::setSb));
        fields.add(new IntStatField(GamePlayerStat::getSo, GamePlayerStat::setSo));
        fields.add(new IntStatField(GamePlayerStat::getSoSwing, GamePlayerStat::setSoSwing));
        fields.add(new IntStatField(GamePlayerStat::getSoLooking, GamePlayerStat::setSoLooking));
        fields.add(new IntStatField(GamePlayerStat::getRbi, GamePlayerStat::setRbi));
        fields.add(new IntStatField(GamePlayerStat::getDoubles, GamePlayerStat::setDoubles));
        fields.add(new IntStatField(GamePlayerStat::getTriples, GamePlayerStat::setTriples));
        fields.add(new IntStatField(GamePlayerStat::getHr, GamePlayerStat::setHr));
        fields.add(new IntStatField(GamePlayerStat::getInsideParkHr,
                GamePlayerStat::setInsideParkHr));
        fields.add(new IntStatField(GamePlayerStat::getGdp, GamePlayerStat::setGdp));
        fields.add(new IntStatField(GamePlayerStat::getSh, GamePlayerStat::setSh));
        fields.add(new IntStatField(GamePlayerStat::getSf, GamePlayerStat::setSf));
        fields.add(new IntStatField(GamePlayerStat::getBb, GamePlayerStat::setBb));
        fields.add(new IntStatField(GamePlayerStat::getIbb, GamePlayerStat::setIbb));
        fields.add(new IntStatField(GamePlayerStat::getHbp, GamePlayerStat::setHbp));
        fields.add(new IntStatField(GamePlayerStat::getIo, GamePlayerStat::setIo));
        fields.add(new IntStatField(GamePlayerStat::getCs, GamePlayerStat::setCs));
        fields.addAll(PITCHER_TRANSFER_INT_FIELDS);
        return List.copyOf(fields);
    }

    /** Replaces and reconstructs all earned-run events for one completed half-inning. */
    @Transactional
    public EarnedRunReconstructionResult replaceHalfInning(
            long tenantId,
            long gameId,
            int inning,
            String half,
            long operatorId,
            List<EarnedRunPlay> plays,
            Long fieldingTeamId) {
        return replaceHalfInning(tenantId, gameId, inning, half, operatorId, plays,
                fieldingTeamId, true);
    }

    /**
     * 重建一个半局；{@code halfComplete=false}（该半局仍在进行）时只算不写。
     *
     * <p>不删旧判定、不插新判定、不作废投手 R/ER 覆写、不改统计，仅回一份与落库同款算术的
     * 预览四栏。线上实测引擎对第 k 棒的裁决只是「该半局前 k-1 棒」的函数（同前缀早晚
     * 重推逐项相同，见 EarnedRunProvisionalPushTest），所以延后入库不会改变任何判定结果；
     * 但旧行为是每个打席都跑一次「删判定＋作废受影响投手覆写＋全场重算」，等于让人工覆写
     * 在该半局还会继续被记录时随时会被无声抹掉（线上 game 261 的赛后覆写就长在这个窗口里）。
     * 副作用因此不再随每个打席发生：自动的逐打席推送只做预览，落库只发生在半局打完（第三
     * 出局）与记录员显式保存／换局／补台账这些定案动作上。后者故意保持为定案写入，因为它们
     * 是撤销／重做后唯一能把判定层重新对回逐棒证据的时机（见前端 resyncEarnedRunHalfInningForSave）。
     */
    @Transactional
    public EarnedRunReconstructionResult replaceHalfInning(
            long tenantId,
            long gameId,
            int inning,
            String half,
            long operatorId,
            List<EarnedRunPlay> plays,
            Long fieldingTeamId,
            boolean halfComplete) {
        validateIdentity(tenantId, gameId, inning, half, operatorId);
        EarnedRunReconstructionResult result = engine.reconstruct(plays);
        List<EarnedRunPlayEntity> existing = playRepository
                .findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(
                        tenantId, gameId, inning, half);
        if (!halfComplete) {
            PitcherPreview preview = previewPitcherTotals(tenantId, gameId, existing, result);
            return result.withPitcherAbsoluteTotals(
                    preview.totals(), preview.overridesToInvalidate());
        }
        Set<Long> affectedPitcherIds = collectAffectedPitcherIds(tenantId, gameId, existing);
        deleteExistingDecisions(tenantId, gameId, existing);
        playRepository.deleteByTenantIdAndGameIdAndInningAndHalf(
                tenantId, gameId, inning, half);
        persistReconstruction(tenantId, gameId, inning, half, operatorId, plays, result);
        for (EarnedRunDecision decision : result.decisions()) {
            if (decision.responsiblePitcherId() > 0) {
                affectedPitcherIds.add(decision.responsiblePitcherId());
            }
        }
        Set<Long> invalidatedPitcherIds = invalidateActiveOverrides(tenantId, gameId,
                affectedPitcherIds, "半局重建后需重新复核投手 R/ER", operatorId);
        Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> absoluteTotals =
                recomputePitcherStats(tenantId, gameId, fieldingTeamId, invalidatedPitcherIds);
        return result.withPitcherAbsoluteTotals(absoluteTotals, List.copyOf(invalidatedPitcherIds));
    }

    /** 该场是否已有 ER 引擎事件；按 gameId 全局唯一判断，避免换赛事/租户后守卫失效。 */
    public boolean hasEngineData(long gameId) {
        return gameId > 0 && playRepository.existsByGameId(gameId);
    }

    /** 该场仍以该投手为有效责任投手的判定数量；删除守卫与诊断共用。 */
    public int countResponsiblePitcherDecisions(long gameId, long pitcherId) {
        if (gameId <= 0 || pitcherId <= 0) {
            return 0;
        }
        List<EarnedRunDecisionEntity> decisions = decisionRepository.findByGameId(gameId);
        if (decisions.isEmpty()) {
            return 0;
        }
        Map<Long, String> sourceEventByPlayId =
                sourceEventByPlayId(playRepository.findByGameId(gameId));
        Map<DecisionKey, ErDecisionAdjustment> adjustments = activeAdjustmentsByKey(
                erDecisionAdjustmentRepository.findByGameIdAndStatus(
                        gameId, ADJUSTMENT_ACTIVE));
        int count = 0;
        for (EarnedRunDecisionEntity decision : decisions) {
            Long effective = effectivePitcherId(decision, sourceEventByPlayId, adjustments);
            if (Objects.equals(effective, pitcherId)) {
                count++;
            }
        }
        return count;
    }

    /** 该场是否仍有以该投手为有效责任投手的自责分判定；删除投手统计行前的守卫。 */
    public boolean hasResponsiblePitcherDecisions(long gameId, long pitcherId) {
        return countResponsiblePitcherDecisions(gameId, pitcherId) > 0;
    }

    /**
     * 该场判定总数（含被人工作废的那批），供半局重建的完整性守门使用。
     * 这里故意不用「有效判定数」：守门问的是「推上来的逐棒资料有没有涵盖记分栏已记的分」，
     * 而人工作废改的是这笔分怎么算（归谁、算不算自责），不是这笔分有没有发生。拿有效数做比对，
     * 「先作废判定、还没改记分」这个正常顺序会被当成缺笔，之后每一个半局的定案推送都会撞 409。
     * 反之记分真的被改小了，afterTotal 只会大于 officialRuns，守门本来就只拦「少」不拦「多」，
     * 不会因此放行错误数据。
     */
    public int countDecisionsForCoverage(long tenantId, long gameId) {
        return decisionRepository.findByTenantIdAndGameId(tenantId, gameId).size();
    }

    /**
     * 指定半局的既有判定数（含人工作废）。重建是整半局替换，守门必须先从全场
     * 扣掉这一半、再加回本次推送的得分跑者数，才推得出「重建后全场判定数」，
     * 再拿它去比记分栏（独立证人）才能发现分母残缺。口径跟 countDecisionsForCoverage。
     */
    public int countDecisionsForCoverageOfHalf(long tenantId, long gameId, int inning, String half) {
        Set<Long> playIds = playRepository
                .findByTenantIdAndGameIdAndInningAndHalfOrderBySequence(tenantId, gameId, inning, half)
                .stream().map(EarnedRunPlayEntity::getId).collect(Collectors.toSet());
        if (playIds.isEmpty()) {
            return 0;
        }
        return (int) decisionRepository.findByTenantIdAndGameId(tenantId, gameId).stream()
                .filter(decision -> playIds.contains(decision.getPlayId()))
                .count();
    }

    /** 赛后复核：列出本场全部投手的 R/ER 合计与生效中的人工覆写。 */
    public List<PitcherRunTotalsDTO> listPitcherTotals(long tenantId, long gameId) {
        Map<Long, GamePlayerStat> pitcherStats =
                pitcherStatsOf(loadTenantStats(tenantId, gameId));
        Map<Long, int[]> engineTotals = enginePitcherTotals(tenantId, gameId);
        Map<Long, PitcherRunOverride> overrides = activeOverridesByPlayer(tenantId, gameId);
        Set<Long> playerIds = new LinkedHashSet<>();
        playerIds.addAll(pitcherStats.keySet());
        playerIds.addAll(engineTotals.keySet());
        playerIds.addAll(overrides.keySet());
        Map<Long, Player> players = loadPlayerMap(playerIds);
        return playerIds.stream()
                .map(playerId -> toPitcherTotals(playerId, pitcherStats.get(playerId),
                        engineTotals.get(playerId), overrides.get(playerId), players.get(playerId)))
                .sorted(Comparator
                        .comparing(PitcherRunTotalsDTO::teamId,
                                Comparator.nullsLast(Long::compareTo))
                        .thenComparing(PitcherRunTotalsDTO::playerId))
                .toList();
    }

    /**
     * 赛后复核：为指定投手保存 R/ER 人工覆写，并按该场全量重算绝对覆盖。
     * 同一投手已有生效覆写时就地更新；撤销/失效后重新覆写新建审计行。
     */
    @Transactional
    public PitcherRunTotalsDTO overridePitcherRuns(long tenantId, long gameId, long playerId,
            Integer runsAllowed, Integer earnedRuns, String note, long operatorId) {
        validatePitcherOverrideInput(tenantId, gameId, playerId, runsAllowed, earnedRuns, note, operatorId);
        ensurePitcherInGame(tenantId, gameId, playerId);
        upsertApprovedOverride(tenantId, gameId, playerId, runsAllowed, earnedRuns, note, operatorId);
        recomputePitcherStats(tenantId, gameId, null);
        return findPitcherTotals(tenantId, gameId, playerId);
    }

    /**
     * 结果录入页保存的人工覆写：只更新所选投手的统计，不按引擎全量重算。
     * 结果页的统计以本次提交为准；引擎只覆盖部分半局时，不能拿不完整判定改写其他投手。
     */
    @Transactional
    public void applyResultPitcherRunOverrides(long tenantId, long gameId,
            Map<Long, int[]> values, long operatorId) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<GamePlayerStat> gameStats = loadTenantStats(tenantId, gameId);
        Map<Long, GamePlayerStat> pitcherStats = pitcherStatsOf(gameStats);
        Map<Long, Long> teamByPlayer = teamByPlayerOf(gameStats);
        for (Map.Entry<Long, int[]> entry : values.entrySet()) {
            long playerId = entry.getKey();
            int runs = entry.getValue()[0];
            int earned = entry.getValue()[1];
            validatePitcherOverrideInput(tenantId, gameId, playerId, runs, earned, null, operatorId);
            ensurePitcherInGame(tenantId, gameId, playerId);
            upsertApprovedOverride(tenantId, gameId, playerId, runs, earned, null, operatorId);
            GamePlayerStat stat = ensurePitcherStat(tenantId, gameId, playerId,
                    pitcherStats, teamByPlayer, null);
            // 结果页契约是“不拿部分半局的残缺判定改写任何投手”，所以不查引擎；
            // 但统计行已有的待判定不能被人手工 R/ER 无声折进非自责，故拿它当 PEND 上限。
            applyPitcherTotals(stat, splitRuns(runs, earned, valueOrZero(stat.getPendingR())));
            gamePlayerStatRepository.save(stat);
        }
    }

    private PitcherRunOverride upsertApprovedOverride(long tenantId, long gameId, long playerId,
            Integer runsAllowed, Integer earnedRuns, String note, long operatorId) {
        PitcherRunOverride row = this.pitcherRunOverrideRepository
                .findFirstByTenantIdAndGameIdAndPlayerIdAndStatusOrderByIdDesc(
                        tenantId, gameId, playerId, OVERRIDE_APPROVED)
                .orElseGet(PitcherRunOverride::new);
        if (row.getId() == null) {
            row.setTenantId(tenantId);
            row.setGameId(gameId);
            row.setPlayerId(playerId);
            row.setCreatedBy(operatorId);
        }
        row.setRunsAllowed(runsAllowed);
        row.setEarnedRuns(earnedRuns);
        row.setNote(normalizeNote(note));
        row.setStatus(OVERRIDE_APPROVED);
        row.setUpdatedBy(operatorId);
        return this.pitcherRunOverrideRepository.save(row);
    }

    /** 撤销人工覆写：恢复引擎绝对值（无判定的投手归零）。 */
    @Transactional
    public PitcherRunTotalsDTO revokePitcherOverride(long tenantId, long gameId,
            long overrideId, long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || overrideId <= 0 || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, override and operator must be positive");
        }
        PitcherRunOverride row = this.pitcherRunOverrideRepository
                .findByIdAndTenantIdAndGameId(overrideId, tenantId, gameId)
                .orElseThrow(() -> new IllegalArgumentException("投手 R/ER 覆写不存在"));
        if (!OVERRIDE_APPROVED.equals(row.getStatus())) {
            throw new IllegalArgumentException("该覆写已撤销或失效");
        }
        row.setStatus(OVERRIDE_REVOKED);
        row.setRevokedBy(operatorId);
        row.setRevokedAt(LocalDateTime.now());
        row.setUpdatedBy(operatorId);
        this.pitcherRunOverrideRepository.save(row);
        recomputePitcherStats(tenantId, gameId, null, Set.of(row.getPlayerId()));
        return findPitcherTotals(tenantId, gameId, row.getPlayerId());
    }

    /**
     * 投手行转记：目标已有值优先，源行可转记字段清零；转记和删除源行是两个独立动作。
     * R/ER/UER/PENDING 仍归 ER 引擎，不参与转记；源行有失分/判定时要求先处理 ER。
     */
    @Transactional
    public PitcherTransferResultDTO transferPitcher(long tenantId, long gameId,
            Long sourceStatId, Long targetPlayerId, String note, Long operatorId) {
        validateTransferIds(tenantId, gameId, sourceStatId, targetPlayerId, operatorId);
        GamePlayerStat source = this.gamePlayerStatRepository.findByIdForUpdate(sourceStatId)
                .orElseThrow(() -> new IllegalArgumentException("原投手统计行不存在"));
        if (!Objects.equals(source.getGameId(), gameId)
                || !Objects.equals(source.getTenantId(), tenantId)
                || !Integer.valueOf(1).equals(source.getIsPitcher())
                || source.getPlayerId() == null) {
            throw new IllegalArgumentException("原统计行不是本场投手行");
        }
        GamePlayerStat target = lockTransferTarget(tenantId, gameId, source, targetPlayerId);
        assertNoEarnedRunData(gameId, source);
        assertNoApprovedFieldingAdjustment(tenantId, gameId, source.getPlayerId(),
                "原投手仍有守备补记，请先撤销补记后再转记");
        List<GameFieldingPosStat> fieldingRows = this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId);
        List<GameFieldingPosStat> sourceFielding = fieldingRowsOf(
                fieldingRows, source.getTeamId(), source.getPlayerId());
        List<GameFieldingPosStat> targetFielding = fieldingRowsOf(
                fieldingRows, target.getTeamId(), target.getPlayerId());
        if (!hasTransferableStats(source) && sourceFielding.isEmpty()) {
            throw new IllegalArgumentException("原投手行没有可转记的数据");
        }
        String sourceBefore = snapshot(source, sourceFielding);
        String targetBefore = snapshot(target, targetFielding);

        mergeTransferableStats(source, target);
        mergeFieldingPositions(gameId, target.getTeamId(), target.getPlayerId(),
                sourceFielding, operatorId);
        if (!GamePlayerStatZeros.isZero(source)) {
            throw new IllegalArgumentException("原投手行仍有不可转记的数据，请先在对应页签清零后再转记");
        }
        syncPitchPaBf(target);
        source.setUpdatedBy(operatorId);
        target.setUpdatedBy(operatorId);
        this.gamePlayerStatRepository.save(source);
        this.gamePlayerStatRepository.save(target);

        List<GameFieldingPosStat> targetFieldingAfter = fieldingRowsOf(
                this.gameFieldingPosStatRepository
                        .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId),
                target.getTeamId(), target.getPlayerId());
        GamePitcherTransfer transfer = new GamePitcherTransfer();
        transfer.setTenantId(tenantId);
        transfer.setGameId(gameId);
        transfer.setSourceStatId(source.getId());
        transfer.setSourceTeamId(source.getTeamId());
        transfer.setSourcePlayerId(source.getPlayerId());
        transfer.setTargetStatId(target.getId());
        transfer.setTargetTeamId(target.getTeamId());
        transfer.setTargetPlayerId(target.getPlayerId());
        transfer.setSourceBefore(sourceBefore);
        transfer.setTargetBefore(targetBefore);
        transfer.setTargetAfter(snapshot(target, targetFieldingAfter));
        transfer.setStatus(TRANSFER_STATUS_TRANSFERRED);
        transfer.setNote(normalizeNote(note));
        transfer.setCreatedBy(operatorId);
        transfer.setUpdatedBy(operatorId);
        this.gamePitcherTransferRepository.save(transfer);
        return new PitcherTransferResultDTO(
                transfer.getId(), source.getId(), source.getPlayerId(),
                target.getId(), target.getPlayerId(), transfer.getStatus());
    }

    /** 转记成功后单独删除源行；源行必须已全零且无自责分判定。 */
    @Transactional
    public PitcherTransferResultDTO deletePitcherTransferSource(long tenantId, long gameId,
            Long transferId, Long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || transferId == null || transferId <= 0
                || operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, transfer and operator must be positive");
        }
        GamePitcherTransfer transfer = this.gamePitcherTransferRepository
                .findByIdAndTenantIdAndGameId(transferId, tenantId, gameId)
                .orElseThrow(() -> new IllegalArgumentException("转记记录不存在"));
        if (!TRANSFER_STATUS_TRANSFERRED.equals(transfer.getStatus())) {
            throw new IllegalArgumentException("该转记已删除源行或不可删除");
        }
        GamePlayerStat source = this.gamePlayerStatRepository.findByIdForUpdate(transfer.getSourceStatId())
                .orElse(null);
        if (source == null) {
            markSourceDeleted(transfer, operatorId);
            return transferResult(transfer);
        }
        if (!GamePlayerStatZeros.isZero(source)) {
            throw new IllegalArgumentException("原投手行仍有数据，请先清零或转记");
        }
        if (source.getPlayerId() != null
                && hasResponsiblePitcherDecisions(gameId, source.getPlayerId())) {
            throw new IllegalArgumentException("原投手仍有自责分判定，请先处理判定");
        }
        assertNoApprovedFieldingAdjustment(tenantId, gameId, source.getPlayerId(),
                "原投手仍有守备补记，请先撤销补记后再删除");
        boolean hasFieldingRows = !fieldingRowsOf(this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId),
                source.getTeamId(), source.getPlayerId()).isEmpty();
        if (hasFieldingRows) {
            throw new IllegalArgumentException("原投手仍有守备位置数据，请先在守备页签处理");
        }
        this.gamePlayerStatRepository.delete(source);
        markSourceDeleted(transfer, operatorId);
        return transferResult(transfer);
    }

    private static PitcherTransferResultDTO transferResult(GamePitcherTransfer transfer) {
        return new PitcherTransferResultDTO(
                transfer.getId(),
                transfer.getSourceStatId(),
                transfer.getSourcePlayerId(),
                transfer.getTargetStatId(),
                transfer.getTargetPlayerId(),
                transfer.getStatus());
    }

    /** 整名球员身份转记预览。 */
    @Transactional(readOnly = true)
    public IdentityTransferPreviewDTO previewIdentityTransfer(
            long tenantId, long gameId, long sourcePlayerId, long targetPlayerId) {
        validateIdentityTransferIds(tenantId, gameId, sourcePlayerId, targetPlayerId);
        Game game = loadIdentityGame(tenantId, gameId);
        List<GamePlayerStat> sourceStats = playerStatsOf(tenantId, gameId, sourcePlayerId);
        List<GamePlayerStat> targetStats = playerStatsOf(tenantId, gameId, targetPlayerId);
        List<GameFieldingPosStat> sourceFielding = playerFieldingOf(gameId, sourcePlayerId);
        List<GameFieldingPosStat> targetFielding = playerFieldingOf(gameId, targetPlayerId);
        Long sourceTeamId = firstTeamId(sourceStats, sourceFielding);
        Long targetTeamId = firstTeamId(targetStats, targetFielding);
        if (sourceTeamId == null) {
            sourceTeamId = this.gameIdentityTransferRepository
                    .findFirstByTenantIdAndGameIdAndSourcePlayerIdAndTargetPlayerIdAndStatusOrderByIdDesc(
                            tenantId, gameId, sourcePlayerId, targetPlayerId,
                            TRANSFER_STATUS_TRANSFERRED)
                    .map(GameIdentityTransfer::getSourceTeamId)
                    .orElse(null);
        }
        Map<Long, Player> players = loadPlayerMap(
                Set.of(sourcePlayerId, targetPlayerId));
        List<String> blockers = new ArrayList<>();
        if (sourcePlayerId == targetPlayerId) {
            blockers.add("源球员与目标球员不能相同");
        }
        if (sourceTeamId == null) {
            blockers.add("源球员没有本场统计或守备数据");
        }
        if (targetTeamId != null && sourceTeamId != null
                && !Objects.equals(sourceTeamId, targetTeamId)) {
            blockers.add("目标球员不在同一球队");
        }
        if (targetTeamId == null && sourceTeamId != null) {
            Player targetPlayer = players.get(targetPlayerId);
            if (targetPlayer == null
                    || !Objects.equals(targetPlayer.getTeamId(), sourceTeamId)) {
                blockers.add("目标球员不在源球队名单中");
            }
        }
        Map<Long, PitcherRunOverride> overrides =
                activeOverridesByPlayer(tenantId, gameId);
        boolean sourceOverride = overrides.containsKey(sourcePlayerId);
        boolean targetOverride = overrides.containsKey(targetPlayerId);
        if (sourceOverride && targetOverride) {
            blockers.add("源/目标球员都有 R/ER 人工覆写，请先撤销一端");
        }
        List<IdentityTransferPreviewDTO.RowPreview> rows = new ArrayList<>();
        for (GamePlayerStat source : sourceStats) {
            GamePlayerStat target = findStatByType(targetStats, source.getIsPitcher());
            rows.add(new IdentityTransferPreviewDTO.RowPreview(
                    source.getIsPitcher(), source.getId(),
                    target != null ? target.getId() : null, target != null));
        }
        List<String> positions = sourceFielding.stream()
                .map(GameFieldingPosStat::getPosition)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        String liveJson = liveSnapshotOf(game);
        boolean liveSnapshotAvailable = liveJson != null && !liveJson.isBlank();
        int liveReferenceCount = liveSnapshotAvailable
                ? countLiveReferences(liveJson, sourcePlayerId, blockers) : 0;
        boolean liveRosterConflict = liveSnapshotAvailable
                && LiveSnapshotIdentityTransfer.hasRosterConflict(
                        liveJson, sourcePlayerId, targetPlayerId, objectMapper);
        List<GameFieldingAdjustment> fieldingAdjustments =
                approvedFieldingAdjustments(tenantId, gameId, sourcePlayerId);
        if (hasFieldingAdjustmentConflict(
                tenantId, gameId, targetPlayerId, fieldingAdjustments)) {
            blockers.add("目标球员在同一失误 Play 已有守备补记");
        }
        int decisionCount = countResponsiblePitcherDecisions(gameId, sourcePlayerId);
        if (sourceFielding.isEmpty() && decisionCount == 0
                && fieldingAdjustments.isEmpty() && liveReferenceCount == 0
                && sourceStats.stream().allMatch(GamePlayerStatZeros::isZero)) {
            blockers.add("源球员没有可转记的数据");
        }
        return new IdentityTransferPreviewDTO(
                sourcePlayerId, playerName(players, sourcePlayerId),
                targetPlayerId, playerName(players, targetPlayerId),
                sourceTeamId != null ? sourceTeamId : targetTeamId,
                blockers.isEmpty(), blockers, rows, positions,
                decisionCount, sourceOverride, targetOverride,
                liveSnapshotAvailable, liveReferenceCount, fieldingAdjustments.size(),
                liveRosterConflict);
    }

    /** 整名球员身份转记：统计、逐位置守备、ER 判定、R/ER 覆写与现场记录。 */
    @Transactional
    public IdentityTransferResultDTO transferIdentity(
            long tenantId, long gameId, long sourcePlayerId, long targetPlayerId,
            List<String> scopes, String note, long operatorId) {
        validateIdentityTransferIds(tenantId, gameId, sourcePlayerId, targetPlayerId);
        if (operatorId <= 0) {
            throw new IllegalArgumentException("操作人必须为正数");
        }
        Game game = loadIdentityGame(tenantId, gameId);
        IdentityTransferPreviewDTO preview = previewIdentityTransfer(
                tenantId, gameId, sourcePlayerId, targetPlayerId);
        if (!preview.transferable()) {
            throw new IllegalArgumentException(String.join("；", preview.blockers()));
        }
        Set<String> scopeSet = normalizeIdentityScopes(scopes);
        String liveSnapshot = liveSnapshotOf(game);
        String liveSnapshotAudit = null;
        if (scopeSet.contains(SCOPE_LIVE_RECORDS)) {
            if (liveSnapshot == null || liveSnapshot.isBlank()) {
                throw new IllegalArgumentException("该场没有现场记录可转记");
            }
            if (LiveSnapshotIdentityTransfer.hasRosterConflict(
                    liveSnapshot, sourcePlayerId, targetPlayerId, objectMapper)) {
                throw new IllegalArgumentException(
                        "源/目标球员同时在同一阵容或替补名单，无法转记现场记录；可取消现场记录范围");
            }
            liveSnapshotAudit = LiveSnapshotIdentityTransfer.auditSnapshot(
                    liveSnapshot, objectMapper);
        }
        String sourceBefore = identitySnapshot(tenantId, gameId, sourcePlayerId);
        if (liveSnapshotAudit != null) {
            sourceBefore = mergeSnapshotField(sourceBefore, "liveSnapshot", liveSnapshotAudit);
        }
        String targetBefore = identitySnapshot(tenantId, gameId, targetPlayerId);

        Long teamId = preview.teamId();
        if (scopeSet.contains(SCOPE_STATS)) {
            moveIdentityStats(tenantId, gameId, sourcePlayerId, targetPlayerId, operatorId);
        }
        if (scopeSet.contains(SCOPE_FIELDING)) {
            List<GameFieldingPosStat> sourceFielding =
                    playerFieldingOf(gameId, sourcePlayerId);
            mergeFieldingPositions(gameId, teamId, targetPlayerId, sourceFielding, operatorId);
            moveIdentityFieldingAdjustments(
                    tenantId, gameId, sourcePlayerId, targetPlayerId, operatorId);
        }

        boolean recompute = false;
        if (scopeSet.contains(SCOPE_ER_DECISIONS)) {
            recompute = moveIdentityOverride(
                    tenantId, gameId, sourcePlayerId, targetPlayerId, operatorId);
            moveIdentityDecisions(tenantId, gameId, sourcePlayerId, targetPlayerId, operatorId);
            recompute = true;
        }
        if (scopeSet.contains(SCOPE_LIVE_RECORDS)) {
            LiveSnapshotIdentityTransfer.Result rewritten =
                    LiveSnapshotIdentityTransfer.rewrite(
                            liveSnapshot, sourcePlayerId, targetPlayerId, objectMapper);
            if (rewritten.changed()) {
                this.gameSnapshotRepository.updateSnapshotJson(gameId, rewritten.json(), operatorId);
                game.setUpdatedBy(operatorId);
                gameRepository.save(game);
            }
        }
        if (recompute) {
            recomputePitcherStats(tenantId, gameId, null, Set.of(sourcePlayerId, targetPlayerId));
        }

        String targetAfter = identitySnapshot(tenantId, gameId, targetPlayerId);
        GameIdentityTransfer transfer = new GameIdentityTransfer();
        transfer.setTenantId(tenantId);
        transfer.setGameId(gameId);
        transfer.setSourcePlayerId(sourcePlayerId);
        transfer.setSourceTeamId(teamId);
        transfer.setTargetPlayerId(targetPlayerId);
        transfer.setTargetTeamId(teamId);
        transfer.setScopes(String.join(",", scopeSet));
        transfer.setSourceBefore(sourceBefore);
        transfer.setTargetBefore(targetBefore);
        transfer.setTargetAfter(targetAfter);
        transfer.setStatus(TRANSFER_STATUS_TRANSFERRED);
        transfer.setNote(normalizeNote(note));
        transfer.setCreatedBy(operatorId);
        transfer.setUpdatedBy(operatorId);
        this.gameIdentityTransferRepository.save(transfer);
        return new IdentityTransferResultDTO(
                transfer.getId(), sourcePlayerId, targetPlayerId, transfer.getStatus());
    }

    /** 转记成功后单独删除源球员数据；必须全零且无守备/判定/覆写/现场引用。 */
    @Transactional
    public IdentityTransferResultDTO deleteIdentityTransferSource(long tenantId, long gameId,
            long transferId, long operatorId) {
        GameIdentityTransfer transfer = gameIdentityTransferRepository
                .findByIdAndTenantIdAndGameId(transferId, tenantId, gameId)
                .orElseThrow(() -> new IllegalArgumentException("身份转记记录不存在"));
        if (!TRANSFER_STATUS_TRANSFERRED.equals(transfer.getStatus())) {
            throw new IllegalArgumentException("该转记已删除源数据或不可删除");
        }
        Long sourcePlayerId = transfer.getSourcePlayerId();
        if (sourcePlayerId == null || sourcePlayerId <= 0) {
            throw new IllegalArgumentException("转记记录缺少源球员");
        }
        List<GamePlayerStat> sourceStats = playerStatsOf(tenantId, gameId, sourcePlayerId);
        for (GamePlayerStat stat : sourceStats) {
            if (!GamePlayerStatZeros.isZero(stat)) {
                throw new IllegalArgumentException("源球员仍有统计/失分数据，请先清零");
            }
        }
        if (!playerFieldingOf(gameId, sourcePlayerId).isEmpty()) {
            throw new IllegalArgumentException("源球员仍有守备位置数据，请先在守备页签处理");
        }
        if (countResponsiblePitcherDecisions(gameId, sourcePlayerId) > 0) {
            throw new IllegalArgumentException("源球员仍有有效 ER 判定，请先处理判定");
        }
        if (activeOverridesByPlayer(tenantId, gameId).containsKey(sourcePlayerId)) {
            throw new IllegalArgumentException("源球员仍有 R/ER 覆写，请先撤销");
        }
        if (this.gameFieldingAdjustmentRepository
                .existsByTenantIdAndGameIdAndPlayerIdAndStatus(
                        tenantId, gameId, sourcePlayerId, OVERRIDE_APPROVED)) {
            throw new IllegalArgumentException("源球员仍有守备补记，请先在守备补记 A 撤销或转记");
        }
        Game game = loadIdentityGame(tenantId, gameId);
        String liveSnapshot = liveSnapshotOf(game);
        if (liveSnapshot != null && !liveSnapshot.isBlank()
                && LiveSnapshotIdentityTransfer.countReferences(
                        liveSnapshot, sourcePlayerId, objectMapper) > 0) {
            throw new IllegalArgumentException("现场记录仍引用源球员，请先按现场记录范围转记");
        }
        for (GamePlayerStat stat : sourceStats) {
            gamePlayerStatRepository.delete(stat);
        }
        transfer.setStatus(TRANSFER_STATUS_DELETED);
        transfer.setSourceDeletedBy(operatorId);
        transfer.setSourceDeletedAt(LocalDateTime.now());
        transfer.setUpdatedBy(operatorId);
        gameIdentityTransferRepository.save(transfer);
        return new IdentityTransferResultDTO(
                transfer.getId(), transfer.getSourcePlayerId(),
                transfer.getTargetPlayerId(), transfer.getStatus());
    }

    private void validateIdentityTransferIds(
            long tenantId, long gameId, long sourcePlayerId, long targetPlayerId) {
        if (tenantId <= 0 || gameId <= 0 || sourcePlayerId <= 0 || targetPlayerId <= 0) {
            throw new IllegalArgumentException("Tenant, game, source and target must be positive");
        }
    }

    private Set<String> normalizeIdentityScopes(List<String> scopes) {
        if (scopes == null) {
            return DEFAULT_IDENTITY_SCOPES;
        }
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个转记范围");
        }
        Set<String> result = new LinkedHashSet<>();
        for (String scope : scopes) {
            if (scope == null || scope.isBlank()) {
                continue;
            }
            String normalized = scope.trim().toUpperCase();
            if (!ALL_IDENTITY_SCOPES.contains(normalized)) {
                throw new IllegalArgumentException("不支持的转记范围：" + scope);
            }
            result.add(normalized);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个转记范围");
        }
        return result;
    }

    private Game loadIdentityGame(long tenantId, long gameId) {
        Game game = this.gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("比赛不存在"));
        if (!Objects.equals(game.getTenantId(), tenantId)) {
            throw new IllegalArgumentException("比赛不属于当前租户");
        }
        return game;
    }

    private int countLiveReferences(String liveJson, long sourcePlayerId,
            List<String> blockers) {
        try {
            return LiveSnapshotIdentityTransfer.countReferences(
                    liveJson, sourcePlayerId, objectMapper);
        } catch (IllegalArgumentException ex) {
            blockers.add("现场记录格式异常，无法转记：" + ex.getMessage());
            return 0;
        }
    }

    private boolean hasFieldingAdjustmentConflict(long tenantId, long gameId,
            long targetPlayerId, List<GameFieldingAdjustment> adjustments) {
        return adjustments.stream()
                .anyMatch(row -> this.gameFieldingAdjustmentRepository
                        .findFirstByTenantIdAndGameIdAndSourceEventIdAndPlayerIdOrderByIdAsc(
                                tenantId, gameId, row.getSourceEventId(), targetPlayerId)
                        .isPresent());
    }

    private List<GameFieldingAdjustment> approvedFieldingAdjustments(
            long tenantId, long gameId, long playerId) {
        return this.gameFieldingAdjustmentRepository
                .findByTenantIdAndGameIdAndStatus(tenantId, gameId, OVERRIDE_APPROVED).stream()
                .filter(row -> Objects.equals(row.getPlayerId(), playerId))
                .toList();
    }

    private void moveIdentityFieldingAdjustments(long tenantId, long gameId,
            long sourcePlayerId, long targetPlayerId, long operatorId) {
        for (GameFieldingAdjustment row
                : approvedFieldingAdjustments(tenantId, gameId, sourcePlayerId)) {
            row.setPlayerId(targetPlayerId);
            row.setUpdatedBy(operatorId);
            this.gameFieldingAdjustmentRepository.save(row);
        }
    }

    private String mergeSnapshotField(String json, String field, String value) {
        try {
            ObjectNode node = (ObjectNode) this.auditMapper.readTree(json);
            JsonNode valueNode = this.auditMapper.readTree(value);
            node.set(field, valueNode);
            return this.auditMapper.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("身份转记审计快照合并失败", ex);
        }
    }

    private List<GamePlayerStat> playerStatsOf(long tenantId, long gameId, long playerId) {
        return loadTenantStats(tenantId, gameId).stream()
                .filter(stat -> Objects.equals(stat.getPlayerId(), playerId))
                .toList();
    }

    private List<GameFieldingPosStat> playerFieldingOf(long gameId, long playerId) {
        return this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId).stream()
                .filter(row -> Objects.equals(row.getPlayerId(), playerId))
                .toList();
    }

    private static Long firstTeamId(
            List<GamePlayerStat> stats, List<GameFieldingPosStat> fielding) {
        for (GamePlayerStat stat : stats) {
            if (stat.getTeamId() != null) {
                return stat.getTeamId();
            }
        }
        for (GameFieldingPosStat row : fielding) {
            if (row.getTeamId() != null) {
                return row.getTeamId();
            }
        }
        return null;
    }

    private static GamePlayerStat findStatByType(
            List<GamePlayerStat> stats, Integer isPitcher) {
        return stats.stream()
                .filter(stat -> Objects.equals(stat.getIsPitcher(), isPitcher))
                .findFirst()
                .orElse(null);
    }

    private void moveIdentityStats(long tenantId, long gameId,
            long sourcePlayerId, long targetPlayerId, long operatorId) {
        List<GamePlayerStat> sourceStats = playerStatsOf(tenantId, gameId, sourcePlayerId);
        List<GamePlayerStat> targetStats = playerStatsOf(tenantId, gameId, targetPlayerId);
        for (GamePlayerStat source : sourceStats) {
            GamePlayerStat target = findStatByType(targetStats, source.getIsPitcher());
            if (target == null) {
                source.setPlayerId(targetPlayerId);
                source.setUpdatedBy(operatorId);
                gamePlayerStatRepository.save(source);
                continue;
            }
            mergeIdentityStats(source, target);
            source.setUpdatedBy(operatorId);
            target.setUpdatedBy(operatorId);
            gamePlayerStatRepository.save(source);
            gamePlayerStatRepository.save(target);
        }
    }

    private boolean moveIdentityOverride(long tenantId, long gameId,
            long sourcePlayerId, long targetPlayerId, long operatorId) {
        Map<Long, PitcherRunOverride> overrides =
                activeOverridesByPlayer(tenantId, gameId);
        PitcherRunOverride sourceOverride = overrides.get(sourcePlayerId);
        if (sourceOverride == null || overrides.get(targetPlayerId) != null) {
            return false;
        }
        sourceOverride.setPlayerId(targetPlayerId);
        sourceOverride.setUpdatedBy(operatorId);
        pitcherRunOverrideRepository.save(sourceOverride);
        return true;
    }

    private void mergeIdentityStats(GamePlayerStat source, GamePlayerStat target) {
        for (IntStatField field : IDENTITY_TRANSFER_INT_FIELDS) {
            Integer value = field.getter().apply(source);
            if (value == null || value == 0) {
                continue;
            }
            Integer current = field.getter().apply(target);
            if (current == null || current == 0) {
                field.setter().accept(target, value);
            }
            field.setter().accept(source, 0);
        }
        for (DoubleStatField field : PITCHER_TRANSFER_DOUBLE_FIELDS) {
            Double value = field.getter().apply(source);
            if (value == null || value == 0d) {
                continue;
            }
            Double current = field.getter().apply(target);
            if (current == null || current == 0d) {
                field.setter().accept(target, value);
            }
            field.setter().accept(source, 0d);
        }
    }

    private void moveIdentityDecisions(long tenantId, long gameId,
            long sourcePlayerId, long targetPlayerId, long operatorId) {
        Map<Long, String> sourceEventByPlayId = sourceEventByPlayId(tenantId, gameId);
        Map<DecisionKey, ErDecisionAdjustment> adjustments =
                activeAdjustmentsByKey(activeAdjustments(tenantId, gameId));
        for (EarnedRunDecisionEntity decision : decisionRepository
                .findByTenantIdAndGameId(tenantId, gameId)) {
            Long effective = effectivePitcherId(decision, sourceEventByPlayId, adjustments);
            if (!Objects.equals(effective, sourcePlayerId)) {
                continue;
            }
            String sourceEventId = sourceEventByPlayId.get(decision.getPlayId());
            if (sourceEventId == null || sourceEventId.isBlank()) {
                continue;
            }
            for (ErDecisionAdjustment current : erDecisionAdjustmentRepository
                    .findByTenantIdAndGameIdAndSourceEventIdAndRunnerIdAndStatus(
                            tenantId, gameId, sourceEventId, decision.getRunnerId(),
                            ADJUSTMENT_ACTIVE)) {
                current.setStatus(ADJUSTMENT_SUPERSEDED);
                current.setUpdatedBy(operatorId);
                erDecisionAdjustmentRepository.saveAndFlush(current);
            }
            ErDecisionAdjustment row = new ErDecisionAdjustment();
            row.setTenantId(tenantId);
            row.setGameId(gameId);
            row.setDecisionId(decision.getId());
            row.setSourceEventId(sourceEventId);
            row.setRunnerId(decision.getRunnerId());
            row.setAction(ADJUSTMENT_REASSIGN);
            row.setTargetPitcherId(targetPlayerId);
            row.setStatus(ADJUSTMENT_ACTIVE);
            row.setReason("身份转记");
            row.setCreatedBy(operatorId);
            row.setUpdatedBy(operatorId);
            erDecisionAdjustmentRepository.save(row);
        }
    }

    private String identitySnapshot(long tenantId, long gameId, long playerId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("stats", playerStatsOf(tenantId, gameId, playerId));
        snapshot.put("fieldingPositions", playerFieldingOf(gameId, playerId));
        snapshot.put("fieldingAdjustments", approvedFieldingAdjustments(tenantId, gameId, playerId));
        try {
            return this.auditMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("身份转记审计快照序列化失败", ex);
        }
    }

    private static String playerName(Map<Long, Player> players, Long playerId) {
        Player player = players.get(playerId);
        return player != null && player.getName() != null
                ? player.getName() : "#" + playerId;
    }

    private void validateTransferIds(long tenantId, long gameId, Long sourceStatId,
            Long targetPlayerId, Long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || sourceStatId == null || sourceStatId <= 0
                || targetPlayerId == null || targetPlayerId <= 0
                || operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, source, target and operator must be positive");
        }
    }

    private GamePlayerStat lockTransferTarget(long tenantId, long gameId,
            GamePlayerStat source, Long targetPlayerId) {
        GamePlayerStat candidate = loadTenantStats(tenantId, gameId).stream()
                .filter(stat -> Integer.valueOf(1).equals(stat.getIsPitcher()))
                .filter(stat -> Objects.equals(stat.getTeamId(), source.getTeamId()))
                .filter(stat -> Objects.equals(stat.getPlayerId(), targetPlayerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("目标投手行不存在，请先加入该投手"));
        GamePlayerStat target = this.gamePlayerStatRepository
                .findByIdForUpdate(candidate.getId()).orElse(candidate);
        if (Objects.equals(source.getId(), target.getId())) {
            throw new IllegalArgumentException("原投手与目标投手不能相同");
        }
        return target;
    }

    private boolean hasTransferableStats(GamePlayerStat source) {
        return PITCHER_TRANSFER_INT_FIELDS.stream()
                .anyMatch(field -> valueOrZero(field.getter().apply(source)) != 0)
                || PITCHER_TRANSFER_DOUBLE_FIELDS.stream()
                        .anyMatch(field -> {
                            Double value = field.getter().apply(source);
                            return value != null && value != 0d;
                        });
    }

    private void assertNoApprovedFieldingAdjustment(long tenantId, long gameId,
            Long playerId, String message) {
        if (playerId != null && this.gameFieldingAdjustmentRepository
                .existsByTenantIdAndGameIdAndPlayerIdAndStatus(
                        tenantId, gameId, playerId, "APPROVED")) {
            throw new IllegalArgumentException(message);
        }
    }

    private void assertNoEarnedRunData(long gameId, GamePlayerStat source) {
        int runData = valueOrZero(source.getPitchR()) + valueOrZero(source.getEr())
                + valueOrZero(source.getUnearnedR()) + valueOrZero(source.getPendingR())
                + valueOrZero(source.getEarnedR());
        if (runData != 0) {
            throw new IllegalArgumentException("原投手仍有失分/自责分，请先在 ER 复核处理后再转记");
        }
        if (source.getPlayerId() != null
                && hasResponsiblePitcherDecisions(gameId, source.getPlayerId())) {
            throw new IllegalArgumentException("原投手仍有自责分判定，请先在 ER 复核改派/作废");
        }
    }

    private void mergeTransferableStats(GamePlayerStat source, GamePlayerStat target) {
        for (IntStatField field : PITCHER_TRANSFER_INT_FIELDS) {
            Integer sourceValue = field.getter().apply(source);
            if (sourceValue == null || sourceValue == 0) {
                continue;
            }
            Integer targetValue = field.getter().apply(target);
            if (targetValue == null || targetValue == 0) {
                field.setter().accept(target, sourceValue);
            }
            field.setter().accept(source, 0);
        }
        for (DoubleStatField field : PITCHER_TRANSFER_DOUBLE_FIELDS) {
            Double sourceValue = field.getter().apply(source);
            if (sourceValue == null || sourceValue == 0d) {
                continue;
            }
            Double targetValue = field.getter().apply(target);
            if (targetValue == null || targetValue == 0d) {
                field.setter().accept(target, sourceValue);
            }
            field.setter().accept(source, 0d);
        }
    }

    private void mergeFieldingPositions(long gameId, Long targetTeamId, Long targetPlayerId,
            List<GameFieldingPosStat> sourceFielding, Long operatorId) {
        Map<String, GameFieldingPosStat> targetByPosition = new LinkedHashMap<>();
        for (GameFieldingPosStat row : fieldingRowsOf(this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(gameId),
                targetTeamId, targetPlayerId)) {
            if (row.getPosition() != null) {
                targetByPosition.put(row.getPosition().trim().toUpperCase(), row);
            }
        }
        for (GameFieldingPosStat sourceRow : sourceFielding) {
            String position = sourceRow.getPosition() == null
                    ? "" : sourceRow.getPosition().trim().toUpperCase();
            GameFieldingPosStat targetRow = targetByPosition.get(position);
            if (targetRow == null) {
                sourceRow.setPlayerId(targetPlayerId);
                sourceRow.setUpdatedBy(operatorId);
                this.gameFieldingPosStatRepository.save(sourceRow);
                continue;
            }
            mergeFieldingRow(targetRow, sourceRow);
            targetRow.setUpdatedBy(operatorId);
            this.gameFieldingPosStatRepository.save(targetRow);
            this.gameFieldingPosStatRepository.delete(sourceRow);
        }
    }

    private void mergeFieldingRow(GameFieldingPosStat target, GameFieldingPosStat source) {
        target.setOuts(mergeInt(target.getOuts(), source.getOuts()));
        target.setPo(mergeInt(target.getPo(), source.getPo()));
        target.setA(mergeInt(target.getA(), source.getA()));
        target.setE(mergeInt(target.getE(), source.getE()));
        target.setDp(mergeInt(target.getDp(), source.getDp()));
        target.setPb(mergeInt(target.getPb(), source.getPb()));
        target.setCatcherCs(mergeInt(target.getCatcherCs(), source.getCatcherCs()));
        target.setCatcherSb(mergeInt(target.getCatcherSb(), source.getCatcherSb()));
        target.setFieldingGs(Boolean.TRUE.equals(target.getFieldingGs())
                || Boolean.TRUE.equals(source.getFieldingGs()));
    }

    private Integer mergeInt(Integer target, Integer source) {
        return (target == null || target == 0) && source != null ? source : target;
    }

    private String snapshot(GamePlayerStat stat, List<GameFieldingPosStat> fielding) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("stat", stat);
        snapshot.put("fieldingPositions", fielding);
        try {
            return this.auditMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("转记审计快照序列化失败", ex);
        }
    }

    private void syncPitchPaBf(GamePlayerStat stat) {
        if (stat.getPitchBf() != null) {
            stat.setPitchPa(stat.getPitchBf());
        } else if (stat.getPitchPa() != null) {
            stat.setPitchBf(stat.getPitchPa());
        }
    }

    private void markSourceDeleted(GamePitcherTransfer transfer, Long operatorId) {
        transfer.setStatus(TRANSFER_STATUS_DELETED);
        transfer.setSourceDeletedBy(operatorId);
        transfer.setSourceDeletedAt(LocalDateTime.now());
        transfer.setUpdatedBy(operatorId);
        this.gamePitcherTransferRepository.save(transfer);
    }

    private static List<GameFieldingPosStat> fieldingRowsOf(
            List<GameFieldingPosStat> rows, Long teamId, Long playerId) {
        return rows.stream()
                .filter(row -> Objects.equals(row.getTeamId(), teamId))
                .filter(row -> Objects.equals(row.getPlayerId(), playerId))
                .toList();
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
        ErDecisionAdjustment adjustment = findActiveAdjustment(tenantId, gameId, entity);
        if (adjustment != null && ADJUSTMENT_VOID.equals(adjustment.getAction())) {
            throw new IllegalArgumentException("该判定已作废，请先恢复再修改判定");
        }
        Long effectivePitcher = effectivePitcherId(entity, adjustment);
        if (effectivePitcher != null
                && this.pitcherRunOverrideRepository.existsByTenantIdAndGameIdAndPlayerIdAndStatus(
                        tenantId, gameId, effectivePitcher, OVERRIDE_APPROVED)) {
            throw new IllegalArgumentException("该投手已手工覆写 R/ER，请先撤销覆写再修改判定");
        }
        entity.setEarnedStatus(next.name());
        entity.setUnearnedReason(next == EarnedRunStatus.UNEARNED ? unearnedReason : null);
        entity.setOverriddenBy(operatorId);
        entity.setOverriddenAt(LocalDateTime.now());
        EarnedRunDecisionEntity saved = decisionRepository.save(entity);
        // 覆核只改单条判定，但仍按该场全量判定绝对重算，避免增量误差残留。
        recomputePitcherStats(tenantId, gameId, null);
        return saved;
    }

    /** 把判定改派给同队另一名投手；原始判定保留，写入人工调整审计。 */
    @Transactional
    public EarnedRunDecisionListDTO reassignDecision(long tenantId, long gameId, long decisionId,
            Long targetPitcherId, String reason, long operatorId) {
        return adjustDecision(tenantId, gameId, decisionId, ADJUSTMENT_REASSIGN,
                targetPitcherId, reason, operatorId);
    }

    /** 作废误录判定：不再计入任何投手 R/ER；原始判定保留。 */
    @Transactional
    public EarnedRunDecisionListDTO voidDecision(long tenantId, long gameId, long decisionId,
            String reason, long operatorId) {
        return adjustDecision(tenantId, gameId, decisionId, ADJUSTMENT_VOID,
                null, reason, operatorId);
    }

    private EarnedRunDecisionListDTO adjustDecision(long tenantId, long gameId, long decisionId,
            String action, Long targetPitcherId, String reason, long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || decisionId <= 0 || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, decision and operator must be positive");
        }
        if (reason != null && reason.trim().length() > 200) {
            throw new IllegalArgumentException("原因不能超过 200 字");
        }
        EarnedRunDecisionEntity decision = decisionRepository
                .findByIdAndTenantIdAndGameId(decisionId, tenantId, gameId)
                .orElseThrow(() -> new IllegalArgumentException("ER 判定不存在"));
        if (decision.getResponsiblePitcherId() == null) {
            throw new IllegalArgumentException("判定缺少责任投手");
        }
        EarnedRunPlayEntity play = playRepository.findById(decision.getPlayId())
                .orElseThrow(() -> new IllegalArgumentException("判定关联 Play 不存在"));
        if (play.getSourceEventId() == null || play.getSourceEventId().isBlank()) {
            throw new IllegalArgumentException("源 Play 缺少业务 ID，无法记录调整");
        }
        ErDecisionAdjustment active = findActiveAdjustment(tenantId, gameId, decision);
        Long currentEffective = effectivePitcherId(decision, active);
        if (ADJUSTMENT_VOID.equals(action)
                && active != null && ADJUSTMENT_VOID.equals(active.getAction())) {
            throw new IllegalArgumentException("该判定已作废");
        }
        if (ADJUSTMENT_REASSIGN.equals(action)) {
            if (targetPitcherId == null || targetPitcherId <= 0) {
                throw new IllegalArgumentException("请选择改派目标投手");
            }
            GamePlayerStat original = requirePitcherStat(
                    tenantId, gameId, decision.getResponsiblePitcherId());
            GamePlayerStat target = requirePitcherStat(tenantId, gameId, targetPitcherId);
            if (!Objects.equals(original.getTeamId(), target.getTeamId())) {
                throw new IllegalArgumentException("只能改派给同队投手");
            }
            if (Objects.equals(currentEffective, targetPitcherId)) {
                throw new IllegalArgumentException("目标投手已是当前有效责任投手");
            }
        }
        Set<Long> affectedPitcherIds = new HashSet<>();
        affectedPitcherIds.add(decision.getResponsiblePitcherId());
        if (active != null && active.getTargetPitcherId() != null) {
            affectedPitcherIds.add(active.getTargetPitcherId());
        }
        if (ADJUSTMENT_REASSIGN.equals(action)) {
            affectedPitcherIds.add(targetPitcherId);
        }
        for (ErDecisionAdjustment current : erDecisionAdjustmentRepository
                .findByTenantIdAndGameIdAndSourceEventIdAndRunnerIdAndStatus(
                        tenantId, gameId, play.getSourceEventId(),
                        decision.getRunnerId(), ADJUSTMENT_ACTIVE)) {
            current.setStatus(ADJUSTMENT_SUPERSEDED);
            current.setUpdatedBy(operatorId);
            // 必须先 flush 旧 ACTIVE 行，避免部分唯一索引在插入新 ACTIVE 时冲突。
            erDecisionAdjustmentRepository.saveAndFlush(current);
        }
        ErDecisionAdjustment row = new ErDecisionAdjustment();
        row.setTenantId(tenantId);
        row.setGameId(gameId);
        row.setDecisionId(decision.getId());
        row.setSourceEventId(play.getSourceEventId());
        row.setRunnerId(decision.getRunnerId());
        row.setAction(action);
        row.setTargetPitcherId(ADJUSTMENT_REASSIGN.equals(action) ? targetPitcherId : null);
        row.setStatus(ADJUSTMENT_ACTIVE);
        row.setReason(normalizeNote(reason));
        row.setCreatedBy(operatorId);
        row.setUpdatedBy(operatorId);
        erDecisionAdjustmentRepository.save(row);
        recomputePitcherStats(tenantId, gameId, null, affectedPitcherIds);
        return findDecisionDto(tenantId, gameId, decisionId);
    }

    /** 撤销最近一次改派/作废，恢复引擎责任投手。 */
    @Transactional
    public EarnedRunDecisionListDTO revokeDecisionAdjustment(long tenantId, long gameId,
            long adjustmentId, long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || adjustmentId <= 0 || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, adjustment and operator must be positive");
        }
        ErDecisionAdjustment row = erDecisionAdjustmentRepository
                .findByIdAndTenantIdAndGameId(adjustmentId, tenantId, gameId)
                .orElseThrow(() -> new IllegalArgumentException("判定调整记录不存在"));
        if (!ADJUSTMENT_ACTIVE.equals(row.getStatus())) {
            throw new IllegalArgumentException("该调整已撤销或被替代");
        }
        row.setStatus(ADJUSTMENT_REVOKED);
        row.setRevokedBy(operatorId);
        row.setRevokedAt(LocalDateTime.now());
        row.setUpdatedBy(operatorId);
        erDecisionAdjustmentRepository.save(row);
        EarnedRunDecisionEntity decision = findDecisionByKey(
                tenantId, gameId, row.getSourceEventId(), row.getRunnerId());
        Set<Long> affectedPitcherIds = new HashSet<>();
        if (decision != null && decision.getResponsiblePitcherId() != null) {
            affectedPitcherIds.add(decision.getResponsiblePitcherId());
        }
        if (row.getTargetPitcherId() != null) {
            affectedPitcherIds.add(row.getTargetPitcherId());
        }
        recomputePitcherStats(tenantId, gameId, null, affectedPitcherIds);
        return decision == null ? null : findDecisionDto(tenantId, gameId, decision.getId());
    }

    /**
     * 以该场全部逐分判定为唯一事实来源，绝对覆盖每名责任投手的 R/ER/UER/PENDING；
     * 有生效中人工覆写的投手以人工值为准，不被引擎结果覆盖。
     */
    private Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> recomputePitcherStats(
            long tenantId, long gameId, Long fallbackTeamId) {
        return recomputePitcherStats(tenantId, gameId, fallbackTeamId, Set.of());
    }

    private Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> recomputePitcherStats(
            long tenantId, long gameId, Long fallbackTeamId, Set<Long> forceResetPlayerIds) {
        List<GamePlayerStat> gameStats = loadTenantStats(tenantId, gameId);
        Map<Long, GamePlayerStat> pitcherStats = pitcherStatsOf(gameStats);
        Map<Long, Long> teamByPlayer = teamByPlayerOf(gameStats);
        Map<Long, int[]> totals = enginePitcherTotals(tenantId, gameId);
        Map<Long, PitcherRunOverride> overrides = activeOverridesByPlayer(tenantId, gameId);

        for (Map.Entry<Long, int[]> entry : totals.entrySet()) {
            if (overrides.containsKey(entry.getKey())) {
                continue;
            }
            GamePlayerStat stat = ensurePitcherStat(tenantId, gameId, entry.getKey(),
                    pitcherStats, teamByPlayer, fallbackTeamId);
            applyPitcherTotals(stat, entry.getValue());
            gamePlayerStatRepository.save(stat);
        }
        for (PitcherRunOverride override : overrides.values()) {
            GamePlayerStat stat = ensurePitcherStat(tenantId, gameId, override.getPlayerId(),
                    pitcherStats, teamByPlayer, fallbackTeamId);
            applyPitcherTotals(stat, overrideTotals(override, pendingOf(totals, override.getPlayerId())));
            gamePlayerStatRepository.save(stat);
        }
        // 覆写刚失效/撤销且该投手已无判定：不能保留旧人工值，四字段归零。
        for (Long playerId : forceResetPlayerIds) {
            if (playerId == null || totals.containsKey(playerId)
                    || overrides.containsKey(playerId)) {
                continue;
            }
            GamePlayerStat stat = pitcherStats.get(playerId);
            if (stat != null) {
                applyPitcherTotals(stat, new int[4]);
                gamePlayerStatRepository.save(stat);
            }
        }
        return absolutePitcherTotals(pitcherStats);
    }

    private GamePlayerStat ensurePitcherStat(long tenantId, long gameId, Long playerId,
            Map<Long, GamePlayerStat> pitcherStats, Map<Long, Long> teamByPlayer,
            Long fallbackTeamId) {
        GamePlayerStat stat = pitcherStats.get(playerId);
        if (stat != null) {
            return stat;
        }
        Long teamId = teamByPlayer.get(playerId);
        if (teamId == null) {
            teamId = fallbackTeamId;
        }
        if (teamId == null) {
            throw new IllegalStateException(
                    "Missing pitcher stat row for responsible pitcher " + playerId);
        }
        stat = createPitcherStatRow(tenantId, gameId, teamId, playerId);
        pitcherStats.put(playerId, stat);
        return stat;
    }

    private void applyPitcherTotals(GamePlayerStat stat, int[] total) {
        stat.setPitchR(total[0]);
        stat.setEr(total[1]);
        stat.setUnearnedR(total[2]);
        stat.setPendingR(total[3]);
    }

    private int[] overrideTotals(PitcherRunOverride override, int enginePending) {
        return splitRuns(valueOrZero(override.getRunsAllowed()),
                valueOrZero(override.getEarnedRuns()), enginePending);
    }

    /**
     * 人工覆写四栏口径：R/ER 以人工值为准，非自责部分先留给引擎的待判定（PEND），剩下的才是 UER。
     * 直接把 runs-earned 全归 UER 会把待判定无声折成「已判定为非自责」，而 R=ER+UER+PEND 照样
     * 成立，四栏自洽检查根本查不出这种折叠（线上实测 game 278 投手 1275）。
     * 人工额度不够容纳全部待判定时，以人工值为准截断 PEND，仍保证恒等式成立。
     */
    private static int[] splitRuns(int runs, int earned, int enginePending) {
        int room = Math.max(0, runs - earned);
        int pending = Math.min(Math.max(0, enginePending), room);
        return new int[]{runs, earned, room - pending, pending};
    }

    private static int pendingOf(Map<Long, int[]> totals, Long playerId) {
        int[] value = totals.get(playerId);
        return value == null ? 0 : value[3];
    }

    private Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> absolutePitcherTotals(
            Map<Long, GamePlayerStat> pitcherStats) {
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

    private List<GamePlayerStat> loadTenantStats(long tenantId, long gameId) {
        return gamePlayerStatRepository.findByGameId(gameId).stream()
                .filter(stat -> stat.getTenantId() != null
                        && stat.getTenantId() == tenantId
                        && stat.getPlayerId() != null)
                .toList();
    }

    private Map<Long, GamePlayerStat> pitcherStatsOf(List<GamePlayerStat> gameStats) {
        Map<Long, GamePlayerStat> pitcherStats = new HashMap<>();
        for (GamePlayerStat stat : gameStats) {
            if (Integer.valueOf(1).equals(stat.getIsPitcher())) {
                pitcherStats.put(stat.getPlayerId(), stat);
            }
        }
        return pitcherStats;
    }

    private Map<Long, Long> teamByPlayerOf(List<GamePlayerStat> gameStats) {
        Map<Long, Long> teamByPlayer = new HashMap<>();
        for (GamePlayerStat stat : gameStats) {
            teamByPlayer.putIfAbsent(stat.getPlayerId(), stat.getTeamId());
        }
        return teamByPlayer;
    }

    private Map<Long, int[]> enginePitcherTotals(long tenantId, long gameId) {
        return aggregatePitcherTotals(tenantId, gameId,
                decisionRepository.findByTenantIdAndGameId(tenantId, gameId));
    }

    /**
     * 给定判定列聚合投手四栏（含人工作废过滤与改派口径）。
     * 引擎合计与局中预览共用这一份算术，避免两条路各算一套数。
     * 返回可变 Map：预览需要在同一分母上继续累加本次尚未入库的判定。
     */
    private Map<Long, int[]> aggregatePitcherTotals(long tenantId, long gameId,
            List<EarnedRunDecisionEntity> decisions) {
        Map<Long, String> sourceEventByPlayId = sourceEventByPlayId(tenantId, gameId);
        Map<DecisionKey, ErDecisionAdjustment> adjustments =
                activeAdjustmentsByKey(activeAdjustments(tenantId, gameId));
        Map<Long, int[]> totals = new HashMap<>();
        for (EarnedRunDecisionEntity decision : decisions) {
            ErDecisionAdjustment adjustment = adjustmentFor(
                    sourceEventByPlayId.get(decision.getPlayId()),
                    decision.getRunnerId(), adjustments);
            if (adjustment != null && ADJUSTMENT_VOID.equals(adjustment.getAction())) {
                continue;
            }
            Long pitcherId = effectivePitcherId(decision, adjustment);
            if (pitcherId == null) {
                continue;
            }
            addPitcherRun(totals, pitcherId, decision.getEarnedStatus());
        }
        return totals;
    }

    /**
     * 一笔判定对投手四栏的归位：R 恒加一，再按状态进 ER/UER/PEND 之一（未知状态只进 R）。
     * 落库重算、引擎合计、局中预览三条路必须走同一个方法，否则同一分会在画面上变出口径。
     */
    private static void addPitcherRun(
            Map<Long, int[]> totals, Long pitcherId, String earnedStatus) {
        if (pitcherId == null) {
            return;
        }
        int[] value = totals.computeIfAbsent(pitcherId, ignored -> new int[4]);
        value[0] += 1;
        if (EarnedRunStatus.EARNED.name().equals(earnedStatus)) {
            value[1] += 1;
        } else if (EarnedRunStatus.UNEARNED.name().equals(earnedStatus)) {
            value[2] += 1;
        } else if (EarnedRunStatus.PENDING.name().equals(earnedStatus)) {
            value[3] += 1;
        }
    }

    /**
     * 局中预览：算出「本半局若在此刻定案会落库的投手四栏」回给前端显示，全程不写库。
     * 分母 = 其他半局有效判定 + 本次逐棒判定；人工覆写只对「定案时不会被作废」的投手继续生效——
     * 定案路径会先取 collectAffectedPitcherIds（该半局旧判定的责任投手）再加上本次新判定的责任
     * 投手，把这批人的 APPROVED 覆写改成 INVALIDATED。预览若照单全叠，显示的就是几个马上会被
     * 推翻的数字，记录员会以为人工决断还在生效。两边用同一个 affected 集合筛，报出来的才是
     * 同一个口径。
     * 本次判定尚未入库，不可能已带人工改派/作废，所以按引擎责任投手归位；
     * 该偏差只影响显示，该半局定案入库时会以完整口径重算。
     */
    private PitcherPreview previewPitcherTotals(
            long tenantId, long gameId, List<EarnedRunPlayEntity> existing,
            EarnedRunReconstructionResult result) {
        Set<Long> replacedPlayIds = existing.stream()
                .map(EarnedRunPlayEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<EarnedRunDecisionEntity> otherHalves = decisionRepository
                .findByTenantIdAndGameId(tenantId, gameId).stream()
                .filter(row -> row.getPlayId() == null
                        || !replacedPlayIds.contains(row.getPlayId()))
                .toList();
        Map<Long, int[]> totals = aggregatePitcherTotals(tenantId, gameId, otherHalves);
        for (EarnedRunDecision decision : result.decisions()) {
            addPitcherRun(totals, decision.responsiblePitcherId(), decision.status().name());
        }
        Set<Long> affectedPitcherIds = collectAffectedPitcherIds(tenantId, gameId, existing);
        for (EarnedRunDecision decision : result.decisions()) {
            if (decision.responsiblePitcherId() > 0) {
                affectedPitcherIds.add(decision.responsiblePitcherId());
            }
        }
        List<Long> overridesToInvalidate = new ArrayList<>();
        activeOverridesByPlayer(tenantId, gameId).forEach((playerId, override) -> {
            if (affectedPitcherIds.contains(playerId)) {
                overridesToInvalidate.add(playerId);
            } else {
                totals.put(playerId, overrideTotals(override, pendingOf(totals, playerId)));
            }
        });
        Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> preview = new HashMap<>();
        totals.forEach((playerId, value) -> preview.put(playerId,
                new EarnedRunReconstructionResult.PitcherRunTotals(
                        value[0], value[1], value[2], value[3])));
        return new PitcherPreview(preview, overridesToInvalidate);
    }

    /** 预览回包：投手四栏，加上「本半局一旦定案就会被作废的覆写所属投手」。 */
    private record PitcherPreview(
            Map<Long, EarnedRunReconstructionResult.PitcherRunTotals> totals,
            List<Long> overridesToInvalidate) {
    }

    private Map<Long, PitcherRunOverride> activeOverridesByPlayer(long tenantId, long gameId) {
        return this.pitcherRunOverrideRepository
                .findByTenantIdAndGameIdAndStatus(tenantId, gameId, OVERRIDE_APPROVED).stream()
                .filter(row -> row.getPlayerId() != null)
                .collect(Collectors.toMap(PitcherRunOverride::getPlayerId, row -> row,
                        (first, second) -> second));
    }

    private PitcherRunTotalsDTO toPitcherTotals(Long playerId, GamePlayerStat stat,
            int[] engineTotal, PitcherRunOverride override, Player player) {
        int[] engine = engineTotal == null ? new int[4] : engineTotal;
        int[] effective = override != null ? overrideTotals(override, engine[3]) : engine;
        int runs = effective[0];
        int earned = effective[1];
        int unearned = effective[2];
        int pending = effective[3];
        Long overriddenBy = null;
        LocalDateTime overriddenAt = null;
        if (override != null) {
            overriddenBy = override.getUpdatedBy() != null
                    ? override.getUpdatedBy() : override.getCreatedBy();
            overriddenAt = override.getUpdatedAt() != null
                    ? override.getUpdatedAt() : override.getCreatedAt();
        }
        Long teamId = stat != null && stat.getTeamId() != null
                ? stat.getTeamId() : (player != null ? player.getTeamId() : null);
        return new PitcherRunTotalsDTO(
                playerId,
                player != null ? player.getName() : null,
                teamId,
                stat != null ? stat.getPitcherOrder() : null,
                runs,
                earned,
                unearned,
                pending,
                engine[0],
                engine[1],
                override != null ? override.getId() : null,
                override != null ? override.getNote() : null,
                overriddenBy,
                overriddenAt);
    }

    private Map<Long, Player> loadPlayerMap(Set<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Player> players = new HashMap<>();
        for (Player player : playerRepository.findAllById(playerIds)) {
            if (player.getId() != null) {
                players.put(player.getId(), player);
            }
        }
        return players;
    }

    private PitcherRunTotalsDTO findPitcherTotals(long tenantId, long gameId, long playerId) {
        return listPitcherTotals(tenantId, gameId).stream()
                .filter(row -> Objects.equals(row.playerId(), playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("投手统计不存在: " + playerId));
    }

    private void validatePitcherOverrideInput(long tenantId, long gameId, long playerId,
            Integer runsAllowed, Integer earnedRuns, String note, long operatorId) {
        if (tenantId <= 0 || gameId <= 0 || playerId <= 0 || operatorId <= 0) {
            throw new IllegalArgumentException("Tenant, game, player and operator must be positive");
        }
        if (runsAllowed == null || earnedRuns == null
                || runsAllowed < 0 || earnedRuns < 0
                || runsAllowed > 99 || earnedRuns > 99) {
            throw new IllegalArgumentException("投手 R/ER 需在 0-99 之间");
        }
        if (earnedRuns > runsAllowed) {
            throw new IllegalArgumentException("ER 不能大于 R");
        }
        if (note != null && note.trim().length() > 200) {
            throw new IllegalArgumentException("备注不能超过 200 字");
        }
    }

    private void ensurePitcherInGame(long tenantId, long gameId, long playerId) {
        if (!hasEngineData(gameId)) {
            throw new IllegalArgumentException("该场没有 ER 引擎判定，请直接在结果录入中编辑 R/ER");
        }
        boolean pitcherRow = loadTenantStats(tenantId, gameId).stream()
                .anyMatch(stat -> Objects.equals(stat.getPlayerId(), playerId)
                        && Integer.valueOf(1).equals(stat.getIsPitcher()));
        if (!pitcherRow && !hasResponsiblePitcherDecisions(gameId, playerId)) {
            throw new IllegalArgumentException("该球员不是本场投手");
        }
    }

    private Set<Long> invalidateActiveOverrides(long tenantId, long gameId, Set<Long> playerIds,
            String reason, long operatorId) {
        Set<Long> invalidated = new HashSet<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return invalidated;
        }
        for (PitcherRunOverride row : this.pitcherRunOverrideRepository
                .findByTenantIdAndGameIdAndStatus(tenantId, gameId, OVERRIDE_APPROVED)) {
            if (row.getPlayerId() == null || !playerIds.contains(row.getPlayerId())) {
                continue;
            }
            row.setStatus(OVERRIDE_INVALIDATED);
            row.setInvalidReason(reason);
            row.setUpdatedBy(operatorId);
            this.pitcherRunOverrideRepository.save(row);
            invalidated.add(row.getPlayerId());
        }
        return invalidated;
    }

    private Set<Long> collectAffectedPitcherIds(long tenantId, long gameId,
            List<EarnedRunPlayEntity> existing) {
        Set<Long> playerIds = new HashSet<>();
        if (existing == null || existing.isEmpty()) {
            return playerIds;
        }
        List<Long> playIds = existing.stream()
                .map(EarnedRunPlayEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        if (playIds.isEmpty()) {
            return playerIds;
        }
        for (EarnedRunDecisionEntity decision : decisionRepository
                .findByTenantIdAndGameIdAndPlayIdIn(tenantId, gameId, playIds)) {
            if (decision.getResponsiblePitcherId() != null) {
                playerIds.add(decision.getResponsiblePitcherId());
            }
        }
        return playerIds;
    }

    private String normalizeNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        stat.setFirstPosition("P");
        stat.setPitchR(0);
        stat.setEr(0);
        stat.setUnearnedR(0);
        stat.setPendingR(0);
        stat.setOutsRecorded(0);
        return stat;
    }

    private static int valueOrZero(Integer value) {
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
        List<EarnedRunDecisionEntity> decisions =
                decisionRepository.findByTenantIdAndGameId(tenantId, gameId);
        Map<DecisionKey, ErDecisionAdjustment> adjustments =
                activeAdjustmentsByKey(activeAdjustments(tenantId, gameId));
        List<DecisionView> views = decisions.stream()
                .map(decision -> toDecisionView(decision, playById, adjustments))
                .toList();
        Set<Long> playerIds = new HashSet<>();
        for (DecisionView view : views) {
            EarnedRunDecisionEntity decision = view.decision();
            addIfPresent(playerIds, decision.getRunnerId());
            addIfPresent(playerIds, decision.getResponsiblePitcherId());
            addIfPresent(playerIds, decision.getReachedBasePitcherId());
            addIfPresent(playerIds, view.effectivePitcherId());
        }
        Map<Long, String> playerNames = loadPlayerNames(playerIds);

        return views.stream()
                .map(view -> {
                    EarnedRunDecisionEntity decision = view.decision();
                    EarnedRunPlayEntity play = view.play();
                    ErDecisionAdjustment adjustment = view.adjustment();
                    return new EarnedRunDecisionListDTO(
                            decision.getId(),
                            decision.getPlayId(),
                            play != null ? play.getInning() : null,
                            play != null ? play.getHalf() : null,
                            play != null ? play.getSequence() : null,
                            decision.getRunnerId(),
                            resolvePlayerName(playerNames, decision.getRunnerId()),
                            decision.getResponsiblePitcherId(),
                            resolvePlayerName(playerNames, decision.getResponsiblePitcherId()),
                            decision.getReachedBasePitcherId(),
                            resolvePlayerName(playerNames, decision.getReachedBasePitcherId()),
                            decision.getEarnedStatus(),
                            decision.getUnearnedReason(),
                            decision.getRunnerOrigin(),
                            decision.getOverriddenBy(),
                            decision.getOverriddenAt(),
                            view.effectivePitcherId(),
                            resolvePlayerName(playerNames, view.effectivePitcherId()),
                            adjustment != null ? adjustment.getId() : null,
                            adjustment != null ? adjustment.getAction() : null,
                            adjustment != null ? adjustment.getReason() : null,
                            view.voided());
                })
                .sorted((a, b) -> {
                    int ia = a.inning() != null ? a.inning() : Integer.MAX_VALUE;
                    int ib = b.inning() != null ? b.inning() : Integer.MAX_VALUE;
                    if (ia != ib) return Integer.compare(ia, ib);
                    int ha = "top".equals(a.half()) ? 0 : 1;
                    int hb = "top".equals(b.half()) ? 0 : 1;
                    if (ha != hb) return Integer.compare(ha, hb);
                    int sa = a.sequence() != null ? a.sequence() : Integer.MAX_VALUE;
                    int sb = b.sequence() != null ? b.sequence() : Integer.MAX_VALUE;
                    return Integer.compare(sa, sb);
                })
                .toList();
    }

    private DecisionView toDecisionView(EarnedRunDecisionEntity decision,
            Map<Long, EarnedRunPlayEntity> playById,
            Map<DecisionKey, ErDecisionAdjustment> adjustments) {
        EarnedRunPlayEntity play = playById.get(decision.getPlayId());
        String sourceEventId = play != null ? play.getSourceEventId() : null;
        ErDecisionAdjustment adjustment = adjustmentFor(
                sourceEventId, decision.getRunnerId(), adjustments);
        boolean voided = adjustment != null
                && ADJUSTMENT_VOID.equals(adjustment.getAction());
        Long effectivePitcherId = effectivePitcherId(decision, adjustment);
        return new DecisionView(decision, play, effectivePitcherId, voided, adjustment);
    }

    private List<ErDecisionAdjustment> activeAdjustments(long tenantId, long gameId) {
        return erDecisionAdjustmentRepository.findByTenantIdAndGameIdAndStatus(
                tenantId, gameId, ADJUSTMENT_ACTIVE);
    }

    private Map<DecisionKey, ErDecisionAdjustment> activeAdjustmentsByKey(
            List<ErDecisionAdjustment> rows) {
        Map<DecisionKey, ErDecisionAdjustment> result = new HashMap<>();
        for (ErDecisionAdjustment row : rows) {
            if (row.getSourceEventId() == null || row.getRunnerId() == null) {
                continue;
            }
            DecisionKey key = new DecisionKey(row.getSourceEventId(), row.getRunnerId());
            ErDecisionAdjustment existing = result.get(key);
            if (existing == null || idOf(row) > idOf(existing)) {
                result.put(key, row);
            }
        }
        return result;
    }

    private static long idOf(ErDecisionAdjustment row) {
        return row.getId() == null ? 0 : row.getId();
    }

    private ErDecisionAdjustment adjustmentFor(String sourceEventId, Long runnerId,
            Map<DecisionKey, ErDecisionAdjustment> adjustments) {
        if (sourceEventId == null || runnerId == null) {
            return null;
        }
        return adjustments.get(new DecisionKey(sourceEventId, runnerId));
    }

    private Long effectivePitcherId(EarnedRunDecisionEntity decision,
            ErDecisionAdjustment adjustment) {
        if (adjustment != null) {
            if (ADJUSTMENT_VOID.equals(adjustment.getAction())) {
                return null;
            }
            if (ADJUSTMENT_REASSIGN.equals(adjustment.getAction())) {
                return adjustment.getTargetPitcherId();
            }
        }
        return decision.getResponsiblePitcherId();
    }

    private Long effectivePitcherId(EarnedRunDecisionEntity decision,
            Map<Long, String> sourceEventByPlayId,
            Map<DecisionKey, ErDecisionAdjustment> adjustments) {
        return effectivePitcherId(decision, adjustmentFor(
                sourceEventByPlayId.get(decision.getPlayId()),
                decision.getRunnerId(), adjustments));
    }

    private Map<Long, String> sourceEventByPlayId(long tenantId, long gameId) {
        return sourceEventByPlayId(playRepository.findByTenantIdAndGameId(tenantId, gameId));
    }

    private Map<Long, String> sourceEventByPlayId(List<EarnedRunPlayEntity> plays) {
        return plays.stream()
                .filter(play -> play.getId() != null && play.getSourceEventId() != null)
                .collect(Collectors.toMap(EarnedRunPlayEntity::getId,
                        EarnedRunPlayEntity::getSourceEventId, (first, second) -> first));
    }

    private ErDecisionAdjustment findActiveAdjustment(long tenantId, long gameId,
            EarnedRunDecisionEntity decision) {
        if (decision.getPlayId() == null || decision.getRunnerId() == null) {
            return null;
        }
        EarnedRunPlayEntity play = playRepository.findById(decision.getPlayId()).orElse(null);
        if (play == null || play.getSourceEventId() == null) {
            return null;
        }
        return erDecisionAdjustmentRepository
                .findFirstByTenantIdAndGameIdAndSourceEventIdAndRunnerIdAndStatusOrderByIdDesc(
                        tenantId, gameId, play.getSourceEventId(),
                        decision.getRunnerId(), ADJUSTMENT_ACTIVE)
                .orElse(null);
    }

    private EarnedRunDecisionEntity findDecisionByKey(long tenantId, long gameId,
            String sourceEventId, Long runnerId) {
        Map<Long, String> sourceEventByPlayId = sourceEventByPlayId(tenantId, gameId);
        for (EarnedRunDecisionEntity decision : decisionRepository
                .findByTenantIdAndGameId(tenantId, gameId)) {
            if (Objects.equals(decision.getRunnerId(), runnerId)
                    && Objects.equals(sourceEventByPlayId.get(decision.getPlayId()),
                            sourceEventId)) {
                return decision;
            }
        }
        return null;
    }

    private EarnedRunDecisionListDTO findDecisionDto(long tenantId, long gameId, long decisionId) {
        return listDecisions(tenantId, gameId).stream()
                .filter(row -> Objects.equals(row.decisionId(), decisionId))
                .findFirst()
                .orElse(null);
    }

    private GamePlayerStat requirePitcherStat(long tenantId, long gameId, long playerId) {
        return loadTenantStats(tenantId, gameId).stream()
                .filter(stat -> Objects.equals(stat.getPlayerId(), playerId))
                .filter(stat -> Integer.valueOf(1).equals(stat.getIsPitcher()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("球员不是本场投手: " + playerId));
    }

    private Map<Long, String> loadPlayerNames(Set<Long> playerIds) {
        Map<Long, String> playerNames = new HashMap<>();
        if (playerIds == null || playerIds.isEmpty()) {
            return playerNames;
        }
        for (Player player : playerRepository.findAllById(playerIds)) {
            playerNames.put(player.getId(), player.getName());
        }
        return playerNames;
    }

    private static void addIfPresent(Set<Long> ids, Long id) {
        if (id != null) ids.add(id);
    }

    private static String resolvePlayerName(Map<Long, String> playerNames, Long playerId) {
        if (playerId == null) return null;
        String name = playerNames.get(playerId);
        return name != null ? name : "#" + playerId;
    }

    // ─── §11 結算驗證 ───────────────────────────────────────────────────────────

    /**
     * 比賽結束時執行，回傳違規訊息列表（空 = 全部通過）。
     * 規範 §11：R=ER+UER+PENDING、ER≤R、UER≥0、不重複判定、責任投手非空；
     * §11.7–§11.9 再補全場級分解自洽（判定數=總分、逐隊投手群 R=對手得分、總分=逐局和），
     * 因為單筆判定正確不代表三層加總對得上，線上實測過這三種漂移。
     */
    public List<String> validateSettlement(long tenantId, long gameId) {
        List<String> violations = new ArrayList<>();
        List<GamePlayerStat> stats = gamePlayerStatRepository.findByGameId(gameId);

        // 1-3: per-pitcher aggregated checks
        for (GamePlayerStat p : stats) {
            if (p.getTenantId() == null || p.getTenantId() != tenantId) continue;
            if (!Integer.valueOf(1).equals(p.getIsPitcher())) continue;
            long pid = p.getPlayerId() != null ? p.getPlayerId() : 0;
            int r = valueOrZero(p.getPitchR());
            int er = valueOrZero(p.getEr());
            int uer = valueOrZero(p.getUnearnedR());
            int pr = valueOrZero(p.getPendingR());
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

        // \u00a711.6 責任投手（含轉記後口徑）在場內卻無投手統計列：這筆分數的 R/ER 無處落帳，
        // box score 會直接看不見它（game 261 判定掛在 114、場內無 114 投手列實測）。
        Set<Long> pitcherRowIds = new HashSet<>();
        for (GamePlayerStat p : stats) {
            if (p.getTenantId() == null || p.getTenantId() != tenantId) continue;
            if (Integer.valueOf(1).equals(p.getIsPitcher()) && p.getPlayerId() != null) {
                pitcherRowIds.add(p.getPlayerId());
            }
        }
        Map<Long, int[]> effectiveTotals = new TreeMap<>(enginePitcherTotals(tenantId, gameId));
        for (Map.Entry<Long, int[]> entry : effectiveTotals.entrySet()) {
            if (entry.getValue()[0] > 0 && !pitcherRowIds.contains(entry.getKey())) {
                violations.add(String.format(
                        "\u00a711.6 投手 %d 有 %d 筆判定但場內無投手統計列（R/ER 無處落帳）",
                        entry.getKey(), entry.getValue()[0]));
            }
        }

        appendTotalBreakage(tenantId, gameId, stats, decisions, violations);

        return violations;
    }

    /**
     * §11.7–§11.9 全場級分解自洽：判定筆數、逐隊投手群失分、總分與逐局比分必須互相扣得上。
     * 只比「同一記錄動作同時產出的兩側」，因為比分欄可被結果錄入補寫（game 260 實測：
     * 快照停在 1 局、判定 16 筆，但比分 18 且投手 R=18 對得上），
     * 所以「判定少於比分」是混合來源場次的常態，不報警；反過來才一定是記錄鏈路壞了。
     */
    private void appendTotalBreakage(long tenantId, long gameId, List<GamePlayerStat> stats,
            List<EarnedRunDecisionEntity> decisions, List<String> violations) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null || !Objects.equals(game.getTenantId(), Long.valueOf(tenantId))) {
            return;
        }
        int awayScore = valueOrZero(game.getAwayScore());
        int homeScore = valueOrZero(game.getHomeScore());
        int totalScore = awayScore + homeScore;
        // 裁定/特殊結果場次的比分按規則判定，不來自逐筆記錄，加總必然對不上：不套這三條。
        if (Boolean.TRUE.equals(game.getIsSpecialResult())) {
            return;
        }

        // §11.7 判定筆數不得超過總分：一筆判定就是一個跑者回本壘，被 VOID 修訂抹掉的不算。
        // 判定多於比分代表判定層有比分層沒的分數（扣分只動比分層、或重複判定），線上實測 255/278 各 +1。
        Map<Long, String> sourceEventByPlayId = sourceEventByPlayId(tenantId, gameId);
        Map<DecisionKey, ErDecisionAdjustment> adjustments =
                activeAdjustmentsByKey(activeAdjustments(tenantId, gameId));
        int judgedRuns = 0;
        for (EarnedRunDecisionEntity decision : decisions) {
            ErDecisionAdjustment adjustment = adjustmentFor(
                    sourceEventByPlayId.get(decision.getPlayId()), decision.getRunnerId(), adjustments);
            if (adjustment != null && ADJUSTMENT_VOID.equals(adjustment.getAction())) {
                continue;
            }
            judgedRuns += 1;
        }
        if (judgedRuns > totalScore) {
            violations.add(String.format(
                    "\u00a711.7 判定 %d 筆多於總分 %d:%d（合計 %d），差額 %+d：判定層有比分層沒記到的分數",
                    judgedRuns, awayScore, homeScore, totalScore, judgedRuns - totalScore));
        }

        // §11.8 逐隊投手群失分合計 = 對隊得分（客隊投手守的是主隊打序，反之亦然）。
        // 全隊 pitchR 都是 NULL 時視為「引擎從未落帳」而不是失 0 分：這種場次的比分與 R 走結果錄入，
        // 拿它跟得分對帳只會誤報（線上實測 235/248 即此形）。
        if (game.getAwayTeamId() != null && game.getHomeTeamId() != null) {
            Map<Long, Integer> runsByTeam = new LinkedHashMap<>();
            Set<Long> settledTeams = new LinkedHashSet<>();
            for (GamePlayerStat p : stats) {
                if (p.getTenantId() == null || p.getTenantId() != tenantId) continue;
                if (!Integer.valueOf(1).equals(p.getIsPitcher()) || p.getTeamId() == null) continue;
                runsByTeam.merge(p.getTeamId(), valueOrZero(p.getPitchR()), Integer::sum);
                if (p.getPitchR() != null) {
                    settledTeams.add(p.getTeamId());
                }
            }
            for (Map.Entry<Long, Integer> entry : runsByTeam.entrySet()) {
                long teamId = entry.getKey();
                if (!settledTeams.contains(teamId)) {
                    continue;
                }
                Integer opponent = Long.valueOf(teamId).equals(game.getAwayTeamId()) ? homeScore
                        : Long.valueOf(teamId).equals(game.getHomeTeamId()) ? awayScore : null;
                if (opponent == null) {
                    violations.add(String.format("\u00a711.8 teamId=%d 有投手失分 %d 但不屬本場兩隊", teamId,
                            entry.getValue()));
                } else if (!entry.getValue().equals(opponent)) {
                    violations.add(String.format(
                            "\u00a711.8 teamId=%d 投手群 R 合計 %d 但對手的得分 %d 不等，差額 %+d：兩層必有一層沒落齊",
                            teamId, entry.getValue(), opponent, entry.getValue() - opponent));
                }
            }
        }

        // §11.9 總分 = 逐局比分加總；逐局資料缺失或解不了時跳過，不硬判違規。
        appendLineScoreBreakage("home", homeScore, inningRunsSum(game.getHomeScoreByInning()), violations);
        appendLineScoreBreakage("away", awayScore, inningRunsSum(game.getAwayScoreByInning()), violations);

        // §11.10 逐半局对帐（线上 game 261 型故障）：全场级的 §11.7/§11.9 会互相抵销
        // （3局下缺笔、5局上多一笔，全场加总照样平），缺在哪一半局只能在半局粒度才看得见。
        appendHalfInningCoverage(tenantId, gameId, game, decisions, violations);
    }

    /**
     * §11.10：半局记分与该半局有效判定笔数必须逐格相等。
     * 对帐范围取并集：有逐棒事件的半局，加上记分栏那一格有分的半局——少了后半段，「记分有分、
     * 判定层一笔都没有」的半局（库里连 play 列都没有，例如被强制结束或局中预览从没等到定案推送）
     * 根本进不了循环，而它正是最该被点名的一种。整个场次没有任何 play 列时完全不参与：纯结果录入
     * 的老场次没有逐棒证据，拿记分栏比它只会误报（彩排实测：不拦这一道，184 场里 147 场老场次
     * 会贡献 780 条假命中）；逐局那一格缺资料也直接跳过。
     * 人工作废单独在消息里报数，默默略过会让「真的缺笔」和「人工抹掉」再也分不开。
     *
     * <p>上线前拿生产资料做过一轮只读彩排（审计脚本 audit_11_10_sim.py，属仓库外的一次性
     * 工具；走同一套并集口径与同一道引擎托管闸门）：旧口径（只看 play 派生半局）184 场
     * final、157 个可对帐半局只命中 3 条；并集口径 35 场、165 个半局命中 11 条，多出的 8 条
     * 全是「整半局零判定」，落在 235（4 条）、248（3 条）、260 的 4 局上（1 条）。这三部都
     * 是投手层已被残缺分母砍过的已知场次（GameService 半局重建完整性守门的注释里点名的就是
     * 261/235/248），所以是补到了真异常，不是新噪音。旧口径已命中的三条：261「3局下 判定 2
     * 对记分 3」（缺笔）、255「1局下 判定 6 对记分 5」、260「1局下 判定 8 对记分 9」。
     */
    private void appendHalfInningCoverage(long tenantId, long gameId, Game game,
            List<EarnedRunDecisionEntity> decisions, List<String> violations) {
        Map<Long, String> halfKeyByPlayId = new HashMap<>();
        for (EarnedRunPlayEntity play : playRepository.findByTenantIdAndGameId(tenantId, gameId)) {
            if (play.getId() != null && play.getInning() != null && play.getHalf() != null) {
                halfKeyByPlayId.put(play.getId(), play.getInning() + "|" + play.getHalf());
            }
        }
        if (halfKeyByPlayId.isEmpty()) {
            return;
        }
        Map<Long, String> sourceEventByPlayId = sourceEventByPlayId(tenantId, gameId);
        Map<DecisionKey, ErDecisionAdjustment> adjustments =
                activeAdjustmentsByKey(activeAdjustments(tenantId, gameId));
        Map<String, int[]> countsByHalf = new HashMap<>();
        for (EarnedRunDecisionEntity decision : decisions) {
            String halfKey = decision.getPlayId() == null
                    ? null : halfKeyByPlayId.get(decision.getPlayId());
            if (halfKey == null) {
                continue;
            }
            ErDecisionAdjustment adjustment = adjustmentFor(
                    sourceEventByPlayId.get(decision.getPlayId()), decision.getRunnerId(), adjustments);
            int[] slot = countsByHalf.computeIfAbsent(halfKey, ignored -> new int[2]);
            if (adjustment != null && ADJUSTMENT_VOID.equals(adjustment.getAction())) {
                slot[1] += 1;
            } else {
                slot[0] += 1;
            }
        }
        List<Integer> awayCells = inningRunCells(game.getAwayScoreByInning());
        List<Integer> homeCells = inningRunCells(game.getHomeScoreByInning());
        // 并集：判定派生的半局（旧口径）＋记分栏有分但判定层可能一笔都没有的半局。
        Set<String> halfKeyUniverse = new LinkedHashSet<>(countsByHalf.keySet());
        for (int inning = 1; inning <= awayCells.size(); inning++) {
            Integer runs = awayCells.get(inning - 1);
            if (runs != null && runs > 0) {
                halfKeyUniverse.add(inning + "|top");
            }
        }
        for (int inning = 1; inning <= homeCells.size(); inning++) {
            Integer runs = homeCells.get(inning - 1);
            if (runs != null && runs > 0) {
                halfKeyUniverse.add(inning + "|bottom");
            }
        }
        List<String> halfKeys = new ArrayList<>(halfKeyUniverse);
        halfKeys.sort(Comparator.comparingInt((String key) ->
                Integer.parseInt(key.substring(0, key.indexOf('|'))))
                .thenComparing(key -> key.substring(key.indexOf('|') + 1)));
        for (String halfKey : halfKeys) {
            int split = halfKey.indexOf('|');
            int inning = Integer.parseInt(halfKey.substring(0, split));
            String half = halfKey.substring(split + 1);
            Integer recorded = "top".equals(half)
                    ? inningRunCell(awayCells, inning)
                    : inningRunCell(homeCells, inning);
            if (recorded == null) {
                continue;
            }
            int[] counts = countsByHalf.getOrDefault(halfKey, new int[2]);
            if (counts[0] == recorded.intValue()) {
                continue;
            }
            String cause;
            if (counts[0] == 0 && counts[1] == 0) {
                cause = "该半局判定层一笔都没有：逐棒事件从未入库（半局被强制结束，或局中预览没等到定案推送）";
            } else if (counts[0] < recorded.intValue()) {
                cause = "逐棒证据缺一笔（该半局的重建没跑完或事件被覆盖）";
            } else {
                cause = "判定层有记分层没记到的分数";
            }
            violations.add(String.format(
                    "\u00a711.10 第 %d 局%s 判定 %d 笔与该半局记分 %d 分不符，差额 %+d：%s%s",
                    inning, "top".equals(half) ? "上" : "下", counts[0], recorded.intValue(),
                    counts[0] - recorded.intValue(), cause,
                    counts[1] > 0 ? "（另有 " + counts[1] + " 笔被人工作废）" : ""));
        }
    }

    /** 第 N 局那一格；越界或空值返回 null 表示「无资料可判」而不是 0 分。 */
    private static Integer inningRunCell(List<Integer> cells, int inning) {
        return inning <= 0 || inning > cells.size() ? null : cells.get(inning - 1);
    }

    /** 逐局比分 JSON 展开成每局一格（保留空位）；空值/非法内容返回空列表。 */
    private List<Integer> inningRunCells(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<?> raw = objectMapper.readValue(json.trim(), List.class);
            List<Integer> cells = new ArrayList<>(raw.size());
            for (Object cell : raw) {
                if (cell == null) {
                    cells.add(null);
                    continue;
                }
                String text = String.valueOf(cell).trim();
                cells.add(text.isEmpty() ? null : Integer.valueOf(Integer.parseInt(text)));
            }
            return cells;
        }
        catch (JsonProcessingException | NumberFormatException | IllegalStateException e) {
            return List.of();
        }
    }

    private static void appendLineScoreBreakage(String side, int score, Integer lineSum,
            List<String> violations) {
        if (lineSum == null || lineSum == score) {
            return;
        }
        violations.add(String.format("\u00a711.9 %s 總分 %d 與逐局比分加總 %d 不符，差額 %+d", side, score,
                lineSum, score - lineSum));
    }

    /** 逐局比分 JSON 加總；空值/非法內容回傳 null 表示「無資料可判」而不是 0。 */
    private Integer inningRunsSum(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            List<?> raw = objectMapper.readValue(json.trim(), List.class);
            int sum = 0;
            for (Object cell : raw) {
                if (cell == null) {
                    continue;
                }
                String text = String.valueOf(cell).trim();
                if (!text.isEmpty()) {
                    sum += Integer.parseInt(text);
                }
            }
            return sum;
        }
        catch (JsonProcessingException | NumberFormatException | IllegalStateException e) {
            return null;
        }
    }

}
