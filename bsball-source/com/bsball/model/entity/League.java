/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.BoolToIntDeserializer
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.League
 *  com.fasterxml.jackson.databind.annotation.JsonDeserialize
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
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
import jakarta.validation.constraints.Size;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_league")
@Comment(value="\u8054\u76df")
public class League
extends BaseEntity {
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u8054\u76df\u540d\u79f0")
    private String name;
    @Comment(value="\u82f1\u6587\u540d")
    private String nameEn;
    @Comment(value="Logo")
    private String logo;
    @Column(length=2000)
    @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u63cf\u8ff0")
    private @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String description;
    @JsonDeserialize(using=BoolToIntDeserializer.class)
    @Comment(value="\u5e73\u53f0\u8ba4\u8bc1\uff1a1=\u5df2\u8ba4\u8bc1\u8054\u76df\uff08\u95e8\u6237\u8054\u76df\u540d\u663e\u793a\u84dd\u8272\u5bf9\u52fe\uff09")
    private Integer verified = 0;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;

    @Generated
    public League() {
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
    public String getNameEn() {
        return this.nameEn;
    }

    @Generated
    public String getLogo() {
        return this.logo;
    }

    @Generated
    public String getDescription() {
        return this.description;
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
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    @Generated
    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
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
        return "League(tenantId=" + this.getTenantId() + ", name=" + this.getName() + ", nameEn=" + this.getNameEn() + ", logo=" + this.getLogo() + ", description=" + this.getDescription() + ", verified=" + this.getVerified() + ", sort=" + this.getSort() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof League)) {
            return false;
        }
        League other = (League)o;
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
        String this$logo = this.getLogo();
        String other$logo = other.getLogo();
        if (this$logo == null ? other$logo != null : !this$logo.equals(other$logo)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        return !(this$description == null ? other$description != null : !this$description.equals(other$description));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof League;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Integer $verified = this.getVerified();
        result = result * 59 + ($verified == null ? 43 : ((Object)$verified).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $nameEn = this.getNameEn();
        result = result * 59 + ($nameEn == null ? 43 : $nameEn.hashCode());
        String $logo = this.getLogo();
        result = result * 59 + ($logo == null ? 43 : $logo.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        return result;
    }
}

