/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.BoolToIntDeserializer
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Team
 *  com.fasterxml.jackson.databind.annotation.JsonDeserialize
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.common.BoolToIntDeserializer;
import com.bsball.model.entity.BaseEntity;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_team")
@Comment(value="\u7403\u961f")
public class Team
extends BaseEntity {
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u8054\u76dfID")
    private Long leagueId;
    @NotBlank(message="\u7403\u961f\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max=100)
    @Comment(value="\u7403\u961f\u540d\u79f0")
    private @NotBlank(message="\u7403\u961f\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a") @Size(max=100) String name;
    @Size(max=100)
    @Comment(value="\u82f1\u6587\u540d")
    private @Size(max=100) String nameEn;
    @Size(max=50)
    @Comment(value="\u7b80\u79f0")
    private @Size(max=50) String shortName;
    @Size(max=500)
    @Comment(value="\u961f\u5fbd\uff08\u56fe\u50cf URL\uff09")
    private @Size(max=500) String logo;
    @Size(max=500)
    @Comment(value="\u6587\u5b57\u6807\u8bc6\u56fe\uff08\u56fe\u7247 URL\uff0c\u7b80\u5316\u7248\u961f\u5fbd\uff09")
    private @Size(max=500) String wordmark;
    @Size(max=500)
    @Comment(value="\u80cc\u666f\u56fe")
    private @Size(max=500) String bgImage;
    @Size(max=100)
    @Comment(value="\u57ce\u5e02")
    private @Size(max=100) String city;
    @Size(max=100)
    @Comment(value="\u4e3b\u573a")
    private @Size(max=100) String stadium;
    @Column(columnDefinition="TEXT")
    @Size(max=2000)
    @Comment(value="\u63cf\u8ff0")
    private @Size(max=2000) String description;
    @Size(max=50)
    @Comment(value="\u8054\u7cfb\u7535\u8bdd")
    private @Size(max=50) String contactPhone;
    @Size(max=100)
    @Comment(value="\u8054\u7cfb\u90ae\u7bb1")
    private @Size(max=100) String contactEmail;
    @Size(max=50)
    @Comment(value="\u8054\u7cfb\u4eba")
    private @Size(max=50) String contactPerson;
    @JsonDeserialize(using=BoolToIntDeserializer.class)
    @Comment(value="\u95e8\u6237\u5c55\u793a")
    private Integer showInPortal = 1;
    @JsonDeserialize(using=BoolToIntDeserializer.class)
    @Comment(value="\u5e73\u53f0\u8ba4\u8bc1\uff1a1=\u5df2\u8ba4\u8bc1\u7403\u961f\uff08\u95e8\u6237\u961f\u540d\u663e\u793a\u7eff\u8272\u5bf9\u52fe\uff09")
    private Integer verified = 0;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;

    @Generated
    public Team() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getLeagueId() {
        return this.leagueId;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getNameEn() {
        return this.nameEn;
    }

    @Generated
    public String getShortName() {
        return this.shortName;
    }

    @Generated
    public String getLogo() {
        return this.logo;
    }

    @Generated
    public String getWordmark() {
        return this.wordmark;
    }

    @Generated
    public String getBgImage() {
        return this.bgImage;
    }

    @Generated
    public String getCity() {
        return this.city;
    }

    @Generated
    public String getStadium() {
        return this.stadium;
    }

    @Generated
    public String getDescription() {
        return this.description;
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
    public String getContactPerson() {
        return this.contactPerson;
    }

    @Generated
    public Integer getShowInPortal() {
        return this.showInPortal;
    }

    @Generated
    public Integer getVerified() {
        return this.verified;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    @Generated
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @Generated
    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Generated
    public void setWordmark(String wordmark) {
        this.wordmark = wordmark;
    }

    @Generated
    public void setBgImage(String bgImage) {
        this.bgImage = bgImage;
    }

    @Generated
    public void setCity(String city) {
        this.city = city;
    }

    @Generated
    public void setStadium(String stadium) {
        this.stadium = stadium;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
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
    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    @Generated
    public void setShowInPortal(Integer showInPortal) {
        this.showInPortal = showInPortal;
    }

    @Generated
    public void setVerified(Integer verified) {
        this.verified = verified;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public String toString() {
        return "Team(tenantId=" + this.getTenantId() + ", leagueId=" + this.getLeagueId() + ", name=" + this.getName() + ", nameEn=" + this.getNameEn() + ", shortName=" + this.getShortName() + ", logo=" + this.getLogo() + ", wordmark=" + this.getWordmark() + ", bgImage=" + this.getBgImage() + ", city=" + this.getCity() + ", stadium=" + this.getStadium() + ", description=" + this.getDescription() + ", contactPhone=" + this.getContactPhone() + ", contactEmail=" + this.getContactEmail() + ", contactPerson=" + this.getContactPerson() + ", showInPortal=" + this.getShowInPortal() + ", verified=" + this.getVerified() + ", sort=" + this.getSort() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Team)) {
            return false;
        }
        Team other = (Team)o;
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
        Long this$leagueId = this.getLeagueId();
        Long other$leagueId = other.getLeagueId();
        if (this$leagueId == null ? other$leagueId != null : !((Object)this$leagueId).equals(other$leagueId)) {
            return false;
        }
        Integer this$showInPortal = this.getShowInPortal();
        Integer other$showInPortal = other.getShowInPortal();
        if (this$showInPortal == null ? other$showInPortal != null : !((Object)this$showInPortal).equals(other$showInPortal)) {
            return false;
        }
        Integer this$verified = this.getVerified();
        Integer other$verified = other.getVerified();
        if (this$verified == null ? other$verified != null : !((Object)this$verified).equals(other$verified)) {
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
        String this$nameEn = this.getNameEn();
        String other$nameEn = other.getNameEn();
        if (this$nameEn == null ? other$nameEn != null : !this$nameEn.equals(other$nameEn)) {
            return false;
        }
        String this$shortName = this.getShortName();
        String other$shortName = other.getShortName();
        if (this$shortName == null ? other$shortName != null : !this$shortName.equals(other$shortName)) {
            return false;
        }
        String this$logo = this.getLogo();
        String other$logo = other.getLogo();
        if (this$logo == null ? other$logo != null : !this$logo.equals(other$logo)) {
            return false;
        }
        String this$wordmark = this.getWordmark();
        String other$wordmark = other.getWordmark();
        if (this$wordmark == null ? other$wordmark != null : !this$wordmark.equals(other$wordmark)) {
            return false;
        }
        String this$bgImage = this.getBgImage();
        String other$bgImage = other.getBgImage();
        if (this$bgImage == null ? other$bgImage != null : !this$bgImage.equals(other$bgImage)) {
            return false;
        }
        String this$city = this.getCity();
        String other$city = other.getCity();
        if (this$city == null ? other$city != null : !this$city.equals(other$city)) {
            return false;
        }
        String this$stadium = this.getStadium();
        String other$stadium = other.getStadium();
        if (this$stadium == null ? other$stadium != null : !this$stadium.equals(other$stadium)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$contactPhone = this.getContactPhone();
        String other$contactPhone = other.getContactPhone();
        if (this$contactPhone == null ? other$contactPhone != null : !this$contactPhone.equals(other$contactPhone)) {
            return false;
        }
        String this$contactEmail = this.getContactEmail();
        String other$contactEmail = other.getContactEmail();
        if (this$contactEmail == null ? other$contactEmail != null : !this$contactEmail.equals(other$contactEmail)) {
            return false;
        }
        String this$contactPerson = this.getContactPerson();
        String other$contactPerson = other.getContactPerson();
        return !(this$contactPerson == null ? other$contactPerson != null : !this$contactPerson.equals(other$contactPerson));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof Team;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $leagueId = this.getLeagueId();
        result = result * 59 + ($leagueId == null ? 43 : ((Object)$leagueId).hashCode());
        Integer $showInPortal = this.getShowInPortal();
        result = result * 59 + ($showInPortal == null ? 43 : ((Object)$showInPortal).hashCode());
        Integer $verified = this.getVerified();
        result = result * 59 + ($verified == null ? 43 : ((Object)$verified).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $nameEn = this.getNameEn();
        result = result * 59 + ($nameEn == null ? 43 : $nameEn.hashCode());
        String $shortName = this.getShortName();
        result = result * 59 + ($shortName == null ? 43 : $shortName.hashCode());
        String $logo = this.getLogo();
        result = result * 59 + ($logo == null ? 43 : $logo.hashCode());
        String $wordmark = this.getWordmark();
        result = result * 59 + ($wordmark == null ? 43 : $wordmark.hashCode());
        String $bgImage = this.getBgImage();
        result = result * 59 + ($bgImage == null ? 43 : $bgImage.hashCode());
        String $city = this.getCity();
        result = result * 59 + ($city == null ? 43 : $city.hashCode());
        String $stadium = this.getStadium();
        result = result * 59 + ($stadium == null ? 43 : $stadium.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $contactPhone = this.getContactPhone();
        result = result * 59 + ($contactPhone == null ? 43 : $contactPhone.hashCode());
        String $contactEmail = this.getContactEmail();
        result = result * 59 + ($contactEmail == null ? 43 : $contactEmail.hashCode());
        String $contactPerson = this.getContactPerson();
        result = result * 59 + ($contactPerson == null ? 43 : $contactPerson.hashCode());
        return result;
    }
}

