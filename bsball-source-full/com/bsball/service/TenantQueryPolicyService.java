/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.TenantQueryPolicyService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.service.ApiPermissionService;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class TenantQueryPolicyService {
    private final ApiPermissionService apiPermissionService;
    private final TenantProperties tenantProperties;

    public boolean isGlobalQueryMode() {
        Long uid = CurrentUserHolder.get();
        Long tid = CurrentUserHolder.getTenantId();
        return uid != null && tid != null && tid == 0L && this.apiPermissionService.isSuperAdmin(uid);
    }

    public long requiredTenantId() {
        Long tid = CurrentUserHolder.getTenantId();
        if (tid != null) {
            return tid;
        }
        if (CurrentUserHolder.get() != null) {
            throw new BusinessException(401, "\u767b\u5f55\u6001\u7f3a\u5c11\u79df\u6237\u4fe1\u606f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55");
        }
        return this.tenantProperties.getDefaultId();
    }

    public Long tenantIdOrNullForQuery() {
        return this.isGlobalQueryMode() ? null : Long.valueOf(this.requiredTenantId());
    }

    @Generated
    public TenantQueryPolicyService(ApiPermissionService apiPermissionService, TenantProperties tenantProperties) {
        this.apiPermissionService = apiPermissionService;
        this.tenantProperties = tenantProperties;
    }
}

