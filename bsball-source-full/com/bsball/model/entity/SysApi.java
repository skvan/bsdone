/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysApi
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_api")
@Comment(value="API\u6743\u9650")
public class SysApi
extends BaseEntity {
    @Comment(value="\u8def\u5f84")
    private String path;
    @Comment(value="\u8bf7\u6c42\u65b9\u6cd5")
    private String method = "GET";
    @Column(length=200)
    @Size(max=200, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc7200\u5b57")
    @Comment(value="\u63cf\u8ff0")
    private @Size(max=200, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc7200\u5b57") String description;
    @Column(name="api_group")
    @Comment(value="\u5206\u7ec4")
    private String groupName;

    @Generated
    public SysApi() {
    }

    @Generated
    public String getPath() {
        return this.path;
    }

    @Generated
    public String getMethod() {
        return this.method;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public String getGroupName() {
        return this.groupName;
    }

    @Generated
    public void setPath(String path) {
        this.path = path;
    }

    @Generated
    public void setMethod(String method) {
        this.method = method;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @Generated
    public String toString() {
        return "SysApi(path=" + this.getPath() + ", method=" + this.getMethod() + ", description=" + this.getDescription() + ", groupName=" + this.getGroupName() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysApi)) {
            return false;
        }
        SysApi other = (SysApi)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        String this$path = this.getPath();
        String other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) {
            return false;
        }
        String this$method = this.getMethod();
        String other$method = other.getMethod();
        if (this$method == null ? other$method != null : !this$method.equals(other$method)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$groupName = this.getGroupName();
        String other$groupName = other.getGroupName();
        return !(this$groupName == null ? other$groupName != null : !this$groupName.equals(other$groupName));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysApi;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        String $path = this.getPath();
        result = result * 59 + ($path == null ? 43 : $path.hashCode());
        String $method = this.getMethod();
        result = result * 59 + ($method == null ? 43 : $method.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $groupName = this.getGroupName();
        result = result * 59 + ($groupName == null ? 43 : $groupName.hashCode());
        return result;
    }
}

