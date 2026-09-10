/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.ApiPathMigrationRunner
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysApi
 *  com.bsball.model.entity.SysRole
 *  com.bsball.model.entity.SysRoleApi
 *  com.bsball.repository.SysApiRepository
 *  com.bsball.repository.SysRoleApiRepository
 *  com.bsball.repository.SysRoleRepository
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.core;

import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(value=0)
public class ApiPathMigrationRunner
implements CommandLineRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ApiPathMigrationRunner.class);
    private final SysApiRepository sysApiRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysRoleApiRepository sysRoleApiRepository;
    private static final String GUEST_ROLE_CODE = "guest";

    @Transactional
    public void run(String ... args) {
        try {
            this.stripApiPrefix();
            this.ensureGuestHasNoticeAndCoachApis();
        }
        catch (Exception e) {
            log.warn("ApiPathMigration \u6267\u884c\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    private void stripApiPrefix() {
        List all = this.sysApiRepository.findAll();
        List toUpdate = all.stream().filter(a -> a.getPath() != null && a.getPath().startsWith("/api/")).toList();
        if (toUpdate.isEmpty()) {
            return;
        }
        for (SysApi a2 : toUpdate) {
            a2.setPath(a2.getPath().substring(5));
            this.sysApiRepository.save((Object)a2);
        }
        log.info("ApiPathMigration: \u5df2\u53bb\u6389 {} \u6761 API \u8def\u5f84\u7684 /api \u524d\u7f00", (Object)toUpdate.size());
    }

    private void ensureGuestHasNoticeAndCoachApis() {
        Optional guestOpt = this.sysRoleRepository.findByTenantIdIsNullAndCode(GUEST_ROLE_CODE);
        if (guestOpt.isEmpty()) {
            return;
        }
        SysRole guestRole = (SysRole)guestOpt.get();
        List apis = this.sysApiRepository.findAll();
        List noticeAndCoachApiIds = apis.stream().filter(a -> "GET".equals(a.getMethod()) && a.getPath() != null && (a.getPath().contains("/sys/notice") || a.getPath().contains("/sys/article") || a.getPath().contains("/coach"))).map(BaseEntity::getId).distinct().toList();
        if (noticeAndCoachApiIds.isEmpty()) {
            return;
        }
        Set existing = this.sysRoleApiRepository.findByRoleId(guestRole.getId()).stream().map(ra -> ra.getApiId()).collect(Collectors.toSet());
        long adminId = 1L;
        int added = 0;
        for (Long apiId : noticeAndCoachApiIds) {
            if (existing.contains(apiId)) continue;
            SysRoleApi ra2 = new SysRoleApi();
            ra2.setRoleId(guestRole.getId());
            ra2.setApiId(apiId);
            ra2.setCreatedBy(Long.valueOf(adminId));
            ra2.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra2);
            existing.add(apiId);
            ++added;
        }
        if (added > 0) {
            log.info("ApiPathMigration: \u5df2\u4e3a\u6e38\u5ba2\u89d2\u8272\u5206\u914d {} \u4e2a\u6587\u7ae0/\u901a\u77e5/\u6559\u7ec3\u76f8\u5173 API", (Object)added);
        }
    }

    @Generated
    public ApiPathMigrationRunner(SysApiRepository sysApiRepository, SysRoleRepository sysRoleRepository, SysRoleApiRepository sysRoleApiRepository) {
        this.sysApiRepository = sysApiRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysRoleApiRepository = sysRoleApiRepository;
    }
}

