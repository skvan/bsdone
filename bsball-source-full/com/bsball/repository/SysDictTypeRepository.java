/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysDictType
 *  com.bsball.repository.SysDictTypeRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.SysDictType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysDictTypeRepository
extends JpaRepository<SysDictType, Long> {
    public Optional<SysDictType> findByType(String var1);

    public List<SysDictType> findAllByType(String var1);
}

