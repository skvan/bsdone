package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;

/** 统一修订台账：人工统计修改的字段级留痕。 */
@Entity
@Table(name = "bs_game_stat_adjustment")
@Comment("比赛统计统一修订台账")
public class GameStatAdjustment extends BaseEntity {
    @Comment("租户ID")
    private Long tenantId;
    @Comment("比赛ID")
    private Long gameId;
    @Column(length = 32)
    @Comment("PLAYER_STAT")
    private String scope;
    @Column(length = 32)
    @Comment("UPDATE_FIELD")
    private String action;
    @Column(length = 32)
    @Comment("RESULT_SAVE")
    private String sourceType;
    @Comment("源统计行ID")
    private Long sourceId;
    @Comment("球队ID")
    private Long teamId;
    @Comment("球员ID")
    private Long playerId;
    @Comment("是否投手行")
    private Integer isPitcher;
    @Column(length = 64)
    @Comment("统计字段 key")
    private String fieldKey;
    @Column(length = 64)
    @Comment("统计字段中文名")
    private String fieldLabel;
    @Column(length = 128)
    @Comment("修改前值")
    private String beforeValue;
    @Column(length = 128)
    @Comment("修改后值")
    private String afterValue;
    @Column(length = 16)
    @Comment("ACTIVE")
    private String status;
    @Column(length = 200)
    @Comment("备注/原因")
    private String reason;
    @Comment("操作人")
    private Long operatorId;
    @Comment("操作时间")
    private LocalDateTime operatedAt;

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

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getIsPitcher() {
        return isPitcher;
    }

    public void setIsPitcher(Integer isPitcher) {
        this.isPitcher = isPitcher;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
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

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }

    public void setOperatedAt(LocalDateTime operatedAt) {
        this.operatedAt = operatedAt;
    }
}
