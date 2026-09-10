/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysDict
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
@Table(name="sys_dict")
@Comment(value="\u5b57\u5178")
public class SysDict
extends BaseEntity {
    @Comment(value="\u7c7b\u578b")
    private String type;
    @Comment(value="\u6807\u7b7e")
    private String label;
    @Comment(value="\u503c")
    private String value;
    @Comment(value="\u6392\u5e8f")
    private Integer sort = 0;
    @Comment(value="\u72b6\u6001")
    private Integer status = 1;

    @Generated
    public SysDict() {
    }

    @Generated
    public String getType() {
        return this.type;
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
    public void setType(String type) {
        this.type = type;
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
    public String toString() {
        return "SysDict(type=" + this.getType() + ", label=" + this.getLabel() + ", value=" + this.getValue() + ", sort=" + this.getSort() + ", status=" + this.getStatus() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysDict)) {
            return false;
        }
        SysDict other = (SysDict)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
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
        String this$type = this.getType();
        String other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) {
            return false;
        }
        String this$value = this.getValue();
        String other$value = other.getValue();
        return !(this$value == null ? other$value != null : !this$value.equals(other$value));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysDict;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Integer $sort = this.getSort();
        result = result * 59 + ($sort == null ? 43 : ((Object)$sort).hashCode());
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        String $type = this.getType();
        result = result * 59 + ($type == null ? 43 : $type.hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        String $value = this.getValue();
        result = result * 59 + ($value == null ? 43 : $value.hashCode());
        return result;
    }
}

