/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysArticle
 *  com.bsball.repository.SysArticleRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.bsball.repository;

import com.bsball.model.entity.SysArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SysArticleRepository
extends JpaRepository<SysArticle, Long>,
JpaSpecificationExecutor<SysArticle> {
    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query(value="UPDATE SysArticle a SET a.viewCount = COALESCE(a.viewCount, 0) + 1 WHERE a.id = :id")
    public int incrementViewCountById(@Param(value="id") Long var1);
}

