/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysConfigId
 *  lombok.Generated
 */
package com.bsball.model.entity;

import java.io.Serializable;
import lombok.Generated;

public class SysConfigId
implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long tenantId;
    private String configKey;

    @Generated
    public SysConfigId() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public String getConfigKey() {
        return this.configKey;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysConfigId)) {
            return false;
        }
        SysConfigId other = (SysConfigId)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        String this$configKey = this.getConfigKey();
        String other$configKey = other.getConfigKey();
        return !(this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysConfigId;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        String $configKey = this.getConfigKey();
        result = result * 59 + ($configKey == null ? 43 : $configKey.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SysConfigId(tenantId=" + this.getTenantId() + ", configKey=" + this.getConfigKey() + ")";
    }
}

