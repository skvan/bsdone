/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.ChinaRegionItemDto
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class ChinaRegionItemDto {
    private String adcode;
    private String name;
    private int level;

    @Generated
    public String getAdcode() {
        return this.adcode;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public int getLevel() {
        return this.level;
    }

    @Generated
    public void setAdcode(String adcode) {
        this.adcode = adcode;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setLevel(int level) {
        this.level = level;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ChinaRegionItemDto)) {
            return false;
        }
        ChinaRegionItemDto other = (ChinaRegionItemDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getLevel() != other.getLevel()) {
            return false;
        }
        String this$adcode = this.getAdcode();
        String other$adcode = other.getAdcode();
        if (this$adcode == null ? other$adcode != null : !this$adcode.equals(other$adcode)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        return !(this$name == null ? other$name != null : !this$name.equals(other$name));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof ChinaRegionItemDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getLevel();
        String $adcode = this.getAdcode();
        result = result * 59 + ($adcode == null ? 43 : $adcode.hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "ChinaRegionItemDto(adcode=" + this.getAdcode() + ", name=" + this.getName() + ", level=" + this.getLevel() + ")";
    }

    @Generated
    public ChinaRegionItemDto() {
    }

    @Generated
    public ChinaRegionItemDto(String adcode, String name, int level) {
        this.adcode = adcode;
        this.name = name;
        this.level = level;
    }
}

