/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysConfig
 *  com.bsball.model.entity.SysConfigId
 *  com.bsball.repository.SysConfigRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.SysConfig;
import com.bsball.model.entity.SysConfigId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysConfigRepository
extends JpaRepository<SysConfig, SysConfigId> {
    public List<SysConfig> findByTenantId(Long var1);

    public Optional<SysConfig> findByTenantIdAndConfigKey(Long var1, String var2);
}

