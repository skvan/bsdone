/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysDictData
 *  com.bsball.repository.SysDictDataRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.SysDictData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysDictDataRepository
extends JpaRepository<SysDictData, Long>,
JpaSpecificationExecutor<SysDictData> {
    public List<SysDictData> findByDictTypeIdOrderBySortAscIdAsc(Long var1);
}

