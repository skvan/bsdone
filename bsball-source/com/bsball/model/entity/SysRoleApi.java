/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysRoleApi
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.persistence.UniqueConstraint
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_role_api", uniqueConstraints={@UniqueConstraint(columnNames={"role_id", "api_id"})})
@Comment(value="\u89d2\u8272API\u5173\u8054")
public class SysRoleApi
extends BaseEntity {
    @Comment(value="\u89d2\u8272ID")
    private Long roleId;
    @Comment(value="API ID")
    private Long apiId;

    @Generated
    public SysRoleApi() {
    }

    @Generated
    public Long getRoleId() {
        return this.roleId;
    }

    @Generated
    public Long getApiId() {
        return this.apiId;
    }

    @Generated
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Generated
    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    @Generated
    public String toString() {
        return "SysRoleApi(roleId=" + this.getRoleId() + ", apiId=" + this.getApiId() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysRoleApi)) {
            return false;
        }
        SysRoleApi other = (SysRoleApi)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$roleId = this.getRoleId();
        Long other$roleId = other.getRoleId();
        if (this$roleId == null ? other$roleId != null : !((Object)this$roleId).equals(other$roleId)) {
            return false;
        }
        Long this$apiId = this.getApiId();
        Long other$apiId = other.getApiId();
        return !(this$apiId == null ? other$apiId != null : !((Object)this$apiId).equals(other$apiId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysRoleApi;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $roleId = this.getRoleId();
        result = result * 59 + ($roleId == null ? 43 : ((Object)$roleId).hashCode());
        Long $apiId = this.getApiId();
        result = result * 59 + ($apiId == null ? 43 : ((Object)$apiId).hashCode());
        return result;
    }
}

