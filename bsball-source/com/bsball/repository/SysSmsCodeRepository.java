/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysSmsCode
 *  com.bsball.repository.SysSmsCodeRepository
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysSmsCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysSmsCodeRepository
extends JpaRepository<SysSmsCode, Long> {
    @Query(value="SELECT s FROM SysSmsCode s\nWHERE s.phone = :phone AND s.scene = :scene AND s.usedAt IS NULL AND s.deletedAt IS NULL\n  AND s.expiresAt > :now\nORDER BY s.createdAt DESC\n")
    public List<SysSmsCode> findValidList(@Param(value="phone") String var1, @Param(value="scene") String var2, @Param(value="now") LocalDateTime var3, Pageable var4);

    default public Optional<SysSmsCode> findLatestValid(String phone, String scene, LocalDateTime now) {
        List list = this.findValidList(phone, scene, now, Pageable.ofSize((int)1));
        return list.isEmpty() ? Optional.empty() : Optional.of((SysSmsCode)list.get(0));
    }

    @Query(value="SELECT COUNT(s) FROM SysSmsCode s\nWHERE s.phone = :phone AND s.scene = :scene AND s.deletedAt IS NULL AND s.createdAt > :after\n")
    public long countRecentByPhoneAndScene(@Param(value="phone") String var1, @Param(value="scene") String var2, @Param(value="after") LocalDateTime var3);

    @Query(value="SELECT COUNT(s) FROM SysSmsCode s\nWHERE s.clientIp = :ip AND s.deletedAt IS NULL AND s.createdAt > :after\n")
    public long countRecentByIp(@Param(value="ip") String var1, @Param(value="after") LocalDateTime var2);

    @Query(value="SELECT s FROM SysSmsCode s\nWHERE s.phone = :phone AND s.scene = :scene AND s.deletedAt IS NULL\nORDER BY s.createdAt DESC\n")
    public List<SysSmsCode> findRecentByPhoneAndScene(@Param(value="phone") String var1, @Param(value="scene") String var2, Pageable var3);
}

