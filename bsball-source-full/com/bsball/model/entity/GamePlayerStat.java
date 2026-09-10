/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.BoolToIntDeserializer
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.GamePlayerStat
 *  com.fasterxml.jackson.databind.annotation.JsonDeserialize
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.common.BoolToIntDeserializer;
import com.bsball.model.entity.BaseEntity;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_game_player_stat")
@Comment(value="\u6bd4\u8d5b\u7403\u5458\u7edf\u8ba1")
public class GamePlayerStat
extends BaseEntity {
    @Comment(value="\u6bd4\u8d5bID")
    private Long gameId;
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u7403\u961fID")
    private Long teamId;
    @Comment(value="\u7403\u5458ID")
    private Long playerId;
    @Column(columnDefinition="integer")
    @Comment(value="\u6253\u5e8f")
    private Integer battingOrder;
    @Column(columnDefinition="integer")
    @Comment(value="\u5217\u8868\u987a\u5e8f")
    private Integer listOrder;
    @Column(columnDefinition="integer")
    @Comment(value="\u767b\u677f\u987a\u5e8f")
    private Integer pitcherOrder;
    @Comment(value="\u5b88\u5907\u4f4d\u7f6e")
    private String position;
    @Comment(value="\u80cc\u53f7")
    private String number;
    @Comment(value="\u6253\u51fb\u624b")
    private String batHand;
    @Comment(value="\u6295\u7403\u624b")
    private String throwHand;
    @Column(columnDefinition="integer")
    @Comment(value="\u6253\u5e2d")
    private Integer pa;
    @Column(columnDefinition="integer")
    @Comment(value="\u6253\u6570")
    private Integer ab;
    @Column(columnDefinition="integer")
    @Comment(value="\u5f97\u5206")
    private Integer r;
    @Column(columnDefinition="integer")
    @Comment(value="\u5b89\u6253")
    private Integer h;
    @Column(columnDefinition="integer")
    @Comment(value="\u5931\u8bef")
    private Integer e;
    @Column(columnDefinition="integer")
    @Comment(value="\u56db\u574f+\u89e6\u8eab")
    private Integer bbHp;
    @Column(columnDefinition="integer")
    @Comment(value="\u76d7\u5792")
    private Integer sb;
    @Column(columnDefinition="integer")
    @Comment(value="\u4e09\u632f")
    private Integer so;
    @Column(columnDefinition="integer")
    @Comment(value="\u6325\u7a7a\u4e09\u632f")
    private Integer soSwing;
    @Column(columnDefinition="integer")
    @Comment(value="\u76ee\u9001\u4e09\u632f")
    private Integer soLooking;
    @Column(columnDefinition="integer")
    @Comment(value="\u6253\u70b9")
    private Integer rbi;
    @Column(columnDefinition="integer")
    @Comment(value="\u4e8c\u5792\u6253")
    private Integer doubles;
    @Column(columnDefinition="integer")
    @Comment(value="\u4e09\u5792\u6253")
    private Integer triples;
    @Column(columnDefinition="integer")
    @Comment(value="\u5168\u5792\u6253")
    private Integer hr;
    @Column(columnDefinition="integer")
    @Comment(value="\u573a\u5185\u5168\u5792\u6253\uff08IPHR\uff09")
    private Integer insideParkHr;
    @Column(columnDefinition="integer")
    @Comment(value="\u5730\u6eda\u53cc\u6740")
    private Integer gdp;
    @Column(columnDefinition="integer")
    @Comment(value="\u727a\u7272\u89e6\u51fb")
    private Integer sh;
    @Column(columnDefinition="integer")
    @Comment(value="\u727a\u7272\u9ad8\u98de")
    private Integer sf;
    @Column(columnDefinition="integer")
    @Comment(value="\u56db\u574f")
    private Integer bb;
    @Column(columnDefinition="integer")
    @Comment(value="\u6545\u610f\u56db\u574f")
    private Integer ibb;
    @Column(columnDefinition="integer")
    @Comment(value="\u89e6\u8eab")
    private Integer hbp;
    @Column(columnDefinition="integer")
    @Comment(value="\u76d7\u5792\u88ab\u6740")
    private Integer cs;
    @JsonDeserialize(using=BoolToIntDeserializer.class)
    @Column(columnDefinition="integer")
    @Comment(value="\u662f\u5426\u6295\u624b")
    private Integer isPitcher;
    @Comment(value="\u6295\u7403\u5c40\u6570")
    private Double ip;
    @Column(columnDefinition="integer")
    @Comment(value="\u5931\u5206")
    private Integer earnedR;
    @Column(columnDefinition="integer")
    @Comment(value="\u81ea\u8d23\u5206")
    private Integer er;
    @Column(columnDefinition="integer")
    @Comment(value="\u88ab\u5b89\u6253")
    private Integer pitchH;
    @Column(columnDefinition="integer")
    @Comment(value="\u56db\u574f+\u89e6\u8eab")
    private Integer pitchBbHp;
    @Column(columnDefinition="integer")
    @Comment(value="\u4e09\u632f")
    private Integer pitchSo;
    @Column(columnDefinition="integer")
    @Comment(value="\u88ab\u5168\u5792\u6253\uff08\u5899\u5916 HR\uff0c\u4e0d\u542b\u573a\u5185\u5168\u5792\u6253\uff09")
    private Integer pitchHr;
    @Column(columnDefinition="integer")
    @Comment(value="\u88ab\u573a\u5185\u5168\u5792\u6253\uff08IPHR\uff0c\u5bf9\u6295\u624b\u5355\u72ec\u7edf\u8ba1\uff09")
    private Integer pitchInsideParkHr;
    @Column(columnDefinition="integer")
    @Comment(value="\u5148\u53d1")
    private Integer gs;
    @Column(columnDefinition="integer")
    @Comment(value="\u6551\u63f4\u673a\u4f1a")
    private Integer svo;
    @Column(columnDefinition="integer")
    @Comment(value="\u5b8c\u6295")
    private Integer cg;
    @Column(columnDefinition="integer")
    @Comment(value="\u5b8c\u7f8e\u6bd4\u8d5b")
    private Integer pg;
    @Column(columnDefinition="integer")
    @Comment(value="\u80dc\u6295")
    private Integer w;
    @Column(columnDefinition="integer")
    @Comment(value="\u8d25\u6295")
    private Integer l;
    @Column(columnDefinition="integer")
    @Comment(value="\u6551\u63f4\u6210\u529f")
    private Integer sv;
    @Column(columnDefinition="integer")
    @Comment(value="\u4e2d\u7ee7\u6210\u529f")
    private Integer hld;
    @Column(columnDefinition="integer")
    @Comment(value="\u9762\u5bf9\u6253\u5e2d")
    private Integer pitchPa;
    @Column(columnDefinition="integer")
    @Comment(value="\u8f6e\u51fb\u6570\uff08\u9762\u5bf9\u6253\u8005 BF\uff09")
    private Integer pitchBf;
    @Column(name="pitch_ab", columnDefinition="integer")
    @Comment(value="\u6295\u624b\u81ea\u7531\u51fb\u7403\u6570\uff08\u4e0e BF \u72ec\u7acb\uff1bNULL \u65f6\u6309 BF\u2212BB\u2212IBB\u2212HBP \u63a8\u7b97\uff09")
    private Integer pitchAb;
    @Column(columnDefinition="integer")
    @Comment(value="\u6295\u7403\u6570")
    private Integer np;
    @Column(columnDefinition="integer")
    @Comment(value="\u56db\u574f")
    private Integer pitchBb;
    @Column(columnDefinition="integer")
    @Comment(value="\u6545\u610f\u56db\u574f")
    private Integer pitchIbb;
    @Column(columnDefinition="integer")
    @Comment(value="\u89e6\u8eab")
    private Integer pitchHbp;
    @Column(columnDefinition="integer")
    @Comment(value="\u66b4\u6295")
    private Integer wp;
    @Column(columnDefinition="integer")
    @Comment(value="\u6295\u624b\u72af\u89c4")
    private Integer bk;
    @Column(columnDefinition="integer")
    @Comment(value="\u5931\u5206")
    private Integer pitchR;
    @Column(columnDefinition="integer")
    @Comment(value="\u5730\u6eda\u51fa\u5c40")
    private Integer go;
    @Column(columnDefinition="integer")
    @Comment(value="\u9ad8\u98de\u51fa\u5c40")
    private Integer fo;
    @Column(columnDefinition="integer")
    @Comment(value="\u9632\u5b88\u673a\u4f1a")
    private Integer tc;
    @Column(columnDefinition="integer")
    @Comment(value="\u63a5\u6740")
    private Integer po;
    @Column(columnDefinition="integer")
    @Comment(value="\u52a9\u6740")
    private Integer a;
    @Column(name="fielding_gs", columnDefinition="integer")
    @Comment(value="\u5b88\u5907\u5148\u53d1\uff08\u8be5\u573a\u8be5\u4f4d\u7f6e\u662f\u5426\u5148\u53d1\uff0c0/1\uff09")
    private Integer fieldingGs;
    @Column(name="def_inn")
    @Comment(value="\u5b88\u5907\u5c40\u6570")
    private Double defInn;
    @Column(columnDefinition="integer")
    @Comment(value="\u53c2\u4e0e\u53cc\u6740")
    private Integer dp;
    @Column(columnDefinition="integer")
    @Comment(value="\u6355\u9038")
    private Integer pb;
    @Column(name="catcher_sb", columnDefinition="integer")
    @Comment(value="\u6355\u624b\u5b88\u5907\uff1a\u76d7\u5792\u6210\u529f\uff08\u5bf9\u624b\uff09")
    private Integer catcherSb;
    @Column(name="catcher_cs", columnDefinition="integer")
    @Comment(value="\u6355\u624b\u5b88\u5907\uff1a\u76d7\u5792\u88ab\u6740")
    private Integer catcherCs;

    @Generated
    public GamePlayerStat() {
    }

    @Generated
    public Long getGameId() {
        return this.gameId;
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
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
    public Integer getPitcherOrder() {
        return this.pitcherOrder;
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
    public Double getIp() {
        return this.ip;
    }

    @Generated
    public Integer getEarnedR() {
        return this.earnedR;
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
    public Integer getPitchR() {
        return this.pitchR;
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
    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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
    public void setPitcherOrder(Integer pitcherOrder) {
        this.pitcherOrder = pitcherOrder;
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
    public void setIp(Double ip) {
        this.ip = ip;
    }

    @Generated
    public void setEarnedR(Integer earnedR) {
        this.earnedR = earnedR;
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
    public void setPitchR(Integer pitchR) {
        this.pitchR = pitchR;
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
    public String toString() {
        return "GamePlayerStat(gameId=" + this.getGameId() + ", tenantId=" + this.getTenantId() + ", teamId=" + this.getTeamId() + ", playerId=" + this.getPlayerId() + ", battingOrder=" + this.getBattingOrder() + ", listOrder=" + this.getListOrder() + ", pitcherOrder=" + this.getPitcherOrder() + ", position=" + this.getPosition() + ", number=" + this.getNumber() + ", batHand=" + this.getBatHand() + ", throwHand=" + this.getThrowHand() + ", pa=" + this.getPa() + ", ab=" + this.getAb() + ", r=" + this.getR() + ", h=" + this.getH() + ", e=" + this.getE() + ", bbHp=" + this.getBbHp() + ", sb=" + this.getSb() + ", so=" + this.getSo() + ", soSwing=" + this.getSoSwing() + ", soLooking=" + this.getSoLooking() + ", rbi=" + this.getRbi() + ", doubles=" + this.getDoubles() + ", triples=" + this.getTriples() + ", hr=" + this.getHr() + ", insideParkHr=" + this.getInsideParkHr() + ", gdp=" + this.getGdp() + ", sh=" + this.getSh() + ", sf=" + this.getSf() + ", bb=" + this.getBb() + ", ibb=" + this.getIbb() + ", hbp=" + this.getHbp() + ", cs=" + this.getCs() + ", isPitcher=" + this.getIsPitcher() + ", ip=" + this.getIp() + ", earnedR=" + this.getEarnedR() + ", er=" + this.getEr() + ", pitchH=" + this.getPitchH() + ", pitchBbHp=" + this.getPitchBbHp() + ", pitchSo=" + this.getPitchSo() + ", pitchHr=" + this.getPitchHr() + ", pitchInsideParkHr=" + this.getPitchInsideParkHr() + ", gs=" + this.getGs() + ", svo=" + this.getSvo() + ", cg=" + this.getCg() + ", pg=" + this.getPg() + ", w=" + this.getW() + ", l=" + this.getL() + ", sv=" + this.getSv() + ", hld=" + this.getHld() + ", pitchPa=" + this.getPitchPa() + ", pitchBf=" + this.getPitchBf() + ", pitchAb=" + this.getPitchAb() + ", np=" + this.getNp() + ", pitchBb=" + this.getPitchBb() + ", pitchIbb=" + this.getPitchIbb() + ", pitchHbp=" + this.getPitchHbp() + ", wp=" + this.getWp() + ", bk=" + this.getBk() + ", pitchR=" + this.getPitchR() + ", go=" + this.getGo() + ", fo=" + this.getFo() + ", tc=" + this.getTc() + ", po=" + this.getPo() + ", a=" + this.getA() + ", fieldingGs=" + this.getFieldingGs() + ", defInn=" + this.getDefInn() + ", dp=" + this.getDp() + ", pb=" + this.getPb() + ", catcherSb=" + this.getCatcherSb() + ", catcherCs=" + this.getCatcherCs() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof GamePlayerStat)) {
            return false;
        }
        GamePlayerStat other = (GamePlayerStat)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$gameId = this.getGameId();
        Long other$gameId = other.getGameId();
        if (this$gameId == null ? other$gameId != null : !((Object)this$gameId).equals(other$gameId)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
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
        Integer this$pitcherOrder = this.getPitcherOrder();
        Integer other$pitcherOrder = other.getPitcherOrder();
        if (this$pitcherOrder == null ? other$pitcherOrder != null : !((Object)this$pitcherOrder).equals(other$pitcherOrder)) {
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
        Double this$ip = this.getIp();
        Double other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !((Object)this$ip).equals(other$ip)) {
            return false;
        }
        Integer this$earnedR = this.getEarnedR();
        Integer other$earnedR = other.getEarnedR();
        if (this$earnedR == null ? other$earnedR != null : !((Object)this$earnedR).equals(other$earnedR)) {
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
        Integer this$pitchR = this.getPitchR();
        Integer other$pitchR = other.getPitchR();
        if (this$pitchR == null ? other$pitchR != null : !((Object)this$pitchR).equals(other$pitchR)) {
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
        return other instanceof GamePlayerStat;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $gameId = this.getGameId();
        result = result * 59 + ($gameId == null ? 43 : ((Object)$gameId).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Long $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : ((Object)$playerId).hashCode());
        Integer $battingOrder = this.getBattingOrder();
        result = result * 59 + ($battingOrder == null ? 43 : ((Object)$battingOrder).hashCode());
        Integer $listOrder = this.getListOrder();
        result = result * 59 + ($listOrder == null ? 43 : ((Object)$listOrder).hashCode());
        Integer $pitcherOrder = this.getPitcherOrder();
        result = result * 59 + ($pitcherOrder == null ? 43 : ((Object)$pitcherOrder).hashCode());
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
        Integer $doubles = this.getDoubles();
        result = result * 59 + ($doubles == null ? 43 : ((Object)$doubles).hashCode());
        Integer $triples = this.getTriples();
        result = result * 59 + ($triples == null ? 43 : ((Object)$triples).hashCode());
        Integer $hr = this.getHr();
        result = result * 59 + ($hr == null ? 43 : ((Object)$hr).hashCode());
        Integer $insideParkHr = this.getInsideParkHr();
        result = result * 59 + ($insideParkHr == null ? 43 : ((Object)$insideParkHr).hashCode());
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
        Double $ip = this.getIp();
        result = result * 59 + ($ip == null ? 43 : ((Object)$ip).hashCode());
        Integer $earnedR = this.getEarnedR();
        result = result * 59 + ($earnedR == null ? 43 : ((Object)$earnedR).hashCode());
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
        Integer $pitchR = this.getPitchR();
        result = result * 59 + ($pitchR == null ? 43 : ((Object)$pitchR).hashCode());
        Integer $go = this.getGo();
        result = result * 59 + ($go == null ? 43 : ((Object)$go).hashCode());
        Integer $fo = this.getFo();
        result = result * 59 + ($fo == null ? 43 : ((Object)$fo).hashCode());
        Integer $tc = this.getTc();
        result = result * 59 + ($tc == null ? 43 : ((Object)$tc).hashCode());
        Integer $po = this.getPo();
        result = result * 59 + ($po == null ? 43 : ((Object)$po).hashCode());
        Integer $a = this.getA();
        result = result * 59 + ($a == null ? 43 : ((Object)$a).hashCode());
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
}

