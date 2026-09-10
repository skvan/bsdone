/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysUserRole
 *  com.bsball.repository.SysUserRoleRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysUserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysUserRoleRepository
extends JpaRepository<SysUserRole, Long> {
    public List<SysUserRole> findByUserId(Long var1);

    @Modifying
    @Query(value="DELETE FROM SysUserRole ur WHERE ur.userId = :userId")
    public void deleteByUserId(@Param(value="userId") Long var1);

    @Modifying
    @Query(value="DELETE FROM SysUserRole ur WHERE ur.roleId = :roleId")
    public void deleteByRoleId(@Param(value="roleId") Long var1);

    @Query(value="select count(distinct ur.userId) from SysUserRole ur join SysUser u on ur.userId = u.id where ur.roleId = :roleId and u.deletedAt is null and ur.userId <> :excludeUserId")
    public long countDistinctActiveUsersWithRoleExcept(@Param(value="roleId") Long var1, @Param(value="excludeUserId") Long var2);
}

