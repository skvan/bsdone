/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysEmailCode
 *  com.bsball.repository.SysEmailCodeRepository
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysEmailCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysEmailCodeRepository
extends JpaRepository<SysEmailCode, Long> {
    @Query(value="SELECT s FROM SysEmailCode s\nWHERE s.email = :email AND s.scene = :scene AND s.usedAt IS NULL AND s.deletedAt IS NULL\n  AND s.expiresAt > :now\nORDER BY s.createdAt DESC\n")
    public List<SysEmailCode> findValidList(@Param(value="email") String var1, @Param(value="scene") String var2, @Param(value="now") LocalDateTime var3, Pageable var4);

    default public Optional<SysEmailCode> findLatestValid(String email, String scene, LocalDateTime now) {
        List list = this.findValidList(email, scene, now, Pageable.ofSize((int)1));
        return list.isEmpty() ? Optional.empty() : Optional.of((SysEmailCode)list.get(0));
    }

    @Query(value="SELECT COUNT(s) FROM SysEmailCode s\nWHERE s.email = :email AND s.scene = :scene AND s.deletedAt IS NULL AND s.createdAt > :after\n")
    public long countRecentByEmailAndScene(@Param(value="email") String var1, @Param(value="scene") String var2, @Param(value="after") LocalDateTime var3);
}

