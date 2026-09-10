/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.repository.SysTenantRepository
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysTenant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysTenantRepository
extends JpaRepository<SysTenant, Long> {
    public Optional<SysTenant> findByCodeAndDeletedAtIsNull(String var1);

    @Query(value="SELECT t FROM SysTenant t WHERE t.deletedAt IS NULL AND LOWER(t.code) = LOWER(:code)")
    public Optional<SysTenant> findByCodeIgnoreCaseAndDeletedAtIsNull(@Param(value="code") String var1);

    public boolean existsByCodeAndDeletedAtIsNull(String var1);

    public boolean existsByCodeAndDeletedAtIsNullAndIdNot(String var1, Long var2);

    public Page<SysTenant> findByDeletedAtIsNull(Pageable var1);

    @Query(value="SELECT t FROM SysTenant t WHERE t.deletedAt IS NULL AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :kw, '%')) OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :kw, '%')))")
    public Page<SysTenant> findByDeletedAtIsNullAndKeyword(@Param(value="kw") String var1, Pageable var2);

    public Optional<SysTenant> findFirstByDeletedAtIsNullOrderByIdAsc();
}

