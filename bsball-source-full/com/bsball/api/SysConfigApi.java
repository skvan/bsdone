/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysConfigApi
 *  com.bsball.common.Result
 *  com.bsball.service.SuperAdminTenantOverrideService
 *  com.bsball.service.SysConfigService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.service.SuperAdminTenantOverrideService;
import com.bsball.service.SysConfigService;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/config"})
public class SysConfigApi {
    private final SysConfigService sysConfigService;
    private final SuperAdminTenantOverrideService superAdminTenantOverrideService;

    @GetMapping
    public Result<Map<String, Object>> get(@RequestParam(required=false) Long tenantId) {
        long tid = this.superAdminTenantOverrideService.resolveConfigTenantId(tenantId);
        Map data = this.sysConfigService.getConfig(tid);
        return Result.ok((Object)data);
    }

    @PutMapping
    public Result<Object> put(@RequestParam(required=false) Long tenantId, @RequestBody Map<String, Object> body) {
        long tid = this.superAdminTenantOverrideService.resolveConfigTenantId(tenantId);
        this.sysConfigService.updateConfig(tid, body);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysConfigApi(SysConfigService sysConfigService, SuperAdminTenantOverrideService superAdminTenantOverrideService) {
        this.sysConfigService = sysConfigService;
        this.superAdminTenantOverrideService = superAdminTenantOverrideService;
    }
}

