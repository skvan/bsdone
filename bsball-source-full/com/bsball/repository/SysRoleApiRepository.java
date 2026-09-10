/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysRoleApi
 *  com.bsball.repository.SysRoleApiRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysRoleApi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysRoleApiRepository
extends JpaRepository<SysRoleApi, Long> {
    public List<SysRoleApi> findByRoleId(Long var1);

    @Modifying
    @Query(value="DELETE FROM SysRoleApi e WHERE e.roleId = :roleId")
    public void deleteByRoleId(@Param(value="roleId") Long var1);

    public List<SysRoleApi> findByRoleIdIn(List<Long> var1);
}

