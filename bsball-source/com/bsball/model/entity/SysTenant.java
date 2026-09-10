/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysTenant
 *  com.fasterxml.jackson.annotation.JsonFormat
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.validation.constraints.NotBlank
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_tenant")
@Comment(value="\u79df\u6237")
public class SysTenant
extends BaseEntity {
    @NotBlank
    @Size(max=200)
    @Comment(value="\u540d\u79f0")
    private @NotBlank @Size(max=200) String name;
    @NotBlank
    @Size(max=64)
    @Comment(value="\u7f16\u7801\uff08\u552f\u4e00\uff09")
    private @NotBlank @Size(max=64) String code;
    @Comment(value="\u72b6\u6001\uff1a1 \u542f\u7528 0 \u505c\u7528")
    private Integer status = 1;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;
    @Size(max=2000)
    @Comment(value="\u8bf4\u660e/\u5907\u6ce8")
    private @Size(max=2000) String description;
    @Column(name="lease_start_date")
    @Comment(value="\u79df\u8d41\u5f00\u59cb\u65e5\u671f")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate leaseStartDate;
    @Column(name="lease_end_date")
    @Comment(value="\u79df\u8d41\u7ed3\u675f\u65e5\u671f")
    @JsonFormat(pattern="yyyy-MM-dd")
    private LocalDate leaseEndDate;

    public boolean isActive() {
        return this.status != null && this.status == 1;
    }

    @Generated
    public SysTenant() {
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getCode() {
        return this.code;
    }

    @Generated
    public Integer getStatus() {
        return this.status;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public LocalDate getLeaseStartDate() {
        return this.leaseStartDate;
    }

    @Generated
    public LocalDate getLeaseEndDate() {
        return this.leaseEndDate;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setCode(String code) {
        this.code = code;
    }

    @Generated
    public void setStatus(Integer status) {
        this.status = status;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setLeaseStartDate(LocalDate leaseStartDate) {
        this.leaseStartDate = leaseStartDate;
    }

    @Generated
    public void setLeaseEndDate(LocalDate leaseEndDate) {
        this.leaseEndDate = leaseEndDate;
    }

    @Generated
    public String toString() {
        return "SysTenant(name=" + this.getName() + ", code=" + this.getCode() + ", status=" + this.getStatus() + ", sort=" + this.getSort() + ", description=" + this.getDescription() + ", leaseStartDate=" + String.valueOf(this.getLeaseStartDate()) + ", leaseEndDate=" + String.valueOf(this.getLeaseEndDate()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysTenant)) {
            return false;
        }
        SysTenant other = (SysTenant)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Integer this$status = this.getStatus();
        Integer other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)this$status).equals(other$status)) {
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
        String this$code = this.getCode();
        String other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        LocalDate this$leaseStartDate = this.getLeaseStartDate();
        LocalDate other$leaseStartDate = other.getLeaseStartDate();
        if (this$leaseStartDate == null ? other$leaseStartDate != null : !((Object)this$leaseStartDate).equals(other$leaseStartDate)) {
            return false;
        }
        LocalDate this$leaseEndDate = this.getLeaseEndDate();
        LocalDate other$leaseEndDate = other.getLeaseEndDate();
        return !(this$leaseEndDate == null ? other$leaseEndDate != null : !((Object)this$leaseEndDate).equals(other$leaseEndDate));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysTenant;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        LocalDate $leaseStartDate = this.getLeaseStartDate();
        result = result * 59 + ($leaseStartDate == null ? 43 : ((Object)$leaseStartDate).hashCode());
        LocalDate $leaseEndDate = this.getLeaseEndDate();
        result = result * 59 + ($leaseEndDate == null ? 43 : ((Object)$leaseEndDate).hashCode());
        return result;
    }
}

