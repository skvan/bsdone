package com.bsball.service;

import com.bsball.model.dto.GameRevisionAuditDTO;
import com.bsball.model.entity.ErDecisionAdjustment;
import com.bsball.model.entity.Game;
import com.bsball.model.entity.GameFieldingAdjustment;
import com.bsball.model.entity.GamePitcherTransfer;
import com.bsball.model.entity.PitcherRunOverride;
import com.bsball.model.entity.Player;
import com.bsball.repository.ErDecisionAdjustmentRepository;
import com.bsball.repository.GameFieldingAdjustmentRepository;
import com.bsball.repository.GamePitcherTransferRepository;
import com.bsball.repository.PitcherRunOverrideRepository;
import com.bsball.repository.PlayerRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 汇总该场现有领域审计表，提供只读修订记录时间线。 */
@Service
public class GameRevisionAuditService {
    private final ErDecisionAdjustmentRepository erDecisionAdjustmentRepository;
    private final PitcherRunOverrideRepository pitcherRunOverrideRepository;
    private final GameFieldingAdjustmentRepository fieldingAdjustmentRepository;
    private final GamePitcherTransferRepository pitcherTransferRepository;
    private final PlayerRepository playerRepository;

    public GameRevisionAuditService(
            ErDecisionAdjustmentRepository erDecisionAdjustmentRepository,
            PitcherRunOverrideRepository pitcherRunOverrideRepository,
            GameFieldingAdjustmentRepository fieldingAdjustmentRepository,
            GamePitcherTransferRepository pitcherTransferRepository,
            PlayerRepository playerRepository) {
        this.erDecisionAdjustmentRepository = erDecisionAdjustmentRepository;
        this.pitcherRunOverrideRepository = pitcherRunOverrideRepository;
        this.fieldingAdjustmentRepository = fieldingAdjustmentRepository;
        this.pitcherTransferRepository = pitcherTransferRepository;
        this.playerRepository = playerRepository;
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
