/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysUserTenant
 *  com.bsball.repository.SysUserTenantRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysUserTenant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysUserTenantRepository
extends JpaRepository<SysUserTenant, Long> {
    public List<SysUserTenant> findByUserIdAndDeletedAtIsNull(Long var1);

    public List<SysUserTenant> findByUserIdInAndDeletedAtIsNull(Collection<Long> var1);

    public boolean existsByUserIdAndTenantIdAndDeletedAtIsNull(Long var1, Long var2);

    @Modifying
    @Query(value="delete from SysUserTenant u where u.userId = :userId")
    public void deleteByUserIdHard(@Param(value="userId") Long var1);
}

