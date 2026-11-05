package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "bs_game_pitcher_transfer")
@Comment("投手行转记审计（赛后补录）")
public class GamePitcherTransfer extends BaseEntity {
    @Comment("租户ID")
    private Long tenantId;
    @Comment("比赛ID")
    private Long gameId;
    @Comment("源投手统计行ID")
    private Long sourceStatId;
    @Comment("源球队ID")
    private Long sourceTeamId;
    @Comment("源球员ID")
    private Long sourcePlayerId;
    @Comment("目标投手统计行ID")
    private Long targetStatId;
    @Comment("目标球队ID")
    private Long targetTeamId;
    @Comment("目标球员ID")
    private Long targetPlayerId;
    @Column(columnDefinition = "text")
    @Comment("源行转记前快照 JSON")
    private String sourceBefore;
    @Column(columnDefinition = "text")
    @Comment("目标行转记前快照 JSON")
    private String targetBefore;
    @Column(columnDefinition = "text")
    @Comment("目标行转记后快照 JSON")
    private String targetAfter;
    @Column(length = 16)
    @Comment("TRANSFERRED/DELETED")
    private String status;
    @Column(length = 200)
    @Comment("转记备注")
    private String note;
    @Comment("删除源行操作人")
    private Long sourceDeletedBy;
    @Comment("删除源行时间")
    private LocalDateTime sourceDeletedAt;

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

    public Long getSourceStatId() {
        return sourceStatId;
    }

    public void setSourceStatId(Long sourceStatId) {
        this.sourceStatId = sourceStatId;
    }

    public Long getSourceTeamId() {
        return sourceTeamId;
    }

    public void setSourceTeamId(Long sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
    }

    public Long getSourcePlayerId() {
        return sourcePlayerId;
    }

    public void setSourcePlayerId(Long sourcePlayerId) {
        this.sourcePlayerId = sourcePlayerId;
    }

    public Long getTargetStatId() {
        return targetStatId;
    }

    public void setTargetStatId(Long targetStatId) {
        this.targetStatId = targetStatId;
    }

    public Long getTargetTeamId() {
        return targetTeamId;
    }

    public void setTargetTeamId(Long targetTeamId) {
        this.targetTeamId = targetTeamId;
    }

    public Long getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Long targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public String getSourceBefore() {
        return sourceBefore;
    }

    public void setSourceBefore(String sourceBefore) {
        this.sourceBefore = sourceBefore;
    }

    public String getTargetBefore() {
        return targetBefore;
    }

    public void setTargetBefore(String targetBefore) {
        this.targetBefore = targetBefore;
    }

    public String getTargetAfter() {
        return targetAfter;
    }

    public void setTargetAfter(String targetAfter) {
        this.targetAfter = targetAfter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getSourceDeletedBy() {
        return sourceDeletedBy;
    }

    public void setSourceDeletedBy(Long sourceDeletedBy) {
        this.sourceDeletedBy = sourceDeletedBy;
    }

    public LocalDateTime getSourceDeletedAt() {
        return sourceDeletedAt;
    }

    public void setSourceDeletedAt(LocalDateTime sourceDeletedAt) {
        this.sourceDeletedAt = sourceDeletedAt;
    }
}
