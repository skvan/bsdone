package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;

/** 比赛统计基座快照：首次 final 保存时的统计行与逐位置守备，不可变。 */
@Entity
@Table(name = "bs_game_stat_base")
@Comment("比赛统计基座快照")
public class GameStatBase extends BaseEntity {
    @Comment("租户ID")
    private Long tenantId;
    @Comment("比赛ID")
    private Long gameId;
    @Comment("球队ID")
    private Long teamId;
    @Comment("球员ID")
    private Long playerId;
    @Comment("是否投手行")
    private Integer isPitcher;
    @Column(length = 16)
    @Comment("RESULT/LIVE/IMPORT")
    private String source;
    @Column(columnDefinition = "text")
    @Comment("统计行 JSON")
    private String statJson;
    @Column(columnDefinition = "text")
    @Comment("逐位置守备 JSON 数组")
    private String fieldingPosJson;
    @Comment("捕获时间")
    private LocalDateTime capturedAt;

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getStatJson() {
        return statJson;
    }

    public void setStatJson(String statJson) {
        this.statJson = statJson;
    }

    public String getFieldingPosJson() {
        return fieldingPosJson;
    }

    public void setFieldingPosJson(String fieldingPosJson) {
        this.fieldingPosJson = fieldingPosJson;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }
}
