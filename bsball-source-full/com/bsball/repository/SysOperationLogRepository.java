/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysOperationLog
 *  com.bsball.repository.SysOperationLogRepository
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.bsball.repository;

import com.bsball.model.entity.SysOperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SysOperationLogRepository
extends JpaRepository<SysOperationLog, Long>,
JpaSpecificationExecutor<SysOperationLog> {
}

