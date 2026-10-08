/*
 * 账号权限重构（批次 3a，评审修复 I2）：联盟主办方精简 DTO。
 *
 * 仅暴露前端所需的最小字段（userId / grantSource / status / createdAt），
 * 刻意不含 tenantId / leagueId / id / createdBy / updatedBy / deletedAt 等审计或租户字段，
 * 避免 GET /league/{id}/owners 回传实体造成的审计字段泄露。
 */
package com.bsball.model.dto;

import java.time.LocalDateTime;

public record LeagueOwnerDto(Long userId, String grantSource, String status, LocalDateTime createdAt) {
}
