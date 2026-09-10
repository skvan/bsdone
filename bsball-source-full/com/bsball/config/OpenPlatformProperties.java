/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.config.OpenPlatformProperties$Common
 *  com.bsball.config.OpenPlatformProperties$Lbs
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 */
package com.bsball.config;

import com.bsball.config.OpenPlatformProperties;
import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.open-platform")
public class OpenPlatformProperties {
    private Common common = new Common();
    private Lbs lbs = new Lbs();

    public static int effectiveMinIntervalMs(int maxQps, int extraSpacingMs) {
        int q = Math.max(1, maxQps);
        int base = (int)Math.ceil(1000.0 / (double)q);
        return base + Math.max(0, extraSpacingMs);
    }

    public boolean anyProviderConfigured() {
        return this.lbs.getProviders().values().stream().anyMatch(p -> p.isEnabled() && p.getKey() != null && !p.getKey().isBlank());
    }

    public int maxBatchIps() {
        return Math.max(1, this.common.getMaxBatchIps());
    }

    public int regionStaleDays() {
        return this.common.getRegionStaleDays();
    }

    public int maxAttempts() {
        return Math.max(1, this.lbs.getMaxAttempts());
    }

    @Generated
    public OpenPlatformProperties() {
    }

    @Generated
    public Common getCommon() {
        return this.common;
    }

    @Generated
    public Lbs getLbs() {
        return this.lbs;
    }

    @Generated
    public void setCommon(Common common) {
        this.common = common;
    }

    @Generated
    public void setLbs(Lbs lbs) {
        this.lbs = lbs;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof OpenPlatformProperties)) {
            return false;
        }
        OpenPlatformProperties other = (OpenPlatformProperties)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Common this$common = this.getCommon();
        Common other$common = other.getCommon();
        if (this$common == null ? other$common != null : !this$common.equals(other$common)) {
            return false;
        }
        Lbs this$lbs = this.getLbs();
        Lbs other$lbs = other.getLbs();
        return !(this$lbs == null ? other$lbs != null : !this$lbs.equals(other$lbs));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof OpenPlatformProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Common $common = this.getCommon();
        result = result * 59 + ($common == null ? 43 : $common.hashCode());
        Lbs $lbs = this.getLbs();
        result = result * 59 + ($lbs == null ? 43 : $lbs.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "OpenPlatformProperties(common=" + String.valueOf(this.getCommon()) + ", lbs=" + String.valueOf(this.getLbs()) + ")";
    }
}

