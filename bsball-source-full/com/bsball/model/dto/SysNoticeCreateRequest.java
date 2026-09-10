/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.SysNoticeCreateRequest
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 */
package com.bsball.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Generated;

public class SysNoticeCreateRequest {
    @NotBlank(message="\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max=80, message="\u6807\u9898\u6700\u591a80\u5b57")
    private @NotBlank(message="\u6807\u9898\u4e0d\u80fd\u4e3a\u7a7a") @Size(max=80, message="\u6807\u9898\u6700\u591a80\u5b57") String title;
    @NotBlank(message="\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max=50000, message="\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc750000\u5b57")
    private @NotBlank(message="\u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a") @Size(max=50000, message="\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc750000\u5b57") String content;
    private String target = "portal";
    private List<Long> tenantIds;

    @Generated
    public SysNoticeCreateRequest() {
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
    public List<Long> getTenantIds() {
        return this.tenantIds;
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
    public void setTenantIds(List<Long> tenantIds) {
        this.tenantIds = tenantIds;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysNoticeCreateRequest)) {
            return false;
        }
        SysNoticeCreateRequest other = (SysNoticeCreateRequest)o;
        if (!other.canEqual((Object)this)) {
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
        if (this$target == null ? other$target != null : !this$target.equals(other$target)) {
            return false;
        }
        List this$tenantIds = this.getTenantIds();
        List other$tenantIds = other.getTenantIds();
        return !(this$tenantIds == null ? other$tenantIds != null : !((Object)this$tenantIds).equals(other$tenantIds));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysNoticeCreateRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $target = this.getTarget();
        result = result * 59 + ($target == null ? 43 : $target.hashCode());
        List $tenantIds = this.getTenantIds();
        result = result * 59 + ($tenantIds == null ? 43 : ((Object)$tenantIds).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SysNoticeCreateRequest(title=" + this.getTitle() + ", content=" + this.getContent() + ", target=" + this.getTarget() + ", tenantIds=" + String.valueOf(this.getTenantIds()) + ")";
    }
}

