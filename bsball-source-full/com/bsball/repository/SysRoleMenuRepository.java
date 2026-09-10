/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysRoleMenu
 *  com.bsball.repository.SysRoleMenuRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysRoleMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysRoleMenuRepository
extends JpaRepository<SysRoleMenu, Long> {
    public List<SysRoleMenu> findByRoleId(Long var1);

    @Modifying
    @Query(value="DELETE FROM SysRoleMenu e WHERE e.roleId = :roleId")
    public void deleteByRoleId(@Param(value="roleId") Long var1);

    public List<SysRoleMenu> findByRoleIdIn(List<Long> var1);
}

