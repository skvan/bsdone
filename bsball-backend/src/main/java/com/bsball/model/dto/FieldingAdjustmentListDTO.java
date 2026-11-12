package com.bsball.model.dto;

import java.time.LocalDateTime;

public record FieldingAdjustmentListDTO(
        Long adjustmentId,
        String sourceEventId,
        Integer inning,
        String half,
        Integer sequence,
        Long teamId,
        Long playerId,
        String playerName,
        String position,
        Integer assistCount,
        String status,
        String sourceText,
        String note,
        Long createdBy,
        LocalDateTime createdAt,
        Long revokedBy,
        LocalDateTime revokedAt,
        String invalidReason,
        Boolean sourceValid) {
}
