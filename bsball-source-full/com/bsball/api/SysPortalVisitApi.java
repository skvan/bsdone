/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysPortalVisitApi
 *  com.bsball.common.Result
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.PortalVisitService
 *  com.bsball.service.PortalVisitService$PortalVisitProvinceCitiesDto
 *  com.bsball.service.PortalVisitService$PortalVisitProvinceSummaryDto
 *  com.bsball.service.PortalVisitService$PortalVisitSummaryDto
 *  lombok.Generated
 *  org.springframework.format.annotation.DateTimeFormat
 *  org.springframework.format.annotation.DateTimeFormat$ISO
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.PortalVisitService;
import java.time.LocalDate;
import lombok.Generated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/portal-visit"})
public class SysPortalVisitApi {
    private final PortalVisitService portalVisitService;
    private final TenantProperties tenantProperties;
    private final ApiPermissionService apiPermissionService;

    private long resolveTenantIdForSummary(Long requestedTenantId) {
        long currentTenantId;
        Long tid = CurrentUserHolder.getTenantId();
        long l = currentTenantId = tid != null ? tid.longValue() : this.tenantProperties.getDefaultId();
        if (requestedTenantId == null) {
            return currentTenantId;
        }
        long req = requestedTenantId;
        if (req == currentTenantId) {
            return currentTenantId;
        }
        Long uid = CurrentUserHolder.get();
        if (uid != null && this.apiPermissionService.isSuperAdmin(uid) && req == 0L) {
            return 0L;
        }
        return currentTenantId;
    }

    @GetMapping(value={"/summary"})
    public Result<PortalVisitService.PortalVisitSummaryDto> summary(@RequestParam(required=false) Integer days, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(required=false) Long tenantId) {
        long scopeTenantId = this.resolveTenantIdForSummary(tenantId);
        return Result.ok((Object)this.portalVisitService.summary(days, from, to, scopeTenantId));
    }

    @GetMapping(value={"/by-province"})
    public Result<PortalVisitService.PortalVisitProvinceSummaryDto> byProvince(@RequestParam(required=false) Integer days, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(required=false) Long tenantId) {
        long scopeTenantId = this.resolveTenantIdForSummary(tenantId);
        return Result.ok((Object)this.portalVisitService.summaryByProvince(days, from, to, scopeTenantId));
    }

    @GetMapping(value={"/by-province/cities"})
    public Result<PortalVisitService.PortalVisitProvinceCitiesDto> byProvinceCities(@RequestParam(value="province") String province, @RequestParam(required=false) Integer days, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam(required=false) Long tenantId) {
        long scopeTenantId = this.resolveTenantIdForSummary(tenantId);
        return Result.ok((Object)this.portalVisitService.summaryByProvinceCities(province, days, from, to, scopeTenantId));
    }

    @Generated
    public SysPortalVisitApi(PortalVisitService portalVisitService, TenantProperties tenantProperties, ApiPermissionService apiPermissionService) {
        this.portalVisitService = portalVisitService;
        this.tenantProperties = tenantProperties;
        this.apiPermissionService = apiPermissionService;
    }
}

