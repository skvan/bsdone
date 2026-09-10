/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysOperationLog
 *  com.bsball.repository.SysOperationLogRepository
 *  com.bsball.service.OperationLogAsyncService
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.scheduling.annotation.Async
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.model.entity.SysOperationLog;
import com.bsball.repository.SysOperationLogRepository;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class OperationLogAsyncService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(OperationLogAsyncService.class);
    private final SysOperationLogRepository sysOperationLogRepository;

    @Async
    public void saveAsync(SysOperationLog logEntity) {
        try {
            this.sysOperationLogRepository.save((Object)logEntity);
        }
        catch (Exception e) {
            log.debug("\u64cd\u4f5c\u65e5\u5fd7\u5f02\u6b65\u4fdd\u5b58\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    @Generated
    public OperationLogAsyncService(SysOperationLogRepository sysOperationLogRepository) {
        this.sysOperationLogRepository = sysOperationLogRepository;
    }
}

