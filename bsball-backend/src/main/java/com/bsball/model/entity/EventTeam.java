package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;

/**
 * 赛事参赛登记（电子秩序册，Issue #310）：
 * 办赛方邀请球队 → 球队确认/谢绝 → 提交报名名单 → 办赛方锁定。
 * 状态机：invited → confirmed / declined；declined 可被重新邀请（重置回 invited）。
 */
@Entity
@Table(name = "bs_event_team")
@Comment(value = "赛事参赛登记")
public class EventTeam
extends BaseEntity {
    public static final String STATUS_INVITED = "invited";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_DECLINED = "declined";

    @Comment(value = "租户ID")
    @Column(nullable = false)
    private Long tenantId;
    @Comment(value = "赛事ID")
    @Column(nullable = false)
    private Long eventId;
    @Comment(value = "球队ID")
    @Column(nullable = false)
    private Long teamId;
    @Comment(value = "状态：invited/confirmed/declined")
    @Column(nullable = false, length = 16)
    private String status = STATUS_INVITED;
    @Comment(value = "邀请人")
    private Long invitedBy;
    @Comment(value = "邀请时间")
    private LocalDateTime invitedAt;
    @Comment(value = "确认时间")
    private LocalDateTime confirmedAt;
    @Comment(value = "谢绝时间")
    private LocalDateTime declinedAt;
    @Comment(value = "最近提交名单时间")
    private LocalDateTime rosterSubmittedAt;
    @Comment(value = "名单锁定标记")
    @Column(nullable = false)
    private Boolean rosterLocked = Boolean.FALSE;
    @Comment(value = "锁定人")
    private Long lockedBy;
    @Comment(value = "锁定时间")
    private LocalDateTime lockedAt;
    @Size(max = 500)
    @Comment(value = "备注")
    private String remark;

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getEventId() {
        return this.eventId;
    }

    public Long getTeamId() {
        return this.teamId;
    }

    public String getStatus() {
        return this.status;
    }

    public Long getInvitedBy() {
        return this.invitedBy;
    }

    public LocalDateTime getInvitedAt() {
        return this.invitedAt;
    }

    public LocalDateTime getConfirmedAt() {
        return this.confirmedAt;
    }

    public LocalDateTime getDeclinedAt() {
        return this.declinedAt;
    }

    public LocalDateTime getRosterSubmittedAt() {
        return this.rosterSubmittedAt;
    }

    public Boolean getRosterLocked() {
        return this.rosterLocked;
    }

    public Long getLockedBy() {
        return this.lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return this.lockedAt;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setInvitedBy(Long invitedBy) {
        this.invitedBy = invitedBy;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public void setDeclinedAt(LocalDateTime declinedAt) {
        this.declinedAt = declinedAt;
    }

    public void setRosterSubmittedAt(LocalDateTime rosterSubmittedAt) {
        this.rosterSubmittedAt = rosterSubmittedAt;
    }

    public void setRosterLocked(Boolean rosterLocked) {
        this.rosterLocked = rosterLocked;
    }

    public void setLockedBy(Long lockedBy) {
        this.lockedBy = lockedBy;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
