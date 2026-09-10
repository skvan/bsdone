/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.GameResponseDTO
 *  com.bsball.model.entity.Game
 *  com.fasterxml.jackson.core.JsonProcessingException
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.entity.Game;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;

/*
 * Exception performing whole class analysis ignored.
 */
public class GameResponseDTO {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Long id;
    private Long eventId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeScore;
    private Integer awayScore;
    private List<String> homeScoreByInning;
    private List<String> awayScoreByInning;
    private Integer totalInnings;
    private Integer homeH;
    private Integer awayH;
    private Integer homeE;
    private Integer awayE;
    private String gameTime;
    private String gameEndTime;
    private String gameday;
    private Integer gameNumber;
    private Long stadiumId;
    private String venue;
    private String status;
    private Integer inning;
    private String topBottom;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean hasLiveSnapshot;

    public static GameResponseDTO from(Game g) {
        if (g == null) {
            return null;
        }
        GameResponseDTO dto = new GameResponseDTO();
        dto.setId(g.getId());
        dto.setEventId(g.getEventId());
        dto.setHomeTeamId(g.getHomeTeamId());
        dto.setAwayTeamId(g.getAwayTeamId());
        dto.setHomeScore(g.getHomeScore());
        dto.setAwayScore(g.getAwayScore());
        dto.setHomeScoreByInning(GameResponseDTO.parseInningScores((String)g.getHomeScoreByInning()));
        dto.setAwayScoreByInning(GameResponseDTO.parseInningScores((String)g.getAwayScoreByInning()));
        dto.setTotalInnings(g.getTotalInnings());
        dto.setHomeH(g.getHomeH());
        dto.setAwayH(g.getAwayH());
        dto.setHomeE(g.getHomeE());
        dto.setAwayE(g.getAwayE());
        dto.setGameTime(g.getGameTime());
        dto.setGameEndTime(g.getGameEndTime());
        dto.setGameday(g.getGameday());
        dto.setGameNumber(g.getGameNumber());
        dto.setStadiumId(g.getStadiumId());
        dto.setVenue(g.getVenue());
        dto.setStatus(g.getStatus());
        dto.setInning(g.getInning());
        dto.setTopBottom(g.getTopBottom());
        dto.setSpectatorCount(g.getSpectatorCount());
        dto.setUmpireHp(g.getUmpireHp());
        dto.setUmpire1b(g.getUmpire1b());
        dto.setUmpire2b(g.getUmpire2b());
        dto.setUmpire3b(g.getUmpire3b());
        dto.setRecorders(g.getRecorders());
        dto.setGameTag(g.getGameTag());
        dto.setWeatherSummary(g.getWeatherSummary());
        dto.setWeatherTempC(g.getWeatherTempC());
        dto.setWeatherWind(g.getWeatherWind());
        dto.setWeatherRainProbPct(g.getWeatherRainProbPct());
        dto.setRemark(g.getRemark());
        dto.setIsSpecialResult(g.getIsSpecialResult());
        dto.setShowRemarkInCard(g.getShowRemarkInCard());
        dto.setIncludeStatsInRanking(g.getIncludeStatsInRanking());
        dto.setCreatedAt(g.getCreatedAt());
        dto.setUpdatedAt(g.getUpdatedAt());
        String snap = g.getLiveSnapshotJson();
        dto.setHasLiveSnapshot(Boolean.valueOf(snap != null && !snap.isBlank()));
        return dto;
    }

    private static List<String> parseInningScores(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            List raw = (List)MAPPER.readValue(json.trim(), (TypeReference)new /* Unavailable Anonymous Inner Class!! */);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            ArrayList<String> result = new ArrayList<String>(raw.size());
            for (Object o : raw) {
                result.add(o == null ? "" : String.valueOf(o));
            }
            return result;
        }
        catch (JsonProcessingException e) {
            return null;
        }
    }

    @Generated
    public GameResponseDTO() {
    }

    @Generated
    public Long getId() {
        return this.id;
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
    public Integer getHomeScore() {
        return this.homeScore;
    }

    @Generated
    public Integer getAwayScore() {
        return this.awayScore;
    }

    @Generated
    public List<String> getHomeScoreByInning() {
        return this.homeScoreByInning;
    }

    @Generated
    public List<String> getAwayScoreByInning() {
        return this.awayScoreByInning;
    }

    @Generated
    public Integer getTotalInnings() {
        return this.totalInnings;
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
    public Integer getInning() {
        return this.inning;
    }

    @Generated
    public String getTopBottom() {
        return this.topBottom;
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
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public Boolean getHasLiveSnapshot() {
        return this.hasLiveSnapshot;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
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
    public void setHomeScore(Integer homeScore) {
        this.homeScore = homeScore;
    }

    @Generated
    public void setAwayScore(Integer awayScore) {
        this.awayScore = awayScore;
    }

    @Generated
    public void setHomeScoreByInning(List<String> homeScoreByInning) {
        this.homeScoreByInning = homeScoreByInning;
    }

    @Generated
    public void setAwayScoreByInning(List<String> awayScoreByInning) {
        this.awayScoreByInning = awayScoreByInning;
    }

    @Generated
    public void setTotalInnings(Integer totalInnings) {
        this.totalInnings = totalInnings;
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
    public void setInning(Integer inning) {
        this.inning = inning;
    }

    @Generated
    public void setTopBottom(String topBottom) {
        this.topBottom = topBottom;
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
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public void setHasLiveSnapshot(Boolean hasLiveSnapshot) {
        this.hasLiveSnapshot = hasLiveSnapshot;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GameResponseDTO)) {
            return false;
        }
        GameResponseDTO other = (GameResponseDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
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
        Integer this$totalInnings = this.getTotalInnings();
        Integer other$totalInnings = other.getTotalInnings();
        if (this$totalInnings == null ? other$totalInnings != null : !((Object)this$totalInnings).equals(other$totalInnings)) {
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
        Integer this$inning = this.getInning();
        Integer other$inning = other.getInning();
        if (this$inning == null ? other$inning != null : !((Object)this$inning).equals(other$inning)) {
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
        Boolean this$hasLiveSnapshot = this.getHasLiveSnapshot();
        Boolean other$hasLiveSnapshot = other.getHasLiveSnapshot();
        if (this$hasLiveSnapshot == null ? other$hasLiveSnapshot != null : !((Object)this$hasLiveSnapshot).equals(other$hasLiveSnapshot)) {
            return false;
        }
        List this$homeScoreByInning = this.getHomeScoreByInning();
        List other$homeScoreByInning = other.getHomeScoreByInning();
        if (this$homeScoreByInning == null ? other$homeScoreByInning != null : !((Object)this$homeScoreByInning).equals(other$homeScoreByInning)) {
            return false;
        }
        List this$awayScoreByInning = this.getAwayScoreByInning();
        List other$awayScoreByInning = other.getAwayScoreByInning();
        if (this$awayScoreByInning == null ? other$awayScoreByInning != null : !((Object)this$awayScoreByInning).equals(other$awayScoreByInning)) {
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
        String this$topBottom = this.getTopBottom();
        String other$topBottom = other.getTopBottom();
        if (this$topBottom == null ? other$topBottom != null : !this$topBottom.equals(other$topBottom)) {
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
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof GameResponseDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
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
        Integer $totalInnings = this.getTotalInnings();
        result = result * 59 + ($totalInnings == null ? 43 : ((Object)$totalInnings).hashCode());
        Integer $homeH = this.getHomeH();
        result = result * 59 + ($homeH == null ? 43 : ((Object)$homeH).hashCode());
        Integer $awayH = this.getAwayH();
        result = result * 59 + ($awayH == null ? 43 : ((Object)$awayH).hashCode());
        Integer $homeE = this.getHomeE();
        result = result * 59 + ($homeE == null ? 43 : ((Object)$homeE).hashCode());
        Integer $awayE = this.getAwayE();
        result = result * 59 + ($awayE == null ? 43 : ((Object)$awayE).hashCode());
        Integer $gameNumber = this.getGameNumber();
        result = result * 59 + ($gameNumber == null ? 43 : ((Object)$gameNumber).hashCode());
        Long $stadiumId = this.getStadiumId();
        result = result * 59 + ($stadiumId == null ? 43 : ((Object)$stadiumId).hashCode());
        Integer $inning = this.getInning();
        result = result * 59 + ($inning == null ? 43 : ((Object)$inning).hashCode());
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
        Boolean $hasLiveSnapshot = this.getHasLiveSnapshot();
        result = result * 59 + ($hasLiveSnapshot == null ? 43 : ((Object)$hasLiveSnapshot).hashCode());
        List $homeScoreByInning = this.getHomeScoreByInning();
        result = result * 59 + ($homeScoreByInning == null ? 43 : ((Object)$homeScoreByInning).hashCode());
        List $awayScoreByInning = this.getAwayScoreByInning();
        result = result * 59 + ($awayScoreByInning == null ? 43 : ((Object)$awayScoreByInning).hashCode());
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
        String $topBottom = this.getTopBottom();
        result = result * 59 + ($topBottom == null ? 43 : $topBottom.hashCode());
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
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "GameResponseDTO(id=" + this.getId() + ", eventId=" + this.getEventId() + ", homeTeamId=" + this.getHomeTeamId() + ", awayTeamId=" + this.getAwayTeamId() + ", homeScore=" + this.getHomeScore() + ", awayScore=" + this.getAwayScore() + ", homeScoreByInning=" + String.valueOf(this.getHomeScoreByInning()) + ", awayScoreByInning=" + String.valueOf(this.getAwayScoreByInning()) + ", totalInnings=" + this.getTotalInnings() + ", homeH=" + this.getHomeH() + ", awayH=" + this.getAwayH() + ", homeE=" + this.getHomeE() + ", awayE=" + this.getAwayE() + ", gameTime=" + this.getGameTime() + ", gameEndTime=" + this.getGameEndTime() + ", gameday=" + this.getGameday() + ", gameNumber=" + this.getGameNumber() + ", stadiumId=" + this.getStadiumId() + ", venue=" + this.getVenue() + ", status=" + this.getStatus() + ", inning=" + this.getInning() + ", topBottom=" + this.getTopBottom() + ", spectatorCount=" + this.getSpectatorCount() + ", umpireHp=" + this.getUmpireHp() + ", umpire1b=" + this.getUmpire1b() + ", umpire2b=" + this.getUmpire2b() + ", umpire3b=" + this.getUmpire3b() + ", recorders=" + this.getRecorders() + ", gameTag=" + this.getGameTag() + ", weatherSummary=" + this.getWeatherSummary() + ", weatherTempC=" + this.getWeatherTempC() + ", weatherWind=" + this.getWeatherWind() + ", weatherRainProbPct=" + this.getWeatherRainProbPct() + ", remark=" + this.getRemark() + ", isSpecialResult=" + this.getIsSpecialResult() + ", showRemarkInCard=" + this.getShowRemarkInCard() + ", includeStatsInRanking=" + this.getIncludeStatsInRanking() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ", hasLiveSnapshot=" + this.getHasLiveSnapshot() + ")";
    }
}

