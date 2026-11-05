package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;

/**
 * ER 逐分判定人工改派/作废。
 *
 * <p>原始 {@code bs_earned_run_decision} 不删除、不覆盖；本表以
 * {@code source_event_id + runner_id} 为稳定键，半局重建后仍可重新匹配到判定。</p>
 */
@Entity
@Table(name = "bs_er_decision_adjustment")
@Comment("ER 逐分判定人工改派/作废")
public class ErDecisionAdjustment extends BaseEntity {
    @Comment("租户ID")
    private Long tenantId;
    @Comment("比赛ID")
    private Long gameId;
    @Comment("当前关联判定ID（仅定位/审计，重建后可能变化）")
    private Long decisionId;
    @Comment("源 Play 业务ID（稳定键）")
    private String sourceEventId;
    @Comment("跑者ID（稳定键）")
    private Long runnerId;
    @Column(length = 16)
    @Comment("REASSIGN/VOID")
    private String action;
    @Comment("改派目标投手ID；作废为空")
    private Long targetPitcherId;
    @Column(length = 16)
    @Comment("ACTIVE/SUPERSEDED/REVOKED")
    private String status;
    @Column(length = 200)
    @Comment("操作原因")
    private String reason;
    @Comment("撤销人")
    private Long revokedBy;
    @Comment("撤销时间")
    private LocalDateTime revokedAt;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(Long runnerId) {
        this.runnerId = runnerId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getTargetPitcherId() {
        return targetPitcherId;
    }

    public void setTargetPitcherId(Long targetPitcherId) {
        this.targetPitcherId = targetPitcherId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(Long revokedBy) {
        this.revokedBy = revokedBy;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
