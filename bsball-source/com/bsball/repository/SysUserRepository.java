/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysUser
 *  com.bsball.repository.SysUserRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysUserRepository
extends JpaRepository<SysUser, Long>,
JpaSpecificationExecutor<SysUser> {
    public Optional<SysUser> findByUsernameAndDeletedAtIsNull(String var1);

    public Optional<SysUser> findByPhoneAndDeletedAtIsNull(String var1);

    public boolean existsByPhoneAndDeletedAtIsNullAndIdNot(String var1, Long var2);

    public boolean existsByPhoneAndDeletedAtIsNull(String var1);

    public Optional<SysUser> findByEmailAndDeletedAtIsNull(String var1);

    public boolean existsByEmailAndDeletedAtIsNullAndIdNot(String var1, Long var2);

    @Query(value="select u.id from SysUser u\nwhere u.deletedAt is null and (\n    lower(u.username) like lower(concat('%', :kw, '%'))\n    or lower(u.realName) like lower(concat('%', :kw, '%'))\n    or u.phone like concat('%', :kw, '%')\n    or lower(u.email) like lower(concat('%', :kw, '%'))\n)\n")
    public List<Long> findIdsByKeyword(@Param(value="kw") String var1);
}

