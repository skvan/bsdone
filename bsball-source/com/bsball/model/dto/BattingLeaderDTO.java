/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.BattingLeaderDTO
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class BattingLeaderDTO {
    private Long playerId;
    private String playerName;
    private Long teamId;
    private String teamName;
    private String position;
    private String number;
    private Integer gp;
    private Integer pa;
    private Integer ab;
    private Integer r;
    private Integer rbi;
    private Integer h;
    private Integer singles;
    private Integer doubles;
    private Integer triples;
    private Integer hr;
    private Integer insideParkHr;
    private Integer tb;
    private Integer so;
    private Integer sb;
    private Integer gdp;
    private Integer sh;
    private Integer sf;
    private Integer bb;
    private Integer ibb;
    private Integer hbp;
    private Integer cs;
    private Double avg;
    private Double obp;
    private Double slg;
    private Double sbPct;

    @Generated
    public BattingLeaderDTO() {
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
    public Integer getRbi() {
        return this.rbi;
    }

    @Generated
    public Integer getH() {
        return this.h;
    }

    @Generated
    public Integer getSingles() {
        return this.singles;
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
    public Integer getHr() {
        return this.hr;
    }

    @Generated
    public Integer getInsideParkHr() {
        return this.insideParkHr;
    }

    @Generated
    public Integer getTb() {
        return this.tb;
    }

    @Generated
    public Integer getSo() {
        return this.so;
    }

    @Generated
    public Integer getSb() {
        return this.sb;
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
    public Double getAvg() {
        return this.avg;
    }

    @Generated
    public Double getObp() {
        return this.obp;
    }

    @Generated
    public Double getSlg() {
        return this.slg;
    }

    @Generated
    public Double getSbPct() {
        return this.sbPct;
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
    public void setRbi(Integer rbi) {
        this.rbi = rbi;
    }

    @Generated
    public void setH(Integer h) {
        this.h = h;
    }

    @Generated
    public void setSingles(Integer singles) {
        this.singles = singles;
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
    public void setHr(Integer hr) {
        this.hr = hr;
    }

    @Generated
    public void setInsideParkHr(Integer insideParkHr) {
        this.insideParkHr = insideParkHr;
    }

    @Generated
    public void setTb(Integer tb) {
        this.tb = tb;
    }

    @Generated
    public void setSo(Integer so) {
        this.so = so;
    }

    @Generated
    public void setSb(Integer sb) {
        this.sb = sb;
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
    public void setAvg(Double avg) {
        this.avg = avg;
    }

    @Generated
    public void setObp(Double obp) {
        this.obp = obp;
    }

    @Generated
    public void setSlg(Double slg) {
        this.slg = slg;
    }

    @Generated
    public void setSbPct(Double sbPct) {
        this.sbPct = sbPct;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof BattingLeaderDTO)) {
            return false;
        }
        BattingLeaderDTO other = (BattingLeaderDTO)o;
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
        Integer this$rbi = this.getRbi();
        Integer other$rbi = other.getRbi();
        if (this$rbi == null ? other$rbi != null : !((Object)this$rbi).equals(other$rbi)) {
            return false;
        }
        Integer this$h = this.getH();
        Integer other$h = other.getH();
        if (this$h == null ? other$h != null : !((Object)this$h).equals(other$h)) {
            return false;
        }
        Integer this$singles = this.getSingles();
        Integer other$singles = other.getSingles();
        if (this$singles == null ? other$singles != null : !((Object)this$singles).equals(other$singles)) {
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
        Integer this$tb = this.getTb();
        Integer other$tb = other.getTb();
        if (this$tb == null ? other$tb != null : !((Object)this$tb).equals(other$tb)) {
            return false;
        }
        Integer this$so = this.getSo();
        Integer other$so = other.getSo();
        if (this$so == null ? other$so != null : !((Object)this$so).equals(other$so)) {
            return false;
        }
        Integer this$sb = this.getSb();
        Integer other$sb = other.getSb();
        if (this$sb == null ? other$sb != null : !((Object)this$sb).equals(other$sb)) {
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
        Double this$avg = this.getAvg();
        Double other$avg = other.getAvg();
        if (this$avg == null ? other$avg != null : !((Object)this$avg).equals(other$avg)) {
            return false;
        }
        Double this$obp = this.getObp();
        Double other$obp = other.getObp();
        if (this$obp == null ? other$obp != null : !((Object)this$obp).equals(other$obp)) {
            return false;
        }
        Double this$slg = this.getSlg();
        Double other$slg = other.getSlg();
        if (this$slg == null ? other$slg != null : !((Object)this$slg).equals(other$slg)) {
            return false;
        }
        Double this$sbPct = this.getSbPct();
        Double other$sbPct = other.getSbPct();
        if (this$sbPct == null ? other$sbPct != null : !((Object)this$sbPct).equals(other$sbPct)) {
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
        return other instanceof BattingLeaderDTO;
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
        Integer $pa = this.getPa();
        result = result * 59 + ($pa == null ? 43 : ((Object)$pa).hashCode());
        Integer $ab = this.getAb();
        result = result * 59 + ($ab == null ? 43 : ((Object)$ab).hashCode());
        Integer $r = this.getR();
        result = result * 59 + ($r == null ? 43 : ((Object)$r).hashCode());
        Integer $rbi = this.getRbi();
        result = result * 59 + ($rbi == null ? 43 : ((Object)$rbi).hashCode());
        Integer $h = this.getH();
        result = result * 59 + ($h == null ? 43 : ((Object)$h).hashCode());
        Integer $singles = this.getSingles();
        result = result * 59 + ($singles == null ? 43 : ((Object)$singles).hashCode());
        Integer $doubles = this.getDoubles();
        result = result * 59 + ($doubles == null ? 43 : ((Object)$doubles).hashCode());
        Integer $triples = this.getTriples();
        result = result * 59 + ($triples == null ? 43 : ((Object)$triples).hashCode());
        Integer $hr = this.getHr();
        result = result * 59 + ($hr == null ? 43 : ((Object)$hr).hashCode());
        Integer $insideParkHr = this.getInsideParkHr();
        result = result * 59 + ($insideParkHr == null ? 43 : ((Object)$insideParkHr).hashCode());
        Integer $tb = this.getTb();
        result = result * 59 + ($tb == null ? 43 : ((Object)$tb).hashCode());
        Integer $so = this.getSo();
        result = result * 59 + ($so == null ? 43 : ((Object)$so).hashCode());
        Integer $sb = this.getSb();
        result = result * 59 + ($sb == null ? 43 : ((Object)$sb).hashCode());
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
        Double $avg = this.getAvg();
        result = result * 59 + ($avg == null ? 43 : ((Object)$avg).hashCode());
        Double $obp = this.getObp();
        result = result * 59 + ($obp == null ? 43 : ((Object)$obp).hashCode());
        Double $slg = this.getSlg();
        result = result * 59 + ($slg == null ? 43 : ((Object)$slg).hashCode());
        Double $sbPct = this.getSbPct();
        result = result * 59 + ($sbPct == null ? 43 : ((Object)$sbPct).hashCode());
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
        return "BattingLeaderDTO(playerId=" + this.getPlayerId() + ", playerName=" + this.getPlayerName() + ", teamId=" + this.getTeamId() + ", teamName=" + this.getTeamName() + ", position=" + this.getPosition() + ", number=" + this.getNumber() + ", gp=" + this.getGp() + ", pa=" + this.getPa() + ", ab=" + this.getAb() + ", r=" + this.getR() + ", rbi=" + this.getRbi() + ", h=" + this.getH() + ", singles=" + this.getSingles() + ", doubles=" + this.getDoubles() + ", triples=" + this.getTriples() + ", hr=" + this.getHr() + ", insideParkHr=" + this.getInsideParkHr() + ", tb=" + this.getTb() + ", so=" + this.getSo() + ", sb=" + this.getSb() + ", gdp=" + this.getGdp() + ", sh=" + this.getSh() + ", sf=" + this.getSf() + ", bb=" + this.getBb() + ", ibb=" + this.getIbb() + ", hbp=" + this.getHbp() + ", cs=" + this.getCs() + ", avg=" + this.getAvg() + ", obp=" + this.getObp() + ", slg=" + this.getSlg() + ", sbPct=" + this.getSbPct() + ")";
    }
}

