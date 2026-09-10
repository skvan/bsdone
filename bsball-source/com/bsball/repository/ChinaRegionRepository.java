/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.ChinaRegion
 *  com.bsball.repository.ChinaRegionRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.ChinaRegion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChinaRegionRepository
extends JpaRepository<ChinaRegion, Long> {
    public Optional<ChinaRegion> findByAdcode(String var1);

    public List<ChinaRegion> findByParentAdcodeOrderByAdcodeAsc(String var1);

    public List<ChinaRegion> findByLevelOrderByAdcodeAsc(int var1);

    public long count();
}

