package com.bsball.service;

import com.bsball.model.dto.GameRevisionAuditDTO;
import com.bsball.model.dto.GameStatAdjustmentDTO;
import com.bsball.model.dto.StatBaseCompareDTO;
import com.bsball.model.entity.ErDecisionAdjustment;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GameFieldingPosStat;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.GamePlayerStat;
import com.bsball.model.entity.GameStatAdjustment;
import com.bsball.model.entity.GameStatBase;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.ErDecisionAdjustmentRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GameFieldingPosStatRepository;
import com.bsball.repository.GamePitcherTransferRepository;
import com.bsball.repository.GamePlayerStatRepository;
import com.bsball.repository.GameStatAdjustmentRepository;
import com.bsball.repository.GameStatBaseRepository;
import com.bsball.repository.PitcherRunOverrideRepository;
import com.bsball.repository.PlayerRepository;
import com.bsball.utils.GamePlayerStatZeros;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** 汇总该场现有领域审计表，提供只读修订记录时间线。 */
@Service
public class GameRevisionAuditService {
    private static final Logger log = LoggerFactory.getLogger(GameRevisionAuditService.class);

    private final ErDecisionAdjustmentRepository erDecisionAdjustmentRepository;
    private final PitcherRunOverrideRepository pitcherRunOverrideRepository;
    private final GameFieldingAdjustmentRepository fieldingAdjustmentRepository;
    private final GamePitcherTransferRepository pitcherTransferRepository;
    private final PlayerRepository playerRepository;
    private final GamePlayerStatRepository gamePlayerStatRepository;
    private final GameFieldingPosStatRepository gameFieldingPosStatRepository;
    private final GameStatBaseRepository gameStatBaseRepository;
    private final GameStatAdjustmentRepository gameStatAdjustmentRepository;
    private final ObjectMapper auditMapper;
    private final TransactionTemplate ledgerTransaction;

    public GameRevisionAuditService(
            ErDecisionAdjustmentRepository erDecisionAdjustmentRepository,
            PitcherRunOverrideRepository pitcherRunOverrideRepository,
            GameFieldingAdjustmentRepository fieldingAdjustmentRepository,
            GamePitcherTransferRepository pitcherTransferRepository,
            PlayerRepository playerRepository,
            GamePlayerStatRepository gamePlayerStatRepository,
            GameFieldingPosStatRepository gameFieldingPosStatRepository,
            GameStatBaseRepository gameStatBaseRepository,
            GameStatAdjustmentRepository gameStatAdjustmentRepository,
            PlatformTransactionManager transactionManager,
            ObjectMapper objectMapper) {
        this.erDecisionAdjustmentRepository = erDecisionAdjustmentRepository;
        this.pitcherRunOverrideRepository = pitcherRunOverrideRepository;
        this.fieldingAdjustmentRepository = fieldingAdjustmentRepository;
        this.pitcherTransferRepository = pitcherTransferRepository;
        this.playerRepository = playerRepository;
        this.gamePlayerStatRepository = gamePlayerStatRepository;
        this.gameFieldingPosStatRepository = gameFieldingPosStatRepository;
        this.gameStatBaseRepository = gameStatBaseRepository;
        this.gameStatAdjustmentRepository = gameStatAdjustmentRepository;
        this.auditMapper = objectMapper.copy().registerModule(new JavaTimeModule());
        this.ledgerTransaction = new TransactionTemplate(transactionManager);
        // 台账是旁路审计：独立事务写入，失败不能回滚结果保存。
        this.ledgerTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 查询该场全部修订记录，按操作时间倒序。 */
    @Transactional(readOnly = true)
    public List<GameRevisionAuditDTO> list(Game game) {
        List<ErDecisionAdjustment> erAdjustments = erDecisionAdjustmentRepository
                .findByTenantIdAndGameIdOrderByIdDesc(game.getTenantId(), game.getId());
        List<PitcherRunOverride> pitcherOverrides = pitcherRunOverrideRepository
                .findByTenantIdAndGameIdOrderByIdDesc(game.getTenantId(), game.getId());
        List<GameFieldingAdjustment> fieldingAdjustments = fieldingAdjustmentRepository
                .findByTenantIdAndGameIdOrderByInningAscTopBottomAscSourceSequenceAscIdAsc(
                        game.getTenantId(), game.getId());
        List<GamePitcherTransfer> transfers = pitcherTransferRepository
                .findByTenantIdAndGameIdOrderByIdDesc(game.getTenantId(), game.getId());
        Map<Long, String> names = loadPlayerNames(
                erAdjustments, pitcherOverrides, fieldingAdjustments, transfers);

        List<GameRevisionAuditDTO> rows = new ArrayList<>();
        for (ErDecisionAdjustment row : erAdjustments) {
            rows.add(toDto(row, names));
        }
        for (PitcherRunOverride row : pitcherOverrides) {
            rows.add(toDto(row, names));
        }
        for (GameFieldingAdjustment row : fieldingAdjustments) {
            rows.add(toDto(row, names));
        }
        for (GamePitcherTransfer row : transfers) {
            rows.add(toDto(row, names));
        }
        rows.sort(Comparator
                .comparing(GameRevisionAuditDTO::operatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(GameRevisionAuditDTO::recordId,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    private static GameRevisionAuditDTO toDto(ErDecisionAdjustment row, Map<Long, String> names) {
        return new GameRevisionAuditDTO(
                "ER_DECISION",
                row.getId(),
                row.getAction(),
                row.getStatus(),
                erSummary(row, names),
                row.getReason(),
                operatorId(row.getRevokedBy(), row.getUpdatedBy(), row.getCreatedBy()),
                operatedAt(row.getRevokedAt(), row.getUpdatedAt(), row.getCreatedAt()));
    }

    private static GameRevisionAuditDTO toDto(PitcherRunOverride row, Map<Long, String> names) {
        return new GameRevisionAuditDTO(
                "PITCHER_RUN_OVERRIDE",
                row.getId(),
                auditAction(row.getStatus(), "OVERRIDE"),
                row.getStatus(),
                "投手 R/ER 覆写：" + playerName(names, row.getPlayerId())
                        + " R " + value(row.getRunsAllowed())
                        + " / ER " + value(row.getEarnedRuns()),
                auditReason(row.getNote(), row.getInvalidReason(), row.getStatus()),
                operatorId(row.getRevokedBy(), row.getUpdatedBy(), row.getCreatedBy()),
                operatedAt(row.getRevokedAt(), row.getUpdatedAt(), row.getCreatedAt()));
    }

    private static GameRevisionAuditDTO toDto(GameFieldingAdjustment row, Map<Long, String> names) {
        return new GameRevisionAuditDTO(
                "FIELDING_ADJUSTMENT",
                row.getId(),
                auditAction(row.getStatus(), "APPROVE"),
                row.getStatus(),
                "守备补记：" + playerName(names, row.getPlayerId())
                        + " · " + value(row.getPosition())
                        + " A +" + value(row.getAssistCount())
                        + "（" + inningLabel(row) + "）",
                auditReason(row.getNote(), row.getInvalidReason(), row.getStatus()),
                operatorId(row.getRevokedBy(), row.getUpdatedBy(), row.getCreatedBy()),
                operatedAt(row.getRevokedAt(), row.getUpdatedAt(), row.getCreatedAt()));
    }

    private static GameRevisionAuditDTO toDto(GamePitcherTransfer row, Map<Long, String> names) {
        return new GameRevisionAuditDTO(
                "PITCHER_TRANSFER",
                row.getId(),
                "DELETED".equals(row.getStatus()) ? "DELETE_SOURCE" : "TRANSFER",
                row.getStatus(),
                "投手行转记：" + playerName(names, row.getSourcePlayerId())
                        + " → " + playerName(names, row.getTargetPlayerId()),
                row.getNote(),
                operatorId(row.getSourceDeletedBy(), row.getUpdatedBy(), row.getCreatedBy()),
                operatedAt(row.getSourceDeletedAt(), row.getUpdatedAt(), row.getCreatedAt()));
    }

    /** 结果页单字段变更，供统一台账记录。 */
    public record StatChange(
            Long teamId,
            Long playerId,
            Integer isPitcher,
            Long sourceId,
            String fieldKey,
            String fieldLabel,
            double beforeValue,
            double afterValue) {
    }

    /** 把结果页字段级修改写入统一台账；失败不阻断结果保存。 */
    public void recordStatChanges(Game game, List<StatChange> changes, Long operatorId) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<GameStatAdjustment> rows = new ArrayList<>();
        for (StatChange change : changes) {
            if (change == null || change.playerId() == null || change.fieldKey() == null) {
                continue;
            }
            GameStatAdjustment row = new GameStatAdjustment();
            row.setTenantId(game.getTenantId());
            row.setGameId(game.getId());
            row.setScope("PLAYER_STAT");
            row.setAction("UPDATE_FIELD");
            row.setSourceType("RESULT_SAVE");
            row.setSourceId(change.sourceId());
            row.setTeamId(change.teamId());
            row.setPlayerId(change.playerId());
            row.setIsPitcher(change.isPitcher());
            row.setFieldKey(change.fieldKey());
            row.setFieldLabel(change.fieldLabel());
            row.setBeforeValue(formatNumber(change.beforeValue()));
            row.setAfterValue(formatNumber(change.afterValue()));
            row.setStatus("ACTIVE");
            row.setOperatorId(operatorId);
            row.setOperatedAt(now);
            if (operatorId != null) {
                row.setCreatedBy(operatorId);
                row.setUpdatedBy(operatorId);
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            return;
        }
        try {
            ledgerTransaction.executeWithoutResult(
                    status -> gameStatAdjustmentRepository.saveAll(rows));
        } catch (RuntimeException error) {
            log.warn("统一修订台账写入失败 gameId={} count={}: {}",
                    game.getId(), rows.size(), error.getMessage());
        }
    }

    /** 统一修订台账（结果页字段级留痕），按时间倒序。 */
    @Transactional(readOnly = true)
    public List<GameStatAdjustmentDTO> listAdjustments(Game game) {
        List<GameStatAdjustment> rows = gameStatAdjustmentRepository
                .findByTenantIdAndGameIdOrderByOperatedAtDescIdDesc(
                        game.getTenantId(), game.getId());
        Set<Long> playerIds = new HashSet<>();
        for (GameStatAdjustment row : rows) {
            addId(playerIds, row.getPlayerId());
        }
        Map<Long, String> names = loadPlayerNames(playerIds);
        List<GameStatAdjustmentDTO> result = new ArrayList<>(rows.size());
        for (GameStatAdjustment row : rows) {
            String name = names.get(row.getPlayerId());
            result.add(new GameStatAdjustmentDTO(
                    row.getId(),
                    row.getScope(),
                    row.getAction(),
                    row.getSourceType(),
                    row.getSourceId(),
                    row.getTeamId(),
                    row.getPlayerId(),
                    name != null && !name.isBlank() ? name : "#" + row.getPlayerId(),
                    row.getIsPitcher(),
                    row.getFieldKey(),
                    row.getFieldLabel(),
                    row.getBeforeValue(),
                    row.getAfterValue(),
                    row.getStatus(),
                    row.getReason(),
                    row.getOperatorId(),
                    row.getOperatedAt()));
        }
        return result;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /** 首次 final 保存时写入基座；已有基座的球员不覆盖。 */
    @Transactional
    public void captureBase(Game game, Long operatorId, String source) {
        if (!"final".equalsIgnoreCase(game.getStatus())) {
            return;
        }
        Set<String> existingKeys = new HashSet<>();
        for (GameStatBase row : loadBases(game)) {
            existingKeys.add(playerKey(row.getPlayerId(), row.getIsPitcher()));
        }
        List<GameFieldingPosStat> fieldingRows = gameFieldingPosStatRepository
                .findByGameIdOrderByTeamIdAscPlayerIdAscIdAsc(game.getId());
        for (GamePlayerStat stat : tenantStats(game)) {
            if (stat.getPlayerId() == null) {
                continue;
            }
            String key = playerKey(stat.getPlayerId(), stat.getIsPitcher());
            if (existingKeys.contains(key)) {
                continue;
            }
            List<GameFieldingPosStat> playerFielding = fieldingRows.stream()
                    .filter(row -> Objects.equals(row.getTeamId(), stat.getTeamId())
                            && Objects.equals(row.getPlayerId(), stat.getPlayerId()))
                    .toList();
            GameStatBase row = new GameStatBase();
            row.setTenantId(game.getTenantId());
            row.setGameId(game.getId());
            row.setTeamId(stat.getTeamId());
            row.setPlayerId(stat.getPlayerId());
            row.setIsPitcher(stat.getIsPitcher());
            row.setSource(source == null || source.isBlank() ? "RESULT" : source);
            row.setCapturedAt(LocalDateTime.now());
            if (operatorId != null) {
                row.setCreatedBy(operatorId);
                row.setUpdatedBy(operatorId);
            }
            try {
                row.setStatJson(this.auditMapper.writeValueAsString(stat));
                row.setFieldingPosJson(this.auditMapper.writeValueAsString(playerFielding));
            } catch (JsonProcessingException error) {
                log.warn("基座快照序列化失败 gameId={} playerId={}: {}",
                        game.getId(), stat.getPlayerId(), error.getMessage());
                continue;
            }
            gameStatBaseRepository.save(row);
            existingKeys.add(key);
        }
    }

    /** 基座与当前有效值逐字段对比；无基座时返回 legacy=true。 */
    @Transactional(readOnly = true)
    public StatBaseCompareDTO compareBase(Game game) {
        List<GameStatBase> bases = loadBases(game);
        if (bases.isEmpty()) {
            return new StatBaseCompareDTO(true, List.of());
        }
        Map<String, GamePlayerStat> currentByKey = new HashMap<>();
        for (GamePlayerStat stat : tenantStats(game)) {
            currentByKey.put(playerKey(stat.getPlayerId(), stat.getIsPitcher()), stat);
        }
        Set<Long> playerIds = new HashSet<>();
        for (GameStatBase base : bases) {
            addId(playerIds, base.getPlayerId());
        }
        Map<Long, String> names = loadPlayerNames(playerIds);

        List<StatBaseCompareDTO.PlayerDiff> rows = new ArrayList<>();
        for (GameStatBase base : bases) {
            GamePlayerStat baseStat = parseStatJson(game.getId(), base);
            if (baseStat == null) {
                continue;
            }
            GamePlayerStat effective = currentByKey.getOrDefault(
                    playerKey(base.getPlayerId(), base.getIsPitcher()), new GamePlayerStat());
            List<StatBaseCompareDTO.FieldDiff> fields = diffFields(baseStat, effective);
            if (fields.isEmpty()) {
                continue;
            }
            rows.add(new StatBaseCompareDTO.PlayerDiff(
                    base.getPlayerId(),
                    playerName(names, base.getPlayerId()),
                    base.getTeamId(),
                    base.getIsPitcher(),
                    base.getSource(),
                    base.getCapturedAt(),
                    fields));
        }
        rows.sort(Comparator
                .comparing(StatBaseCompareDTO.PlayerDiff::playerId,
                        Comparator.nullsLast(Long::compareTo))
                .thenComparing(StatBaseCompareDTO.PlayerDiff::isPitcher,
                        Comparator.nullsLast(Integer::compareTo)));
        return new StatBaseCompareDTO(false, rows);
    }

    private List<GameStatBase> loadBases(Game game) {
        return gameStatBaseRepository
                .findByTenantIdAndGameIdOrderByPlayerIdAscIsPitcherAsc(
                        game.getTenantId(), game.getId());
    }

    /** 该场当前统计行（按租户隔离）。 */
    private List<GamePlayerStat> tenantStats(Game game) {
        return gamePlayerStatRepository.findByGameId(game.getId()).stream()
                .filter(stat -> Objects.equals(stat.getTenantId(), game.getTenantId()))
                .toList();
    }

    /** 解析基座统计行 JSON；失败时记录告警并返回 null。 */
    private GamePlayerStat parseStatJson(Long gameId, GameStatBase base) {
        try {
            return this.auditMapper.readValue(base.getStatJson(), GamePlayerStat.class);
        } catch (JsonProcessingException | IllegalArgumentException error) {
            log.warn("基座快照解析失败 gameId={} playerId={}: {}",
                    gameId, base.getPlayerId(), error.getMessage());
            return null;
        }
    }

    /** 基座与有效值不同的统计字段；无差异时返回空列表。 */
    private static List<StatBaseCompareDTO.FieldDiff> diffFields(
            GamePlayerStat baseStat, GamePlayerStat effective) {
        Map<String, Double> effectiveValues = new HashMap<>();
        for (GamePlayerStatZeros.FieldValue value : GamePlayerStatZeros.values(effective)) {
            effectiveValues.put(value.key(), value.value());
        }
        List<StatBaseCompareDTO.FieldDiff> fields = new ArrayList<>();
        for (GamePlayerStatZeros.FieldValue baseValue : GamePlayerStatZeros.values(baseStat)) {
            double effectiveValue = effectiveValues.getOrDefault(baseValue.key(), 0d);
            if (Double.compare(baseValue.value(), effectiveValue) != 0) {
                fields.add(new StatBaseCompareDTO.FieldDiff(
                        baseValue.key(), baseValue.label(), baseValue.value(), effectiveValue));
            }
        }
        return fields;
    }

    private static String playerKey(Long playerId, Integer isPitcher) {
        return playerId + ":" + Objects.requireNonNullElse(isPitcher, 0);
    }

    private Map<Long, String> loadPlayerNames(
            List<ErDecisionAdjustment> erAdjustments,
            List<PitcherRunOverride> pitcherOverrides,
            List<GameFieldingAdjustment> fieldingAdjustments,
            List<GamePitcherTransfer> transfers) {
        Set<Long> ids = new HashSet<>();
        for (ErDecisionAdjustment row : erAdjustments) {
            addId(ids, row.getRunnerId());
            addId(ids, row.getTargetPitcherId());
        }
        for (PitcherRunOverride row : pitcherOverrides) {
            addId(ids, row.getPlayerId());
        }
        for (GameFieldingAdjustment row : fieldingAdjustments) {
            addId(ids, row.getPlayerId());
        }
        for (GamePitcherTransfer row : transfers) {
            addId(ids, row.getSourcePlayerId());
            addId(ids, row.getTargetPlayerId());
        }
        return loadPlayerNames(ids);
    }

    private Map<Long, String> loadPlayerNames(Set<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) {
            return names;
        }
        for (Player player : playerRepository.findAllById(ids)) {
            names.put(player.getId(), player.getName());
        }
        return names;
    }

    private static void addId(Set<Long> ids, Long id) {
        if (id != null) {
            ids.add(id);
        }
    }

    private static String erSummary(ErDecisionAdjustment row, Map<Long, String> names) {
        String runner = playerName(names, row.getRunnerId());
        if ("VOID".equals(row.getAction())) {
            return "ER 判定作废：跑者 " + runner;
        }
        return "ER 判定改派：跑者 " + runner + " → " + playerName(names, row.getTargetPitcherId());
    }

    private static String auditAction(String status, String approvedAction) {
        if ("APPROVED".equals(status)) return approvedAction;
        if ("INVALIDATED".equals(status)) return "INVALIDATE";
        return "REVOKE";
    }

    private static String inningLabel(GameFieldingAdjustment row) {
        String half = "bottom".equalsIgnoreCase(row.getTopBottom()) ? "下" : "上";
        return value(row.getInning()) + half + " #" + value(row.getSourceSequence());
    }

    private static String playerName(Map<Long, String> names, Long playerId) {
        if (playerId == null) return "—";
        String name = names.get(playerId);
        return name != null && !name.isBlank() ? name : "#" + playerId;
    }

    private static Long operatorId(Long preferred, Long updatedBy, Long createdBy) {
        if (preferred != null) return preferred;
        if (updatedBy != null) return updatedBy;
        return createdBy;
    }

    private static LocalDateTime operatedAt(
            LocalDateTime preferred, LocalDateTime updatedAt, LocalDateTime createdAt) {
        if (preferred != null) return preferred;
        return updatedAt != null ? updatedAt : createdAt;
    }

    private static String auditReason(String note, String invalidReason, String status) {
        if ("INVALIDATED".equals(status) && invalidReason != null && !invalidReason.isBlank()) {
            return invalidReason;
        }
        return firstNonBlank(note, invalidReason);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }

    private static int value(Integer value) {
        return Objects.requireNonNullElse(value, 0);
    }

    private static String value(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
