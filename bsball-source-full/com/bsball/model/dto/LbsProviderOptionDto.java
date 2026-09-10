/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.LbsProviderOptionDto
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class LbsProviderOptionDto {
    private String code;
    private String label;
    private boolean configured;

    @Generated
    public String getCode() {
        return this.code;
    }

    @Generated
    public String getLabel() {
        return this.label;
    }

    @Generated
    public boolean isConfigured() {
        return this.configured;
    }

    @Generated
    public void setCode(String code) {
        this.code = code;
    }

    @Generated
    public void setLabel(String label) {
        this.label = label;
    }

    @Generated
    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LbsProviderOptionDto)) {
            return false;
        }
        LbsProviderOptionDto other = (LbsProviderOptionDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.isConfigured() != other.isConfigured()) {
            return false;
        }
        String this$code = this.getCode();
        String other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
            return false;
        }
        String this$label = this.getLabel();
        String other$label = other.getLabel();
        return !(this$label == null ? other$label != null : !this$label.equals(other$label));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LbsProviderOptionDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isConfigured() ? 79 : 97);
        String $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        String $label = this.getLabel();
        result = result * 59 + ($label == null ? 43 : $label.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "LbsProviderOptionDto(code=" + this.getCode() + ", label=" + this.getLabel() + ", configured=" + this.isConfigured() + ")";
    }

    @Generated
    public LbsProviderOptionDto() {
    }

    @Generated
    public LbsProviderOptionDto(String code, String label, boolean configured) {
        this.code = code;
        this.label = label;
        this.configured = configured;
    }
}

