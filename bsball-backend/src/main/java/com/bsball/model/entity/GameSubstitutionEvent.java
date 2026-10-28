package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "bs_game_substitution_event")
@Comment("实时录入换人事务事件")
public class GameSubstitutionEvent extends BaseEntity {
    @Comment("比赛ID")
    private Long gameId;
    @Comment("球队ID")
    private Long teamId;
    @Comment("租户ID")
    private Long tenantId;
    @Column(length = 64)
    @Comment("事务ID：同一次换人模式提交共用")
    private String transactionId;
    @Column(columnDefinition = "integer")
    @Comment("局数")
    private Integer inning;
    @Column(length = 8)
    @Comment("上/下半局：top/bottom")
    private String topBottom;
    @Column(columnDefinition = "integer")
    @Comment("全场事件顺序")
    private Integer sequence;
    @Column(columnDefinition = "integer")
    @Comment("事务内顺序")
    private Integer changeSeq;
    @Column(length = 24)
    @Comment("SUBSTITUTION/POSITION_CHANGE/PITCHING_CHANGE/DH_CANCEL")
    private String changeType;
    @Column(columnDefinition = "integer")
    @Comment("打序槽位：1-12；换投可为空")
    private Integer slot;
    @Comment("换下球员ID")
    private Long outPlayerId;
    @Comment("换上球员ID")
    private Long inPlayerId;
    @Column(length = 8)
    @Comment("换下前守位")
    private String outPosition;
    @Column(length = 8)
    @Comment("换上的守位")
    private String inPosition;

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

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getInning() {
        return inning;
    }

    public void setInning(Integer inning) {
        this.inning = inning;
    }

    public String getTopBottom() {
        return topBottom;
    }

    public void setTopBottom(String topBottom) {
        this.topBottom = topBottom;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Integer getChangeSeq() {
        return changeSeq;
    }

    public void setChangeSeq(Integer changeSeq) {
        this.changeSeq = changeSeq;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Integer getSlot() {
        return slot;
    }

    public void setSlot(Integer slot) {
        this.slot = slot;
    }

    public Long getOutPlayerId() {
        return outPlayerId;
    }

    public void setOutPlayerId(Long outPlayerId) {
        this.outPlayerId = outPlayerId;
    }

    public Long getInPlayerId() {
        return inPlayerId;
    }

    public void setInPlayerId(Long inPlayerId) {
        this.inPlayerId = inPlayerId;
    }

    public String getOutPosition() {
        return outPosition;
    }

    public void setOutPosition(String outPosition) {
        this.outPosition = outPosition;
    }

    public String getInPosition() {
        return inPosition;
    }

    public void setInPosition(String inPosition) {
        this.inPosition = inPosition;
    }
}
