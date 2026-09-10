/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysConfig
 *  com.bsball.model.entity.SysConfigId
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Id
 *  jakarta.persistence.IdClass
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.SysConfigId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_config")
@IdClass(value=SysConfigId.class)
@Comment(value="\u7cfb\u7edf\u914d\u7f6e")
public class SysConfig {
    @Id
    @Comment(value="\u79df\u6237 ID")
    private Long tenantId;
    @Id
    @Column(length=100)
    @Comment(value="\u914d\u7f6e\u952e")
    private String configKey;
    @Column(columnDefinition="TEXT")
    @Comment(value="\u914d\u7f6e\u503c")
    private String configValue;

    @Generated
    public SysConfig() {
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
    public String getConfigValue() {
        return this.configValue;
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
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    @Generated
    public String toString() {
        return "SysConfig(tenantId=" + this.getTenantId() + ", configKey=" + this.getConfigKey() + ", configValue=" + this.getConfigValue() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysConfig)) {
            return false;
        }
        SysConfig other = (SysConfig)o;
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
        if (this$configKey == null ? other$configKey != null : !this$configKey.equals(other$configKey)) {
            return false;
        }
        String this$configValue = this.getConfigValue();
        String other$configValue = other.getConfigValue();
        return !(this$configValue == null ? other$configValue != null : !this$configValue.equals(other$configValue));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysConfig;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        String $configKey = this.getConfigKey();
        result = result * 59 + ($configKey == null ? 43 : $configKey.hashCode());
        String $configValue = this.getConfigValue();
        result = result * 59 + ($configValue == null ? 43 : $configValue.hashCode());
        return result;
    }
}

