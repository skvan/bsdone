/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysNotice
 *  com.bsball.repository.SysNoticeRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.SysNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysNoticeRepository
extends JpaRepository<SysNotice, Long>,
JpaSpecificationExecutor<SysNotice> {
}

