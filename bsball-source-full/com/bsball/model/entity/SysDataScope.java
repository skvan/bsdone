/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysDataScope
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
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_data_scope")
@Comment(value="\u6570\u636e\u8303\u56f4")
public class SysDataScope
extends BaseEntity {
    public static final String TYPE_LEAGUE = "LEAGUE";
    public static final String TYPE_TEAM = "TEAM";
    public static final String EXP_SELF = "SELF";
    public static final String EXP_INCLUDE_DESCENDANTS = "INCLUDE_DESCENDANTS";
    @Comment(value="\u7528\u6237ID")
    private Long userId;
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Column(length=32)
    @Comment(value="LEAGUE | TEAM")
    private String scopeType;
    @Comment(value="\u8054\u76dfID \u6216 \u7403\u961fID")
    private Long refId;
    @Column(length=32)
    @Comment(value="SELF | INCLUDE_DESCENDANTS")
    private String expansion = "SELF";

    @Generated
    public SysDataScope() {
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
    public String getScopeType() {
        return this.scopeType;
    }

    @Generated
    public Long getRefId() {
        return this.refId;
    }

    @Generated
    public String getExpansion() {
        return this.expansion;
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
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Generated
    public void setRefId(Long refId) {
        this.refId = refId;
    }

    @Generated
    public void setExpansion(String expansion) {
        this.expansion = expansion;
    }

    @Generated
    public String toString() {
        return "SysDataScope(userId=" + this.getUserId() + ", tenantId=" + this.getTenantId() + ", scopeType=" + this.getScopeType() + ", refId=" + this.getRefId() + ", expansion=" + this.getExpansion() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysDataScope)) {
            return false;
        }
        SysDataScope other = (SysDataScope)o;
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
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Long this$refId = this.getRefId();
        Long other$refId = other.getRefId();
        if (this$refId == null ? other$refId != null : !((Object)this$refId).equals(other$refId)) {
            return false;
        }
        String this$scopeType = this.getScopeType();
        String other$scopeType = other.getScopeType();
        if (this$scopeType == null ? other$scopeType != null : !this$scopeType.equals(other$scopeType)) {
            return false;
        }
        String this$expansion = this.getExpansion();
        String other$expansion = other.getExpansion();
        return !(this$expansion == null ? other$expansion != null : !this$expansion.equals(other$expansion));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysDataScope;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $refId = this.getRefId();
        result = result * 59 + ($refId == null ? 43 : ((Object)$refId).hashCode());
        String $scopeType = this.getScopeType();
        result = result * 59 + ($scopeType == null ? 43 : $scopeType.hashCode());
        String $expansion = this.getExpansion();
        result = result * 59 + ($expansion == null ? 43 : $expansion.hashCode());
        return result;
    }
}

