/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.DictCacheWarmupRunner
 *  com.bsball.repository.SysDictTypeRepository
 *  com.bsball.service.SysDictDataService
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 */
package com.bsball.core;

import com.bsball.repository.SysDictTypeRepository;
import com.bsball.service.SysDictDataService;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value=2)
@ConditionalOnProperty(prefix="app.cache", name={"type"}, havingValue="local", matchIfMissing=true)
public class DictCacheWarmupRunner
implements CommandLineRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(DictCacheWarmupRunner.class);
    private final SysDictTypeRepository sysDictTypeRepository;
    private final SysDictDataService sysDictDataService;

    public void run(String ... args) {
        try {
            this.sysDictTypeRepository.findAll().forEach(t -> this.sysDictDataService.getListByDictTypeId(t.getId()));
            log.debug("\u5b57\u5178\u7f13\u5b58\u9884\u70ed\u5b8c\u6210");
        }
        catch (Exception e) {
            log.warn("\u5b57\u5178\u7f13\u5b58\u9884\u70ed\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    @Generated
    public DictCacheWarmupRunner(SysDictTypeRepository sysDictTypeRepository, SysDictDataService sysDictDataService) {
        this.sysDictTypeRepository = sysDictTypeRepository;
        this.sysDictDataService = sysDictDataService;
    }
}

