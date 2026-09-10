/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.HitSprayDTO
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class HitSprayDTO {
    private Long gameId;
    private Long playerId;
    private Long teamId;
    private Short inning;
    private String half;
    private Short batterOrder;
    private Short outsBefore;
    private String runnersBefore;
    private String resultCode;
    private String bipCode;
    private Short rbi;
    private Double sprayX;
    private Double sprayY;
    private String sprayZone;
    private String sprayDepth;

    @Generated
    public HitSprayDTO() {
    }

    @Generated
    public Long getGameId() {
        return this.gameId;
    }

    @Generated
    public Long getPlayerId() {
        return this.playerId;
    }

    @Generated
    public Long getTeamId() {
        return this.teamId;
    }

    @Generated
    public Short getInning() {
        return this.inning;
    }

    @Generated
    public String getHalf() {
        return this.half;
    }

    @Generated
    public Short getBatterOrder() {
        return this.batterOrder;
    }

    @Generated
    public Short getOutsBefore() {
        return this.outsBefore;
    }

    @Generated
    public String getRunnersBefore() {
        return this.runnersBefore;
    }

    @Generated
    public String getResultCode() {
        return this.resultCode;
    }

    @Generated
    public String getBipCode() {
        return this.bipCode;
    }

    @Generated
    public Short getRbi() {
        return this.rbi;
    }

    @Generated
    public Double getSprayX() {
        return this.sprayX;
    }

    @Generated
    public Double getSprayY() {
        return this.sprayY;
    }

    @Generated
    public String getSprayZone() {
        return this.sprayZone;
    }

    @Generated
    public String getSprayDepth() {
        return this.sprayDepth;
    }

    @Generated
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    @Generated
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    @Generated
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    @Generated
    public void setInning(Short inning) {
        this.inning = inning;
    }

    @Generated
    public void setHalf(String half) {
        this.half = half;
    }

    @Generated
    public void setBatterOrder(Short batterOrder) {
        this.batterOrder = batterOrder;
    }

    @Generated
    public void setOutsBefore(Short outsBefore) {
        this.outsBefore = outsBefore;
    }

    @Generated
    public void setRunnersBefore(String runnersBefore) {
        this.runnersBefore = runnersBefore;
    }

    @Generated
    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    @Generated
    public void setBipCode(String bipCode) {
        this.bipCode = bipCode;
    }

    @Generated
    public void setRbi(Short rbi) {
        this.rbi = rbi;
    }

    @Generated
    public void setSprayX(Double sprayX) {
        this.sprayX = sprayX;
    }

    @Generated
    public void setSprayY(Double sprayY) {
        this.sprayY = sprayY;
    }

    @Generated
    public void setSprayZone(String sprayZone) {
        this.sprayZone = sprayZone;
    }

    @Generated
    public void setSprayDepth(String sprayDepth) {
        this.sprayDepth = sprayDepth;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HitSprayDTO)) {
            return false;
        }
        HitSprayDTO other = (HitSprayDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$gameId = this.getGameId();
        Long other$gameId = other.getGameId();
        if (this$gameId == null ? other$gameId != null : !((Object)this$gameId).equals(other$gameId)) {
            return false;
        }
        Long this$playerId = this.getPlayerId();
        Long other$playerId = other.getPlayerId();
        if (this$playerId == null ? other$playerId != null : !((Object)this$playerId).equals(other$playerId)) {
            return false;
        }
        Long this$teamId = this.getTeamId();
        Long other$teamId = other.getTeamId();
        if (this$teamId == null ? other$teamId != null : !((Object)this$teamId).equals(other$teamId)) {
            return false;
        }
        Short this$inning = this.getInning();
        Short other$inning = other.getInning();
        if (this$inning == null ? other$inning != null : !((Object)this$inning).equals(other$inning)) {
            return false;
        }
        Short this$batterOrder = this.getBatterOrder();
        Short other$batterOrder = other.getBatterOrder();
        if (this$batterOrder == null ? other$batterOrder != null : !((Object)this$batterOrder).equals(other$batterOrder)) {
            return false;
        }
        Short this$outsBefore = this.getOutsBefore();
        Short other$outsBefore = other.getOutsBefore();
        if (this$outsBefore == null ? other$outsBefore != null : !((Object)this$outsBefore).equals(other$outsBefore)) {
            return false;
        }
        Short this$rbi = this.getRbi();
        Short other$rbi = other.getRbi();
        if (this$rbi == null ? other$rbi != null : !((Object)this$rbi).equals(other$rbi)) {
            return false;
        }
        Double this$sprayX = this.getSprayX();
        Double other$sprayX = other.getSprayX();
        if (this$sprayX == null ? other$sprayX != null : !((Object)this$sprayX).equals(other$sprayX)) {
            return false;
        }
        Double this$sprayY = this.getSprayY();
        Double other$sprayY = other.getSprayY();
        if (this$sprayY == null ? other$sprayY != null : !((Object)this$sprayY).equals(other$sprayY)) {
            return false;
        }
        String this$half = this.getHalf();
        String other$half = other.getHalf();
        if (this$half == null ? other$half != null : !this$half.equals(other$half)) {
            return false;
        }
        String this$runnersBefore = this.getRunnersBefore();
        String other$runnersBefore = other.getRunnersBefore();
        if (this$runnersBefore == null ? other$runnersBefore != null : !this$runnersBefore.equals(other$runnersBefore)) {
            return false;
        }
        String this$resultCode = this.getResultCode();
        String other$resultCode = other.getResultCode();
        if (this$resultCode == null ? other$resultCode != null : !this$resultCode.equals(other$resultCode)) {
            return false;
        }
        String this$bipCode = this.getBipCode();
        String other$bipCode = other.getBipCode();
        if (this$bipCode == null ? other$bipCode != null : !this$bipCode.equals(other$bipCode)) {
            return false;
        }
        String this$sprayZone = this.getSprayZone();
        String other$sprayZone = other.getSprayZone();
        if (this$sprayZone == null ? other$sprayZone != null : !this$sprayZone.equals(other$sprayZone)) {
            return false;
        }
        String this$sprayDepth = this.getSprayDepth();
        String other$sprayDepth = other.getSprayDepth();
        return !(this$sprayDepth == null ? other$sprayDepth != null : !this$sprayDepth.equals(other$sprayDepth));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HitSprayDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $gameId = this.getGameId();
        result = result * 59 + ($gameId == null ? 43 : ((Object)$gameId).hashCode());
        Long $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : ((Object)$playerId).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Short $inning = this.getInning();
        result = result * 59 + ($inning == null ? 43 : ((Object)$inning).hashCode());
        Short $batterOrder = this.getBatterOrder();
        result = result * 59 + ($batterOrder == null ? 43 : ((Object)$batterOrder).hashCode());
        Short $outsBefore = this.getOutsBefore();
        result = result * 59 + ($outsBefore == null ? 43 : ((Object)$outsBefore).hashCode());
        Short $rbi = this.getRbi();
        result = result * 59 + ($rbi == null ? 43 : ((Object)$rbi).hashCode());
        Double $sprayX = this.getSprayX();
        result = result * 59 + ($sprayX == null ? 43 : ((Object)$sprayX).hashCode());
        Double $sprayY = this.getSprayY();
        result = result * 59 + ($sprayY == null ? 43 : ((Object)$sprayY).hashCode());
        String $half = this.getHalf();
        result = result * 59 + ($half == null ? 43 : $half.hashCode());
        String $runnersBefore = this.getRunnersBefore();
        result = result * 59 + ($runnersBefore == null ? 43 : $runnersBefore.hashCode());
        String $resultCode = this.getResultCode();
        result = result * 59 + ($resultCode == null ? 43 : $resultCode.hashCode());
        String $bipCode = this.getBipCode();
        result = result * 59 + ($bipCode == null ? 43 : $bipCode.hashCode());
        String $sprayZone = this.getSprayZone();
        result = result * 59 + ($sprayZone == null ? 43 : $sprayZone.hashCode());
        String $sprayDepth = this.getSprayDepth();
        result = result * 59 + ($sprayDepth == null ? 43 : $sprayDepth.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "HitSprayDTO(gameId=" + this.getGameId() + ", playerId=" + this.getPlayerId() + ", teamId=" + this.getTeamId() + ", inning=" + this.getInning() + ", half=" + this.getHalf() + ", batterOrder=" + this.getBatterOrder() + ", outsBefore=" + this.getOutsBefore() + ", runnersBefore=" + this.getRunnersBefore() + ", resultCode=" + this.getResultCode() + ", bipCode=" + this.getBipCode() + ", rbi=" + this.getRbi() + ", sprayX=" + this.getSprayX() + ", sprayY=" + this.getSprayY() + ", sprayZone=" + this.getSprayZone() + ", sprayDepth=" + this.getSprayDepth() + ")";
    }
}

