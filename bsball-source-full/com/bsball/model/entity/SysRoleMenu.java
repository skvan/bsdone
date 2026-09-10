/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysRoleMenu
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
@Table(name="sys_role_menu", uniqueConstraints={@UniqueConstraint(columnNames={"role_id", "menu_id"})})
@Comment(value="\u89d2\u8272\u83dc\u5355\u5173\u8054")
public class SysRoleMenu
extends BaseEntity {
    @Comment(value="\u89d2\u8272ID")
    private Long roleId;
    @Comment(value="\u83dc\u5355ID")
    private Long menuId;

    @Generated
    public SysRoleMenu() {
    }

    @Generated
    public Long getRoleId() {
        return this.roleId;
    }

    @Generated
    public Long getMenuId() {
        return this.menuId;
    }

    @Generated
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Generated
    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    @Generated
    public String toString() {
        return "SysRoleMenu(roleId=" + this.getRoleId() + ", menuId=" + this.getMenuId() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysRoleMenu)) {
            return false;
        }
        SysRoleMenu other = (SysRoleMenu)o;
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
        Long this$menuId = this.getMenuId();
        Long other$menuId = other.getMenuId();
        return !(this$menuId == null ? other$menuId != null : !((Object)this$menuId).equals(other$menuId));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysRoleMenu;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $roleId = this.getRoleId();
        result = result * 59 + ($roleId == null ? 43 : ((Object)$roleId).hashCode());
        Long $menuId = this.getMenuId();
        result = result * 59 + ($menuId == null ? 43 : ((Object)$menuId).hashCode());
        return result;
    }
}

