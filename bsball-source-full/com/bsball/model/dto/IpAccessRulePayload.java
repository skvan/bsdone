/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.IpAccessRulePayload
 *  lombok.Generated
 */
package com.bsball.model.dto;

import lombok.Generated;

public class IpAccessRulePayload {
    private String ruleKind;
    private String scopeType;
    private Long roleId;
    private String apiPattern;
    private String cidrOrIp;
    private Integer priority;
    private Integer enabled;
    private String remark;

    @Generated
    public IpAccessRulePayload() {
    }

    @Generated
    public String getRuleKind() {
        return this.ruleKind;
    }

    @Generated
    public String getScopeType() {
        return this.scopeType;
    }

    @Generated
    public Long getRoleId() {
        return this.roleId;
    }

    @Generated
    public String getApiPattern() {
        return this.apiPattern;
    }

    @Generated
    public String getCidrOrIp() {
        return this.cidrOrIp;
    }

    @Generated
    public Integer getPriority() {
        return this.priority;
    }

    @Generated
    public Integer getEnabled() {
        return this.enabled;
    }

    @Generated
    public String getRemark() {
        return this.remark;
    }

    @Generated
    public void setRuleKind(String ruleKind) {
        this.ruleKind = ruleKind;
    }

    @Generated
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Generated
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Generated
    public void setApiPattern(String apiPattern) {
        this.apiPattern = apiPattern;
    }

    @Generated
    public void setCidrOrIp(String cidrOrIp) {
        this.cidrOrIp = cidrOrIp;
    }

    @Generated
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    @Generated
    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IpAccessRulePayload)) {
            return false;
        }
        IpAccessRulePayload other = (IpAccessRulePayload)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$roleId = this.getRoleId();
        Long other$roleId = other.getRoleId();
        if (this$roleId == null ? other$roleId != null : !((Object)this$roleId).equals(other$roleId)) {
            return false;
        }
        Integer this$priority = this.getPriority();
        Integer other$priority = other.getPriority();
        if (this$priority == null ? other$priority != null : !((Object)this$priority).equals(other$priority)) {
            return false;
        }
        Integer this$enabled = this.getEnabled();
        Integer other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !((Object)this$enabled).equals(other$enabled)) {
            return false;
        }
        String this$ruleKind = this.getRuleKind();
        String other$ruleKind = other.getRuleKind();
        if (this$ruleKind == null ? other$ruleKind != null : !this$ruleKind.equals(other$ruleKind)) {
            return false;
        }
        String this$scopeType = this.getScopeType();
        String other$scopeType = other.getScopeType();
        if (this$scopeType == null ? other$scopeType != null : !this$scopeType.equals(other$scopeType)) {
            return false;
        }
        String this$apiPattern = this.getApiPattern();
        String other$apiPattern = other.getApiPattern();
        if (this$apiPattern == null ? other$apiPattern != null : !this$apiPattern.equals(other$apiPattern)) {
            return false;
        }
        String this$cidrOrIp = this.getCidrOrIp();
        String other$cidrOrIp = other.getCidrOrIp();
        if (this$cidrOrIp == null ? other$cidrOrIp != null : !this$cidrOrIp.equals(other$cidrOrIp)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        return !(this$remark == null ? other$remark != null : !this$remark.equals(other$remark));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof IpAccessRulePayload;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $roleId = this.getRoleId();
        result = result * 59 + ($roleId == null ? 43 : ((Object)$roleId).hashCode());
        Integer $priority = this.getPriority();
        result = result * 59 + ($priority == null ? 43 : ((Object)$priority).hashCode());
        Integer $enabled = this.getEnabled();
        result = result * 59 + ($enabled == null ? 43 : ((Object)$enabled).hashCode());
        String $ruleKind = this.getRuleKind();
        result = result * 59 + ($ruleKind == null ? 43 : $ruleKind.hashCode());
        String $scopeType = this.getScopeType();
        result = result * 59 + ($scopeType == null ? 43 : $scopeType.hashCode());
        String $apiPattern = this.getApiPattern();
        result = result * 59 + ($apiPattern == null ? 43 : $apiPattern.hashCode());
        String $cidrOrIp = this.getCidrOrIp();
        result = result * 59 + ($cidrOrIp == null ? 43 : $cidrOrIp.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "IpAccessRulePayload(ruleKind=" + this.getRuleKind() + ", scopeType=" + this.getScopeType() + ", roleId=" + this.getRoleId() + ", apiPattern=" + this.getApiPattern() + ", cidrOrIp=" + this.getCidrOrIp() + ", priority=" + this.getPriority() + ", enabled=" + this.getEnabled() + ", remark=" + this.getRemark() + ")";
    }
}

