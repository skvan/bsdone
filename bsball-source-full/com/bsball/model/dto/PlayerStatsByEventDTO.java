/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PlayerStatsByEventDTO
 *  lombok.Generated
 */
package com.bsball.model.dto;

import java.util.Map;
import lombok.Generated;

public class PlayerStatsByEventDTO {
    private Long eventId;
    private String eventName;
    private String season;
    private String teamNames;
    private Map<String, Object> batting;
    private Map<String, Object> pitching;
    private Map<String, Object> fielding;

    @Generated
    public PlayerStatsByEventDTO() {
    }

    @Generated
    public Long getEventId() {
        return this.eventId;
    }

    @Generated
    public String getEventName() {
        return this.eventName;
    }

    @Generated
    public String getSeason() {
        return this.season;
    }

    @Generated
    public String getTeamNames() {
        return this.teamNames;
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
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    @Generated
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Generated
    public void setSeason(String season) {
        this.season = season;
    }

    @Generated
    public void setTeamNames(String teamNames) {
        this.teamNames = teamNames;
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
        if (!(o instanceof PlayerStatsByEventDTO)) {
            return false;
        }
        PlayerStatsByEventDTO other = (PlayerStatsByEventDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$eventId = this.getEventId();
        Long other$eventId = other.getEventId();
        if (this$eventId == null ? other$eventId != null : !((Object)this$eventId).equals(other$eventId)) {
            return false;
        }
        String this$eventName = this.getEventName();
        String other$eventName = other.getEventName();
        if (this$eventName == null ? other$eventName != null : !this$eventName.equals(other$eventName)) {
            return false;
        }
        String this$season = this.getSeason();
        String other$season = other.getSeason();
        if (this$season == null ? other$season != null : !this$season.equals(other$season)) {
            return false;
        }
        String this$teamNames = this.getTeamNames();
        String other$teamNames = other.getTeamNames();
        if (this$teamNames == null ? other$teamNames != null : !this$teamNames.equals(other$teamNames)) {
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
        return other instanceof PlayerStatsByEventDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $eventId = this.getEventId();
        result = result * 59 + ($eventId == null ? 43 : ((Object)$eventId).hashCode());
        String $eventName = this.getEventName();
        result = result * 59 + ($eventName == null ? 43 : $eventName.hashCode());
        String $season = this.getSeason();
        result = result * 59 + ($season == null ? 43 : $season.hashCode());
        String $teamNames = this.getTeamNames();
        result = result * 59 + ($teamNames == null ? 43 : $teamNames.hashCode());
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
        return "PlayerStatsByEventDTO(eventId=" + this.getEventId() + ", eventName=" + this.getEventName() + ", season=" + this.getSeason() + ", teamNames=" + this.getTeamNames() + ", batting=" + String.valueOf(this.getBatting()) + ", pitching=" + String.valueOf(this.getPitching()) + ", fielding=" + String.valueOf(this.getFielding()) + ")";
    }
}

