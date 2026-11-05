package com.bsball.service;

import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.dto.PitcherRunTotalsDTO;
import com.bsball.model.dto.IdentityTransferPreviewDTO;
import com.bsball.model.dto.IdentityTransferResultDTO;
import com.bsball.model.dto.PitcherTransferResultDTO;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.ErDecisionAdjustment;
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
import com.bsball.repository.PitcherRunOverrideRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.stats.earnedrun.EarnedRunDecision;
import com.bsball.stats.earnedrun.EarnedRunPlay;
import com.bsball.stats.earnedrun.EarnedRunReconstructionEngine;
import com.bsball.stats.earnedrun.EarnedRunReconstructionResult;
import com.bsball.stats.earnedrun.EarnedRunStatus;
import com.bsball.stats.earnedrun.ScoringRunner;
import com.bsball.utils.GamePlayerStatZeros;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final Set<String> DEFAULT_IDENTITY_SCOPES =
            Set.of(SCOPE_STATS, SCOPE_FIELDING, SCOPE_ER_DECISIONS);
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
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
        // 审计快照包含 BaseEntity 的 LocalDateTime；项目 ObjectMapper 未注册 JavaTimeModule，使用副本注册。
        this.auditMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        this.engine = new EarnedRunReconstructionEngine();
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
        return result.withPitcherAbsoluteTotals(absoluteTotals);
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
            applyPitcherTotals(stat, new int[]{runs, earned, Math.max(0, runs - earned), 0});
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
        List<GamePlayerStat> sourceStats = playerStatsOf(tenantId, gameId, sourcePlayerId);
        List<GamePlayerStat> targetStats = playerStatsOf(tenantId, gameId, targetPlayerId);
        List<GameFieldingPosStat> sourceFielding = playerFieldingOf(gameId, sourcePlayerId);
        List<GameFieldingPosStat> targetFielding = playerFieldingOf(gameId, targetPlayerId);
        Long sourceTeamId = firstTeamId(sourceStats, sourceFielding);
        Long targetTeamId = firstTeamId(targetStats, targetFielding);
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
        int decisionCount = countResponsiblePitcherDecisions(gameId, sourcePlayerId);
        if (sourceFielding.isEmpty() && decisionCount == 0
                && sourceStats.stream().allMatch(GamePlayerStatZeros::isZero)) {
            blockers.add("源球员没有可转记的数据");
        }
        return new IdentityTransferPreviewDTO(
                sourcePlayerId, playerName(players, sourcePlayerId),
                targetPlayerId, playerName(players, targetPlayerId),
                sourceTeamId != null ? sourceTeamId : targetTeamId,
                blockers.isEmpty(), blockers, rows, positions,
                decisionCount, sourceOverride, targetOverride);
    }

    /** 整名球员身份转记：统计、逐位置守备、ER 判定与 R/ER 覆写。 */
    @Transactional
    public IdentityTransferResultDTO transferIdentity(
            long tenantId, long gameId, long sourcePlayerId, long targetPlayerId,
            List<String> scopes, String note, long operatorId) {
        validateIdentityTransferIds(tenantId, gameId, sourcePlayerId, targetPlayerId);
        if (operatorId <= 0) {
            throw new IllegalArgumentException("操作人必须为正数");
        }
        IdentityTransferPreviewDTO preview = previewIdentityTransfer(
                tenantId, gameId, sourcePlayerId, targetPlayerId);
        if (!preview.transferable()) {
            throw new IllegalArgumentException(String.join("；", preview.blockers()));
        }
        Set<String> scopeSet = normalizeIdentityScopes(scopes);
        String sourceBefore = identitySnapshot(tenantId, gameId, sourcePlayerId);
        String targetBefore = identitySnapshot(tenantId, gameId, targetPlayerId);

        Long teamId = preview.teamId();
        if (scopeSet.contains(SCOPE_STATS)) {
            moveIdentityStats(tenantId, gameId, sourcePlayerId, targetPlayerId, operatorId);
        }
        if (scopeSet.contains(SCOPE_FIELDING)) {
            List<GameFieldingPosStat> sourceFielding =
                    playerFieldingOf(gameId, sourcePlayerId);
            mergeFieldingPositions(gameId, teamId, targetPlayerId, sourceFielding, operatorId);
        }

        boolean recompute = false;
        if (scopeSet.contains(SCOPE_ER_DECISIONS)) {
            recompute = moveIdentityOverride(
                    tenantId, gameId, sourcePlayerId, targetPlayerId, operatorId);
            moveIdentityDecisions(tenantId, gameId, sourcePlayerId, targetPlayerId, operatorId);
            recompute = true;
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

    /** 转记成功后单独删除源球员数据；必须全零且无守备/判定/覆写。 */
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
            if (!DEFAULT_IDENTITY_SCOPES.contains(normalized)) {
                throw new IllegalArgumentException("不支持的转记范围：" + scope);
            }
            result.add(normalized);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个转记范围");
        }
        return result;
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
            applyPitcherTotals(stat, overrideTotals(override));
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

    private int[] overrideTotals(PitcherRunOverride override) {
        int runs = valueOrZero(override.getRunsAllowed());
        int earned = valueOrZero(override.getEarnedRuns());
        return new int[]{runs, earned, Math.max(0, runs - earned), 0};
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
        Map<Long, String> sourceEventByPlayId = sourceEventByPlayId(tenantId, gameId);
        Map<DecisionKey, ErDecisionAdjustment> adjustments =
                activeAdjustmentsByKey(activeAdjustments(tenantId, gameId));
        Map<Long, int[]> totals = new HashMap<>();
        for (EarnedRunDecisionEntity decision : decisionRepository
                .findByTenantIdAndGameId(tenantId, gameId)) {
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
        return totals;
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
        int[] effective = override != null ? overrideTotals(override) : engine;
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
        return new PitcherRunTotalsDTO(
                playerId,
                player != null ? player.getName() : null,
                stat != null ? stat.getTeamId() : null,
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
     * 規範 §11：R=ER+UER+PENDING、ER≤R、UER≥0、不重複判定、責任投手非空。
     */
    public List<String> validateSettlement(long tenantId, long gameId) {
        List<String> violations = new ArrayList<>();

        // 1-3: per-pitcher aggregated checks
        for (GamePlayerStat p : gamePlayerStatRepository.findByGameId(gameId)) {
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

        return violations;
    }

}
