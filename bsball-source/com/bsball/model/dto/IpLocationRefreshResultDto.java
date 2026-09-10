/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.IpLocationRefreshResultDto
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class IpLocationRefreshResultDto {
    private String regionText;

    @Generated
    public String getRegionText() {
        return this.regionText;
    }

    @Generated
    public void setRegionText(String regionText) {
        this.regionText = regionText;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IpLocationRefreshResultDto)) {
            return false;
        }
        IpLocationRefreshResultDto other = (IpLocationRefreshResultDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$regionText = this.getRegionText();
        String other$regionText = other.getRegionText();
        return !(this$regionText == null ? other$regionText != null : !this$regionText.equals(other$regionText));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof IpLocationRefreshResultDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $regionText = this.getRegionText();
        result = result * 59 + ($regionText == null ? 43 : $regionText.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "IpLocationRefreshResultDto(regionText=" + this.getRegionText() + ")";
    }

    @Generated
    public IpLocationRefreshResultDto() {
    }

    @Generated
    public IpLocationRefreshResultDto(String regionText) {
        this.regionText = regionText;
    }
}

