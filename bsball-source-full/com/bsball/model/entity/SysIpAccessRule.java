/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysIpAccessRule
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_ip_access_rule")
@Comment(value="IP \u8bbf\u95ee\u89c4\u5219")
public class SysIpAccessRule {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="rule_kind", nullable=false, length=8)
    @Comment(value="ALLOW \u6216 DENY")
    private String ruleKind;
    @Column(name="scope_type", nullable=false, length=32)
    @Comment(value="GLOBAL / ROLE / API_PREFIX")
    private String scopeType = "GLOBAL";
    @Column(name="tenant_id", nullable=false)
    @Comment(value="\u79df\u6237\uff1a\u89c4\u5219\u4ec5\u4f5c\u7528\u4e8e\u8be5\u79df\u6237 IP \u7b56\u7565\u4e0a\u4e0b\u6587")
    private Long tenantId = 1L;
    @Column(name="role_id")
    private Long roleId;
    @Column(name="api_pattern", length=512)
    private String apiPattern;
    @Column(name="cidr_or_ip", nullable=false, length=128)
    private String cidrOrIp;
    @Column(nullable=false)
    private Integer priority = 0;
    @Column(nullable=false, columnDefinition="SMALLINT")
    private Short enabled = 1;
    @Column(length=512)
    private String remark;
    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;
    @Column(name="created_by")
    private Long createdBy;
    @Column(name="updated_by")
    private Long updatedBy;

    @PrePersist
    void prePersist() {
        LocalDateTime n = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = n;
        }
        if (this.updatedAt == null) {
            this.updatedAt = n;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Generated
    public SysIpAccessRule() {
    }

    @Generated
    public Long getId() {
        return this.id;
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
    public Long getTenantId() {
        return this.tenantId;
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
    public Short getEnabled() {
        return this.enabled;
    }

    @Generated
    public String getRemark() {
        return this.remark;
    }

    @Generated
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public Long getCreatedBy() {
        return this.createdBy;
    }

    @Generated
    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
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
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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
    public void setEnabled(Short enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    @Generated
    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysIpAccessRule)) {
            return false;
        }
        SysIpAccessRule other = (SysIpAccessRule)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
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
        Short this$enabled = this.getEnabled();
        Short other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !((Object)this$enabled).equals(other$enabled)) {
            return false;
        }
        Long this$createdBy = this.getCreatedBy();
        Long other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !((Object)this$createdBy).equals(other$createdBy)) {
            return false;
        }
        Long this$updatedBy = this.getUpdatedBy();
        Long other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !((Object)this$updatedBy).equals(other$updatedBy)) {
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
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysIpAccessRule;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $roleId = this.getRoleId();
        result = result * 59 + ($roleId == null ? 43 : ((Object)$roleId).hashCode());
        Integer $priority = this.getPriority();
        result = result * 59 + ($priority == null ? 43 : ((Object)$priority).hashCode());
        Short $enabled = this.getEnabled();
        result = result * 59 + ($enabled == null ? 43 : ((Object)$enabled).hashCode());
        Long $createdBy = this.getCreatedBy();
        result = result * 59 + ($createdBy == null ? 43 : ((Object)$createdBy).hashCode());
        Long $updatedBy = this.getUpdatedBy();
        result = result * 59 + ($updatedBy == null ? 43 : ((Object)$updatedBy).hashCode());
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
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SysIpAccessRule(id=" + this.getId() + ", ruleKind=" + this.getRuleKind() + ", scopeType=" + this.getScopeType() + ", tenantId=" + this.getTenantId() + ", roleId=" + this.getRoleId() + ", apiPattern=" + this.getApiPattern() + ", cidrOrIp=" + this.getCidrOrIp() + ", priority=" + this.getPriority() + ", enabled=" + this.getEnabled() + ", remark=" + this.getRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ")";
    }
}

