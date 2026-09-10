/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysRole
 *  com.bsball.repository.SysRoleRepository
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysRoleRepository
extends JpaRepository<SysRole, Long> {
    public Optional<SysRole> findByTenantIdIsNullAndCode(String var1);

    @Query(value="SELECT r FROM SysRole r WHERE COALESCE(:kw, '') = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :kw, '%'))")
    public Page<SysRole> findAllWithKeyword(@Param(value="kw") String var1, Pageable var2);

    public Page<SysRole> findByTenantId(Long var1, Pageable var2);

    @Query(value="SELECT r FROM SysRole r WHERE r.tenantId = :tid AND (COALESCE(:kw, '') = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :kw, '%')))")
    public Page<SysRole> findByTenantIdAndKeyword(@Param(value="tid") Long var1, @Param(value="kw") String var2, Pageable var3);

    public boolean existsByTenantIdAndCode(Long var1, String var2);

    public Optional<SysRole> findByTenantIdAndCode(Long var1, String var2);

    public boolean existsByTenantIdIsNullAndCode(String var1);

    @Query(value="SELECT r FROM SysRole r WHERE (r.tenantId = :tid OR (r.tenantId IS NULL AND r.code IN :codes)) AND (COALESCE(:kw, '') = '' OR LOWER(r.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(COALESCE(r.description, '')) LIKE LOWER(CONCAT('%', :kw, '%')))")
    public Page<SysRole> findForAssignOptions(@Param(value="tid") Long var1, @Param(value="codes") List<String> var2, @Param(value="kw") String var3, Pageable var4);
}

