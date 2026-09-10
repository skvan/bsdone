/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysRole
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.persistence.Transient
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_role")
@Comment(value="\u7cfb\u7edf\u89d2\u8272")
public class SysRole
extends BaseEntity {
    @Comment(value="\u89d2\u8272\u540d\u79f0")
    private String name;
    @Comment(value="\u89d2\u8272\u7f16\u7801")
    private String code;
    @Comment(value="\u72b6\u6001")
    private Integer status = 1;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;
    @Size(max=500)
    @Comment(value="\u8bf4\u660e/\u5907\u6ce8")
    private @Size(max=500) String description;
    @Comment(value="\u79df\u6237ID\uff1aNULL=\u7cfb\u7edf\u7ea7")
    @Column(name="tenant_id")
    private Long tenantId;
    @Transient
    private List<Long> menuIds;
    @Transient
    private List<Long> apiIds;

    @Generated
    public SysRole() {
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getCode() {
        return this.code;
    }

    @Generated
    public Integer getStatus() {
        return this.status;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public List<Long> getMenuIds() {
        return this.menuIds;
    }

    @Generated
    public List<Long> getApiIds() {
        return this.apiIds;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setCode(String code) {
        this.code = code;
    }

    @Generated
    public void setStatus(Integer status) {
        this.status = status;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }

    @Generated
    public void setApiIds(List<Long> apiIds) {
        this.apiIds = apiIds;
    }

    @Generated
    public String toString() {
        return "SysRole(name=" + this.getName() + ", code=" + this.getCode() + ", status=" + this.getStatus() + ", sort=" + this.getSort() + ", description=" + this.getDescription() + ", tenantId=" + this.getTenantId() + ", menuIds=" + String.valueOf(this.getMenuIds()) + ", apiIds=" + String.valueOf(this.getApiIds()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysRole)) {
            return false;
        }
        SysRole other = (SysRole)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Integer this$status = this.getStatus();
        Integer other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)this$status).equals(other$status)) {
            return false;
        }
        Integer this$sort = this.getSort();
        Integer other$sort = other.getSort();
        if (this$sort == null ? other$sort != null : !((Object)this$sort).equals(other$sort)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$code = this.getCode();
        String other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        List this$menuIds = this.getMenuIds();
        List other$menuIds = other.getMenuIds();
        if (this$menuIds == null ? other$menuIds != null : !((Object)this$menuIds).equals(other$menuIds)) {
            return false;
        }
        List this$apiIds = this.getApiIds();
        List other$apiIds = other.getApiIds();
        return !(this$apiIds == null ? other$apiIds != null : !((Object)this$apiIds).equals(other$apiIds));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysRole;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        List $menuIds = this.getMenuIds();
        result = result * 59 + ($menuIds == null ? 43 : ((Object)$menuIds).hashCode());
        List $apiIds = this.getApiIds();
        result = result * 59 + ($apiIds == null ? 43 : ((Object)$apiIds).hashCode());
        return result;
    }
}

