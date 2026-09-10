/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.FieldingLeaderDTO
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class FieldingLeaderDTO {
    private Long playerId;
    private String playerName;
    private Long teamId;
    private String teamName;
    private String position;
    private String number;
    private Integer gp;
    private Integer gs;
    private Double inn;
    private Integer tc;
    private Integer po;
    private Integer a;
    private Integer e;
    private Integer dp;
    private Double tcPct;

    @Generated
    public FieldingLeaderDTO() {
    }

    @Generated
    public Long getPlayerId() {
        return this.playerId;
    }

    @Generated
    public String getPlayerName() {
        return this.playerName;
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
    public String getPosition() {
        return this.position;
    }

    @Generated
    public String getNumber() {
        return this.number;
    }

    @Generated
    public Integer getGp() {
        return this.gp;
    }

    @Generated
    public Integer getGs() {
        return this.gs;
    }

    @Generated
    public Double getInn() {
        return this.inn;
    }

    @Generated
    public Integer getTc() {
        return this.tc;
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
    public Integer getE() {
        return this.e;
    }

    @Generated
    public Integer getDp() {
        return this.dp;
    }

    @Generated
    public Double getTcPct() {
        return this.tcPct;
    }

    @Generated
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    @Generated
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
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
    public void setPosition(String position) {
        this.position = position;
    }

    @Generated
    public void setNumber(String number) {
        this.number = number;
    }

    @Generated
    public void setGp(Integer gp) {
        this.gp = gp;
    }

    @Generated
    public void setGs(Integer gs) {
        this.gs = gs;
    }

    @Generated
    public void setInn(Double inn) {
        this.inn = inn;
    }

    @Generated
    public void setTc(Integer tc) {
        this.tc = tc;
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
    public void setE(Integer e) {
        this.e = e;
    }

    @Generated
    public void setDp(Integer dp) {
        this.dp = dp;
    }

    @Generated
    public void setTcPct(Double tcPct) {
        this.tcPct = tcPct;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FieldingLeaderDTO)) {
            return false;
        }
        FieldingLeaderDTO other = (FieldingLeaderDTO)o;
        if (!other.canEqual((Object)this)) {
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
        Integer this$gp = this.getGp();
        Integer other$gp = other.getGp();
        if (this$gp == null ? other$gp != null : !((Object)this$gp).equals(other$gp)) {
            return false;
        }
        Integer this$gs = this.getGs();
        Integer other$gs = other.getGs();
        if (this$gs == null ? other$gs != null : !((Object)this$gs).equals(other$gs)) {
            return false;
        }
        Double this$inn = this.getInn();
        Double other$inn = other.getInn();
        if (this$inn == null ? other$inn != null : !((Object)this$inn).equals(other$inn)) {
            return false;
        }
        Integer this$tc = this.getTc();
        Integer other$tc = other.getTc();
        if (this$tc == null ? other$tc != null : !((Object)this$tc).equals(other$tc)) {
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
        Integer this$e = this.getE();
        Integer other$e = other.getE();
        if (this$e == null ? other$e != null : !((Object)this$e).equals(other$e)) {
            return false;
        }
        Integer this$dp = this.getDp();
        Integer other$dp = other.getDp();
        if (this$dp == null ? other$dp != null : !((Object)this$dp).equals(other$dp)) {
            return false;
        }
        Double this$tcPct = this.getTcPct();
        Double other$tcPct = other.getTcPct();
        if (this$tcPct == null ? other$tcPct != null : !((Object)this$tcPct).equals(other$tcPct)) {
            return false;
        }
        String this$playerName = this.getPlayerName();
        String other$playerName = other.getPlayerName();
        if (this$playerName == null ? other$playerName != null : !this$playerName.equals(other$playerName)) {
            return false;
        }
        String this$teamName = this.getTeamName();
        String other$teamName = other.getTeamName();
        if (this$teamName == null ? other$teamName != null : !this$teamName.equals(other$teamName)) {
            return false;
        }
        String this$position = this.getPosition();
        String other$position = other.getPosition();
        if (this$position == null ? other$position != null : !this$position.equals(other$position)) {
            return false;
        }
        String this$number = this.getNumber();
        String other$number = other.getNumber();
        return !(this$number == null ? other$number != null : !this$number.equals(other$number));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof FieldingLeaderDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : ((Object)$playerId).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Integer $gp = this.getGp();
        result = result * 59 + ($gp == null ? 43 : ((Object)$gp).hashCode());
        Integer $gs = this.getGs();
        result = result * 59 + ($gs == null ? 43 : ((Object)$gs).hashCode());
        Double $inn = this.getInn();
        result = result * 59 + ($inn == null ? 43 : ((Object)$inn).hashCode());
        Integer $tc = this.getTc();
        result = result * 59 + ($tc == null ? 43 : ((Object)$tc).hashCode());
        Integer $po = this.getPo();
        result = result * 59 + ($po == null ? 43 : ((Object)$po).hashCode());
        Integer $a = this.getA();
        result = result * 59 + ($a == null ? 43 : ((Object)$a).hashCode());
        Integer $e = this.getE();
        result = result * 59 + ($e == null ? 43 : ((Object)$e).hashCode());
        Integer $dp = this.getDp();
        result = result * 59 + ($dp == null ? 43 : ((Object)$dp).hashCode());
        Double $tcPct = this.getTcPct();
        result = result * 59 + ($tcPct == null ? 43 : ((Object)$tcPct).hashCode());
        String $playerName = this.getPlayerName();
        result = result * 59 + ($playerName == null ? 43 : $playerName.hashCode());
        String $teamName = this.getTeamName();
        result = result * 59 + ($teamName == null ? 43 : $teamName.hashCode());
        String $position = this.getPosition();
        result = result * 59 + ($position == null ? 43 : $position.hashCode());
        String $number = this.getNumber();
        result = result * 59 + ($number == null ? 43 : $number.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "FieldingLeaderDTO(playerId=" + this.getPlayerId() + ", playerName=" + this.getPlayerName() + ", teamId=" + this.getTeamId() + ", teamName=" + this.getTeamName() + ", position=" + this.getPosition() + ", number=" + this.getNumber() + ", gp=" + this.getGp() + ", gs=" + this.getGs() + ", inn=" + this.getInn() + ", tc=" + this.getTc() + ", po=" + this.getPo() + ", a=" + this.getA() + ", e=" + this.getE() + ", dp=" + this.getDp() + ", tcPct=" + this.getTcPct() + ")";
    }
}

