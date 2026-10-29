package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "bs_game_fielding_pos_stat")
@Comment("比赛球员逐位置守备统计")
public class GameFieldingPosStat extends BaseEntity {
    @Comment("租户ID")
    private Long tenantId;
    @Comment("比赛ID")
    private Long gameId;
    @Comment("球队ID")
    private Long teamId;
    @Comment("球员ID")
    private Long playerId;
    @Column(length = 8)
    @Comment("守位")
    private String position;
    @Comment("守备出局数（3 outs = 1 局）")
    private Integer outs;
    @Comment("接杀数")
    private Integer po;
    @Comment("助杀数")
    private Integer a;
    @Comment("失误数")
    private Integer e;
    @Column(name = "fielding_gs")
    @Comment("该位置是否先发")
    private Boolean fieldingGs;
    @Column(length = 16)
    @Comment("数据来源：LIVE/DERIVED/MANUAL")
    private String source;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Integer getOuts() {
        return outs;
    }

    public void setOuts(Integer outs) {
        this.outs = outs;
    }

    public Integer getPo() {
        return po;
    }

    public void setPo(Integer po) {
        this.po = po;
    }

    public Integer getA() {
        return a;
    }

    public void setA(Integer a) {
        this.a = a;
    }

    public Integer getE() {
        return e;
    }

    public void setE(Integer e) {
        this.e = e;
    }

    public Boolean getFieldingGs() {
        return fieldingGs;
    }

    public void setFieldingGs(Boolean fieldingGs) {
        this.fieldingGs = fieldingGs;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
