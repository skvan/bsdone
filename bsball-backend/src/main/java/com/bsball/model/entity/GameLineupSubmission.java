package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 赛前阵容提交（电子打序单）：一场比赛每队一条。
 * <p>只承载球队负责人赛前编辑的先发打线/守位/替补/先发投手；办赛方开始录入后，
 * 以 {@code bs_game.live_snapshot_json} 为权威数据。</p>
 */
@Entity
@Table(name = "bs_game_lineup_submission")
@Comment(value = "赛前阵容提交（电子打序单）")
public class GameLineupSubmission extends BaseEntity {
    @Comment(value = "租户ID")
    @Column(nullable = false)
    private Long tenantId;

    @Comment(value = "赛事ID")
    @Column(nullable = false)
    private Long eventId;

    @Comment(value = "比赛ID")
    @Column(nullable = false)
    private Long gameId;

    @Comment(value = "球队ID")
    @Column(nullable = false)
    private Long teamId;

    @Comment(value = "本队赛前阵容 JSON")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lineupJson;

    @Comment(value = "前端标记阵容是否完整（展示用）")
    @Column(nullable = false)
    private Boolean complete = Boolean.FALSE;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getLineupJson() {
        return lineupJson;
    }

    public void setLineupJson(String lineupJson) {
        this.lineupJson = lineupJson;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }
}
