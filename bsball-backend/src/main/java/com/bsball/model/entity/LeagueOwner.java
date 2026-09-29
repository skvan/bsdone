package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_league_owner")
@Comment(value="联盟主办方归属关系")
public class LeagueOwner
extends BaseEntity {
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_INACTIVE = "inactive";
    public static final String GRANT_SELF_CREATE = "SELF_CREATE";
    public static final String GRANT_ADMIN_ASSIGN = "ADMIN_ASSIGN";
    @Comment(value="租户ID")
    private Long tenantId;
    @Comment(value="联盟ID")
    private Long leagueId;
    @Comment(value="主办方用户ID")
    private Long userId;
    @Comment(value="状态: active | inactive")
    private String status = STATUS_ACTIVE;
    @Comment(value="授予来源: SELF_CREATE | ADMIN_ASSIGN")
    private String grantSource = GRANT_ADMIN_ASSIGN;

    @Generated
    public LeagueOwner() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getLeagueId() {
        return this.leagueId;
    }

    @Generated
    public Long getUserId() {
        return this.userId;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public String getGrantSource() {
        return this.grantSource;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }

    @Generated
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setGrantSource(String grantSource) {
        this.grantSource = grantSource;
    }

    @Generated
    public String toString() {
        return "LeagueOwner(tenantId=" + this.getTenantId() + ", leagueId=" + this.getLeagueId() + ", userId=" + this.getUserId() + ", status=" + this.getStatus() + ", grantSource=" + this.getGrantSource() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LeagueOwner)) {
            return false;
        }
        LeagueOwner other = (LeagueOwner)o;
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
        Long this$leagueId = this.getLeagueId();
        Long other$leagueId = other.getLeagueId();
        if (this$leagueId == null ? other$leagueId != null : !((Object)this$leagueId).equals(other$leagueId)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$grantSource = this.getGrantSource();
        String other$grantSource = other.getGrantSource();
        return !(this$grantSource == null ? other$grantSource != null : !this$grantSource.equals(other$grantSource));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LeagueOwner;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $leagueId = this.getLeagueId();
        result = result * 59 + ($leagueId == null ? 43 : ((Object)$leagueId).hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $grantSource = this.getGrantSource();
        result = result * 59 + ($grantSource == null ? 43 : $grantSource.hashCode());
        return result;
    }
}
