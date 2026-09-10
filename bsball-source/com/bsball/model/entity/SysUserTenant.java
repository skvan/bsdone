/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysUserTenant
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_user_tenant")
@Comment(value="\u7528\u6237\u79df\u6237\u6210\u5458")
public class SysUserTenant
extends BaseEntity {
    @Comment(value="\u7528\u6237ID")
    private Long userId;
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;

    @Generated
    public SysUserTenant() {
    }

    @Generated
    public Long getUserId() {
        return this.userId;
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public String toString() {
        return "SysUserTenant(userId=" + this.getUserId() + ", tenantId=" + this.getTenantId() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysUserTenant)) {
            return false;
        }
        SysUserTenant other = (SysUserTenant)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        return !(this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysUserTenant;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        return result;
    }
}

