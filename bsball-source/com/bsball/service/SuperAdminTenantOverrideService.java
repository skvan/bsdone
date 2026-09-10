/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SuperAdminTenantOverrideService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.repository.SysTenantRepository;
import com.bsball.service.ApiPermissionService;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class SuperAdminTenantOverrideService {
    private final ApiPermissionService apiPermissionService;
    private final TenantProperties tenantProperties;
    private final SysTenantRepository sysTenantRepository;

    public long resolveConfigTenantId(Long requestedTenantId) {
        Long uid = CurrentUserHolder.get();
        if (uid != null && this.apiPermissionService.isSuperAdmin(uid) && requestedTenantId != null) {
            if (!this.sysTenantRepository.existsById((Object)requestedTenantId)) {
                throw new BusinessException(400, "\u79df\u6237\u4e0d\u5b58\u5728");
            }
            return requestedTenantId;
        }
        Long t = CurrentUserHolder.getTenantId();
        return t != null ? t.longValue() : this.tenantProperties.getDefaultId();
    }

    @Generated
    public SuperAdminTenantOverrideService(ApiPermissionService apiPermissionService, TenantProperties tenantProperties, SysTenantRepository sysTenantRepository) {
        this.apiPermissionService = apiPermissionService;
        this.tenantProperties = tenantProperties;
        this.sysTenantRepository = sysTenantRepository;
    }
}

