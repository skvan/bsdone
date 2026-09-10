/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PlayerGameLogEntryDTO
 *  lombok.Generated
 */
package com.bsball.model.dto;

import java.util.Map;
import lombok.Generated;

public class PlayerGameLogEntryDTO {
    private Long gameId;
    private Long eventId;
    private String gameday;
    private String eventName;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private Long teamId;
    private String teamName;
    private Boolean isHome;
    private Map<String, Object> batting;
    private Map<String, Object> pitching;
    private Map<String, Object> fielding;

    @Generated
    public PlayerGameLogEntryDTO() {
    }

    @Generated
    public Long getGameId() {
        return this.gameId;
    }

    @Generated
    public Long getEventId() {
        return this.eventId;
    }

    @Generated
    public String getGameday() {
        return this.gameday;
    }

    @Generated
    public String getEventName() {
        return this.eventName;
    }

    @Generated
    public Long getHomeTeamId() {
        return this.homeTeamId;
    }

    @Generated
    public Long getAwayTeamId() {
        return this.awayTeamId;
    }

    @Generated
    public Integer getHomeScore() {
        return this.homeScore;
    }

    @Generated
    public Integer getAwayScore() {
        return this.awayScore;
    }

    @Generated
    public Long getTeamId() {
        return this.teamId;
    }

    @Generated
    public String getTeamName() {
        return this.teamName;
    }

    @Generated
    public Boolean getIsHome() {
        return this.isHome;
    }

    @Generated
    public Map<String, Object> getBatting() {
        return this.batting;
    }

    @Generated
    public Map<String, Object> getPitching() {
        return this.pitching;
    }

    @Generated
    public Map<String, Object> getFielding() {
        return this.fielding;
    }

    @Generated
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    @Generated
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    @Generated
    public void setGameday(String gameday) {
        this.gameday = gameday;
    }

    @Generated
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Generated
    public void setHomeTeamId(Long homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    @Generated
    public void setAwayTeamId(Long awayTeamId) {
        this.awayTeamId = awayTeamId;
    }

    @Generated
    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    @Generated
    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    @Generated
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Generated
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    @Generated
    public void setIsHome(Boolean isHome) {
        this.isHome = isHome;
    }

    @Generated
    public void setBatting(Map<String, Object> batting) {
        this.batting = batting;
    }

    @Generated
    public void setPitching(Map<String, Object> pitching) {
        this.pitching = pitching;
    }

    @Generated
    public void setFielding(Map<String, Object> fielding) {
        this.fielding = fielding;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlayerGameLogEntryDTO)) {
            return false;
        }
        PlayerGameLogEntryDTO other = (PlayerGameLogEntryDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$gameId = this.getGameId();
        Long other$gameId = other.getGameId();
        if (this$gameId == null ? other$gameId != null : !((Object)this$gameId).equals(other$gameId)) {
            return false;
        }
        Long this$eventId = this.getEventId();
        Long other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !((Object)this$eventId).equals(other$eventId)) {
            return false;
        }
        Long this$homeTeamId = this.getHomeTeamId();
        Long other$homeTeamId = other.getHomeTeamId();
        if (this$homeTeamId == null ? other$homeTeamId != null : !((Object)this$homeTeamId).equals(other$homeTeamId)) {
            return false;
        }
        Long this$awayTeamId = this.getAwayTeamId();
        Long other$awayTeamId = other.getAwayTeamId();
        if (this$awayTeamId == null ? other$awayTeamId != null : !((Object)this$awayTeamId).equals(other$awayTeamId)) {
            return false;
        }
        Integer this$homeScore = this.getHomeScore();
        Integer other$homeScore = other.getHomeScore();
        if (this$homeScore == null ? other$homeScore != null : !((Object)this$homeScore).equals(other$homeScore)) {
            return false;
        }
        Integer this$awayScore = this.getAwayScore();
        Integer other$awayScore = other.getAwayScore();
        if (this$awayScore == null ? other$awayScore != null : !((Object)this$awayScore).equals(other$awayScore)) {
            return false;
        }
        Long this$teamId = this.getTeamId();
        Long other$teamId = other.getTeamId();
        if (this$teamId == null ? other$teamId != null : !((Object)this$teamId).equals(other$teamId)) {
            return false;
        }
        Boolean this$isHome = this.getIsHome();
        Boolean other$isHome = other.getIsHome();
        if (this$isHome == null ? other$isHome != null : !((Object)this$isHome).equals(other$isHome)) {
            return false;
        }
        String this$gameday = this.getGameday();
        String other$gameday = other.getGameday();
        if (this$gameday == null ? other$gameday != null : !this$gameday.equals(other$gameday)) {
            return false;
        }
        String this$eventName = this.getEventName();
        String other$eventName = other.getEventName();
        if (this$eventName == null ? other$eventName != null : !this$eventName.equals(other$eventName)) {
            return false;
        }
        String this$teamName = this.getTeamName();
        String other$teamName = other.getTeamName();
        if (this$teamName == null ? other$teamName != null : !this$teamName.equals(other$teamName)) {
            return false;
        }
        Map this$batting = this.getBatting();
        Map other$batting = other.getBatting();
        if (this$batting == null ? other$batting != null : !((Object)this$batting).equals(other$batting)) {
            return false;
        }
        Map this$pitching = this.getPitching();
        Map other$pitching = other.getPitching();
        if (this$pitching == null ? other$pitching != null : !((Object)this$pitching).equals(other$pitching)) {
            return false;
        }
        Map this$fielding = this.getFielding();
        Map other$fielding = other.getFielding();
        return !(this$fielding == null ? other$fielding != null : !((Object)this$fielding).equals(other$fielding));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PlayerGameLogEntryDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $gameId = this.getGameId();
        result = result * 59 + ($gameId == null ? 43 : ((Object)$gameId).hashCode());
        Long $eventId = this.getEventId();
        result = result * 59 + ($eventId == null ? 43 : ((Object)$eventId).hashCode());
        Long $homeTeamId = this.getHomeTeamId();
        result = result * 59 + ($homeTeamId == null ? 43 : ((Object)$homeTeamId).hashCode());
        Long $awayTeamId = this.getAwayTeamId();
        result = result * 59 + ($awayTeamId == null ? 43 : ((Object)$awayTeamId).hashCode());
        Integer $homeScore = this.getHomeScore();
        result = result * 59 + ($homeScore == null ? 43 : ((Object)$homeScore).hashCode());
        Integer $awayScore = this.getAwayScore();
        result = result * 59 + ($awayScore == null ? 43 : ((Object)$awayScore).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Boolean $isHome = this.getIsHome();
        result = result * 59 + ($isHome == null ? 43 : ((Object)$isHome).hashCode());
        String $gameday = this.getGameday();
        result = result * 59 + ($gameday == null ? 43 : $gameday.hashCode());
        String $eventName = this.getEventName();
        result = result * 59 + ($eventName == null ? 43 : $eventName.hashCode());
        String $teamName = this.getTeamName();
        result = result * 59 + ($teamName == null ? 43 : $teamName.hashCode());
        Map $batting = this.getBatting();
        result = result * 59 + ($batting == null ? 43 : ((Object)$batting).hashCode());
        Map $pitching = this.getPitching();
        result = result * 59 + ($pitching == null ? 43 : ((Object)$pitching).hashCode());
        Map $fielding = this.getFielding();
        result = result * 59 + ($fielding == null ? 43 : ((Object)$fielding).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "PlayerGameLogEntryDTO(gameId=" + this.getGameId() + ", eventId=" + this.getEventId() + ", gameday=" + this.getGameday() + ", eventName=" + this.getEventName() + ", homeTeamId=" + this.getHomeTeamId() + ", awayTeamId=" + this.getAwayTeamId() + ", homeScore=" + this.getHomeScore() + ", awayScore=" + this.getAwayScore() + ", teamId=" + this.getTeamId() + ", teamName=" + this.getTeamName() + ", isHome=" + this.getIsHome() + ", batting=" + String.valueOf(this.getBatting()) + ", pitching=" + String.valueOf(this.getPitching()) + ", fielding=" + String.valueOf(this.getFielding()) + ")";
    }
}

