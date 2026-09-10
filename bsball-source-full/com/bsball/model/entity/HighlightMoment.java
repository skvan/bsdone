/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.HighlightMoment
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
@Table(name="bs_highlight_moment")
@Comment(value="\u9ad8\u5149\u65f6\u523b")
public class HighlightMoment
extends BaseEntity {
    @Comment(value="\u4e3b\u4f53\u7c7b\u578b\uff1aleague/team/coach/player")
    private String subjectType = "player";
    @Comment(value="\u4e3b\u4f53ID")
    private Long subjectId;
    @Comment(value="\u5173\u8054\u6cbf\u9769ID")
    private Long historyEventId;
    @Comment(value="\u5c55\u793a\u4f4d\u952e\uff1aplayer_profile/player_career/player_timeline")
    private String displayKey = "player_profile";
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u6807\u9898")
    @Size(max=200, message="\u6807\u9898\u4e0d\u80fd\u8d85\u8fc7200\u5b57")
    private @Size(max=200, message="\u6807\u9898\u4e0d\u80fd\u8d85\u8fc7200\u5b57") String title;
    @Column(length=2000)
    @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u63cf\u8ff0")
    private @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String description;
    @Comment(value="\u7d20\u6750\u7c7b\u578b\uff1aimage/video")
    private String mediaType = "image";
    @Comment(value="\u7d20\u6750\u6765\u6e90\uff1aupload/external")
    private String mediaSource = "upload";
    @Comment(value="\u7d20\u6750\u5730\u5740")
    @Size(max=2000, message="\u7d20\u6750\u5730\u5740\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    private @Size(max=2000, message="\u7d20\u6750\u5730\u5740\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String mediaUrl;
    @Comment(value="\u5c01\u9762\u5730\u5740")
    @Size(max=2000, message="\u5c01\u9762\u5730\u5740\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    private @Size(max=2000, message="\u5c01\u9762\u5730\u5740\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String coverUrl;
    @Comment(value="\u53d1\u751f\u65f6\u95f4")
    private String happenedAt;
    @Comment(value="\u6392\u5e8f\u6743\u91cd")
    private Integer sortWeight = 0;
    @Comment(value="\u662f\u5426\u7f6e\u9876")
    private Integer pinned = 0;
    @Comment(value="\u72b6\u6001\uff1adraft/published/rejected")
    private String status = "published";

    @Generated
    public HighlightMoment() {
    }

    @Generated
    public String getSubjectType() {
        return this.subjectType;
    }

    @Generated
    public Long getSubjectId() {
        return this.subjectId;
    }

    @Generated
    public Long getHistoryEventId() {
        return this.historyEventId;
    }

    @Generated
    public String getDisplayKey() {
        return this.displayKey;
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
    public String getDescription() {
        return this.description;
    }

    @Generated
    public String getMediaType() {
        return this.mediaType;
    }

    @Generated
    public String getMediaSource() {
        return this.mediaSource;
    }

    @Generated
    public String getMediaUrl() {
        return this.mediaUrl;
    }

    @Generated
    public String getCoverUrl() {
        return this.coverUrl;
    }

    @Generated
    public String getHappenedAt() {
        return this.happenedAt;
    }

    @Generated
    public Integer getSortWeight() {
        return this.sortWeight;
    }

    @Generated
    public Integer getPinned() {
        return this.pinned;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    @Generated
    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    @Generated
    public void setHistoryEventId(Long historyEventId) {
        this.historyEventId = historyEventId;
    }

    @Generated
    public void setDisplayKey(String displayKey) {
        this.displayKey = displayKey;
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
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    @Generated
    public void setMediaSource(String mediaSource) {
        this.mediaSource = mediaSource;
    }

    @Generated
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    @Generated
    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    @Generated
    public void setHappenedAt(String happenedAt) {
        this.happenedAt = happenedAt;
    }

    @Generated
    public void setSortWeight(Integer sortWeight) {
        this.sortWeight = sortWeight;
    }

    @Generated
    public void setPinned(Integer pinned) {
        this.pinned = pinned;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public String toString() {
        return "HighlightMoment(subjectType=" + this.getSubjectType() + ", subjectId=" + this.getSubjectId() + ", historyEventId=" + this.getHistoryEventId() + ", displayKey=" + this.getDisplayKey() + ", tenantId=" + this.getTenantId() + ", title=" + this.getTitle() + ", description=" + this.getDescription() + ", mediaType=" + this.getMediaType() + ", mediaSource=" + this.getMediaSource() + ", mediaUrl=" + this.getMediaUrl() + ", coverUrl=" + this.getCoverUrl() + ", happenedAt=" + this.getHappenedAt() + ", sortWeight=" + this.getSortWeight() + ", pinned=" + this.getPinned() + ", status=" + this.getStatus() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof HighlightMoment)) {
            return false;
        }
        HighlightMoment other = (HighlightMoment)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$subjectId = this.getSubjectId();
        Long other$subjectId = other.getSubjectId();
        if (this$subjectId == null ? other$subjectId != null : !((Object)this$subjectId).equals(other$subjectId)) {
            return false;
        }
        Long this$historyEventId = this.getHistoryEventId();
        Long other$historyEventId = other.getHistoryEventId();
        if (this$historyEventId == null ? other$historyEventId != null : !((Object)this$historyEventId).equals(other$historyEventId)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Integer this$sortWeight = this.getSortWeight();
        Integer other$sortWeight = other.getSortWeight();
        if (this$sortWeight == null ? other$sortWeight != null : !((Object)this$sortWeight).equals(other$sortWeight)) {
            return false;
        }
        Integer this$pinned = this.getPinned();
        Integer other$pinned = other.getPinned();
        if (this$pinned == null ? other$pinned != null : !((Object)this$pinned).equals(other$pinned)) {
            return false;
        }
        String this$subjectType = this.getSubjectType();
        String other$subjectType = other.getSubjectType();
        if (this$subjectType == null ? other$subjectType != null : !this$subjectType.equals(other$subjectType)) {
            return false;
        }
        String this$displayKey = this.getDisplayKey();
        String other$displayKey = other.getDisplayKey();
        if (this$displayKey == null ? other$displayKey != null : !this$displayKey.equals(other$displayKey)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$mediaType = this.getMediaType();
        String other$mediaType = other.getMediaType();
        if (this$mediaType == null ? other$mediaType != null : !this$mediaType.equals(other$mediaType)) {
            return false;
        }
        String this$mediaSource = this.getMediaSource();
        String other$mediaSource = other.getMediaSource();
        if (this$mediaSource == null ? other$mediaSource != null : !this$mediaSource.equals(other$mediaSource)) {
            return false;
        }
        String this$mediaUrl = this.getMediaUrl();
        String other$mediaUrl = other.getMediaUrl();
        if (this$mediaUrl == null ? other$mediaUrl != null : !this$mediaUrl.equals(other$mediaUrl)) {
            return false;
        }
        String this$coverUrl = this.getCoverUrl();
        String other$coverUrl = other.getCoverUrl();
        if (this$coverUrl == null ? other$coverUrl != null : !this$coverUrl.equals(other$coverUrl)) {
            return false;
        }
        String this$happenedAt = this.getHappenedAt();
        String other$happenedAt = other.getHappenedAt();
        if (this$happenedAt == null ? other$happenedAt != null : !this$happenedAt.equals(other$happenedAt)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        return !(this$status == null ? other$status != null : !this$status.equals(other$status));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof HighlightMoment;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $subjectId = this.getSubjectId();
        result = result * 59 + ($subjectId == null ? 43 : ((Object)$subjectId).hashCode());
        Long $historyEventId = this.getHistoryEventId();
        result = result * 59 + ($historyEventId == null ? 43 : ((Object)$historyEventId).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Integer $sortWeight = this.getSortWeight();
        result = result * 59 + ($sortWeight == null ? 43 : ((Object)$sortWeight).hashCode());
        Integer $pinned = this.getPinned();
        result = result * 59 + ($pinned == null ? 43 : ((Object)$pinned).hashCode());
        String $subjectType = this.getSubjectType();
        result = result * 59 + ($subjectType == null ? 43 : $subjectType.hashCode());
        String $displayKey = this.getDisplayKey();
        result = result * 59 + ($displayKey == null ? 43 : $displayKey.hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $mediaType = this.getMediaType();
        result = result * 59 + ($mediaType == null ? 43 : $mediaType.hashCode());
        String $mediaSource = this.getMediaSource();
        result = result * 59 + ($mediaSource == null ? 43 : $mediaSource.hashCode());
        String $mediaUrl = this.getMediaUrl();
        result = result * 59 + ($mediaUrl == null ? 43 : $mediaUrl.hashCode());
        String $coverUrl = this.getCoverUrl();
        result = result * 59 + ($coverUrl == null ? 43 : $coverUrl.hashCode());
        String $happenedAt = this.getHappenedAt();
        result = result * 59 + ($happenedAt == null ? 43 : $happenedAt.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        return result;
    }
}

