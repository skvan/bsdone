package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "bs_pitcher_run_override")
@Comment("投手 R/ER 人工覆写（赛后复核）")
public class PitcherRunOverride extends BaseEntity {
    @Comment("租户ID")
    private Long tenantId;
    @Comment("比赛ID")
    private Long gameId;
    @Comment("投手球员ID")
    private Long playerId;
    @Comment("责任失分 R（含非自责）")
    private Integer runsAllowed;
    @Comment("自责分 ER")
    private Integer earnedRuns;
    @Column(length = 16)
    @Comment("APPROVED/REVOKED/INVALIDATED")
    private String status;
    @Column(length = 200)
    @Comment("覆写备注")
    private String note;
    @Comment("撤销人")
    private Long revokedBy;
    @Comment("撤销时间")
    private LocalDateTime revokedAt;
    @Column(length = 200)
    @Comment("失效原因")
    private String invalidReason;

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

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Integer getRunsAllowed() {
        return runsAllowed;
    }

    public void setRunsAllowed(Integer runsAllowed) {
        this.runsAllowed = runsAllowed;
    }

    public Integer getEarnedRuns() {
        return earnedRuns;
    }

    public void setEarnedRuns(Integer earnedRuns) {
        this.earnedRuns = earnedRuns;
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

    public String getInvalidReason() {
        return invalidReason;
    }

    public void setInvalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
    }
}
