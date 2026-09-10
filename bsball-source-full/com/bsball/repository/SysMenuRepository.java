/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysMenu
 *  com.bsball.repository.SysMenuRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.bsball.repository;

import com.bsball.model.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysMenuRepository
extends JpaRepository<SysMenu, Long> {
}

