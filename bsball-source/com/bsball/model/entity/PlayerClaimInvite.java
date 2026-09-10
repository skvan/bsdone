/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PlayerClaimInvite
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_player_claim_invite")
@Comment(value="\u7403\u5458\u8ba4\u9886\u9080\u8bf7")
public class PlayerClaimInvite {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_EXPIRED = "expired";
    public static final String STATUS_REVOKED = "revoked";
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u7403\u961fID")
    private Long teamId;
    @Comment(value="\u6307\u5b9a\u7403\u5458ID\uff08\u53ef\u7a7a=\u8ba4\u9886\u8be5\u961f\u4efb\u610f\u672a\u7ed1\u5b9a\u7403\u5458\uff09")
    private Long playerId;
    @Comment(value="\u9080\u8bf7\u4ee4\u724c")
    private String token;
    @Comment(value="\u521b\u5efa\u4eba")
    private Long createdBy;
    @Comment(value="\u8fc7\u671f\u65f6\u95f4")
    private LocalDateTime expiresAt;
    @Comment(value="\u6700\u5927\u4f7f\u7528\u6b21\u6570")
    private Integer maxUses = 1;
    @Comment(value="\u5df2\u4f7f\u7528\u6b21\u6570")
    private Integer usedCount = 0;
    @Comment(value="\u72b6\u6001")
    private String status = "active";
    @Comment(value="\u5907\u6ce8")
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Generated
    public PlayerClaimInvite() {
    }

    @Generated
    public Long getId() {
        return this.id;
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
    public String getToken() {
        return this.token;
    }

    @Generated
    public Long getCreatedBy() {
        return this.createdBy;
    }

    @Generated
    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    @Generated
    public Integer getMaxUses() {
        return this.maxUses;
    }

    @Generated
    public Integer getUsedCount() {
        return this.usedCount;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public String getRemark() {
        return this.remark;
    }

    @Generated
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
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
    public void setToken(String token) {
        this.token = token;
    }

    @Generated
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    @Generated
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Generated
    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }

    @Generated
    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PlayerClaimInvite)) {
            return false;
        }
        PlayerClaimInvite other = (PlayerClaimInvite)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
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
        Long this$createdBy = this.getCreatedBy();
        Long other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !((Object)this$createdBy).equals(other$createdBy)) {
            return false;
        }
        Integer this$maxUses = this.getMaxUses();
        Integer other$maxUses = other.getMaxUses();
        if (this$maxUses == null ? other$maxUses != null : !((Object)this$maxUses).equals(other$maxUses)) {
            return false;
        }
        Integer this$usedCount = this.getUsedCount();
        Integer other$usedCount = other.getUsedCount();
        if (this$usedCount == null ? other$usedCount != null : !((Object)this$usedCount).equals(other$usedCount)) {
            return false;
        }
        String this$token = this.getToken();
        String other$token = other.getToken();
        if (this$token == null ? other$token != null : !this$token.equals(other$token)) {
            return false;
        }
        LocalDateTime this$expiresAt = this.getExpiresAt();
        LocalDateTime other$expiresAt = other.getExpiresAt();
        if (this$expiresAt == null ? other$expiresAt != null : !((Object)this$expiresAt).equals(other$expiresAt)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt)) {
            return false;
        }
        LocalDateTime this$deletedAt = this.getDeletedAt();
        LocalDateTime other$deletedAt = other.getDeletedAt();
        return !(this$deletedAt == null ? other$deletedAt != null : !((Object)this$deletedAt).equals(other$deletedAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PlayerClaimInvite;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Long $playerId = this.getPlayerId();
        result = result * 59 + ($playerId == null ? 43 : ((Object)$playerId).hashCode());
        Long $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : ((Object)$createdBy).hashCode());
        Integer $maxUses = this.getMaxUses();
        result = result * 59 + ($maxUses == null ? 43 : ((Object)$maxUses).hashCode());
        Integer $usedCount = this.getUsedCount();
        result = result * 59 + ($usedCount == null ? 43 : ((Object)$usedCount).hashCode());
        String $token = this.getToken();
        result = result * 59 + ($token == null ? 43 : $token.hashCode());
        LocalDateTime $expiresAt = this.getExpiresAt();
        result = result * 59 + ($expiresAt == null ? 43 : ((Object)$expiresAt).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        LocalDateTime $deletedAt = this.getDeletedAt();
        result = result * 59 + ($deletedAt == null ? 43 : ((Object)$deletedAt).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "PlayerClaimInvite(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", teamId=" + this.getTeamId() + ", playerId=" + this.getPlayerId() + ", token=" + this.getToken() + ", createdBy=" + this.getCreatedBy() + ", expiresAt=" + String.valueOf(this.getExpiresAt()) + ", maxUses=" + this.getMaxUses() + ", usedCount=" + this.getUsedCount() + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ", deletedAt=" + String.valueOf(this.getDeletedAt()) + ")";
    }
}

