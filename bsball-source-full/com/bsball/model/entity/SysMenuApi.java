/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysMenuApi
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.persistence.UniqueConstraint
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_menu_api", uniqueConstraints={@UniqueConstraint(columnNames={"menu_id", "api_id"})})
@Comment(value="\u83dc\u5355\u4e0eAPI\u5173\u8054")
public class SysMenuApi
extends BaseEntity {
    @Comment(value="\u83dc\u5355ID")
    @Column(name="menu_id", nullable=false)
    private Long menuId;
    @Comment(value="API ID")
    @Column(name="api_id", nullable=false)
    private Long apiId;

    @Generated
    public SysMenuApi() {
    }

    @Generated
    public Long getMenuId() {
        return this.menuId;
    }

    @Generated
    public Long getApiId() {
        return this.apiId;
    }

    @Generated
    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    @Generated
    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    @Generated
    public String toString() {
        return "SysMenuApi(menuId=" + this.getMenuId() + ", apiId=" + this.getApiId() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysMenuApi)) {
            return false;
        }
        SysMenuApi other = (SysMenuApi)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$menuId = this.getMenuId();
        Long other$menuId = other.getMenuId();
        if (this$menuId == null ? other$menuId != null : !((Object)this$menuId).equals(other$menuId)) {
            return false;
        }
        Long this$apiId = this.getApiId();
        Long other$apiId = other.getApiId();
        return !(this$apiId == null ? other$apiId != null : !((Object)this$apiId).equals(other$apiId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysMenuApi;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $menuId = this.getMenuId();
        result = result * 59 + ($menuId == null ? 43 : ((Object)$menuId).hashCode());
        Long $apiId = this.getApiId();
        result = result * 59 + ($apiId == null ? 43 : ((Object)$apiId).hashCode());
        return result;
    }
}

