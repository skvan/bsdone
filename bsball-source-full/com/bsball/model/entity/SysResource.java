/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysResource
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
@Table(name="sys_resource")
@Comment(value="\u8d44\u6e90\u6587\u4ef6")
public class SysResource
extends BaseEntity {
    @Comment(value="\u6240\u5c5e\u79df\u6237")
    private Long tenantId;
    @Comment(value="\u6587\u4ef6\u540d")
    private String name;
    @Comment(value="\u5b58\u50a8\u8def\u5f84")
    private String path;
    @Comment(value="\u8bbf\u95eeURL")
    private String url;
    @Comment(value="\u5927\u5c0f")
    private Long size;
    @Comment(value="MIME\u7c7b\u578b")
    private String mime;

    @Generated
    public SysResource() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getPath() {
        return this.path;
    }

    @Generated
    public String getUrl() {
        return this.url;
    }

    @Generated
    public Long getSize() {
        return this.size;
    }

    @Generated
    public String getMime() {
        return this.mime;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setPath(String path) {
        this.path = path;
    }

    @Generated
    public void setUrl(String url) {
        this.url = url;
    }

    @Generated
    public void setSize(Long size) {
        this.size = size;
    }

    @Generated
    public void setMime(String mime) {
        this.mime = mime;
    }

    @Generated
    public String toString() {
        return "SysResource(tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", path=" + this.getPath() + ", url=" + this.getUrl() + ", size=" + this.getSize() + ", mime=" + this.getMime() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysResource)) {
            return false;
        }
        SysResource other = (SysResource)o;
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
        Long this$size = this.getSize();
        Long other$size = other.getSize();
        if (this$size == null ? other$size != null : !((Object)this$size).equals(other$size)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$path = this.getPath();
        String other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) {
            return false;
        }
        String this$url = this.getUrl();
        String other$url = other.getUrl();
        if (this$url == null ? other$url != null : !this$url.equals(other$url)) {
            return false;
        }
        String this$mime = this.getMime();
        String other$mime = other.getMime();
        return !(this$mime == null ? other$mime != null : !this$mime.equals(other$mime));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysResource;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $size = this.getSize();
        result = result * 59 + ($size == null ? 43 : ((Object)$size).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $path = this.getPath();
        result = result * 59 + ($path == null ? 43 : $path.hashCode());
        String $url = this.getUrl();
        result = result * 59 + ($url == null ? 43 : $url.hashCode());
        String $mime = this.getMime();
        result = result * 59 + ($mime == null ? 43 : $mime.hashCode());
        return result;
    }
}

