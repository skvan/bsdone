/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysArticle
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.ColumnDefault
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Generated;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_article")
@Comment(value="\u6587\u7ae0")
public class SysArticle
extends BaseEntity {
    @Comment(value="\u79df\u6237ID\uff08\u7a7a=\u5e73\u53f0\u7ea7\u6587\u6863\uff09")
    @Column(nullable=true)
    private Long tenantId;
    @Comment(value="\u6807\u9898")
    private String title;
    @Comment(value="\u53d1\u5e03\u5bf9\u8c61")
    private String publishTarget = "both";
    @Column(length=1000)
    @Size(max=1000, message="\u6458\u8981\u4e0d\u80fd\u8d85\u8fc71000\u5b57")
    @Comment(value="\u6458\u8981")
    private @Size(max=1000, message="\u6458\u8981\u4e0d\u80fd\u8d85\u8fc71000\u5b57") String summary;
    @Comment(value="\u5c01\u9762")
    private String cover;
    @Column(columnDefinition="TEXT")
    @Size(max=50000, message="\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc750000\u5b57")
    @Comment(value="\u5185\u5bb9")
    private @Size(max=50000, message="\u5185\u5bb9\u4e0d\u80fd\u8d85\u8fc750000\u5b57") String content;
    @Comment(value="\u7c7b\u578b")
    private String type;
    @Comment(value="\u4f5c\u8005")
    private String author;
    @Comment(value="\u5185\u5bb9\u7c7b\u578b")
    private String contentType = "markdown";
    @Column(columnDefinition="TEXT")
    @Comment(value="\u9644\u4ef6")
    private String attachments;
    @Comment(value="\u662f\u5426\u7f6e\u9876")
    private Integer isPinned = 0;
    @Comment(value="\u7f6e\u9876\u65f6\u95f4")
    private String pinnedAt;
    @Comment(value="\u8f6e\u64ad\u5c55\u793a")
    private Integer showInCarousel = 0;
    @Comment(value="\u8f6e\u64ad\u987a\u5e8f")
    private Integer carouselOrder = 0;
    @Comment(value="\u72b6\u6001")
    private Integer status = 1;
    @Comment(value="\u6d4f\u89c8\u6b21\u6570\uff08\u524d\u53f0\u8be6\u60c5\u9875\u8bbf\u95ee\u7d2f\u8ba1\uff09")
    @ColumnDefault(value="0")
    @Column(nullable=false)
    private long viewCount = 0L;
    @Column(length=2000)
    @Size(max=2000, message="\u539f\u6587\u94fe\u63a5\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u539f\u6587\u94fe\u63a5\uff08\u5916\u94fe\uff09")
    private @Size(max=2000, message="\u539f\u6587\u94fe\u63a5\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String sourceUrl;
    @Column(length=128)
    @Comment(value="\u53d1\u8868/\u4fdd\u5b58\u65f6\u5ba2\u6237\u7aef IP\uff08\u670d\u52a1\u7aef\u89e3\u6790\uff09")
    private String submitIp;
    @Column(length=512)
    @Comment(value="\u53d1\u8868/\u4fdd\u5b58\u65f6 IP \u5bf9\u5e94\u7701\u7ea7\u7b80\u79f0\uff08\u53bb\u6389\u300c\u7701\u300d\u540e\u7f00\uff0c\u4e0e ip_location_cache.province \u5bf9\u5e94\u4f46\u65e0\u300c\u7701\u300d\u5b57\uff09")
    private String submitIpRegion;

    @Generated
    public SysArticle() {
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
    public String getPublishTarget() {
        return this.publishTarget;
    }

    @Generated
    public String getSummary() {
        return this.summary;
    }

    @Generated
    public String getCover() {
        return this.cover;
    }

    @Generated
    public String getContent() {
        return this.content;
    }

    @Generated
    public String getType() {
        return this.type;
    }

    @Generated
    public String getAuthor() {
        return this.author;
    }

    @Generated
    public String getContentType() {
        return this.contentType;
    }

    @Generated
    public String getAttachments() {
        return this.attachments;
    }

    @Generated
    public Integer getIsPinned() {
        return this.isPinned;
    }

    @Generated
    public String getPinnedAt() {
        return this.pinnedAt;
    }

    @Generated
    public Integer getShowInCarousel() {
        return this.showInCarousel;
    }

    @Generated
    public Integer getCarouselOrder() {
        return this.carouselOrder;
    }

    @Generated
    public Integer getStatus() {
        return this.status;
    }

    @Generated
    public long getViewCount() {
        return this.viewCount;
    }

    @Generated
    public String getSourceUrl() {
        return this.sourceUrl;
    }

    @Generated
    public String getSubmitIp() {
        return this.submitIp;
    }

    @Generated
    public String getSubmitIpRegion() {
        return this.submitIpRegion;
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
    public void setPublishTarget(String publishTarget) {
        this.publishTarget = publishTarget;
    }

    @Generated
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Generated
    public void setCover(String cover) {
        this.cover = cover;
    }

    @Generated
    public void setContent(String content) {
        this.content = content;
    }

    @Generated
    public void setType(String type) {
        this.type = type;
    }

    @Generated
    public void setAuthor(String author) {
        this.author = author;
    }

    @Generated
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Generated
    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    @Generated
    public void setIsPinned(Integer isPinned) {
        this.isPinned = isPinned;
    }

    @Generated
    public void setPinnedAt(String pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    @Generated
    public void setShowInCarousel(Integer showInCarousel) {
        this.showInCarousel = showInCarousel;
    }

    @Generated
    public void setCarouselOrder(Integer carouselOrder) {
        this.carouselOrder = carouselOrder;
    }

    @Generated
    public void setStatus(Integer status) {
        this.status = status;
    }

    @Generated
    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    @Generated
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    @Generated
    public void setSubmitIp(String submitIp) {
        this.submitIp = submitIp;
    }

    @Generated
    public void setSubmitIpRegion(String submitIpRegion) {
        this.submitIpRegion = submitIpRegion;
    }

    @Generated
    public String toString() {
        return "SysArticle(tenantId=" + this.getTenantId() + ", title=" + this.getTitle() + ", publishTarget=" + this.getPublishTarget() + ", summary=" + this.getSummary() + ", cover=" + this.getCover() + ", content=" + this.getContent() + ", type=" + this.getType() + ", author=" + this.getAuthor() + ", contentType=" + this.getContentType() + ", attachments=" + this.getAttachments() + ", isPinned=" + this.getIsPinned() + ", pinnedAt=" + this.getPinnedAt() + ", showInCarousel=" + this.getShowInCarousel() + ", carouselOrder=" + this.getCarouselOrder() + ", status=" + this.getStatus() + ", viewCount=" + this.getViewCount() + ", sourceUrl=" + this.getSourceUrl() + ", submitIp=" + this.getSubmitIp() + ", submitIpRegion=" + this.getSubmitIpRegion() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysArticle)) {
            return false;
        }
        SysArticle other = (SysArticle)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        if (this.getViewCount() != other.getViewCount()) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Integer this$isPinned = this.getIsPinned();
        Integer other$isPinned = other.getIsPinned();
        if (this$isPinned == null ? other$isPinned != null : !((Object)this$isPinned).equals(other$isPinned)) {
            return false;
        }
        Integer this$showInCarousel = this.getShowInCarousel();
        Integer other$showInCarousel = other.getShowInCarousel();
        if (this$showInCarousel == null ? other$showInCarousel != null : !((Object)this$showInCarousel).equals(other$showInCarousel)) {
            return false;
        }
        Integer this$carouselOrder = this.getCarouselOrder();
        Integer other$carouselOrder = other.getCarouselOrder();
        if (this$carouselOrder == null ? other$carouselOrder != null : !((Object)this$carouselOrder).equals(other$carouselOrder)) {
            return false;
        }
        Integer this$status = this.getStatus();
        Integer other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)this$status).equals(other$status)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$publishTarget = this.getPublishTarget();
        String other$publishTarget = other.getPublishTarget();
        if (this$publishTarget == null ? other$publishTarget != null : !this$publishTarget.equals(other$publishTarget)) {
            return false;
        }
        String this$summary = this.getSummary();
        String other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) {
            return false;
        }
        String this$cover = this.getCover();
        String other$cover = other.getCover();
        if (this$cover == null ? other$cover != null : !this$cover.equals(other$cover)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$author = this.getAuthor();
        String other$author = other.getAuthor();
        if (this$author == null ? other$author != null : !this$author.equals(other$author)) {
            return false;
        }
        String this$contentType = this.getContentType();
        String other$contentType = other.getContentType();
        if (this$contentType == null ? other$contentType != null : !this$contentType.equals(other$contentType)) {
            return false;
        }
        String this$attachments = this.getAttachments();
        String other$attachments = other.getAttachments();
        if (this$attachments == null ? other$attachments != null : !this$attachments.equals(other$attachments)) {
            return false;
        }
        String this$pinnedAt = this.getPinnedAt();
        String other$pinnedAt = other.getPinnedAt();
        if (this$pinnedAt == null ? other$pinnedAt != null : !this$pinnedAt.equals(other$pinnedAt)) {
            return false;
        }
        String this$sourceUrl = this.getSourceUrl();
        String other$sourceUrl = other.getSourceUrl();
        if (this$sourceUrl == null ? other$sourceUrl != null : !this$sourceUrl.equals(other$sourceUrl)) {
            return false;
        }
        String this$submitIp = this.getSubmitIp();
        String other$submitIp = other.getSubmitIp();
        if (this$submitIp == null ? other$submitIp != null : !this$submitIp.equals(other$submitIp)) {
            return false;
        }
        String this$submitIpRegion = this.getSubmitIpRegion();
        String other$submitIpRegion = other.getSubmitIpRegion();
        return !(this$submitIpRegion == null ? other$submitIpRegion != null : !this$submitIpRegion.equals(other$submitIpRegion));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysArticle;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        long $viewCount = this.getViewCount();
        result = result * 59 + (int)($viewCount >>> 32 ^ $viewCount);
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Integer $isPinned = this.getIsPinned();
        result = result * 59 + ($isPinned == null ? 43 : ((Object)$isPinned).hashCode());
        Integer $showInCarousel = this.getShowInCarousel();
        result = result * 59 + ($showInCarousel == null ? 43 : ((Object)$showInCarousel).hashCode());
        Integer $carouselOrder = this.getCarouselOrder();
        result = result * 59 + ($carouselOrder == null ? 43 : ((Object)$carouselOrder).hashCode());
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $publishTarget = this.getPublishTarget();
        result = result * 59 + ($publishTarget == null ? 43 : $publishTarget.hashCode());
        String $summary = this.getSummary();
        result = result * 59 + ($summary == null ? 43 : $summary.hashCode());
        String $cover = this.getCover();
        result = result * 59 + ($cover == null ? 43 : $cover.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $author = this.getAuthor();
        result = result * 59 + ($author == null ? 43 : $author.hashCode());
        String $contentType = this.getContentType();
        result = result * 59 + ($contentType == null ? 43 : $contentType.hashCode());
        String $attachments = this.getAttachments();
        result = result * 59 + ($attachments == null ? 43 : $attachments.hashCode());
        String $pinnedAt = this.getPinnedAt();
        result = result * 59 + ($pinnedAt == null ? 43 : $pinnedAt.hashCode());
        String $sourceUrl = this.getSourceUrl();
        result = result * 59 + ($sourceUrl == null ? 43 : $sourceUrl.hashCode());
        String $submitIp = this.getSubmitIp();
        result = result * 59 + ($submitIp == null ? 43 : $submitIp.hashCode());
        String $submitIpRegion = this.getSubmitIpRegion();
        result = result * 59 + ($submitIpRegion == null ? 43 : $submitIpRegion.hashCode());
        return result;
    }
}

