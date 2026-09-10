/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.SaveGameResultDTO
 *  com.bsball.model.dto.SaveGameResultDTO$GamePart
 *  com.bsball.model.dto.SaveGameResultDTO$StatPart
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.common.BoolToIntDeserializer;
import com.bsball.model.dto.SaveGameResultDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonDeserializeAs;
import java.util.List;
import lombok.Generated;

public class SaveGameResultDTO {
    private GamePart game;
    private List<StatPart> stats;

    @Generated
    public SaveGameResultDTO() {
    }

    @Generated
    public GamePart getGame() {
        return this.game;
    }

    @Generated
    public List<StatPart> getStats() {
        return this.stats;
    }

    @Generated
    public void setGame(GamePart game) {
        this.game = game;
    }

    @Generated
    public void setStats(List<StatPart> stats) {
        this.stats = stats;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SaveGameResultDTO)) {
            return false;
        }
        SaveGameResultDTO other = (SaveGameResultDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        GamePart this$game = this.getGame();
        GamePart other$game = other.getGame();
        if (this$game == null ? other$game != null : !this$game.equals(other$game)) {
            return false;
        }
        List this$stats = this.getStats();
        List other$stats = other.getStats();
        return !(this$stats == null ? other$stats != null : !((Object)this$stats).equals(other$stats));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SaveGameResultDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        GamePart $game = this.getGame();
        result = result * 59 + ($game == null ? 43 : $game.hashCode());
        List $stats = this.getStats();
        result = result * 59 + ($stats == null ? 43 : ((Object)$stats).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SaveGameResultDTO(game=" + String.valueOf(this.getGame()) + ", stats=" + String.valueOf(this.getStats()) + ")";
    }

    public static class GamePart {
        private Long eventId;
        private Long homeTeamId;
        private Long awayTeamId;
        private String gameTime;
        private String gameEndTime;
        private String gameday;
        private Integer gameNumber;
        private Long stadiumId;
        private String venue;
        private String status;
        private String gameMode;
        private Integer totalInnings;
        private Integer homeScore;
        private Integer awayScore;
        private Object homeScoreByInning;
        private Object awayScoreByInning;
        private Integer homeH;
        private Integer awayH;
        private Integer homeE;
        private Integer awayE;
        private Integer spectatorCount;
        private String umpireHp;
        private String umpire1b;
        private String umpire2b;
        private String umpire3b;
        private String recorders;
        private String gameTag;
        private String weatherSummary;
        private Double weatherTempC;
        private String weatherWind;
        private Integer weatherRainProbPct;
        private String remark;
        private Boolean isSpecialResult;
        private Boolean showRemarkInCard;
        private Boolean includeStatsInRanking;

        @Generated
        public GamePart() {
        }

        @Generated
        public Long getEventId() {
            return this.eventId;
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
        public String getGameTime() {
            return this.gameTime;
        }

        @Generated
        public String getGameEndTime() {
            return this.gameEndTime;
        }

        @Generated
        public String getGameday() {
            return this.gameday;
        }

        @Generated
        public Integer getGameNumber() {
            return this.gameNumber;
        }

        @Generated
        public Long getStadiumId() {
            return this.stadiumId;
        }

        @Generated
        public String getVenue() {
            return this.venue;
        }

        @Generated
        public String getStatus() {
            return this.status;
        }

        @Generated
        public String getGameMode() {
            return this.gameMode;
        }

        @Generated
        public Integer getTotalInnings() {
            return this.totalInnings;
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
        public Object getHomeScoreByInning() {
            return this.homeScoreByInning;
        }

        @Generated
        public Object getAwayScoreByInning() {
            return this.awayScoreByInning;
        }

        @Generated
        public Integer getHomeH() {
            return this.homeH;
        }

        @Generated
        public Integer getAwayH() {
            return this.awayH;
        }

        @Generated
        public Integer getHomeE() {
            return this.homeE;
        }

        @Generated
        public Integer getAwayE() {
            return this.awayE;
        }

        @Generated
        public Integer getSpectatorCount() {
            return this.spectatorCount;
        }

        @Generated
        public String getUmpireHp() {
            return this.umpireHp;
        }

        @Generated
        public String getUmpire1b() {
            return this.umpire1b;
        }

        @Generated
        public String getUmpire2b() {
            return this.umpire2b;
        }

        @Generated
        public String getUmpire3b() {
            return this.umpire3b;
        }

        @Generated
        public String getRecorders() {
            return this.recorders;
        }

        @Generated
        public String getGameTag() {
            return this.gameTag;
        }

        @Generated
        public String getWeatherSummary() {
            return this.weatherSummary;
        }

        @Generated
        public Double getWeatherTempC() {
            return this.weatherTempC;
        }

        @Generated
        public String getWeatherWind() {
            return this.weatherWind;
        }

        @Generated
        public Integer getWeatherRainProbPct() {
            return this.weatherRainProbPct;
        }

        @Generated
        public String getRemark() {
            return this.remark;
        }

        @Generated
        public Boolean getIsSpecialResult() {
            return this.isSpecialResult;
        }

        @Generated
        public Boolean getShowRemarkInCard() {
            return this.showRemarkInCard;
        }

        @Generated
        public Boolean getIncludeStatsInRanking() {
            return this.includeStatsInRanking;
        }

        @Generated
        public void setEventId(Long eventId) {
            this.eventId = eventId;
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
        public void setGameTime(String gameTime) {
            this.gameTime = gameTime;
        }

        @Generated
        public void setGameEndTime(String gameEndTime) {
            this.gameEndTime = gameEndTime;
        }

        @Generated
        public void setGameday(String gameday) {
            this.gameday = gameday;
        }

        @Generated
        public void setGameNumber(Integer gameNumber) {
            this.gameNumber = gameNumber;
        }

        @Generated
        public void setStadiumId(Long stadiumId) {
            this.stadiumId = stadiumId;
        }

        @Generated
        public void setVenue(String venue) {
            this.venue = venue;
        }

        @Generated
        public void setStatus(String status) {
            this.status = status;
        }

        @Generated
        public void setGameMode(String gameMode) {
            this.gameMode = gameMode;
        }

        @Generated
        public void setTotalInnings(Integer totalInnings) {
            this.totalInnings = totalInnings;
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
        public void setHomeScoreByInning(Object homeScoreByInning) {
            this.homeScoreByInning = homeScoreByInning;
        }

        @Generated
        public void setAwayScoreByInning(Object awayScoreByInning) {
            this.awayScoreByInning = awayScoreByInning;
        }

        @Generated
        public void setHomeH(Integer homeH) {
            this.homeH = homeH;
        }

        @Generated
        public void setAwayH(Integer awayH) {
            this.awayH = awayH;
        }

        @Generated
        public void setHomeE(Integer homeE) {
            this.homeE = homeE;
        }

        @Generated
        public void setAwayE(Integer awayE) {
            this.awayE = awayE;
        }

        @Generated
        public void setSpectatorCount(Integer spectatorCount) {
            this.spectatorCount = spectatorCount;
        }

        @Generated
        public void setUmpireHp(String umpireHp) {
            this.umpireHp = umpireHp;
        }

        @Generated
        public void setUmpire1b(String umpire1b) {
            this.umpire1b = umpire1b;
        }

        @Generated
        public void setUmpire2b(String umpire2b) {
            this.umpire2b = umpire2b;
        }

        @Generated
        public void setUmpire3b(String umpire3b) {
            this.umpire3b = umpire3b;
        }

        @Generated
        public void setRecorders(String recorders) {
            this.recorders = recorders;
        }

        @Generated
        public void setGameTag(String gameTag) {
            this.gameTag = gameTag;
        }

        @Generated
        public void setWeatherSummary(String weatherSummary) {
            this.weatherSummary = weatherSummary;
        }

        @Generated
        public void setWeatherTempC(Double weatherTempC) {
            this.weatherTempC = weatherTempC;
        }

        @Generated
        public void setWeatherWind(String weatherWind) {
            this.weatherWind = weatherWind;
        }

        @Generated
        public void setWeatherRainProbPct(Integer weatherRainProbPct) {
            this.weatherRainProbPct = weatherRainProbPct;
        }

        @Generated
        public void setRemark(String remark) {
            this.remark = remark;
        }

        @Generated
        public void setIsSpecialResult(Boolean isSpecialResult) {
            this.isSpecialResult = isSpecialResult;
        }

        @Generated
        public void setShowRemarkInCard(Boolean showRemarkInCard) {
            this.showRemarkInCard = showRemarkInCard;
        }

        @Generated
        public void setIncludeStatsInRanking(Boolean includeStatsInRanking) {
            this.includeStatsInRanking = includeStatsInRanking;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof GamePart)) {
                return false;
            }
            SaveGameResultDTO.GamePart other = (GamePart)o;
            if (!other.canEqual(this)) {
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
            Integer this$gameNumber = this.getGameNumber();
            Integer other$gameNumber = other.getGameNumber();
            if (this$gameNumber == null ? other$gameNumber != null : !((Object)this$gameNumber).equals(other$gameNumber)) {
                return false;
            }
            Long this$stadiumId = this.getStadiumId();
            Long other$stadiumId = other.getStadiumId();
            if (this$stadiumId == null ? other$stadiumId != null : !((Object)this$stadiumId).equals(other$stadiumId)) {
                return false;
            }
            Integer this$totalInnings = this.getTotalInnings();
            Integer other$totalInnings = other.getTotalInnings();
            if (this$totalInnings == null ? other$totalInnings != null : !((Object)this$totalInnings).equals(other$totalInnings)) {
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
            Integer this$homeH = this.getHomeH();
            Integer other$homeH = other.getHomeH();
            if (this$homeH == null ? other$homeH != null : !((Object)this$homeH).equals(other$homeH)) {
                return false;
            }
            Integer this$awayH = this.getAwayH();
            Integer other$awayH = other.getAwayH();
            if (this$awayH == null ? other$awayH != null : !((Object)this$awayH).equals(other$awayH)) {
                return false;
            }
            Integer this$homeE = this.getHomeE();
            Integer other$homeE = other.getHomeE();
            if (this$homeE == null ? other$homeE != null : !((Object)this$homeE).equals(other$homeE)) {
                return false;
            }
            Integer this$awayE = this.getAwayE();
            Integer other$awayE = other.getAwayE();
            if (this$awayE == null ? other$awayE != null : !((Object)this$awayE).equals(other$awayE)) {
                return false;
            }
            Integer this$spectatorCount = this.getSpectatorCount();
            Integer other$spectatorCount = other.getSpectatorCount();
            if (this$spectatorCount == null ? other$spectatorCount != null : !((Object)this$spectatorCount).equals(other$spectatorCount)) {
                return false;
            }
            Double this$weatherTempC = this.getWeatherTempC();
            Double other$weatherTempC = other.getWeatherTempC();
            if (this$weatherTempC == null ? other$weatherTempC != null : !((Object)this$weatherTempC).equals(other$weatherTempC)) {
                return false;
            }
            Integer this$weatherRainProbPct = this.getWeatherRainProbPct();
            Integer other$weatherRainProbPct = other.getWeatherRainProbPct();
            if (this$weatherRainProbPct == null ? other$weatherRainProbPct != null : !((Object)this$weatherRainProbPct).equals(other$weatherRainProbPct)) {
                return false;
            }
            Boolean this$isSpecialResult = this.getIsSpecialResult();
            Boolean other$isSpecialResult = other.getIsSpecialResult();
            if (this$isSpecialResult == null ? other$isSpecialResult != null : !((Object)this$isSpecialResult).equals(other$isSpecialResult)) {
                return false;
            }
            Boolean this$showRemarkInCard = this.getShowRemarkInCard();
            Boolean other$showRemarkInCard = other.getShowRemarkInCard();
            if (this$showRemarkInCard == null ? other$showRemarkInCard != null : !((Object)this$showRemarkInCard).equals(other$showRemarkInCard)) {
                return false;
            }
            Boolean this$includeStatsInRanking = this.getIncludeStatsInRanking();
            Boolean other$includeStatsInRanking = other.getIncludeStatsInRanking();
            if (this$includeStatsInRanking == null ? other$includeStatsInRanking != null : !((Object)this$includeStatsInRanking).equals(other$includeStatsInRanking)) {
                return false;
            }
            String this$gameTime = this.getGameTime();
            String other$gameTime = other.getGameTime();
            if (this$gameTime == null ? other$gameTime != null : !this$gameTime.equals(other$gameTime)) {
                return false;
            }
            String this$gameEndTime = this.getGameEndTime();
            String other$gameEndTime = other.getGameEndTime();
            if (this$gameEndTime == null ? other$gameEndTime != null : !this$gameEndTime.equals(other$gameEndTime)) {
                return false;
            }
            String this$gameday = this.getGameday();
            String other$gameday = other.getGameday();
            if (this$gameday == null ? other$gameday != null : !this$gameday.equals(other$gameday)) {
                return false;
            }
            String this$venue = this.getVenue();
            String other$venue = other.getVenue();
            if (this$venue == null ? other$venue != null : !this$venue.equals(other$venue)) {
                return false;
            }
            String this$status = this.getStatus();
            String other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
                return false;
            }
            String this$gameMode = this.getGameMode();
            String other$gameMode = other.getGameMode();
            if (this$gameMode == null ? other$gameMode != null : !this$gameMode.equals(other$gameMode)) {
                return false;
            }
            Object this$homeScoreByInning = this.getHomeScoreByInning();
            Object other$homeScoreByInning = other.getHomeScoreByInning();
            if (this$homeScoreByInning == null ? other$homeScoreByInning != null : !this$homeScoreByInning.equals(other$homeScoreByInning)) {
                return false;
            }
            Object this$awayScoreByInning = this.getAwayScoreByInning();
            Object other$awayScoreByInning = other.getAwayScoreByInning();
            if (this$awayScoreByInning == null ? other$awayScoreByInning != null : !this$awayScoreByInning.equals(other$awayScoreByInning)) {
                return false;
            }
            String this$umpireHp = this.getUmpireHp();
            String other$umpireHp = other.getUmpireHp();
            if (this$umpireHp == null ? other$umpireHp != null : !this$umpireHp.equals(other$umpireHp)) {
                return false;
            }
            String this$umpire1b = this.getUmpire1b();
            String other$umpire1b = other.getUmpire1b();
            if (this$umpire1b == null ? other$umpire1b != null : !this$umpire1b.equals(other$umpire1b)) {
                return false;
            }
            String this$umpire2b = this.getUmpire2b();
            String other$umpire2b = other.getUmpire2b();
            if (this$umpire2b == null ? other$umpire2b != null : !this$umpire2b.equals(other$umpire2b)) {
                return false;
            }
            String this$umpire3b = this.getUmpire3b();
            String other$umpire3b = other.getUmpire3b();
            if (this$umpire3b == null ? other$umpire3b != null : !this$umpire3b.equals(other$umpire3b)) {
                return false;
            }
            String this$recorders = this.getRecorders();
            String other$recorders = other.getRecorders();
            if (this$recorders == null ? other$recorders != null : !this$recorders.equals(other$recorders)) {
                return false;
            }
            String this$gameTag = this.getGameTag();
            String other$gameTag = other.getGameTag();
            if (this$gameTag == null ? other$gameTag != null : !this$gameTag.equals(other$gameTag)) {
                return false;
            }
            String this$weatherSummary = this.getWeatherSummary();
            String other$weatherSummary = other.getWeatherSummary();
            if (this$weatherSummary == null ? other$weatherSummary != null : !this$weatherSummary.equals(other$weatherSummary)) {
                return false;
            }
            String this$weatherWind = this.getWeatherWind();
            String other$weatherWind = other.getWeatherWind();
            if (this$weatherWind == null ? other$weatherWind != null : !this$weatherWind.equals(other$weatherWind)) {
                return false;
            }
            String this$remark = this.getRemark();
            String other$remark = other.getRemark();
            return !(this$remark == null ? other$remark != null : !this$remark.equals(other$remark));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof GamePart;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            Long $eventId = this.getEventId();
            result = result * 59 + ($eventId == null ? 43 : ((Object)$eventId).hashCode());
            Long $homeTeamId = this.getHomeTeamId();
            result = result * 59 + ($homeTeamId == null ? 43 : ((Object)$homeTeamId).hashCode());
            Long $awayTeamId = this.getAwayTeamId();
            result = result * 59 + ($awayTeamId == null ? 43 : ((Object)$awayTeamId).hashCode());
            Integer $gameNumber = this.getGameNumber();
            result = result * 59 + ($gameNumber == null ? 43 : ((Object)$gameNumber).hashCode());
            Long $stadiumId = this.getStadiumId();
            result = result * 59 + ($stadiumId == null ? 43 : ((Object)$stadiumId).hashCode());
            Integer $totalInnings = this.getTotalInnings();
            result = result * 59 + ($totalInnings == null ? 43 : ((Object)$totalInnings).hashCode());
            Integer $homeScore = this.getHomeScore();
            result = result * 59 + ($homeScore == null ? 43 : ((Object)$homeScore).hashCode());
            Integer $awayScore = this.getAwayScore();
            result = result * 59 + ($awayScore == null ? 43 : ((Object)$awayScore).hashCode());
            Integer $homeH = this.getHomeH();
            result = result * 59 + ($homeH == null ? 43 : ((Object)$homeH).hashCode());
            Integer $awayH = this.getAwayH();
            result = result * 59 + ($awayH == null ? 43 : ((Object)$awayH).hashCode());
            Integer $homeE = this.getHomeE();
            result = result * 59 + ($homeE == null ? 43 : ((Object)$homeE).hashCode());
            Integer $awayE = this.getAwayE();
            result = result * 59 + ($awayE == null ? 43 : ((Object)$awayE).hashCode());
            Integer $spectatorCount = this.getSpectatorCount();
            result = result * 59 + ($spectatorCount == null ? 43 : ((Object)$spectatorCount).hashCode());
            Double $weatherTempC = this.getWeatherTempC();
            result = result * 59 + ($weatherTempC == null ? 43 : ((Object)$weatherTempC).hashCode());
            Integer $weatherRainProbPct = this.getWeatherRainProbPct();
            result = result * 59 + ($weatherRainProbPct == null ? 43 : ((Object)$weatherRainProbPct).hashCode());
            Boolean $isSpecialResult = this.getIsSpecialResult();
            result = result * 59 + ($isSpecialResult == null ? 43 : ((Object)$isSpecialResult).hashCode());
            Boolean $showRemarkInCard = this.getShowRemarkInCard();
            result = result * 59 + ($showRemarkInCard == null ? 43 : ((Object)$showRemarkInCard).hashCode());
            Boolean $includeStatsInRanking = this.getIncludeStatsInRanking();
            result = result * 59 + ($includeStatsInRanking == null ? 43 : ((Object)$includeStatsInRanking).hashCode());
            String $gameTime = this.getGameTime();
            result = result * 59 + ($gameTime == null ? 43 : $gameTime.hashCode());
            String $gameEndTime = this.getGameEndTime();
            result = result * 59 + ($gameEndTime == null ? 43 : $gameEndTime.hashCode());
            String $gameday = this.getGameday();
            result = result * 59 + ($gameday == null ? 43 : $gameday.hashCode());
            String $venue = this.getVenue();
            result = result * 59 + ($venue == null ? 43 : $venue.hashCode());
            String $status = this.getStatus();
            result = result * 59 + ($status == null ? 43 : $status.hashCode());
            String $gameMode = this.getGameMode();
            result = result * 59 + ($gameMode == null ? 43 : $gameMode.hashCode());
            Object $homeScoreByInning = this.getHomeScoreByInning();
            result = result * 59 + ($homeScoreByInning == null ? 43 : $homeScoreByInning.hashCode());
            Object $awayScoreByInning = this.getAwayScoreByInning();
            result = result * 59 + ($awayScoreByInning == null ? 43 : $awayScoreByInning.hashCode());
            String $umpireHp = this.getUmpireHp();
            result = result * 59 + ($umpireHp == null ? 43 : $umpireHp.hashCode());
            String $umpire1b = this.getUmpire1b();
            result = result * 59 + ($umpire1b == null ? 43 : $umpire1b.hashCode());
            String $umpire2b = this.getUmpire2b();
            result = result * 59 + ($umpire2b == null ? 43 : $umpire2b.hashCode());
            String $umpire3b = this.getUmpire3b();
            result = result * 59 + ($umpire3b == null ? 43 : $umpire3b.hashCode());
            String $recorders = this.getRecorders();
            result = result * 59 + ($recorders == null ? 43 : $recorders.hashCode());
            String $gameTag = this.getGameTag();
            result = result * 59 + ($gameTag == null ? 43 : $gameTag.hashCode());
            String $weatherSummary = this.getWeatherSummary();
            result = result * 59 + ($weatherSummary == null ? 43 : $weatherSummary.hashCode());
            String $weatherWind = this.getWeatherWind();
            result = result * 59 + ($weatherWind == null ? 43 : $weatherWind.hashCode());
            String $remark = this.getRemark();
            result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "SaveGameResultDTO.GamePart(eventId=" + this.getEventId() + ", homeTeamId=" + this.getHomeTeamId() + ", awayTeamId=" + this.getAwayTeamId() + ", gameTime=" + this.getGameTime() + ", gameEndTime=" + this.getGameEndTime() + ", gameday=" + this.getGameday() + ", gameNumber=" + this.getGameNumber() + ", stadiumId=" + this.getStadiumId() + ", venue=" + this.getVenue() + ", status=" + this.getStatus() + ", gameMode=" + this.getGameMode() + ", totalInnings=" + this.getTotalInnings() + ", homeScore=" + this.getHomeScore() + ", awayScore=" + this.getAwayScore() + ", homeScoreByInning=" + String.valueOf(this.getHomeScoreByInning()) + ", awayScoreByInning=" + String.valueOf(this.getAwayScoreByInning()) + ", homeH=" + this.getHomeH() + ", awayH=" + this.getAwayH() + ", homeE=" + this.getHomeE() + ", awayE=" + this.getAwayE() + ", spectatorCount=" + this.getSpectatorCount() + ", umpireHp=" + this.getUmpireHp() + ", umpire1b=" + this.getUmpire1b() + ", umpire2b=" + this.getUmpire2b() + ", umpire3b=" + this.getUmpire3b() + ", recorders=" + this.getRecorders() + ", gameTag=" + this.getGameTag() + ", weatherSummary=" + this.getWeatherSummary() + ", weatherTempC=" + this.getWeatherTempC() + ", weatherWind=" + this.getWeatherWind() + ", weatherRainProbPct=" + this.getWeatherRainProbPct() + ", remark=" + this.getRemark() + ", isSpecialResult=" + this.getIsSpecialResult() + ", showRemarkInCard=" + this.getShowRemarkInCard() + ", includeStatsInRanking=" + this.getIncludeStatsInRanking() + ")";
        }
    }

    public static class StatPart {
        private Long id;
        private Long teamId;
        private Long playerId;
        private Integer battingOrder;
        private Integer listOrder;
        private String position;
        private String number;
        private String batHand;
        private String throwHand;
        private Integer pa;
        private Integer ab;
        private Integer r;
        private Integer h;
        private Integer e;
        private Integer bbHp;
        private Integer sb;
        private Integer so;
        private Integer soSwing;
        private Integer soLooking;
        private Integer rbi;
        private Integer hr;
        private Integer insideParkHr;
        private Integer doubles;
        private Integer triples;
        private Integer gdp;
        private Integer sh;
        private Integer sf;
        private Integer bb;
        private Integer ibb;
        private Integer hbp;
        private Integer cs;
        @JsonDeserializeAs(BoolToIntDeserializer.class)
        private Integer isPitcher;
        private Integer pitcherOrder;
        private Double ip;
        private Integer er;
        private Integer pitchH;
        private Integer pitchBbHp;
        private Integer pitchSo;
        private Integer pitchHr;
        private Integer pitchInsideParkHr;
        private Integer pitchR;
        private Integer pitchPa;
        private Integer pitchBf;
        @JsonAlias(value={"pitAb"})
        private Integer pitchAb;
        private Integer np;
        private Integer pitchBb;
        private Integer pitchIbb;
        private Integer pitchHbp;
        private Integer wp;
        private Integer bk;
        private Integer go;
        private Integer fo;
        private Integer gs;
        private Integer svo;
        private Integer cg;
        private Integer pg;
        private Integer w;
        private Integer l;
        private Integer sv;
        private Integer hld;
        private Integer po;
        private Integer a;
        private Integer tc;
        private Integer fieldingGs;
        private Double defInn;
        private Integer dp;
        private Integer pb;
        private Integer catcherSb;
        private Integer catcherCs;

        @Generated
        public StatPart() {
        }

        @Generated
        public Long getId() {
            return this.id;
        }

        @Generated
        public Long getTeamId() {
            return this.teamId;
        }

        @Generated
        public Long getPlayerId() {
            return this.playerId;
        }

        @Generated
        public Integer getBattingOrder() {
            return this.battingOrder;
        }

        @Generated
        public Integer getListOrder() {
            return this.listOrder;
        }

        @Generated
        public String getPosition() {
            return this.position;
        }

        @Generated
        public String getNumber() {
            return this.number;
        }

        @Generated
        public String getBatHand() {
            return this.batHand;
        }

        @Generated
        public String getThrowHand() {
            return this.throwHand;
        }

        @Generated
        public Integer getPa() {
            return this.pa;
        }

        @Generated
        public Integer getAb() {
            return this.ab;
        }

        @Generated
        public Integer getR() {
            return this.r;
        }

        @Generated
        public Integer getH() {
            return this.h;
        }

        @Generated
        public Integer getE() {
            return this.e;
        }

        @Generated
        public Integer getBbHp() {
            return this.bbHp;
        }

        @Generated
        public Integer getSb() {
            return this.sb;
        }

        @Generated
        public Integer getSo() {
            return this.so;
        }

        @Generated
        public Integer getSoSwing() {
            return this.soSwing;
        }

        @Generated
        public Integer getSoLooking() {
            return this.soLooking;
        }

        @Generated
        public Integer getRbi() {
            return this.rbi;
        }

        @Generated
        public Integer getHr() {
            return this.hr;
        }

        @Generated
        public Integer getInsideParkHr() {
            return this.insideParkHr;
        }

        @Generated
        public Integer getDoubles() {
            return this.doubles;
        }

        @Generated
        public Integer getTriples() {
            return this.triples;
        }

        @Generated
        public Integer getGdp() {
            return this.gdp;
        }

        @Generated
        public Integer getSh() {
            return this.sh;
        }

        @Generated
        public Integer getSf() {
            return this.sf;
        }

        @Generated
        public Integer getBb() {
            return this.bb;
        }

        @Generated
        public Integer getIbb() {
            return this.ibb;
        }

        @Generated
        public Integer getHbp() {
            return this.hbp;
        }

        @Generated
        public Integer getCs() {
            return this.cs;
        }

        @Generated
        public Integer getIsPitcher() {
            return this.isPitcher;
        }

        @Generated
        public Integer getPitcherOrder() {
            return this.pitcherOrder;
        }

        @Generated
        public Double getIp() {
            return this.ip;
        }

        @Generated
        public Integer getEr() {
            return this.er;
        }

        @Generated
        public Integer getPitchH() {
            return this.pitchH;
        }

        @Generated
        public Integer getPitchBbHp() {
            return this.pitchBbHp;
        }

        @Generated
        public Integer getPitchSo() {
            return this.pitchSo;
        }

        @Generated
        public Integer getPitchHr() {
            return this.pitchHr;
        }

        @Generated
        public Integer getPitchInsideParkHr() {
            return this.pitchInsideParkHr;
        }

        @Generated
        public Integer getPitchR() {
            return this.pitchR;
        }

        @Generated
        public Integer getPitchPa() {
            return this.pitchPa;
        }

        @Generated
        public Integer getPitchBf() {
            return this.pitchBf;
        }

        @Generated
        public Integer getPitchAb() {
            return this.pitchAb;
        }

        @Generated
        public Integer getNp() {
            return this.np;
        }

        @Generated
        public Integer getPitchBb() {
            return this.pitchBb;
        }

        @Generated
        public Integer getPitchIbb() {
            return this.pitchIbb;
        }

        @Generated
        public Integer getPitchHbp() {
            return this.pitchHbp;
        }

        @Generated
        public Integer getWp() {
            return this.wp;
        }

        @Generated
        public Integer getBk() {
            return this.bk;
        }

        @Generated
        public Integer getGo() {
            return this.go;
        }

        @Generated
        public Integer getFo() {
            return this.fo;
        }

        @Generated
        public Integer getGs() {
            return this.gs;
        }

        @Generated
        public Integer getSvo() {
            return this.svo;
        }

        @Generated
        public Integer getCg() {
            return this.cg;
        }

        @Generated
        public Integer getPg() {
            return this.pg;
        }

        @Generated
        public Integer getW() {
            return this.w;
        }

        @Generated
        public Integer getL() {
            return this.l;
        }

        @Generated
        public Integer getSv() {
            return this.sv;
        }

        @Generated
        public Integer getHld() {
            return this.hld;
        }

        @Generated
        public Integer getPo() {
            return this.po;
        }

        @Generated
        public Integer getA() {
            return this.a;
        }

        @Generated
        public Integer getTc() {
            return this.tc;
        }

        @Generated
        public Integer getFieldingGs() {
            return this.fieldingGs;
        }

        @Generated
        public Double getDefInn() {
            return this.defInn;
        }

        @Generated
        public Integer getDp() {
            return this.dp;
        }

        @Generated
        public Integer getPb() {
            return this.pb;
        }

        @Generated
        public Integer getCatcherSb() {
            return this.catcherSb;
        }

        @Generated
        public Integer getCatcherCs() {
            return this.catcherCs;
        }

        @Generated
        public void setId(Long id) {
            this.id = id;
        }

        @Generated
        public void setTeamId(Long teamId) {
            this.teamId = teamId;
        }

        @Generated
        public void setPlayerId(Long playerId) {
            this.playerId = playerId;
        }

        @Generated
        public void setBattingOrder(Integer battingOrder) {
            this.battingOrder = battingOrder;
        }

        @Generated
        public void setListOrder(Integer listOrder) {
            this.listOrder = listOrder;
        }

        @Generated
        public void setPosition(String position) {
            this.position = position;
        }

        @Generated
        public void setNumber(String number) {
            this.number = number;
        }

        @Generated
        public void setBatHand(String batHand) {
            this.batHand = batHand;
        }

        @Generated
        public void setThrowHand(String throwHand) {
            this.throwHand = throwHand;
        }

        @Generated
        public void setPa(Integer pa) {
            this.pa = pa;
        }

        @Generated
        public void setAb(Integer ab) {
            this.ab = ab;
        }

        @Generated
        public void setR(Integer r) {
            this.r = r;
        }

        @Generated
        public void setH(Integer h) {
            this.h = h;
        }

        @Generated
        public void setE(Integer e) {
            this.e = e;
        }

        @Generated
        public void setBbHp(Integer bbHp) {
            this.bbHp = bbHp;
        }

        @Generated
        public void setSb(Integer sb) {
            this.sb = sb;
        }

        @Generated
        public void setSo(Integer so) {
            this.so = so;
        }

        @Generated
        public void setSoSwing(Integer soSwing) {
            this.soSwing = soSwing;
        }

        @Generated
        public void setSoLooking(Integer soLooking) {
            this.soLooking = soLooking;
        }

        @Generated
        public void setRbi(Integer rbi) {
            this.rbi = rbi;
        }

        @Generated
        public void setHr(Integer hr) {
            this.hr = hr;
        }

        @Generated
        public void setInsideParkHr(Integer insideParkHr) {
            this.insideParkHr = insideParkHr;
        }

        @Generated
        public void setDoubles(Integer doubles) {
            this.doubles = doubles;
        }

        @Generated
        public void setTriples(Integer triples) {
            this.triples = triples;
        }

        @Generated
        public void setGdp(Integer gdp) {
            this.gdp = gdp;
        }

        @Generated
        public void setSh(Integer sh) {
            this.sh = sh;
        }

        @Generated
        public void setSf(Integer sf) {
            this.sf = sf;
        }

        @Generated
        public void setBb(Integer bb) {
            this.bb = bb;
        }

        @Generated
        public void setIbb(Integer ibb) {
            this.ibb = ibb;
        }

        @Generated
        public void setHbp(Integer hbp) {
            this.hbp = hbp;
        }

        @Generated
        public void setCs(Integer cs) {
            this.cs = cs;
        }

        @Generated
        public void setIsPitcher(Integer isPitcher) {
            this.isPitcher = isPitcher;
        }

        @Generated
        public void setPitcherOrder(Integer pitcherOrder) {
            this.pitcherOrder = pitcherOrder;
        }

        @Generated
        public void setIp(Double ip) {
            this.ip = ip;
        }

        @Generated
        public void setEr(Integer er) {
            this.er = er;
        }

        @Generated
        public void setPitchH(Integer pitchH) {
            this.pitchH = pitchH;
        }

        @Generated
        public void setPitchBbHp(Integer pitchBbHp) {
            this.pitchBbHp = pitchBbHp;
        }

        @Generated
        public void setPitchSo(Integer pitchSo) {
            this.pitchSo = pitchSo;
        }

        @Generated
        public void setPitchHr(Integer pitchHr) {
            this.pitchHr = pitchHr;
        }

        @Generated
        public void setPitchInsideParkHr(Integer pitchInsideParkHr) {
            this.pitchInsideParkHr = pitchInsideParkHr;
        }

        @Generated
        public void setPitchR(Integer pitchR) {
            this.pitchR = pitchR;
        }

        @Generated
        public void setPitchPa(Integer pitchPa) {
            this.pitchPa = pitchPa;
        }

        @Generated
        public void setPitchBf(Integer pitchBf) {
            this.pitchBf = pitchBf;
        }

        @Generated
        public void setPitchAb(Integer pitchAb) {
            this.pitchAb = pitchAb;
        }

        @Generated
        public void setNp(Integer np) {
            this.np = np;
        }

        @Generated
        public void setPitchBb(Integer pitchBb) {
            this.pitchBb = pitchBb;
        }

        @Generated
        public void setPitchIbb(Integer pitchIbb) {
            this.pitchIbb = pitchIbb;
        }

        @Generated
        public void setPitchHbp(Integer pitchHbp) {
            this.pitchHbp = pitchHbp;
        }

        @Generated
        public void setWp(Integer wp) {
            this.wp = wp;
        }

        @Generated
        public void setBk(Integer bk) {
            this.bk = bk;
        }

        @Generated
        public void setGo(Integer go) {
            this.go = go;
        }

        @Generated
        public void setFo(Integer fo) {
            this.fo = fo;
        }

        @Generated
        public void setGs(Integer gs) {
            this.gs = gs;
        }

        @Generated
        public void setSvo(Integer svo) {
            this.svo = svo;
        }

        @Generated
        public void setCg(Integer cg) {
            this.cg = cg;
        }

        @Generated
        public void setPg(Integer pg) {
            this.pg = pg;
        }

        @Generated
        public void setW(Integer w) {
            this.w = w;
        }

        @Generated
        public void setL(Integer l) {
            this.l = l;
        }

        @Generated
        public void setSv(Integer sv) {
            this.sv = sv;
        }

        @Generated
        public void setHld(Integer hld) {
            this.hld = hld;
        }

        @Generated
        public void setPo(Integer po) {
            this.po = po;
        }

        @Generated
        public void setA(Integer a) {
            this.a = a;
        }

        @Generated
        public void setTc(Integer tc) {
            this.tc = tc;
        }

        @Generated
        public void setFieldingGs(Integer fieldingGs) {
            this.fieldingGs = fieldingGs;
        }

        @Generated
        public void setDefInn(Double defInn) {
            this.defInn = defInn;
        }

        @Generated
        public void setDp(Integer dp) {
            this.dp = dp;
        }

        @Generated
        public void setPb(Integer pb) {
            this.pb = pb;
        }

        @Generated
        public void setCatcherSb(Integer catcherSb) {
            this.catcherSb = catcherSb;
        }

        @Generated
        public void setCatcherCs(Integer catcherCs) {
            this.catcherCs = catcherCs;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof StatPart)) {
                return false;
            }
            SaveGameResultDTO.StatPart other = (StatPart)o;
            if (!other.canEqual(this)) {
                return false;
            }
            Long this$id = this.getId();
            Long other$id = other.getId();
            if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
                return false;
            }
            Long this$teamId = this.getTeamId();
            Long other$teamId = other.getTeamId();
            if (this$teamId == null ? other$teamId != null : !((Object)this$teamId).equals(other$teamId)) {
                return false;
            }
            Long this$playerId = this.getPlayerId();
            Long other$playerId = other.getPlayerId();
            if (this$playerId == null ? other$playerId != null : !((Object)this$playerId).equals(other$playerId)) {
                return false;
            }
            Integer this$battingOrder = this.getBattingOrder();
            Integer other$battingOrder = other.getBattingOrder();
            if (this$battingOrder == null ? other$battingOrder != null : !((Object)this$battingOrder).equals(other$battingOrder)) {
                return false;
            }
            Integer this$listOrder = this.getListOrder();
            Integer other$listOrder = other.getListOrder();
            if (this$listOrder == null ? other$listOrder != null : !((Object)this$listOrder).equals(other$listOrder)) {
                return false;
            }
            Integer this$pa = this.getPa();
            Integer other$pa = other.getPa();
            if (this$pa == null ? other$pa != null : !((Object)this$pa).equals(other$pa)) {
                return false;
            }
            Integer this$ab = this.getAb();
            Integer other$ab = other.getAb();
            if (this$ab == null ? other$ab != null : !((Object)this$ab).equals(other$ab)) {
                return false;
            }
            Integer this$r = this.getR();
            Integer other$r = other.getR();
            if (this$r == null ? other$r != null : !((Object)this$r).equals(other$r)) {
                return false;
            }
            Integer this$h = this.getH();
            Integer other$h = other.getH();
            if (this$h == null ? other$h != null : !((Object)this$h).equals(other$h)) {
                return false;
            }
            Integer this$e = this.getE();
            Integer other$e = other.getE();
            if (this$e == null ? other$e != null : !((Object)this$e).equals(other$e)) {
                return false;
            }
            Integer this$bbHp = this.getBbHp();
            Integer other$bbHp = other.getBbHp();
            if (this$bbHp == null ? other$bbHp != null : !((Object)this$bbHp).equals(other$bbHp)) {
                return false;
            }
            Integer this$sb = this.getSb();
            Integer other$sb = other.getSb();
            if (this$sb == null ? other$sb != null : !((Object)this$sb).equals(other$sb)) {
                return false;
            }
            Integer this$so = this.getSo();
            Integer other$so = other.getSo();
            if (this$so == null ? other$so != null : !((Object)this$so).equals(other$so)) {
                return false;
            }
            Integer this$soSwing = this.getSoSwing();
            Integer other$soSwing = other.getSoSwing();
            if (this$soSwing == null ? other$soSwing != null : !((Object)this$soSwing).equals(other$soSwing)) {
                return false;
            }
            Integer this$soLooking = this.getSoLooking();
            Integer other$soLooking = other.getSoLooking();
            if (this$soLooking == null ? other$soLooking != null : !((Object)this$soLooking).equals(other$soLooking)) {
                return false;
            }
            Integer this$rbi = this.getRbi();
            Integer other$rbi = other.getRbi();
            if (this$rbi == null ? other$rbi != null : !((Object)this$rbi).equals(other$rbi)) {
                return false;
            }
            Integer this$hr = this.getHr();
            Integer other$hr = other.getHr();
            if (this$hr == null ? other$hr != null : !((Object)this$hr).equals(other$hr)) {
                return false;
            }
            Integer this$insideParkHr = this.getInsideParkHr();
            Integer other$insideParkHr = other.getInsideParkHr();
            if (this$insideParkHr == null ? other$insideParkHr != null : !((Object)this$insideParkHr).equals(other$insideParkHr)) {
                return false;
            }
            Integer this$doubles = this.getDoubles();
            Integer other$doubles = other.getDoubles();
            if (this$doubles == null ? other$doubles != null : !((Object)this$doubles).equals(other$doubles)) {
                return false;
            }
            Integer this$triples = this.getTriples();
            Integer other$triples = other.getTriples();
            if (this$triples == null ? other$triples != null : !((Object)this$triples).equals(other$triples)) {
                return false;
            }
            Integer this$gdp = this.getGdp();
            Integer other$gdp = other.getGdp();
            if (this$gdp == null ? other$gdp != null : !((Object)this$gdp).equals(other$gdp)) {
                return false;
            }
            Integer this$sh = this.getSh();
            Integer other$sh = other.getSh();
            if (this$sh == null ? other$sh != null : !((Object)this$sh).equals(other$sh)) {
                return false;
            }
            Integer this$sf = this.getSf();
            Integer other$sf = other.getSf();
            if (this$sf == null ? other$sf != null : !((Object)this$sf).equals(other$sf)) {
                return false;
            }
            Integer this$bb = this.getBb();
            Integer other$bb = other.getBb();
            if (this$bb == null ? other$bb != null : !((Object)this$bb).equals(other$bb)) {
                return false;
            }
            Integer this$ibb = this.getIbb();
            Integer other$ibb = other.getIbb();
            if (this$ibb == null ? other$ibb != null : !((Object)this$ibb).equals(other$ibb)) {
                return false;
            }
            Integer this$hbp = this.getHbp();
            Integer other$hbp = other.getHbp();
            if (this$hbp == null ? other$hbp != null : !((Object)this$hbp).equals(other$hbp)) {
                return false;
            }
            Integer this$cs = this.getCs();
            Integer other$cs = other.getCs();
            if (this$cs == null ? other$cs != null : !((Object)this$cs).equals(other$cs)) {
                return false;
            }
            Integer this$isPitcher = this.getIsPitcher();
            Integer other$isPitcher = other.getIsPitcher();
            if (this$isPitcher == null ? other$isPitcher != null : !((Object)this$isPitcher).equals(other$isPitcher)) {
                return false;
            }
            Integer this$pitcherOrder = this.getPitcherOrder();
            Integer other$pitcherOrder = other.getPitcherOrder();
            if (this$pitcherOrder == null ? other$pitcherOrder != null : !((Object)this$pitcherOrder).equals(other$pitcherOrder)) {
                return false;
            }
            Double this$ip = this.getIp();
            Double other$ip = other.getIp();
            if (this$ip == null ? other$ip != null : !((Object)this$ip).equals(other$ip)) {
                return false;
            }
            Integer this$er = this.getEr();
            Integer other$er = other.getEr();
            if (this$er == null ? other$er != null : !((Object)this$er).equals(other$er)) {
                return false;
            }
            Integer this$pitchH = this.getPitchH();
            Integer other$pitchH = other.getPitchH();
            if (this$pitchH == null ? other$pitchH != null : !((Object)this$pitchH).equals(other$pitchH)) {
                return false;
            }
            Integer this$pitchBbHp = this.getPitchBbHp();
            Integer other$pitchBbHp = other.getPitchBbHp();
            if (this$pitchBbHp == null ? other$pitchBbHp != null : !((Object)this$pitchBbHp).equals(other$pitchBbHp)) {
                return false;
            }
            Integer this$pitchSo = this.getPitchSo();
            Integer other$pitchSo = other.getPitchSo();
            if (this$pitchSo == null ? other$pitchSo != null : !((Object)this$pitchSo).equals(other$pitchSo)) {
                return false;
            }
            Integer this$pitchHr = this.getPitchHr();
            Integer other$pitchHr = other.getPitchHr();
            if (this$pitchHr == null ? other$pitchHr != null : !((Object)this$pitchHr).equals(other$pitchHr)) {
                return false;
            }
            Integer this$pitchInsideParkHr = this.getPitchInsideParkHr();
            Integer other$pitchInsideParkHr = other.getPitchInsideParkHr();
            if (this$pitchInsideParkHr == null ? other$pitchInsideParkHr != null : !((Object)this$pitchInsideParkHr).equals(other$pitchInsideParkHr)) {
                return false;
            }
            Integer this$pitchR = this.getPitchR();
            Integer other$pitchR = other.getPitchR();
            if (this$pitchR == null ? other$pitchR != null : !((Object)this$pitchR).equals(other$pitchR)) {
                return false;
            }
            Integer this$pitchPa = this.getPitchPa();
            Integer other$pitchPa = other.getPitchPa();
            if (this$pitchPa == null ? other$pitchPa != null : !((Object)this$pitchPa).equals(other$pitchPa)) {
                return false;
            }
            Integer this$pitchBf = this.getPitchBf();
            Integer other$pitchBf = other.getPitchBf();
            if (this$pitchBf == null ? other$pitchBf != null : !((Object)this$pitchBf).equals(other$pitchBf)) {
                return false;
            }
            Integer this$pitchAb = this.getPitchAb();
            Integer other$pitchAb = other.getPitchAb();
            if (this$pitchAb == null ? other$pitchAb != null : !((Object)this$pitchAb).equals(other$pitchAb)) {
                return false;
            }
            Integer this$np = this.getNp();
            Integer other$np = other.getNp();
            if (this$np == null ? other$np != null : !((Object)this$np).equals(other$np)) {
                return false;
            }
            Integer this$pitchBb = this.getPitchBb();
            Integer other$pitchBb = other.getPitchBb();
            if (this$pitchBb == null ? other$pitchBb != null : !((Object)this$pitchBb).equals(other$pitchBb)) {
                return false;
            }
            Integer this$pitchIbb = this.getPitchIbb();
            Integer other$pitchIbb = other.getPitchIbb();
            if (this$pitchIbb == null ? other$pitchIbb != null : !((Object)this$pitchIbb).equals(other$pitchIbb)) {
                return false;
            }
            Integer this$pitchHbp = this.getPitchHbp();
            Integer other$pitchHbp = other.getPitchHbp();
            if (this$pitchHbp == null ? other$pitchHbp != null : !((Object)this$pitchHbp).equals(other$pitchHbp)) {
                return false;
            }
            Integer this$wp = this.getWp();
            Integer other$wp = other.getWp();
            if (this$wp == null ? other$wp != null : !((Object)this$wp).equals(other$wp)) {
                return false;
            }
            Integer this$bk = this.getBk();
            Integer other$bk = other.getBk();
            if (this$bk == null ? other$bk != null : !((Object)this$bk).equals(other$bk)) {
                return false;
            }
            Integer this$go = this.getGo();
            Integer other$go = other.getGo();
            if (this$go == null ? other$go != null : !((Object)this$go).equals(other$go)) {
                return false;
            }
            Integer this$fo = this.getFo();
            Integer other$fo = other.getFo();
            if (this$fo == null ? other$fo != null : !((Object)this$fo).equals(other$fo)) {
                return false;
            }
            Integer this$gs = this.getGs();
            Integer other$gs = other.getGs();
            if (this$gs == null ? other$gs != null : !((Object)this$gs).equals(other$gs)) {
                return false;
            }
            Integer this$svo = this.getSvo();
            Integer other$svo = other.getSvo();
            if (this$svo == null ? other$svo != null : !((Object)this$svo).equals(other$svo)) {
                return false;
            }
            Integer this$cg = this.getCg();
            Integer other$cg = other.getCg();
            if (this$cg == null ? other$cg != null : !((Object)this$cg).equals(other$cg)) {
                return false;
            }
            Integer this$pg = this.getPg();
            Integer other$pg = other.getPg();
            if (this$pg == null ? other$pg != null : !((Object)this$pg).equals(other$pg)) {
                return false;
            }
            Integer this$w = this.getW();
            Integer other$w = other.getW();
            if (this$w == null ? other$w != null : !((Object)this$w).equals(other$w)) {
                return false;
            }
            Integer this$l = this.getL();
            Integer other$l = other.getL();
            if (this$l == null ? other$l != null : !((Object)this$l).equals(other$l)) {
                return false;
            }
            Integer this$sv = this.getSv();
            Integer other$sv = other.getSv();
            if (this$sv == null ? other$sv != null : !((Object)this$sv).equals(other$sv)) {
                return false;
            }
            Integer this$hld = this.getHld();
            Integer other$hld = other.getHld();
            if (this$hld == null ? other$hld != null : !((Object)this$hld).equals(other$hld)) {
                return false;
            }
            Integer this$po = this.getPo();
            Integer other$po = other.getPo();
            if (this$po == null ? other$po != null : !((Object)this$po).equals(other$po)) {
                return false;
            }
            Integer this$a = this.getA();
            Integer other$a = other.getA();
            if (this$a == null ? other$a != null : !((Object)this$a).equals(other$a)) {
                return false;
            }
            Integer this$tc = this.getTc();
            Integer other$tc = other.getTc();
            if (this$tc == null ? other$tc != null : !((Object)this$tc).equals(other$tc)) {
                return false;
            }
            Integer this$fieldingGs = this.getFieldingGs();
            Integer other$fieldingGs = other.getFieldingGs();
            if (this$fieldingGs == null ? other$fieldingGs != null : !((Object)this$fieldingGs).equals(other$fieldingGs)) {
                return false;
            }
            Double this$defInn = this.getDefInn();
            Double other$defInn = other.getDefInn();
            if (this$defInn == null ? other$defInn != null : !((Object)this$defInn).equals(other$defInn)) {
                return false;
            }
            Integer this$dp = this.getDp();
            Integer other$dp = other.getDp();
            if (this$dp == null ? other$dp != null : !((Object)this$dp).equals(other$dp)) {
                return false;
            }
            Integer this$pb = this.getPb();
            Integer other$pb = other.getPb();
            if (this$pb == null ? other$pb != null : !((Object)this$pb).equals(other$pb)) {
                return false;
            }
            Integer this$catcherSb = this.getCatcherSb();
            Integer other$catcherSb = other.getCatcherSb();
            if (this$catcherSb == null ? other$catcherSb != null : !((Object)this$catcherSb).equals(other$catcherSb)) {
                return false;
            }
            Integer this$catcherCs = this.getCatcherCs();
            Integer other$catcherCs = other.getCatcherCs();
            if (this$catcherCs == null ? other$catcherCs != null : !((Object)this$catcherCs).equals(other$catcherCs)) {
                return false;
            }
            String this$position = this.getPosition();
            String other$position = other.getPosition();
            if (this$position == null ? other$position != null : !this$position.equals(other$position)) {
                return false;
            }
            String this$number = this.getNumber();
            String other$number = other.getNumber();
            if (this$number == null ? other$number != null : !this$number.equals(other$number)) {
                return false;
            }
            String this$batHand = this.getBatHand();
            String other$batHand = other.getBatHand();
            if (this$batHand == null ? other$batHand != null : !this$batHand.equals(other$batHand)) {
                return false;
            }
            String this$throwHand = this.getThrowHand();
            String other$throwHand = other.getThrowHand();
            return !(this$throwHand == null ? other$throwHand != null : !this$throwHand.equals(other$throwHand));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof StatPart;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            Long $id = this.getId();
            result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
            Long $teamId = this.getTeamId();
            result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
            Long $playerId = this.getPlayerId();
            result = result * 59 + ($playerId == null ? 43 : ((Object)$playerId).hashCode());
            Integer $battingOrder = this.getBattingOrder();
            result = result * 59 + ($battingOrder == null ? 43 : ((Object)$battingOrder).hashCode());
            Integer $listOrder = this.getListOrder();
            result = result * 59 + ($listOrder == null ? 43 : ((Object)$listOrder).hashCode());
            Integer $pa = this.getPa();
            result = result * 59 + ($pa == null ? 43 : ((Object)$pa).hashCode());
            Integer $ab = this.getAb();
            result = result * 59 + ($ab == null ? 43 : ((Object)$ab).hashCode());
            Integer $r = this.getR();
            result = result * 59 + ($r == null ? 43 : ((Object)$r).hashCode());
            Integer $h = this.getH();
            result = result * 59 + ($h == null ? 43 : ((Object)$h).hashCode());
            Integer $e = this.getE();
            result = result * 59 + ($e == null ? 43 : ((Object)$e).hashCode());
            Integer $bbHp = this.getBbHp();
            result = result * 59 + ($bbHp == null ? 43 : ((Object)$bbHp).hashCode());
            Integer $sb = this.getSb();
            result = result * 59 + ($sb == null ? 43 : ((Object)$sb).hashCode());
            Integer $so = this.getSo();
            result = result * 59 + ($so == null ? 43 : ((Object)$so).hashCode());
            Integer $soSwing = this.getSoSwing();
            result = result * 59 + ($soSwing == null ? 43 : ((Object)$soSwing).hashCode());
            Integer $soLooking = this.getSoLooking();
            result = result * 59 + ($soLooking == null ? 43 : ((Object)$soLooking).hashCode());
            Integer $rbi = this.getRbi();
            result = result * 59 + ($rbi == null ? 43 : ((Object)$rbi).hashCode());
            Integer $hr = this.getHr();
            result = result * 59 + ($hr == null ? 43 : ((Object)$hr).hashCode());
            Integer $insideParkHr = this.getInsideParkHr();
            result = result * 59 + ($insideParkHr == null ? 43 : ((Object)$insideParkHr).hashCode());
            Integer $doubles = this.getDoubles();
            result = result * 59 + ($doubles == null ? 43 : ((Object)$doubles).hashCode());
            Integer $triples = this.getTriples();
            result = result * 59 + ($triples == null ? 43 : ((Object)$triples).hashCode());
            Integer $gdp = this.getGdp();
            result = result * 59 + ($gdp == null ? 43 : ((Object)$gdp).hashCode());
            Integer $sh = this.getSh();
            result = result * 59 + ($sh == null ? 43 : ((Object)$sh).hashCode());
            Integer $sf = this.getSf();
            result = result * 59 + ($sf == null ? 43 : ((Object)$sf).hashCode());
            Integer $bb = this.getBb();
            result = result * 59 + ($bb == null ? 43 : ((Object)$bb).hashCode());
            Integer $ibb = this.getIbb();
            result = result * 59 + ($ibb == null ? 43 : ((Object)$ibb).hashCode());
            Integer $hbp = this.getHbp();
            result = result * 59 + ($hbp == null ? 43 : ((Object)$hbp).hashCode());
            Integer $cs = this.getCs();
            result = result * 59 + ($cs == null ? 43 : ((Object)$cs).hashCode());
            Integer $isPitcher = this.getIsPitcher();
            result = result * 59 + ($isPitcher == null ? 43 : ((Object)$isPitcher).hashCode());
            Integer $pitcherOrder = this.getPitcherOrder();
            result = result * 59 + ($pitcherOrder == null ? 43 : ((Object)$pitcherOrder).hashCode());
            Double $ip = this.getIp();
            result = result * 59 + ($ip == null ? 43 : ((Object)$ip).hashCode());
            Integer $er = this.getEr();
            result = result * 59 + ($er == null ? 43 : ((Object)$er).hashCode());
            Integer $pitchH = this.getPitchH();
            result = result * 59 + ($pitchH == null ? 43 : ((Object)$pitchH).hashCode());
            Integer $pitchBbHp = this.getPitchBbHp();
            result = result * 59 + ($pitchBbHp == null ? 43 : ((Object)$pitchBbHp).hashCode());
            Integer $pitchSo = this.getPitchSo();
            result = result * 59 + ($pitchSo == null ? 43 : ((Object)$pitchSo).hashCode());
            Integer $pitchHr = this.getPitchHr();
            result = result * 59 + ($pitchHr == null ? 43 : ((Object)$pitchHr).hashCode());
            Integer $pitchInsideParkHr = this.getPitchInsideParkHr();
            result = result * 59 + ($pitchInsideParkHr == null ? 43 : ((Object)$pitchInsideParkHr).hashCode());
            Integer $pitchR = this.getPitchR();
            result = result * 59 + ($pitchR == null ? 43 : ((Object)$pitchR).hashCode());
            Integer $pitchPa = this.getPitchPa();
            result = result * 59 + ($pitchPa == null ? 43 : ((Object)$pitchPa).hashCode());
            Integer $pitchBf = this.getPitchBf();
            result = result * 59 + ($pitchBf == null ? 43 : ((Object)$pitchBf).hashCode());
            Integer $pitchAb = this.getPitchAb();
            result = result * 59 + ($pitchAb == null ? 43 : ((Object)$pitchAb).hashCode());
            Integer $np = this.getNp();
            result = result * 59 + ($np == null ? 43 : ((Object)$np).hashCode());
            Integer $pitchBb = this.getPitchBb();
            result = result * 59 + ($pitchBb == null ? 43 : ((Object)$pitchBb).hashCode());
            Integer $pitchIbb = this.getPitchIbb();
            result = result * 59 + ($pitchIbb == null ? 43 : ((Object)$pitchIbb).hashCode());
            Integer $pitchHbp = this.getPitchHbp();
            result = result * 59 + ($pitchHbp == null ? 43 : ((Object)$pitchHbp).hashCode());
            Integer $wp = this.getWp();
            result = result * 59 + ($wp == null ? 43 : ((Object)$wp).hashCode());
            Integer $bk = this.getBk();
            result = result * 59 + ($bk == null ? 43 : ((Object)$bk).hashCode());
            Integer $go = this.getGo();
            result = result * 59 + ($go == null ? 43 : ((Object)$go).hashCode());
            Integer $fo = this.getFo();
            result = result * 59 + ($fo == null ? 43 : ((Object)$fo).hashCode());
            Integer $gs = this.getGs();
            result = result * 59 + ($gs == null ? 43 : ((Object)$gs).hashCode());
            Integer $svo = this.getSvo();
            result = result * 59 + ($svo == null ? 43 : ((Object)$svo).hashCode());
            Integer $cg = this.getCg();
            result = result * 59 + ($cg == null ? 43 : ((Object)$cg).hashCode());
            Integer $pg = this.getPg();
            result = result * 59 + ($pg == null ? 43 : ((Object)$pg).hashCode());
            Integer $w = this.getW();
            result = result * 59 + ($w == null ? 43 : ((Object)$w).hashCode());
            Integer $l = this.getL();
            result = result * 59 + ($l == null ? 43 : ((Object)$l).hashCode());
            Integer $sv = this.getSv();
            result = result * 59 + ($sv == null ? 43 : ((Object)$sv).hashCode());
            Integer $hld = this.getHld();
            result = result * 59 + ($hld == null ? 43 : ((Object)$hld).hashCode());
            Integer $po = this.getPo();
            result = result * 59 + ($po == null ? 43 : ((Object)$po).hashCode());
            Integer $a = this.getA();
            result = result * 59 + ($a == null ? 43 : ((Object)$a).hashCode());
            Integer $tc = this.getTc();
            result = result * 59 + ($tc == null ? 43 : ((Object)$tc).hashCode());
            Integer $fieldingGs = this.getFieldingGs();
            result = result * 59 + ($fieldingGs == null ? 43 : ((Object)$fieldingGs).hashCode());
            Double $defInn = this.getDefInn();
            result = result * 59 + ($defInn == null ? 43 : ((Object)$defInn).hashCode());
            Integer $dp = this.getDp();
            result = result * 59 + ($dp == null ? 43 : ((Object)$dp).hashCode());
            Integer $pb = this.getPb();
            result = result * 59 + ($pb == null ? 43 : ((Object)$pb).hashCode());
            Integer $catcherSb = this.getCatcherSb();
            result = result * 59 + ($catcherSb == null ? 43 : ((Object)$catcherSb).hashCode());
            Integer $catcherCs = this.getCatcherCs();
            result = result * 59 + ($catcherCs == null ? 43 : ((Object)$catcherCs).hashCode());
            String $position = this.getPosition();
            result = result * 59 + ($position == null ? 43 : $position.hashCode());
            String $number = this.getNumber();
            result = result * 59 + ($number == null ? 43 : $number.hashCode());
            String $batHand = this.getBatHand();
            result = result * 59 + ($batHand == null ? 43 : $batHand.hashCode());
            String $throwHand = this.getThrowHand();
            result = result * 59 + ($throwHand == null ? 43 : $throwHand.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "SaveGameResultDTO.StatPart(id=" + this.getId() + ", teamId=" + this.getTeamId() + ", playerId=" + this.getPlayerId() + ", battingOrder=" + this.getBattingOrder() + ", listOrder=" + this.getListOrder() + ", position=" + this.getPosition() + ", number=" + this.getNumber() + ", batHand=" + this.getBatHand() + ", throwHand=" + this.getThrowHand() + ", pa=" + this.getPa() + ", ab=" + this.getAb() + ", r=" + this.getR() + ", h=" + this.getH() + ", e=" + this.getE() + ", bbHp=" + this.getBbHp() + ", sb=" + this.getSb() + ", so=" + this.getSo() + ", soSwing=" + this.getSoSwing() + ", soLooking=" + this.getSoLooking() + ", rbi=" + this.getRbi() + ", hr=" + this.getHr() + ", insideParkHr=" + this.getInsideParkHr() + ", doubles=" + this.getDoubles() + ", triples=" + this.getTriples() + ", gdp=" + this.getGdp() + ", sh=" + this.getSh() + ", sf=" + this.getSf() + ", bb=" + this.getBb() + ", ibb=" + this.getIbb() + ", hbp=" + this.getHbp() + ", cs=" + this.getCs() + ", isPitcher=" + this.getIsPitcher() + ", pitcherOrder=" + this.getPitcherOrder() + ", ip=" + this.getIp() + ", er=" + this.getEr() + ", pitchH=" + this.getPitchH() + ", pitchBbHp=" + this.getPitchBbHp() + ", pitchSo=" + this.getPitchSo() + ", pitchHr=" + this.getPitchHr() + ", pitchInsideParkHr=" + this.getPitchInsideParkHr() + ", pitchR=" + this.getPitchR() + ", pitchPa=" + this.getPitchPa() + ", pitchBf=" + this.getPitchBf() + ", pitchAb=" + this.getPitchAb() + ", np=" + this.getNp() + ", pitchBb=" + this.getPitchBb() + ", pitchIbb=" + this.getPitchIbb() + ", pitchHbp=" + this.getPitchHbp() + ", wp=" + this.getWp() + ", bk=" + this.getBk() + ", go=" + this.getGo() + ", fo=" + this.getFo() + ", gs=" + this.getGs() + ", svo=" + this.getSvo() + ", cg=" + this.getCg() + ", pg=" + this.getPg() + ", w=" + this.getW() + ", l=" + this.getL() + ", sv=" + this.getSv() + ", hld=" + this.getHld() + ", po=" + this.getPo() + ", a=" + this.getA() + ", tc=" + this.getTc() + ", fieldingGs=" + this.getFieldingGs() + ", defInn=" + this.getDefInn() + ", dp=" + this.getDp() + ", pb=" + this.getPb() + ", catcherSb=" + this.getCatcherSb() + ", catcherCs=" + this.getCatcherCs() + ")";
        }
    }
}



