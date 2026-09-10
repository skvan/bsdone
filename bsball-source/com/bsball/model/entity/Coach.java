/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Coach
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
@Table(name="bs_coach")
@Comment(value="\u6559\u7ec3")
public class Coach
extends BaseEntity {
    @Comment(value="\u7403\u961fID")
    private Long teamId;
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u59d3\u540d")
    private String name;
    @Comment(value="\u804c\u4f4d")
    private String position = "coach";
    @Comment(value="\u7167\u7247")
    private String photo;
    @Comment(value="\u8054\u7cfb\u7535\u8bdd")
    private String contactPhone;
    @Comment(value="\u8054\u7cfb\u90ae\u7bb1")
    private String contactEmail;
    @Comment(value="\u5f53\u524d\u52a0\u5165\u8bb0\u5f55ID")
    private Long currentJoinRecordId;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;

    @Generated
    public Coach() {
    }

    @Generated
    public Long getTeamId() {
        return this.teamId;
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
    public String getPosition() {
        return this.position;
    }

    @Generated
    public String getPhoto() {
        return this.photo;
    }

    @Generated
    public String getContactPhone() {
        return this.contactPhone;
    }

    @Generated
    public String getContactEmail() {
        return this.contactEmail;
    }

    @Generated
    public Long getCurrentJoinRecordId() {
        return this.currentJoinRecordId;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
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
    public void setPosition(String position) {
        this.position = position;
    }

    @Generated
    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @Generated
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    @Generated
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Generated
    public void setCurrentJoinRecordId(Long currentJoinRecordId) {
        this.currentJoinRecordId = currentJoinRecordId;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public String toString() {
        return "Coach(teamId=" + this.getTeamId() + ", tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", position=" + this.getPosition() + ", photo=" + this.getPhoto() + ", contactPhone=" + this.getContactPhone() + ", contactEmail=" + this.getContactEmail() + ", currentJoinRecordId=" + this.getCurrentJoinRecordId() + ", sort=" + this.getSort() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Coach)) {
            return false;
        }
        Coach other = (Coach)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$teamId = this.getTeamId();
        Long other$teamId = other.getTeamId();
        if (this$teamId == null ? other$teamId != null : !((Object)this$teamId).equals(other$teamId)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Long this$currentJoinRecordId = this.getCurrentJoinRecordId();
        Long other$currentJoinRecordId = other.getCurrentJoinRecordId();
        if (this$currentJoinRecordId == null ? other$currentJoinRecordId != null : !((Object)this$currentJoinRecordId).equals(other$currentJoinRecordId)) {
            return false;
        }
        Integer this$sort = this.getSort();
        Integer other$sort = other.getSort();
        if (this$sort == null ? other$sort != null : !((Object)this$sort).equals(other$sort)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$position = this.getPosition();
        String other$position = other.getPosition();
        if (this$position == null ? other$position != null : !this$position.equals(other$position)) {
            return false;
        }
        String this$photo = this.getPhoto();
        String other$photo = other.getPhoto();
        if (this$photo == null ? other$photo != null : !this$photo.equals(other$photo)) {
            return false;
        }
        String this$contactPhone = this.getContactPhone();
        String other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) {
            return false;
        }
        String this$contactEmail = this.getContactEmail();
        String other$contactEmail = other.getContactEmail();
        return !(this$contactEmail == null ? other$contactEmail != null : !this$contactEmail.equals(other$contactEmail));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Coach;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $teamId = this.getTeamId();
        result = result * 59 + ($teamId == null ? 43 : ((Object)$teamId).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $currentJoinRecordId = this.getCurrentJoinRecordId();
        result = result * 59 + ($currentJoinRecordId == null ? 43 : ((Object)$currentJoinRecordId).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $position = this.getPosition();
        result = result * 59 + ($position == null ? 43 : $position.hashCode());
        String $photo = this.getPhoto();
        result = result * 59 + ($photo == null ? 43 : $photo.hashCode());
        String $contactPhone = this.getContactPhone();
        result = result * 59 + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        String $contactEmail = this.getContactEmail();
        result = result * 59 + ($contactEmail == null ? 43 : $contactEmail.hashCode());
        return result;
    }
}

