package com.bsball.model.dto;

import java.time.LocalDateTime;

/** 比赛统一修订记录（聚合 ER 调整/R/ER 覆写/守备补记/投手转记）。 */
public record GameRevisionAuditDTO(
        String auditType,
        Long recordId,
        String action,
        String status,
        String summary,
        String reason,
        Long operatorId,
        LocalDateTime operatedAt) {
}
