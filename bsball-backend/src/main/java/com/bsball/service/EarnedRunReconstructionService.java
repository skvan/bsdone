package com.bsball.service;

import com.bsball.model.dto.EarnedRunDecisionListDTO;
import com.bsball.model.dto.PitcherRunTotalsDTO;
import com.bsball.model.dto.PitcherTransferResultDTO;
import com.bsball.model.entity.EarnedRunDecisionEntity;
import com.bsball.model.entity.EarnedRunPlayEntity;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.EarnedRunDecisionRepository;
import com.bsball.repository.EarnedRunPlayRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
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

    private final EarnedRunPlayRepository playRepository;
    private final EarnedRunDecisionRepository decisionRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final PitcherRunOverrideRepository pitcherRunOverrideRepository;
    private final GameFieldingPosStatRepository gameFieldingPosStatRepository;
    private final GameFieldingAdjustmentRepository gameFieldingAdjustmentRepository;
    private final GamePitcherTransferRepository gamePitcherTransferRepository;
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
            PlayerRepository playerRepository,
            ObjectMapper objectMapper) {
        this.playRepository = playRepository;
        this.decisionRepository = decisionRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.pitcherRunOverrideRepository = pitcherRunOverrideRepository;
        this.gameFieldingPosStatRepository = gameFieldingPosStatRepository;
        this.gameFieldingAdjustmentRepository = gameFieldingAdjustmentRepository;
        this.gamePitcherTransferRepository = gamePitcherTransferRepository;
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

    /** 该场是否仍有以该投手为责任投手的自责分判定；删除投手统计行前的守卫。 */
    public boolean hasResponsiblePitcherDecisions(long gameId, long pitcherId) {
        return gameId > 0 && pitcherId > 0
                && decisionRepository.existsByGameIdAndResponsiblePitcherId(
                        Long.valueOf(gameId), Long.valueOf(pitcherId));
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
        mergeFieldingPositions(source, target, sourceFielding, operatorId);
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
    public void deletePitcherTransferSource(long tenantId, long gameId,
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
            return;
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

    private void mergeFieldingPositions(GamePlayerStat source, GamePlayerStat target,
            List<GameFieldingPosStat> sourceFielding, Long operatorId) {
        Map<String, GameFieldingPosStat> targetByPosition = new LinkedHashMap<>();
        for (GameFieldingPosStat row : fieldingRowsOf(this.gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(source.getGameId()),
                target.getTeamId(), target.getPlayerId())) {
            if (row.getPosition() != null) {
                targetByPosition.put(row.getPosition().trim().toUpperCase(), row);
            }
        }
        for (GameFieldingPosStat sourceRow : sourceFielding) {
            String position = sourceRow.getPosition() == null
                    ? "" : sourceRow.getPosition().trim().toUpperCase();
            GameFieldingPosStat targetRow = targetByPosition.get(position);
            if (targetRow == null) {
                sourceRow.setPlayerId(target.getPlayerId());
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
        if (entity.getResponsiblePitcherId() != null
                && this.pitcherRunOverrideRepository.existsByTenantIdAndGameIdAndPlayerIdAndStatus(
                        tenantId, gameId, entity.getResponsiblePitcherId(), OVERRIDE_APPROVED)) {
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
        Map<Long, String> playerNames = loadPlayerNames(decisions);

        return decisions.stream()
                .map(d -> {
                    EarnedRunPlayEntity play = playById.get(d.getPlayId());
                    return new EarnedRunDecisionListDTO(
                            d.getId(),
                            d.getPlayId(),
                            play != null ? play.getInning() : null,
                            play != null ? play.getHalf() : null,
                            play != null ? play.getSequence() : null,
                            d.getRunnerId(),
                            resolvePlayerName(playerNames, d.getRunnerId()),
                            d.getResponsiblePitcherId(),
                            resolvePlayerName(playerNames, d.getResponsiblePitcherId()),
                            d.getReachedBasePitcherId(),
                            resolvePlayerName(playerNames, d.getReachedBasePitcherId()),
                            d.getEarnedStatus(),
                            d.getUnearnedReason(),
                            d.getRunnerOrigin(),
                            d.getOverriddenBy(),
                            d.getOverriddenAt());
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

    /** 批次載入判定列涉及的球員名稱，避免逐列查詢。 */
    private Map<Long, String> loadPlayerNames(List<EarnedRunDecisionEntity> decisions) {
        Set<Long> playerIds = new HashSet<>();
        for (EarnedRunDecisionEntity decision : decisions) {
            addIfPresent(playerIds, decision.getRunnerId());
            addIfPresent(playerIds, decision.getResponsiblePitcherId());
            addIfPresent(playerIds, decision.getReachedBasePitcherId());
        }
        Map<Long, String> playerNames = new HashMap<>();
        if (!playerIds.isEmpty()) {
            for (Player player : playerRepository.findAllById(playerIds)) {
                playerNames.put(player.getId(), player.getName());
            }
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
