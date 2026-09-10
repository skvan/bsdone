/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysDictData
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
@Table(name="sys_dict_data")
@Comment(value="\u5b57\u5178\u6570\u636e")
public class SysDictData
extends BaseEntity {
    @Comment(value="\u5b57\u5178\u7c7b\u578bID")
    private Long dictTypeId;
    @Comment(value="\u6807\u7b7e")
    private String label;
    @Comment(value="\u503c")
    private String value;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;
    @Comment(value="\u72b6\u6001")
    private Integer status = 1;
    @Column(length=2000)
    @Size(max=2000, message="\u5907\u6ce8\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u5907\u6ce8")
    private @Size(max=2000, message="\u5907\u6ce8\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String remark;

    @Generated
    public SysDictData() {
    }

    @Generated
    public Long getDictTypeId() {
        return this.dictTypeId;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public String getValue() {
        return this.value;
    }

    @Generated
    public Integer getSort() {
        return this.sort;
    }

    @Generated
    public Integer getStatus() {
        return this.status;
    }

    @Generated
    public String getRemark() {
        return this.remark;
    }

    @Generated
    public void setDictTypeId(Long dictTypeId) {
        this.dictTypeId = dictTypeId;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setValue(String value) {
        this.value = value;
    }

    @Generated
    public void setSort(Integer sort) {
        this.sort = sort;
    }

    @Generated
    public void setStatus(Integer status) {
        this.status = status;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public String toString() {
        return "SysDictData(dictTypeId=" + this.getDictTypeId() + ", label=" + this.getLabel() + ", value=" + this.getValue() + ", sort=" + this.getSort() + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysDictData)) {
            return false;
        }
        SysDictData other = (SysDictData)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$dictTypeId = this.getDictTypeId();
        Long other$dictTypeId = other.getDictTypeId();
        if (this$dictTypeId == null ? other$dictTypeId != null : !((Object)this$dictTypeId).equals(other$dictTypeId)) {
            return false;
        }
        Integer this$sort = this.getSort();
        Integer other$sort = other.getSort();
        if (this$sort == null ? other$sort != null : !((Object)this$sort).equals(other$sort)) {
            return false;
        }
        Integer this$status = this.getStatus();
        Integer other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)this$status).equals(other$status)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) {
            return false;
        }
        String this$value = this.getValue();
        String other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        return !(this$remark == null ? other$remark != null : !this$remark.equals(other$remark));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysDictData;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $dictTypeId = this.getDictTypeId();
        result = result * 59 + ($dictTypeId == null ? 43 : ((Object)$dictTypeId).hashCode());
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        String $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : $value.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }
}

