/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysDataScope
 *  com.bsball.repository.SysDataScopeRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.SysDataScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysDataScopeRepository
extends JpaRepository<SysDataScope, Long> {
    public List<SysDataScope> findByUserIdAndTenantIdAndDeletedAtIsNull(Long var1, Long var2);

    public void deleteByUserIdAndTenantId(Long var1, Long var2);
}

