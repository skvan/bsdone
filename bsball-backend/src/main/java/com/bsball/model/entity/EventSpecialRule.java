package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Comment;

/**
 * 赛事级特殊规则开关（Issue #275）：extra_batter（EP 制）/ force_half（强制换局）。
 * 语义：行存在 = 以记录为准；无记录 = 默认值。是否允许配置由联赛收费能力或赛事既有规则决定。
 */
@Entity
@Table(name = "bs_event_special_rule", uniqueConstraints = {@UniqueConstraint(columnNames = {"event_id", "rule_key"})})
@Comment(value = "赛事级特殊规则开关")
public class EventSpecialRule
extends BaseEntity {
    @Comment(value = "租户ID")
    @Column(nullable = false)
    private Long tenantId;
    @Comment(value = "赛事ID（bs_event.id，逻辑关联）")
    @Column(nullable = false)
    private Long eventId;
    @Comment(value = "规则键：extra_batter / force_half")
    @Column(nullable = false, length = 64)
    private String ruleKey;
    @Comment(value = "是否启用")
    @Column(nullable = false)
    private Boolean enabled = Boolean.FALSE;

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getEventId() {
        return this.eventId;
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

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
