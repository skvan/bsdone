package com.bsball.model.dto;

import java.time.LocalDateTime;

/** 统一修订台账行（结果页字段级留痕）。 */
public record GameStatAdjustmentDTO(
        Long id,
        String scope,
        String action,
        String sourceType,
        Long sourceId,
        Long teamId,
        Long playerId,
        String playerName,
        Integer isPitcher,
        String fieldKey,
        String fieldLabel,
        String beforeValue,
        String afterValue,
        String status,
        String reason,
        Long operatorId,
        LocalDateTime operatedAt) {
}
