/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysResource
 *  com.bsball.repository.SysResourceRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.SysResource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysResourceRepository
extends JpaRepository<SysResource, Long>,
JpaSpecificationExecutor<SysResource> {
    public Optional<SysResource> findFirstByTenantIdAndPath(Long var1, String var2);
}

