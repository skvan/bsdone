/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysNotice
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
@Table(name="sys_notice")
@Comment(value="\u7cfb\u7edf\u901a\u77e5")
public class SysNotice
extends BaseEntity {
    @Comment(value="\u79df\u6237ID")
    @Column(name="tenant_id")
    private Long tenantId;
    @Comment(value="\u6807\u9898")
    private String title;
    @Column(columnDefinition="TEXT")
    @Size(max=50000, message="\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc750000\u5b57")
    @Comment(value="\u5185\u5bb9")
    private @Size(max=50000, message="\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc750000\u5b57") String content;
    @Comment(value="\u76ee\u6807")
    private String target = "portal";

    @Generated
    public SysNotice() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public String getTitle() {
        return this.title;
    }

    @Generated
    public String getContent() {
        return this.content;
    }

    @Generated
    public String getTarget() {
        return this.target;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setTitle(String title) {
        this.title = title;
    }

    @Generated
    public void setContent(String content) {
        this.content = content;
    }

    @Generated
    public void setTarget(String target) {
        this.target = target;
    }

    @Generated
    public String toString() {
        return "SysNotice(tenantId=" + this.getTenantId() + ", title=" + this.getTitle() + ", content=" + this.getContent() + ", target=" + this.getTarget() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysNotice)) {
            return false;
        }
        SysNotice other = (SysNotice)o;
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
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        String this$target = this.getTarget();
        String other$target = other.getTarget();
        return !(this$target == null ? other$target != null : !this$target.equals(other$target));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysNotice;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $target = this.getTarget();
        result = result * 59 + ($target == null ? 43 : $target.hashCode());
        return result;
    }
}

