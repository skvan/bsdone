/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.IpAccessPolicyVo
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class IpAccessPolicyVo {
    private String mode;
    private String bypassPaths;
    private boolean trustXForwardedFor;
    private boolean ipAccessFilterEnabled;

    @Generated
    public IpAccessPolicyVo() {
    }

    @Generated
    public String getMode() {
        return this.mode;
    }

    @Generated
    public String getBypassPaths() {
        return this.bypassPaths;
    }

    @Generated
    public boolean isTrustXForwardedFor() {
        return this.trustXForwardedFor;
    }

    @Generated
    public boolean isIpAccessFilterEnabled() {
        return this.ipAccessFilterEnabled;
    }

    @Generated
    public void setMode(String mode) {
        this.mode = mode;
    }

    @Generated
    public void setBypassPaths(String bypassPaths) {
        this.bypassPaths = bypassPaths;
    }

    @Generated
    public void setTrustXForwardedFor(boolean trustXForwardedFor) {
        this.trustXForwardedFor = trustXForwardedFor;
    }

    @Generated
    public void setIpAccessFilterEnabled(boolean ipAccessFilterEnabled) {
        this.ipAccessFilterEnabled = ipAccessFilterEnabled;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IpAccessPolicyVo)) {
            return false;
        }
        IpAccessPolicyVo other = (IpAccessPolicyVo)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.isTrustXForwardedFor() != other.isTrustXForwardedFor()) {
            return false;
        }
        if (this.isIpAccessFilterEnabled() != other.isIpAccessFilterEnabled()) {
            return false;
        }
        String this$mode = this.getMode();
        String other$mode = other.getMode();
        if (this$mode == null ? other$mode != null : !this$mode.equals(other$mode)) {
            return false;
        }
        String this$bypassPaths = this.getBypassPaths();
        String other$bypassPaths = other.getBypassPaths();
        return !(this$bypassPaths == null ? other$bypassPaths != null : !this$bypassPaths.equals(other$bypassPaths));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof IpAccessPolicyVo;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isTrustXForwardedFor() ? 79 : 97);
        result = result * 59 + (this.isIpAccessFilterEnabled() ? 79 : 97);
        String $mode = this.getMode();
        result = result * 59 + ($mode == null ? 43 : $mode.hashCode());
        String $bypassPaths = this.getBypassPaths();
        result = result * 59 + ($bypassPaths == null ? 43 : $bypassPaths.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "IpAccessPolicyVo(mode=" + this.getMode() + ", bypassPaths=" + this.getBypassPaths() + ", trustXForwardedFor=" + this.isTrustXForwardedFor() + ", ipAccessFilterEnabled=" + this.isIpAccessFilterEnabled() + ")";
    }
}

