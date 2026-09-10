/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.GameSaveLiveDTO
 *  com.bsball.model.dto.GameSaveLiveDTO$GamePlayerStatPart
 *  com.bsball.model.dto.GameSaveLiveDTO$GameUpdatePart
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.common.BoolToIntDeserializer;
import com.bsball.model.dto.GameSaveLiveDTO;
import com.fasterxml.jackson.annotation.JsonDeserializeAs;
import java.util.List;
import lombok.Generated;

public class GameSaveLiveDTO {
    private GameUpdatePart game;
    private List<GamePlayerStatPart> stats;

    @Generated
    public GameSaveLiveDTO() {
    }

    @Generated
    public GameUpdatePart getGame() {
        return this.game;
    }

    @Generated
    public List<GamePlayerStatPart> getStats() {
        return this.stats;
    }

    @Generated
    public void setGame(GameUpdatePart game) {
        this.game = game;
    }

    @Generated
    public void setStats(List<GamePlayerStatPart> stats) {
        this.stats = stats;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GameSaveLiveDTO)) {
            return false;
        }
        GameSaveLiveDTO other = (GameSaveLiveDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        GameUpdatePart this$game = this.getGame();
        GameUpdatePart other$game = other.getGame();
        if (this$game == null ? other$game != null : !this$game.equals(other$game)) {
            return false;
        }
        List this$stats = this.getStats();
        List other$stats = other.getStats();
        return !(this$stats == null ? other$stats != null : !((Object)this$stats).equals(other$stats));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof GameSaveLiveDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        GameUpdatePart $game = this.getGame();
        result = result * 59 + ($game == null ? 43 : $game.hashCode());
        List $stats = this.getStats();
        result = result * 59 + ($stats == null ? 43 : ((Object)$stats).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "GameSaveLiveDTO(game=" + String.valueOf(this.getGame()) + ", stats=" + String.valueOf(this.getStats()) + ")";
    }

    public static class GamePlayerStatPart {
        private Long teamId;
        private Long playerId;
        private Integer battingOrder;
        private Integer listOrder;
        private String position;
        private Integer pa;
        private Integer ab;
        private Integer r;
        private Integer h;
        private Integer bbHp;
        private Integer sb;
        private Integer so;
        private Integer soSwing;
        private Integer soLooking;
        private Integer e;
        private Integer rbi;
        private Integer doubles;
        private Integer triples;
        private Integer hr;
        private Integer insideParkHr;
        private Integer sh;
        private Integer sf;
        private Integer bb;
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
        private Integer np;
        private Integer wp;
        private Integer bk;
        private Integer po;
        private Integer a;
        private Integer tc;

        @Generated
        public GamePlayerStatPart() {
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
        public Integer getE() {
            return this.e;
        }

        @Generated
        public Integer getRbi() {
            return this.rbi;
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
        public Integer getNp() {
            return this.np;
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
        public void setE(Integer e) {
            this.e = e;
        }

        @Generated
        public void setRbi(Integer rbi) {
            this.rbi = rbi;
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
        public void setNp(Integer np) {
            this.np = np;
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
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof GamePlayerStatPart)) {
                return false;
            }
            GameSaveLiveDTO.GamePlayerStatPart other = (GamePlayerStatPart)o;
            if (!other.canEqual(this)) {
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
            Integer this$e = this.getE();
            Integer other$e = other.getE();
            if (this$e == null ? other$e != null : !((Object)this$e).equals(other$e)) {
                return false;
            }
            Integer this$rbi = this.getRbi();
            Integer other$rbi = other.getRbi();
            if (this$rbi == null ? other$rbi != null : !((Object)this$rbi).equals(other$rbi)) {
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
            Integer this$np = this.getNp();
            Integer other$np = other.getNp();
            if (this$np == null ? other$np != null : !((Object)this$np).equals(other$np)) {
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
            String this$position = this.getPosition();
            String other$position = other.getPosition();
            return !(this$position == null ? other$position != null : !this$position.equals(other$position));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof GamePlayerStatPart;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
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
            Integer $e = this.getE();
            result = result * 59 + ($e == null ? 43 : ((Object)$e).hashCode());
            Integer $rbi = this.getRbi();
            result = result * 59 + ($rbi == null ? 43 : ((Object)$rbi).hashCode());
            Integer $doubles = this.getDoubles();
            result = result * 59 + ($doubles == null ? 43 : ((Object)$doubles).hashCode());
            Integer $triples = this.getTriples();
            result = result * 59 + ($triples == null ? 43 : ((Object)$triples).hashCode());
            Integer $hr = this.getHr();
            result = result * 59 + ($hr == null ? 43 : ((Object)$hr).hashCode());
            Integer $insideParkHr = this.getInsideParkHr();
            result = result * 59 + ($insideParkHr == null ? 43 : ((Object)$insideParkHr).hashCode());
            Integer $sh = this.getSh();
            result = result * 59 + ($sh == null ? 43 : ((Object)$sh).hashCode());
            Integer $sf = this.getSf();
            result = result * 59 + ($sf == null ? 43 : ((Object)$sf).hashCode());
            Integer $bb = this.getBb();
            result = result * 59 + ($bb == null ? 43 : ((Object)$bb).hashCode());
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
            Integer $np = this.getNp();
            result = result * 59 + ($np == null ? 43 : ((Object)$np).hashCode());
            Integer $wp = this.getWp();
            result = result * 59 + ($wp == null ? 43 : ((Object)$wp).hashCode());
            Integer $bk = this.getBk();
            result = result * 59 + ($bk == null ? 43 : ((Object)$bk).hashCode());
            Integer $po = this.getPo();
            result = result * 59 + ($po == null ? 43 : ((Object)$po).hashCode());
            Integer $a = this.getA();
            result = result * 59 + ($a == null ? 43 : ((Object)$a).hashCode());
            Integer $tc = this.getTc();
            result = result * 59 + ($tc == null ? 43 : ((Object)$tc).hashCode());
            String $position = this.getPosition();
            result = result * 59 + ($position == null ? 43 : $position.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "GameSaveLiveDTO.GamePlayerStatPart(teamId=" + this.getTeamId() + ", playerId=" + this.getPlayerId() + ", battingOrder=" + this.getBattingOrder() + ", listOrder=" + this.getListOrder() + ", position=" + this.getPosition() + ", pa=" + this.getPa() + ", ab=" + this.getAb() + ", r=" + this.getR() + ", h=" + this.getH() + ", bbHp=" + this.getBbHp() + ", sb=" + this.getSb() + ", so=" + this.getSo() + ", soSwing=" + this.getSoSwing() + ", soLooking=" + this.getSoLooking() + ", e=" + this.getE() + ", rbi=" + this.getRbi() + ", doubles=" + this.getDoubles() + ", triples=" + this.getTriples() + ", hr=" + this.getHr() + ", insideParkHr=" + this.getInsideParkHr() + ", sh=" + this.getSh() + ", sf=" + this.getSf() + ", bb=" + this.getBb() + ", hbp=" + this.getHbp() + ", cs=" + this.getCs() + ", isPitcher=" + this.getIsPitcher() + ", pitcherOrder=" + this.getPitcherOrder() + ", ip=" + this.getIp() + ", er=" + this.getEr() + ", pitchH=" + this.getPitchH() + ", pitchBbHp=" + this.getPitchBbHp() + ", pitchSo=" + this.getPitchSo() + ", pitchHr=" + this.getPitchHr() + ", pitchInsideParkHr=" + this.getPitchInsideParkHr() + ", np=" + this.getNp() + ", wp=" + this.getWp() + ", bk=" + this.getBk() + ", po=" + this.getPo() + ", a=" + this.getA() + ", tc=" + this.getTc() + ")";
        }
    }

    public static class GameUpdatePart {
        private Integer homeScore;
        private Integer awayScore;
        private Object homeScoreByInning;
        private Object awayScoreByInning;
        private Integer homeH;
        private Integer awayH;
        private Integer homeE;
        private Integer awayE;
        private String status;
        private String gameEndTime;
        private Integer inning;
        private String topBottom;

        @Generated
        public GameUpdatePart() {
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
        public String getStatus() {
            return this.status;
        }

        @Generated
        public String getGameEndTime() {
            return this.gameEndTime;
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
        public void setStatus(String status) {
            this.status = status;
        }

        @Generated
        public void setGameEndTime(String gameEndTime) {
            this.gameEndTime = gameEndTime;
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
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof GameUpdatePart)) {
                return false;
            }
            GameSaveLiveDTO.GameUpdatePart other = (GameUpdatePart)o;
            if (!other.canEqual(this)) {
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
            Integer this$inning = this.getInning();
            Integer other$inning = other.getInning();
            if (this$inning == null ? other$inning != null : !((Object)this$inning).equals(other$inning)) {
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
            String this$status = this.getStatus();
            String other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
                return false;
            }
            String this$gameEndTime = this.getGameEndTime();
            String other$gameEndTime = other.getGameEndTime();
            if (this$gameEndTime == null ? other$gameEndTime != null : !this$gameEndTime.equals(other$gameEndTime)) {
                return false;
            }
            String this$topBottom = this.getTopBottom();
            String other$topBottom = other.getTopBottom();
            return !(this$topBottom == null ? other$topBottom != null : !this$topBottom.equals(other$topBottom));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof GameUpdatePart;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
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
            Integer $inning = this.getInning();
            result = result * 59 + ($inning == null ? 43 : ((Object)$inning).hashCode());
            Object $homeScoreByInning = this.getHomeScoreByInning();
            result = result * 59 + ($homeScoreByInning == null ? 43 : $homeScoreByInning.hashCode());
            Object $awayScoreByInning = this.getAwayScoreByInning();
            result = result * 59 + ($awayScoreByInning == null ? 43 : $awayScoreByInning.hashCode());
            String $status = this.getStatus();
            result = result * 59 + ($status == null ? 43 : $status.hashCode());
            String $gameEndTime = this.getGameEndTime();
            result = result * 59 + ($gameEndTime == null ? 43 : $gameEndTime.hashCode());
            String $topBottom = this.getTopBottom();
            result = result * 59 + ($topBottom == null ? 43 : $topBottom.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "GameSaveLiveDTO.GameUpdatePart(homeScore=" + this.getHomeScore() + ", awayScore=" + this.getAwayScore() + ", homeScoreByInning=" + String.valueOf(this.getHomeScoreByInning()) + ", awayScoreByInning=" + String.valueOf(this.getAwayScoreByInning()) + ", homeH=" + this.getHomeH() + ", awayH=" + this.getAwayH() + ", homeE=" + this.getHomeE() + ", awayE=" + this.getAwayE() + ", status=" + this.getStatus() + ", gameEndTime=" + this.getGameEndTime() + ", inning=" + this.getInning() + ", topBottom=" + this.getTopBottom() + ")";
        }
    }
}



