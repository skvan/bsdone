/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.StandingGameRowDTO
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class StandingGameRowDTO {
    private Long id;
    private String gameday;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeScore;
    private Integer awayScore;

    @Generated
    public StandingGameRowDTO() {
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public String getGameday() {
        return this.gameday;
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
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setGameday(String gameday) {
        this.gameday = gameday;
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof StandingGameRowDTO)) {
            return false;
        }
        StandingGameRowDTO other = (StandingGameRowDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
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
        String this$gameday = this.getGameday();
        String other$gameday = other.getGameday();
        return !(this$gameday == null ? other$gameday != null : !this$gameday.equals(other$gameday));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof StandingGameRowDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $homeTeamId = this.getHomeTeamId();
        result = result * 59 + ($homeTeamId == null ? 43 : ((Object)$homeTeamId).hashCode());
        Long $awayTeamId = this.getAwayTeamId();
        result = result * 59 + ($awayTeamId == null ? 43 : ((Object)$awayTeamId).hashCode());
        Integer $homeScore = this.getHomeScore();
        result = result * 59 + ($homeScore == null ? 43 : ((Object)$homeScore).hashCode());
        Integer $awayScore = this.getAwayScore();
        result = result * 59 + ($awayScore == null ? 43 : ((Object)$awayScore).hashCode());
        String $gameday = this.getGameday();
        result = result * 59 + ($gameday == null ? 43 : $gameday.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "StandingGameRowDTO(id=" + this.getId() + ", gameday=" + this.getGameday() + ", homeTeamId=" + this.getHomeTeamId() + ", awayTeamId=" + this.getAwayTeamId() + ", homeScore=" + this.getHomeScore() + ", awayScore=" + this.getAwayScore() + ")";
    }
}

