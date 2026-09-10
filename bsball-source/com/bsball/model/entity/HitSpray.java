/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.HitSpray
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_hit_spray")
@Comment(value="\u51fb\u7403\u843d\u70b9\u8bb0\u5f55\u8868")
public class HitSpray
extends BaseEntity {
    @Comment(value="\u6bd4\u8d5bID")
    private Long gameId;
    @Comment(value="\u51fb\u7403\u5458ID")
    private Long playerId;
    @Comment(value="\u7403\u961fID")
    private Long teamId;
    @Column(columnDefinition="smallint")
    @Comment(value="\u5c40\u6570")
    private Short inning;
    @Comment(value="\u4e0a\u4e0b\u534a\u5c40")
    private String half;
    @Column(columnDefinition="smallint")
    @Comment(value="\u68d2\u6b21")
    private Short batterOrder;
    @Column(columnDefinition="smallint")
    @Comment(value="\u51fa\u5c40\u6570\uff08\u51fb\u7403\u524d\uff09")
    private Short outsBefore;
    @Comment(value="\u5792\u51b5\uff08\u5982\"1,3\"\u8868\u793a\u4e00\u4e09\u5792\u6709\u4eba\uff09")
    private String runnersBefore;
    @Comment(value="\u7ed3\u679c\u4ee3\u7801\uff08H1/H2/H3/HR/OUT_F\u7b49\uff09")
    private String resultCode;
    @Comment(value="BIP\u8be6\u7ec6\u4ee3\u7801\uff08bip:fly:1b\u7b49\uff09")
    private String bipCode;
    @Column(columnDefinition="smallint")
    @Comment(value="\u6253\u70b9")
    private Short rbi;
    @Comment(value="\u843d\u70b9X\u5750\u6807\uff08viewBox 0-480\uff09")
    private BigDecimal sprayX;
    @Comment(value="\u843d\u70b9Y\u5750\u6807\uff08viewBox 0-640\uff09")
    private BigDecimal sprayY;
    @Comment(value="\u843d\u70b9\u533a\u57df\uff08IF/LF/LCF/CF/RCF/RF\uff09")
    private String sprayZone;
    @Comment(value="\u843d\u70b9\u6df1\u5ea6\uff08\u6d45/\u4e2d/\u6df1\uff09")
    private String sprayDepth;
    @Comment(value="\u8bb0\u5f55\u65f6\u95f4")
    private LocalDateTime recordedAt;

    @Generated
    public HitSpray() {
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
    public BigDecimal getSprayX() {
        return this.sprayX;
    }

    @Generated
    public BigDecimal getSprayY() {
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
    public LocalDateTime getRecordedAt() {
        return this.recordedAt;
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
    public void setSprayX(BigDecimal sprayX) {
        this.sprayX = sprayX;
    }

    @Generated
    public void setSprayY(BigDecimal sprayY) {
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
    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    @Generated
    public String toString() {
        return "HitSpray(gameId=" + this.getGameId() + ", playerId=" + this.getPlayerId() + ", teamId=" + this.getTeamId() + ", inning=" + this.getInning() + ", half=" + this.getHalf() + ", batterOrder=" + this.getBatterOrder() + ", outsBefore=" + this.getOutsBefore() + ", runnersBefore=" + this.getRunnersBefore() + ", resultCode=" + this.getResultCode() + ", bipCode=" + this.getBipCode() + ", rbi=" + this.getRbi() + ", sprayX=" + String.valueOf(this.getSprayX()) + ", sprayY=" + String.valueOf(this.getSprayY()) + ", sprayZone=" + this.getSprayZone() + ", sprayDepth=" + this.getSprayDepth() + ", recordedAt=" + String.valueOf(this.getRecordedAt()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HitSpray)) {
            return false;
        }
        HitSpray other = (HitSpray)o;
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
        BigDecimal this$sprayX = this.getSprayX();
        BigDecimal other$sprayX = other.getSprayX();
        if (this$sprayX == null ? other$sprayX != null : !((Object)this$sprayX).equals(other$sprayX)) {
            return false;
        }
        BigDecimal this$sprayY = this.getSprayY();
        BigDecimal other$sprayY = other.getSprayY();
        if (this$sprayY == null ? other$sprayY != null : !((Object)this$sprayY).equals(other$sprayY)) {
            return false;
        }
        String this$sprayZone = this.getSprayZone();
        String other$sprayZone = other.getSprayZone();
        if (this$sprayZone == null ? other$sprayZone != null : !this$sprayZone.equals(other$sprayZone)) {
            return false;
        }
        String this$sprayDepth = this.getSprayDepth();
        String other$sprayDepth = other.getSprayDepth();
        if (this$sprayDepth == null ? other$sprayDepth != null : !this$sprayDepth.equals(other$sprayDepth)) {
            return false;
        }
        LocalDateTime this$recordedAt = this.getRecordedAt();
        LocalDateTime other$recordedAt = other.getRecordedAt();
        return !(this$recordedAt == null ? other$recordedAt != null : !((Object)this$recordedAt).equals(other$recordedAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HitSpray;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
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
        String $half = this.getHalf();
        result = result * 59 + ($half == null ? 43 : $half.hashCode());
        String $runnersBefore = this.getRunnersBefore();
        result = result * 59 + ($runnersBefore == null ? 43 : $runnersBefore.hashCode());
        String $resultCode = this.getResultCode();
        result = result * 59 + ($resultCode == null ? 43 : $resultCode.hashCode());
        String $bipCode = this.getBipCode();
        result = result * 59 + ($bipCode == null ? 43 : $bipCode.hashCode());
        BigDecimal $sprayX = this.getSprayX();
        result = result * 59 + ($sprayX == null ? 43 : ((Object)$sprayX).hashCode());
        BigDecimal $sprayY = this.getSprayY();
        result = result * 59 + ($sprayY == null ? 43 : ((Object)$sprayY).hashCode());
        String $sprayZone = this.getSprayZone();
        result = result * 59 + ($sprayZone == null ? 43 : $sprayZone.hashCode());
        String $sprayDepth = this.getSprayDepth();
        result = result * 59 + ($sprayDepth == null ? 43 : $sprayDepth.hashCode());
        LocalDateTime $recordedAt = this.getRecordedAt();
        result = result * 59 + ($recordedAt == null ? 43 : ((Object)$recordedAt).hashCode());
        return result;
    }
}

