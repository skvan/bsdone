package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

/**
 * 联赛级特殊规则开关（Issue #275）：extra_batter（EP 制）/ force_half（强制换局）。
 * 语义：行存在 = 以记录为准（支持显式关闭）；无记录 = 默认值（extra_batter=false、force_half=true）。
 * 规则行只写不删：无软删语义，deleted_* 恒空。
 */
@Entity
@Table(name = "bs_league_special_rule", uniqueConstraints = {@UniqueConstraint(columnNames = {"league_id", "rule_key"})})
@Comment(value = "联赛级特殊规则开关")
public class LeagueSpecialRule
extends BaseEntity {
    @Comment(value = "租户ID")
    @Column(nullable = false)
    private Long tenantId;
    @Comment(value = "联赛ID（bs_league.id，逻辑关联）")
    @Column(nullable = false)
    private Long leagueId;
    @Comment(value = "规则键：extra_batter / force_half")
    @Column(nullable = false, length = 64)
    private String ruleKey;
    @Comment(value = "是否启用")
    @Column(nullable = false)
    private Boolean enabled = Boolean.FALSE;

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getLeagueId() {
        return this.leagueId;
    }

    public String getRuleKey() {
        return this.ruleKey;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
