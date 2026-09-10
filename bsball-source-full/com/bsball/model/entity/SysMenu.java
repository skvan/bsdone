/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.json.HideMenuDeserializer
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysMenu
 *  com.fasterxml.jackson.databind.annotation.JsonDeserialize
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.persistence.Transient
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.common.json.HideMenuDeserializer;
import com.bsball.model.entity.BaseEntity;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.List;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_menu")
@Comment(value="\u7cfb\u7edf\u83dc\u5355")
public class SysMenu
extends BaseEntity {
    public static final int TYPE_DIRECTORY = 1;
    public static final int TYPE_MENU = 2;
    public static final int TYPE_BUTTON = 3;
    @Comment(value="\u7236\u83dc\u5355ID")
    private Long parentId = 0L;
    @Comment(value="\u83dc\u5355\u540d")
    private String name;
    @Comment(value="\u6807\u9898")
    private String title;
    @Comment(value="\u8def\u5f84")
    private String path;
    @Comment(value="\u8def\u7531\u540d")
    private String routeName;
    @Comment(value="\u7ec4\u4ef6")
    private String component;
    @Comment(value="\u91cd\u5b9a\u5411")
    private String redirect;
    @Comment(value="\u56fe\u6807")
    private String icon;
    @Comment(value="\u9690\u85cf\u83dc\u5355")
    @JsonDeserialize(using=HideMenuDeserializer.class)
    private Integer hideMenu = 0;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;
    @Comment(value="\u7c7b\u578b\uff1a1\u76ee\u5f55 2\u83dc\u5355 3\u6309\u94ae")
    @Column(name="menu_type")
    private Integer menuType = 2;
    @Column(length=100)
    @Comment(value="\u6743\u9650\u6807\u8bc6\uff0c\u5982 game:export\uff1b\u6309\u94ae\u5efa\u8bae\u586b\u5199")
    private String permission;
    @Transient
    private List<Long> apiIds;
    @Transient
    private List<SysMenu> children;

    @Generated
    public SysMenu() {
    }

    @Generated
    public Long getParentId() {
        return this.parentId;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getTitle() {
        return this.title;
    }

    @Generated
    public String getPath() {
        return this.path;
    }

    @Generated
    public String getRouteName() {
        return this.routeName;
    }

    @Generated
    public String getComponent() {
        return this.component;
    }

    @Generated
    public String getRedirect() {
        return this.redirect;
    }

    @Generated
    public String getIcon() {
        return this.icon;
    }

    @Generated
    public Integer getHideMenu() {
        return this.hideMenu;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public Integer getMenuType() {
        return this.menuType;
    }

    @Generated
    public String getPermission() {
        return this.permission;
    }

    @Generated
    public List<Long> getApiIds() {
        return this.apiIds;
    }

    @Generated
    public List<SysMenu> getChildren() {
        return this.children;
    }

    @Generated
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setTitle(String title) {
        this.title = title;
    }

    @Generated
    public void setPath(String path) {
        this.path = path;
    }

    @Generated
    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    @Generated
    public void setComponent(String component) {
        this.component = component;
    }

    @Generated
    public void setRedirect(String redirect) {
        this.redirect = redirect;
    }

    @Generated
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Generated
    public void setHideMenu(Integer hideMenu) {
        this.hideMenu = hideMenu;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public void setMenuType(Integer menuType) {
        this.menuType = menuType;
    }

    @Generated
    public void setPermission(String permission) {
        this.permission = permission;
    }

    @Generated
    public void setApiIds(List<Long> apiIds) {
        this.apiIds = apiIds;
    }

    @Generated
    public void setChildren(List<SysMenu> children) {
        this.children = children;
    }

    @Generated
    public String toString() {
        return "SysMenu(parentId=" + this.getParentId() + ", name=" + this.getName() + ", title=" + this.getTitle() + ", path=" + this.getPath() + ", routeName=" + this.getRouteName() + ", component=" + this.getComponent() + ", redirect=" + this.getRedirect() + ", icon=" + this.getIcon() + ", hideMenu=" + this.getHideMenu() + ", sort=" + this.getSort() + ", menuType=" + this.getMenuType() + ", permission=" + this.getPermission() + ", apiIds=" + String.valueOf(this.getApiIds()) + ", children=" + String.valueOf(this.getChildren()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysMenu)) {
            return false;
        }
        SysMenu other = (SysMenu)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$parentId = this.getParentId();
        Long other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !((Object)this$parentId).equals(other$parentId)) {
            return false;
        }
        Integer this$hideMenu = this.getHideMenu();
        Integer other$hideMenu = other.getHideMenu();
        if (this$hideMenu == null ? other$hideMenu != null : !((Object)this$hideMenu).equals(other$hideMenu)) {
            return false;
        }
        Integer this$sort = this.getSort();
        Integer other$sort = other.getSort();
        if (this$sort == null ? other$sort != null : !((Object)this$sort).equals(other$sort)) {
            return false;
        }
        Integer this$menuType = this.getMenuType();
        Integer other$menuType = other.getMenuType();
        if (this$menuType == null ? other$menuType != null : !((Object)this$menuType).equals(other$menuType)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$path = this.getPath();
        String other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) {
            return false;
        }
        String this$routeName = this.getRouteName();
        String other$routeName = other.getRouteName();
        if (this$routeName == null ? other$routeName != null : !this$routeName.equals(other$routeName)) {
            return false;
        }
        String this$component = this.getComponent();
        String other$component = other.getComponent();
        if (this$component == null ? other$component != null : !this$component.equals(other$component)) {
            return false;
        }
        String this$redirect = this.getRedirect();
        String other$redirect = other.getRedirect();
        if (this$redirect == null ? other$redirect != null : !this$redirect.equals(other$redirect)) {
            return false;
        }
        String this$icon = this.getIcon();
        String other$icon = other.getIcon();
        if (this$icon == null ? other$icon != null : !this$icon.equals(other$icon)) {
            return false;
        }
        String this$permission = this.getPermission();
        String other$permission = other.getPermission();
        if (this$permission == null ? other$permission != null : !this$permission.equals(other$permission)) {
            return false;
        }
        List this$apiIds = this.getApiIds();
        List other$apiIds = other.getApiIds();
        if (this$apiIds == null ? other$apiIds != null : !((Object)this$apiIds).equals(other$apiIds)) {
            return false;
        }
        List this$children = this.getChildren();
        List other$children = other.getChildren();
        return !(this$children == null ? other$children != null : !((Object)this$children).equals(other$children));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysMenu;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $parentId = this.getParentId();
        result = result * 59 + ($parentId == null ? 43 : ((Object)$parentId).hashCode());
        Integer $hideMenu = this.getHideMenu();
        result = result * 59 + ($hideMenu == null ? 43 : ((Object)$hideMenu).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        Integer $menuType = this.getMenuType();
        result = result * 59 + ($menuType == null ? 43 : ((Object)$menuType).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $path = this.getPath();
        result = result * 59 + ($path == null ? 43 : $path.hashCode());
        String $routeName = this.getRouteName();
        result = result * 59 + ($routeName == null ? 43 : $routeName.hashCode());
        String $component = this.getComponent();
        result = result * 59 + ($component == null ? 43 : $component.hashCode());
        String $redirect = this.getRedirect();
        result = result * 59 + ($redirect == null ? 43 : $redirect.hashCode());
        String $icon = this.getIcon();
        result = result * 59 + ($icon == null ? 43 : $icon.hashCode());
        String $permission = this.getPermission();
        result = result * 59 + ($permission == null ? 43 : $permission.hashCode());
        List $apiIds = this.getApiIds();
        result = result * 59 + ($apiIds == null ? 43 : ((Object)$apiIds).hashCode());
        List $children = this.getChildren();
        result = result * 59 + ($children == null ? 43 : ((Object)$children).hashCode());
        return result;
    }
}

