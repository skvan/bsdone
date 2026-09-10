/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.PlayerClaim
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_player_claim")
@Comment(value="\u7403\u5458\u8eab\u4efd\u8ba4\u9886")
public class PlayerClaim
extends BaseEntity {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CANCELLED = "cancelled";
    public static final String REVIEWER_TEAM_MANAGER = "team_manager";
    public static final String REVIEWER_PLATFORM_ADMIN = "platform_admin";
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u7533\u8bf7\u7528\u6237ID")
    private Long userId;
    @Comment(value="\u7403\u5458ID")
    private Long playerId;
    @Comment(value="\u72b6\u6001")
    private String status = "pending";
    @Comment(value="\u5ba1\u6838\u4eba\u7c7b\u578b")
    private String reviewerType;
    @Comment(value="\u5ba1\u6838\u4ebaID")
    private Long reviewerId;
    @Comment(value="\u5ba1\u6838\u65f6\u95f4")
    private LocalDateTime reviewedAt;
    @Comment(value="\u62d2\u7edd\u539f\u56e0")
    private String rejectReason;
    @Comment(value="\u7533\u8bf7\u5907\u6ce8")
    private String remark;
    @Comment(value="\u9080\u8bf7ID")
    private Long inviteId;

    @Generated
    public PlayerClaim() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getUserId() {
        return this.userId;
    }

    @Generated
    public Long getPlayerId() {
        return this.playerId;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public String getReviewerType() {
        return this.reviewerType;
    }

    @Generated
    public Long getReviewerId() {
        return this.reviewerId;
    }

    @Generated
    public LocalDateTime getReviewedAt() {
        return this.reviewedAt;
    }

    @Generated
    public String getRejectReason() {
        return this.rejectReason;
    }

    @Generated
    public String getRemark() {
        return this.remark;
    }

    @Generated
    public Long getInviteId() {
        return this.inviteId;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Generated
    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setReviewerType(String reviewerType) {
        this.reviewerType = reviewerType;
    }

    @Generated
    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    @Generated
    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    @Generated
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public void setInviteId(Long inviteId) {
        this.inviteId = inviteId;
    }

    @Generated
    public String toString() {
        return "PlayerClaim(tenantId=" + this.getTenantId() + ", userId=" + this.getUserId() + ", playerId=" + this.getPlayerId() + ", status=" + this.getStatus() + ", reviewerType=" + this.getReviewerType() + ", reviewerId=" + this.getReviewerId() + ", reviewedAt=" + String.valueOf(this.getReviewedAt()) + ", rejectReason=" + this.getRejectReason() + ", remark=" + this.getRemark() + ", inviteId=" + this.getInviteId() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlayerClaim)) {
            return false;
        }
        PlayerClaim other = (PlayerClaim)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        Long this$playerId = this.getPlayerId();
        Long other$playerId = other.getPlayerId();
        if (this$playerId == null ? other$playerId != null : !((Object)this$playerId).equals(other$playerId)) {
            return false;
        }
        Long this$reviewerId = this.getReviewerId();
        Long other$reviewerId = other.getReviewerId();
        if (this$reviewerId == null ? other$reviewerId != null : !((Object)this$reviewerId).equals(other$reviewerId)) {
            return false;
        }
        Long this$inviteId = this.getInviteId();
        Long other$inviteId = other.getInviteId();
        if (this$inviteId == null ? other$inviteId != null : !((Object)this$inviteId).equals(other$inviteId)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$reviewerType = this.getReviewerType();
        String other$reviewerType = other.getReviewerType();
        if (this$reviewerType == null ? other$reviewerType != null : !this$reviewerType.equals(other$reviewerType)) {
            return false;
        }
        LocalDateTime this$reviewedAt = this.getReviewedAt();
        LocalDateTime other$reviewedAt = other.getReviewedAt();
        if (this$reviewedAt == null ? other$reviewedAt != null : !((Object)this$reviewedAt).equals(other$reviewedAt)) {
            return false;
        }
        String this$rejectReason = this.getRejectReason();
        String other$rejectReason = other.getRejectReason();
        if (this$rejectReason == null ? other$rejectReason != null : !this$rejectReason.equals(other$rejectReason)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        return !(this$remark == null ? other$remark != null : !this$remark.equals(other$remark));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PlayerClaim;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        Long $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : ((Object)$playerId).hashCode());
        Long $reviewerId = this.getReviewerId();
        result = result * 59 + ($reviewerId == null ? 43 : ((Object)$reviewerId).hashCode());
        Long $inviteId = this.getInviteId();
        result = result * 59 + ($inviteId == null ? 43 : ((Object)$inviteId).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $reviewerType = this.getReviewerType();
        result = result * 59 + ($reviewerType == null ? 43 : $reviewerType.hashCode());
        LocalDateTime $reviewedAt = this.getReviewedAt();
        result = result * 59 + ($reviewedAt == null ? 43 : ((Object)$reviewedAt).hashCode());
        String $rejectReason = this.getRejectReason();
        result = result * 59 + ($rejectReason == null ? 43 : $rejectReason.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }
}

