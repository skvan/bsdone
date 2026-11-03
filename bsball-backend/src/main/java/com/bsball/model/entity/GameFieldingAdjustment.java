package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Comment;

@Entity
@Table(name = "bs_game_fielding_adjustment")
@Comment("比赛守备补记（赛后复核）")
public class GameFieldingAdjustment extends BaseEntity {
    @Comment("租户ID")
    private Long tenantId;
    @Comment("比赛ID")
    private Long gameId;
    @Column(length = 128)
    @Comment("源 Play 事件ID")
    private String sourceEventId;
    @Comment("源 Play 全场顺序")
    private Integer sourceSequence;
    @Comment("局数")
    private Integer inning;
    @Column(length = 8)
    @Comment("上/下半局：top/bottom")
    private String topBottom;
    @Comment("守备方球队ID")
    private Long teamId;
    @Comment("补记守备员ID")
    private Long playerId;
    @Column(length = 8)
    @Comment("补记守位")
    private String position;
    @Comment("补记助杀数")
    private Integer assistCount;
    @Column(length = 16)
    @Comment("APPROVED/REVOKED/INVALIDATED")
    private String status;
    @Column(length = 512)
    @Comment("源 Play 文本快照")
    private String sourceText;
    @Column(length = 200)
    @Comment("补记备注")
    private String note;
    @Comment("撤销人")
    private Long revokedBy;
    @Comment("撤销时间")
    private LocalDateTime revokedAt;
    @Column(length = 200)
    @Comment("失效原因")
    private String invalidReason;

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

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Integer getSourceSequence() {
        return sourceSequence;
    }

    public void setSourceSequence(Integer sourceSequence) {
        this.sourceSequence = sourceSequence;
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

    public Integer getAssistCount() {
        return assistCount;
    }

    public void setAssistCount(Integer assistCount) {
        this.assistCount = assistCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(Long revokedBy) {
        this.revokedBy = revokedBy;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getInvalidReason() {
        return invalidReason;
    }

    public void setInvalidReason(String invalidReason) {
        this.invalidReason = invalidReason;
    }
}
