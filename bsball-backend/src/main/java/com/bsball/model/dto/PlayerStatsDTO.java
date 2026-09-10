/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PlayerStatsDTO
 *  com.bsball.model.dto.PlayerStatsDTO$Batting
 *  com.bsball.model.dto.PlayerStatsDTO$Fielding
 *  com.bsball.model.dto.PlayerStatsDTO$Pitching
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.bsball.model.dto.PlayerStatsDTO;
import lombok.Generated;

public class PlayerStatsDTO {
    private Batting batting;
    private Pitching pitching;
    private Fielding fielding;

    @Generated
    public PlayerStatsDTO() {
    }

    @Generated
    public Batting getBatting() {
        return this.batting;
    }

    @Generated
    public Pitching getPitching() {
        return this.pitching;
    }

    @Generated
    public Fielding getFielding() {
        return this.fielding;
    }

    @Generated
    public void setBatting(Batting batting) {
        this.batting = batting;
    }

    @Generated
    public void setPitching(Pitching pitching) {
        this.pitching = pitching;
    }

    @Generated
    public void setFielding(Fielding fielding) {
        this.fielding = fielding;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlayerStatsDTO)) {
            return false;
        }
        PlayerStatsDTO other = (PlayerStatsDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Batting this$batting = this.getBatting();
        Batting other$batting = other.getBatting();
        if (this$batting == null ? other$batting != null : !this$batting.equals(other$batting)) {
            return false;
        }
        Pitching this$pitching = this.getPitching();
        Pitching other$pitching = other.getPitching();
        if (this$pitching == null ? other$pitching != null : !this$pitching.equals(other$pitching)) {
            return false;
        }
        Fielding this$fielding = this.getFielding();
        Fielding other$fielding = other.getFielding();
        return !(this$fielding == null ? other$fielding != null : !this$fielding.equals(other$fielding));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PlayerStatsDTO;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Batting $batting = this.getBatting();
        result = result * 59 + ($batting == null ? 43 : $batting.hashCode());
        Pitching $pitching = this.getPitching();
        result = result * 59 + ($pitching == null ? 43 : $pitching.hashCode());
        Fielding $fielding = this.getFielding();
        result = result * 59 + ($fielding == null ? 43 : $fielding.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "PlayerStatsDTO(batting=" + String.valueOf(this.getBatting()) + ", pitching=" + String.valueOf(this.getPitching()) + ", fielding=" + String.valueOf(this.getFielding()) + ")";
    }

    public static class Batting {
        private int gp;
        private int pa;
        private int ab;
        private int r;
        private int h;
        private int rbi;
        private int hr;
        private int insideParkHr;
        private int sb;
        private int bbHp;
        private int so;
        private int doubles;
        private int triples;
        private int e;
        private int gdp;
        private int sh;
        private int sf;
        private int bb;
        private int ibb;
        private int hbp;
        private int cs;
        private Integer singles;
        private Integer tb;
        private String avg;
        private String obp;
        private String slg;
        private String ops;
        private String sbPct;

        @Generated
        public Batting() {
        }

        @Generated
        public int getGp() {
            return this.gp;
        }

        @Generated
        public int getPa() {
            return this.pa;
        }

        @Generated
        public int getAb() {
            return this.ab;
        }

        @Generated
        public int getR() {
            return this.r;
        }

        @Generated
        public int getH() {
            return this.h;
        }

        @Generated
        public int getRbi() {
            return this.rbi;
        }

        @Generated
        public int getHr() {
            return this.hr;
        }

        @Generated
        public int getInsideParkHr() {
            return this.insideParkHr;
        }

        @Generated
        public int getSb() {
            return this.sb;
        }

        @Generated
        public int getBbHp() {
            return this.bbHp;
        }

        @Generated
        public int getSo() {
            return this.so;
        }

        @Generated
        public int getDoubles() {
            return this.doubles;
        }

        @Generated
        public int getTriples() {
            return this.triples;
        }

        @Generated
        public int getE() {
            return this.e;
        }

        @Generated
        public int getGdp() {
            return this.gdp;
        }

        @Generated
        public int getSh() {
            return this.sh;
        }

        @Generated
        public int getSf() {
            return this.sf;
        }

        @Generated
        public int getBb() {
            return this.bb;
        }

        @Generated
        public int getIbb() {
            return this.ibb;
        }

        @Generated
        public int getHbp() {
            return this.hbp;
        }

        @Generated
        public int getCs() {
            return this.cs;
        }

        @Generated
        public Integer getSingles() {
            return this.singles;
        }

        @Generated
        public Integer getTb() {
            return this.tb;
        }

        @Generated
        public String getAvg() {
            return this.avg;
        }

        @Generated
        public String getObp() {
            return this.obp;
        }

        @Generated
        public String getSlg() {
            return this.slg;
        }

        @Generated
        public String getOps() {
            return this.ops;
        }

        @Generated
        public String getSbPct() {
            return this.sbPct;
        }

        @Generated
        public void setGp(int gp) {
            this.gp = gp;
        }

        @Generated
        public void setPa(int pa) {
            this.pa = pa;
        }

        @Generated
        public void setAb(int ab) {
            this.ab = ab;
        }

        @Generated
        public void setR(int r) {
            this.r = r;
        }

        @Generated
        public void setH(int h) {
            this.h = h;
        }

        @Generated
        public void setRbi(int rbi) {
            this.rbi = rbi;
        }

        @Generated
        public void setHr(int hr) {
            this.hr = hr;
        }

        @Generated
        public void setInsideParkHr(int insideParkHr) {
            this.insideParkHr = insideParkHr;
        }

        @Generated
        public void setSb(int sb) {
            this.sb = sb;
        }

        @Generated
        public void setBbHp(int bbHp) {
            this.bbHp = bbHp;
        }

        @Generated
        public void setSo(int so) {
            this.so = so;
        }

        @Generated
        public void setDoubles(int doubles) {
            this.doubles = doubles;
        }

        @Generated
        public void setTriples(int triples) {
            this.triples = triples;
        }

        @Generated
        public void setE(int e) {
            this.e = e;
        }

        @Generated
        public void setGdp(int gdp) {
            this.gdp = gdp;
        }

        @Generated
        public void setSh(int sh) {
            this.sh = sh;
        }

        @Generated
        public void setSf(int sf) {
            this.sf = sf;
        }

        @Generated
        public void setBb(int bb) {
            this.bb = bb;
        }

        @Generated
        public void setIbb(int ibb) {
            this.ibb = ibb;
        }

        @Generated
        public void setHbp(int hbp) {
            this.hbp = hbp;
        }

        @Generated
        public void setCs(int cs) {
            this.cs = cs;
        }

        @Generated
        public void setSingles(Integer singles) {
            this.singles = singles;
        }

        @Generated
        public void setTb(Integer tb) {
            this.tb = tb;
        }

        @Generated
        public void setAvg(String avg) {
            this.avg = avg;
        }

        @Generated
        public void setObp(String obp) {
            this.obp = obp;
        }

        @Generated
        public void setSlg(String slg) {
            this.slg = slg;
        }

        @Generated
        public void setOps(String ops) {
            this.ops = ops;
        }

        @Generated
        public void setSbPct(String sbPct) {
            this.sbPct = sbPct;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Batting)) {
                return false;
            }
            PlayerStatsDTO.Batting other = (Batting)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getGp() != other.getGp()) {
                return false;
            }
            if (this.getPa() != other.getPa()) {
                return false;
            }
            if (this.getAb() != other.getAb()) {
                return false;
            }
            if (this.getR() != other.getR()) {
                return false;
            }
            if (this.getH() != other.getH()) {
                return false;
            }
            if (this.getRbi() != other.getRbi()) {
                return false;
            }
            if (this.getHr() != other.getHr()) {
                return false;
            }
            if (this.getInsideParkHr() != other.getInsideParkHr()) {
                return false;
            }
            if (this.getSb() != other.getSb()) {
                return false;
            }
            if (this.getBbHp() != other.getBbHp()) {
                return false;
            }
            if (this.getSo() != other.getSo()) {
                return false;
            }
            if (this.getDoubles() != other.getDoubles()) {
                return false;
            }
            if (this.getTriples() != other.getTriples()) {
                return false;
            }
            if (this.getE() != other.getE()) {
                return false;
            }
            if (this.getGdp() != other.getGdp()) {
                return false;
            }
            if (this.getSh() != other.getSh()) {
                return false;
            }
            if (this.getSf() != other.getSf()) {
                return false;
            }
            if (this.getBb() != other.getBb()) {
                return false;
            }
            if (this.getIbb() != other.getIbb()) {
                return false;
            }
            if (this.getHbp() != other.getHbp()) {
                return false;
            }
            if (this.getCs() != other.getCs()) {
                return false;
            }
            Integer this$singles = this.getSingles();
            Integer other$singles = other.getSingles();
            if (this$singles == null ? other$singles != null : !((Object)this$singles).equals(other$singles)) {
                return false;
            }
            Integer this$tb = this.getTb();
            Integer other$tb = other.getTb();
            if (this$tb == null ? other$tb != null : !((Object)this$tb).equals(other$tb)) {
                return false;
            }
            String this$avg = this.getAvg();
            String other$avg = other.getAvg();
            if (this$avg == null ? other$avg != null : !this$avg.equals(other$avg)) {
                return false;
            }
            String this$obp = this.getObp();
            String other$obp = other.getObp();
            if (this$obp == null ? other$obp != null : !this$obp.equals(other$obp)) {
                return false;
            }
            String this$slg = this.getSlg();
            String other$slg = other.getSlg();
            if (this$slg == null ? other$slg != null : !this$slg.equals(other$slg)) {
                return false;
            }
            String this$ops = this.getOps();
            String other$ops = other.getOps();
            if (this$ops == null ? other$ops != null : !this$ops.equals(other$ops)) {
                return false;
            }
            String this$sbPct = this.getSbPct();
            String other$sbPct = other.getSbPct();
            return !(this$sbPct == null ? other$sbPct != null : !this$sbPct.equals(other$sbPct));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Batting;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getGp();
            result = result * 59 + this.getPa();
            result = result * 59 + this.getAb();
            result = result * 59 + this.getR();
            result = result * 59 + this.getH();
            result = result * 59 + this.getRbi();
            result = result * 59 + this.getHr();
            result = result * 59 + this.getInsideParkHr();
            result = result * 59 + this.getSb();
            result = result * 59 + this.getBbHp();
            result = result * 59 + this.getSo();
            result = result * 59 + this.getDoubles();
            result = result * 59 + this.getTriples();
            result = result * 59 + this.getE();
            result = result * 59 + this.getGdp();
            result = result * 59 + this.getSh();
            result = result * 59 + this.getSf();
            result = result * 59 + this.getBb();
            result = result * 59 + this.getIbb();
            result = result * 59 + this.getHbp();
            result = result * 59 + this.getCs();
            Integer $singles = this.getSingles();
            result = result * 59 + ($singles == null ? 43 : ((Object)$singles).hashCode());
            Integer $tb = this.getTb();
            result = result * 59 + ($tb == null ? 43 : ((Object)$tb).hashCode());
            String $avg = this.getAvg();
            result = result * 59 + ($avg == null ? 43 : $avg.hashCode());
            String $obp = this.getObp();
            result = result * 59 + ($obp == null ? 43 : $obp.hashCode());
            String $slg = this.getSlg();
            result = result * 59 + ($slg == null ? 43 : $slg.hashCode());
            String $ops = this.getOps();
            result = result * 59 + ($ops == null ? 43 : $ops.hashCode());
            String $sbPct = this.getSbPct();
            result = result * 59 + ($sbPct == null ? 43 : $sbPct.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "PlayerStatsDTO.Batting(gp=" + this.getGp() + ", pa=" + this.getPa() + ", ab=" + this.getAb() + ", r=" + this.getR() + ", h=" + this.getH() + ", rbi=" + this.getRbi() + ", hr=" + this.getHr() + ", insideParkHr=" + this.getInsideParkHr() + ", sb=" + this.getSb() + ", bbHp=" + this.getBbHp() + ", so=" + this.getSo() + ", doubles=" + this.getDoubles() + ", triples=" + this.getTriples() + ", e=" + this.getE() + ", gdp=" + this.getGdp() + ", sh=" + this.getSh() + ", sf=" + this.getSf() + ", bb=" + this.getBb() + ", ibb=" + this.getIbb() + ", hbp=" + this.getHbp() + ", cs=" + this.getCs() + ", singles=" + this.getSingles() + ", tb=" + this.getTb() + ", avg=" + this.getAvg() + ", obp=" + this.getObp() + ", slg=" + this.getSlg() + ", ops=" + this.getOps() + ", sbPct=" + this.getSbPct() + ")";
        }
    }

    public static class Fielding {
        private int gp;
        private double inn;
        private int tc;
        private int po;
        private int a;
        private int e;
        private int pb;
        private int catcherCs;
        private String tcPct;

        @Generated
        public Fielding() {
        }

        @Generated
        public int getGp() {
            return this.gp;
        }

        @Generated
        public double getInn() {
            return this.inn;
        }

        @Generated
        public int getTc() {
            return this.tc;
        }

        @Generated
        public int getPo() {
            return this.po;
        }

        @Generated
        public int getA() {
            return this.a;
        }

        @Generated
        public int getE() {
            return this.e;
        }

        @Generated
        public int getPb() {
            return this.pb;
        }

        @Generated
        public int getCatcherCs() {
            return this.catcherCs;
        }

        @Generated
        public String getTcPct() {
            return this.tcPct;
        }

        @Generated
        public void setGp(int gp) {
            this.gp = gp;
        }

        @Generated
        public void setInn(double inn) {
            this.inn = inn;
        }

        @Generated
        public void setTc(int tc) {
            this.tc = tc;
        }

        @Generated
        public void setPo(int po) {
            this.po = po;
        }

        @Generated
        public void setA(int a) {
            this.a = a;
        }

        @Generated
        public void setE(int e) {
            this.e = e;
        }

        @Generated
        public void setPb(int pb) {
            this.pb = pb;
        }

        @Generated
        public void setCatcherCs(int catcherCs) {
            this.catcherCs = catcherCs;
        }

        @Generated
        public void setTcPct(String tcPct) {
            this.tcPct = tcPct;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Fielding)) {
                return false;
            }
            PlayerStatsDTO.Fielding other = (Fielding)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getGp() != other.getGp()) {
                return false;
            }
            if (Double.compare(this.getInn(), other.getInn()) != 0) {
                return false;
            }
            if (this.getTc() != other.getTc()) {
                return false;
            }
            if (this.getPo() != other.getPo()) {
                return false;
            }
            if (this.getA() != other.getA()) {
                return false;
            }
            if (this.getE() != other.getE()) {
                return false;
            }
            if (this.getPb() != other.getPb()) {
                return false;
            }
            if (this.getCatcherCs() != other.getCatcherCs()) {
                return false;
            }
            String this$tcPct = this.getTcPct();
            String other$tcPct = other.getTcPct();
            return !(this$tcPct == null ? other$tcPct != null : !this$tcPct.equals(other$tcPct));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Fielding;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getGp();
            long $inn = Double.doubleToLongBits(this.getInn());
            result = result * 59 + (int)($inn >>> 32 ^ $inn);
            result = result * 59 + this.getTc();
            result = result * 59 + this.getPo();
            result = result * 59 + this.getA();
            result = result * 59 + this.getE();
            result = result * 59 + this.getPb();
            result = result * 59 + this.getCatcherCs();
            String $tcPct = this.getTcPct();
            result = result * 59 + ($tcPct == null ? 43 : $tcPct.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "PlayerStatsDTO.Fielding(gp=" + this.getGp() + ", inn=" + this.getInn() + ", tc=" + this.getTc() + ", po=" + this.getPo() + ", a=" + this.getA() + ", e=" + this.getE() + ", pb=" + this.getPb() + ", catcherCs=" + this.getCatcherCs() + ", tcPct=" + this.getTcPct() + ")";
        }
    }

    public static class Pitching {
        private int gp;
        private double ip;
        private int er;
        private int pitchH;
        private int pitchBbHp;
        private int pitchSo;
        private int pitchHr;
        private int pitchInsideParkHr;
        private int gs;
        private int svo;
        private int cg;
        private int pg;
        private int w;
        private int l;
        private int sv;
        private int hld;
        private int pitchPa;
        private int pitchBf;
        private int pitchAb;
        private int np;
        private int pitchBb;
        private int pitchIbb;
        private int pitchHbp;
        private int wp;
        private int bk;
        private int pitchR;
        private int go;
        private int fo;
        private String whip;
        private String era;
        private String goFo;

        @Generated
        public Pitching() {
        }

        @Generated
        public int getGp() {
            return this.gp;
        }

        @Generated
        public double getIp() {
            return this.ip;
        }

        @Generated
        public int getEr() {
            return this.er;
        }

        @Generated
        public int getPitchH() {
            return this.pitchH;
        }

        @Generated
        public int getPitchBbHp() {
            return this.pitchBbHp;
        }

        @Generated
        public int getPitchSo() {
            return this.pitchSo;
        }

        @Generated
        public int getPitchHr() {
            return this.pitchHr;
        }

        @Generated
        public int getPitchInsideParkHr() {
            return this.pitchInsideParkHr;
        }

        @Generated
        public int getGs() {
            return this.gs;
        }

        @Generated
        public int getSvo() {
            return this.svo;
        }

        @Generated
        public int getCg() {
            return this.cg;
        }

        @Generated
        public int getPg() {
            return this.pg;
        }

        @Generated
        public int getW() {
            return this.w;
        }

        @Generated
        public int getL() {
            return this.l;
        }

        @Generated
        public int getSv() {
            return this.sv;
        }

        @Generated
        public int getHld() {
            return this.hld;
        }

        @Generated
        public int getPitchPa() {
            return this.pitchPa;
        }

        @Generated
        public int getPitchBf() {
            return this.pitchBf;
        }

        @Generated
        public int getPitchAb() {
            return this.pitchAb;
        }

        @Generated
        public int getNp() {
            return this.np;
        }

        @Generated
        public int getPitchBb() {
            return this.pitchBb;
        }

        @Generated
        public int getPitchIbb() {
            return this.pitchIbb;
        }

        @Generated
        public int getPitchHbp() {
            return this.pitchHbp;
        }

        @Generated
        public int getWp() {
            return this.wp;
        }

        @Generated
        public int getBk() {
            return this.bk;
        }

        @Generated
        public int getPitchR() {
            return this.pitchR;
        }

        @Generated
        public int getGo() {
            return this.go;
        }

        @Generated
        public int getFo() {
            return this.fo;
        }

        @Generated
        public String getWhip() {
            return this.whip;
        }

        @Generated
        public String getEra() {
            return this.era;
        }

        @Generated
        public String getGoFo() {
            return this.goFo;
        }

        @Generated
        public void setGp(int gp) {
            this.gp = gp;
        }

        @Generated
        public void setIp(double ip) {
            this.ip = ip;
        }

        @Generated
        public void setEr(int er) {
            this.er = er;
        }

        @Generated
        public void setPitchH(int pitchH) {
            this.pitchH = pitchH;
        }

        @Generated
        public void setPitchBbHp(int pitchBbHp) {
            this.pitchBbHp = pitchBbHp;
        }

        @Generated
        public void setPitchSo(int pitchSo) {
            this.pitchSo = pitchSo;
        }

        @Generated
        public void setPitchHr(int pitchHr) {
            this.pitchHr = pitchHr;
        }

        @Generated
        public void setPitchInsideParkHr(int pitchInsideParkHr) {
            this.pitchInsideParkHr = pitchInsideParkHr;
        }

        @Generated
        public void setGs(int gs) {
            this.gs = gs;
        }

        @Generated
        public void setSvo(int svo) {
            this.svo = svo;
        }

        @Generated
        public void setCg(int cg) {
            this.cg = cg;
        }

        @Generated
        public void setPg(int pg) {
            this.pg = pg;
        }

        @Generated
        public void setW(int w) {
            this.w = w;
        }

        @Generated
        public void setL(int l) {
            this.l = l;
        }

        @Generated
        public void setSv(int sv) {
            this.sv = sv;
        }

        @Generated
        public void setHld(int hld) {
            this.hld = hld;
        }

        @Generated
        public void setPitchPa(int pitchPa) {
            this.pitchPa = pitchPa;
        }

        @Generated
        public void setPitchBf(int pitchBf) {
            this.pitchBf = pitchBf;
        }

        @Generated
        public void setPitchAb(int pitchAb) {
            this.pitchAb = pitchAb;
        }

        @Generated
        public void setNp(int np) {
            this.np = np;
        }

        @Generated
        public void setPitchBb(int pitchBb) {
            this.pitchBb = pitchBb;
        }

        @Generated
        public void setPitchIbb(int pitchIbb) {
            this.pitchIbb = pitchIbb;
        }

        @Generated
        public void setPitchHbp(int pitchHbp) {
            this.pitchHbp = pitchHbp;
        }

        @Generated
        public void setWp(int wp) {
            this.wp = wp;
        }

        @Generated
        public void setBk(int bk) {
            this.bk = bk;
        }

        @Generated
        public void setPitchR(int pitchR) {
            this.pitchR = pitchR;
        }

        @Generated
        public void setGo(int go) {
            this.go = go;
        }

        @Generated
        public void setFo(int fo) {
            this.fo = fo;
        }

        @Generated
        public void setWhip(String whip) {
            this.whip = whip;
        }

        @Generated
        public void setEra(String era) {
            this.era = era;
        }

        @Generated
        public void setGoFo(String goFo) {
            this.goFo = goFo;
        }

        @Generated
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof Pitching)) {
                return false;
            }
            PlayerStatsDTO.Pitching other = (Pitching)o;
            if (!other.canEqual(this)) {
                return false;
            }
            if (this.getGp() != other.getGp()) {
                return false;
            }
            if (Double.compare(this.getIp(), other.getIp()) != 0) {
                return false;
            }
            if (this.getEr() != other.getEr()) {
                return false;
            }
            if (this.getPitchH() != other.getPitchH()) {
                return false;
            }
            if (this.getPitchBbHp() != other.getPitchBbHp()) {
                return false;
            }
            if (this.getPitchSo() != other.getPitchSo()) {
                return false;
            }
            if (this.getPitchHr() != other.getPitchHr()) {
                return false;
            }
            if (this.getPitchInsideParkHr() != other.getPitchInsideParkHr()) {
                return false;
            }
            if (this.getGs() != other.getGs()) {
                return false;
            }
            if (this.getSvo() != other.getSvo()) {
                return false;
            }
            if (this.getCg() != other.getCg()) {
                return false;
            }
            if (this.getPg() != other.getPg()) {
                return false;
            }
            if (this.getW() != other.getW()) {
                return false;
            }
            if (this.getL() != other.getL()) {
                return false;
            }
            if (this.getSv() != other.getSv()) {
                return false;
            }
            if (this.getHld() != other.getHld()) {
                return false;
            }
            if (this.getPitchPa() != other.getPitchPa()) {
                return false;
            }
            if (this.getPitchBf() != other.getPitchBf()) {
                return false;
            }
            if (this.getPitchAb() != other.getPitchAb()) {
                return false;
            }
            if (this.getNp() != other.getNp()) {
                return false;
            }
            if (this.getPitchBb() != other.getPitchBb()) {
                return false;
            }
            if (this.getPitchIbb() != other.getPitchIbb()) {
                return false;
            }
            if (this.getPitchHbp() != other.getPitchHbp()) {
                return false;
            }
            if (this.getWp() != other.getWp()) {
                return false;
            }
            if (this.getBk() != other.getBk()) {
                return false;
            }
            if (this.getPitchR() != other.getPitchR()) {
                return false;
            }
            if (this.getGo() != other.getGo()) {
                return false;
            }
            if (this.getFo() != other.getFo()) {
                return false;
            }
            String this$whip = this.getWhip();
            String other$whip = other.getWhip();
            if (this$whip == null ? other$whip != null : !this$whip.equals(other$whip)) {
                return false;
            }
            String this$era = this.getEra();
            String other$era = other.getEra();
            if (this$era == null ? other$era != null : !this$era.equals(other$era)) {
                return false;
            }
            String this$goFo = this.getGoFo();
            String other$goFo = other.getGoFo();
            return !(this$goFo == null ? other$goFo != null : !this$goFo.equals(other$goFo));
        }

        @Generated
        protected boolean canEqual(Object other) {
            return other instanceof Pitching;
        }

        @Generated
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            result = result * 59 + this.getGp();
            long $ip = Double.doubleToLongBits(this.getIp());
            result = result * 59 + (int)($ip >>> 32 ^ $ip);
            result = result * 59 + this.getEr();
            result = result * 59 + this.getPitchH();
            result = result * 59 + this.getPitchBbHp();
            result = result * 59 + this.getPitchSo();
            result = result * 59 + this.getPitchHr();
            result = result * 59 + this.getPitchInsideParkHr();
            result = result * 59 + this.getGs();
            result = result * 59 + this.getSvo();
            result = result * 59 + this.getCg();
            result = result * 59 + this.getPg();
            result = result * 59 + this.getW();
            result = result * 59 + this.getL();
            result = result * 59 + this.getSv();
            result = result * 59 + this.getHld();
            result = result * 59 + this.getPitchPa();
            result = result * 59 + this.getPitchBf();
            result = result * 59 + this.getPitchAb();
            result = result * 59 + this.getNp();
            result = result * 59 + this.getPitchBb();
            result = result * 59 + this.getPitchIbb();
            result = result * 59 + this.getPitchHbp();
            result = result * 59 + this.getWp();
            result = result * 59 + this.getBk();
            result = result * 59 + this.getPitchR();
            result = result * 59 + this.getGo();
            result = result * 59 + this.getFo();
            String $whip = this.getWhip();
            result = result * 59 + ($whip == null ? 43 : $whip.hashCode());
            String $era = this.getEra();
            result = result * 59 + ($era == null ? 43 : $era.hashCode());
            String $goFo = this.getGoFo();
            result = result * 59 + ($goFo == null ? 43 : $goFo.hashCode());
            return result;
        }

        @Generated
        public String toString() {
            return "PlayerStatsDTO.Pitching(gp=" + this.getGp() + ", ip=" + this.getIp() + ", er=" + this.getEr() + ", pitchH=" + this.getPitchH() + ", pitchBbHp=" + this.getPitchBbHp() + ", pitchSo=" + this.getPitchSo() + ", pitchHr=" + this.getPitchHr() + ", pitchInsideParkHr=" + this.getPitchInsideParkHr() + ", gs=" + this.getGs() + ", svo=" + this.getSvo() + ", cg=" + this.getCg() + ", pg=" + this.getPg() + ", w=" + this.getW() + ", l=" + this.getL() + ", sv=" + this.getSv() + ", hld=" + this.getHld() + ", pitchPa=" + this.getPitchPa() + ", pitchBf=" + this.getPitchBf() + ", pitchAb=" + this.getPitchAb() + ", np=" + this.getNp() + ", pitchBb=" + this.getPitchBb() + ", pitchIbb=" + this.getPitchIbb() + ", pitchHbp=" + this.getPitchHbp() + ", wp=" + this.getWp() + ", bk=" + this.getBk() + ", pitchR=" + this.getPitchR() + ", go=" + this.getGo() + ", fo=" + this.getFo() + ", whip=" + this.getWhip() + ", era=" + this.getEra() + ", goFo=" + this.getGoFo() + ")";
        }
    }
}




