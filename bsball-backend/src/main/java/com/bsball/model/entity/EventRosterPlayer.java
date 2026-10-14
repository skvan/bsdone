package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Comment;

/**
 * 秩序册报名球员（电子秩序册，Issue #310）：球队确认参赛后提交的报名名单行。
 * 名单为快照语义：提交时校验球员为该队当前在役；软删 = 移除报名；背号为报名时快照（展示用）。
 */
@Entity
@Table(name = "bs_event_roster_player")
@Comment(value = "秩序册报名球员")
public class EventRosterPlayer
extends BaseEntity {
    @Comment(value = "租户ID")
    @Column(nullable = false)
    private Long tenantId;
    @Comment(value = "赛事ID")
    @Column(nullable = false)
    private Long eventId;
    @Comment(value = "球队ID")
    @Column(nullable = false)
    private Long teamId;
    @Comment(value = "球员ID")
    @Column(nullable = false)
    private Long playerId;
    @Size(max = 64)
    @Comment(value = "报名时背号快照（展示用）")
    private String number;
    @Comment(value = "排序")
    private Integer sort = 0;

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getEventId() {
        return this.eventId;
    }

    public Long getTeamId() {
        return this.teamId;
    }

    public Long getPlayerId() {
        return this.playerId;
    }

    public String getNumber() {
        return this.number;
    }

    public Integer getSort() {
        return this.sort;
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

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
