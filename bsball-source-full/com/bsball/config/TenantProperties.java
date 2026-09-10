/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.config.TenantProperties$HostMapping
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 */
package com.bsball.config;

import com.bsball.config.TenantProperties;
import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.tenant")
public class TenantProperties {
    private long defaultId = 1L;
    private boolean strictDataScope = false;
    private HostMapping hostMapping = new HostMapping();

    @Generated
    public TenantProperties() {
    }

    @Generated
    public long getDefaultId() {
        return this.defaultId;
    }

    @Generated
    public boolean isStrictDataScope() {
        return this.strictDataScope;
    }

    @Generated
    public HostMapping getHostMapping() {
        return this.hostMapping;
    }

    @Generated
    public void setDefaultId(long defaultId) {
        this.defaultId = defaultId;
    }

    @Generated
    public void setStrictDataScope(boolean strictDataScope) {
        this.strictDataScope = strictDataScope;
    }

    @Generated
    public void setHostMapping(HostMapping hostMapping) {
        this.hostMapping = hostMapping;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof TenantProperties)) {
            return false;
        }
        TenantProperties other = (TenantProperties)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getDefaultId() != other.getDefaultId()) {
            return false;
        }
        if (this.isStrictDataScope() != other.isStrictDataScope()) {
            return false;
        }
        HostMapping this$hostMapping = this.getHostMapping();
        HostMapping other$hostMapping = other.getHostMapping();
        return !(this$hostMapping == null ? other$hostMapping != null : !this$hostMapping.equals(other$hostMapping));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof TenantProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $defaultId = this.getDefaultId();
        result = result * 59 + (int)($defaultId >>> 32 ^ $defaultId);
        result = result * 59 + (this.isStrictDataScope() ? 79 : 97);
        HostMapping $hostMapping = this.getHostMapping();
        result = result * 59 + ($hostMapping == null ? 43 : $hostMapping.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "TenantProperties(defaultId=" + this.getDefaultId() + ", strictDataScope=" + this.isStrictDataScope() + ", hostMapping=" + String.valueOf(this.getHostMapping()) + ")";
    }
}

