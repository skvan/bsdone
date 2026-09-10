/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysIpAccessRule
 *  com.bsball.repository.SysIpAccessRuleRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.SysIpAccessRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysIpAccessRuleRepository
extends JpaRepository<SysIpAccessRule, Long> {
    public List<SysIpAccessRule> findByTenantIdAndScopeTypeAndEnabledOrderByPriorityDesc(Long var1, String var2, Short var3);

    public List<SysIpAccessRule> findByTenantIdAndScopeTypeOrderByPriorityDesc(Long var1, String var2);
}

