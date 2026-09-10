/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysMenuApi
 *  com.bsball.repository.SysMenuApiRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 */
package com.bsball.repository;

import com.bsball.model.entity.SysMenuApi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SysMenuApiRepository
extends JpaRepository<SysMenuApi, Long> {
    public List<SysMenuApi> findByMenuId(Long var1);

    public List<SysMenuApi> findByMenuIdIn(List<Long> var1);

    @Modifying
    @Query(value="DELETE FROM SysMenuApi e WHERE e.menuId = :menuId")
    public void deleteByMenuId(Long var1);
}

