/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.IpLocationCache
 *  com.bsball.repository.IpLocationCacheRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.IpLocationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IpLocationCacheRepository
extends JpaRepository<IpLocationCache, String>,
JpaSpecificationExecutor<IpLocationCache> {
}

