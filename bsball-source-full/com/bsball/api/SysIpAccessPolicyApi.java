/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysIpAccessPolicyApi
 *  com.bsball.common.Result
 *  com.bsball.model.dto.IpAccessPolicyVo
 *  com.bsball.service.IpAccessPolicyService
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
import com.bsball.model.dto.IpAccessPolicyVo;
import com.bsball.service.IpAccessPolicyService;
import com.bsball.service.SuperAdminTenantOverrideService;
import com.bsball.service.SysConfigService;
import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/sys/ip-access-policy"})
public class SysIpAccessPolicyApi {
    private final SysConfigService sysConfigService;
    private final IpAccessPolicyService ipAccessPolicyService;
    private final SuperAdminTenantOverrideService superAdminTenantOverrideService;

    @GetMapping
    public Result<IpAccessPolicyVo> get(@RequestParam(required=false) Long tenantId) {
        long tid = this.superAdminTenantOverrideService.resolveConfigTenantId(tenantId);
        return Result.ok((Object)this.ipAccessPolicyService.getPolicyVoForTenant(tid));
    }

    @PutMapping
    public Result<Map<String, Object>> update(@RequestParam(required=false) Long tenantId, @RequestBody IpAccessPolicyVo body) {
        if (body == null || body.getMode() == null) {
            return Result.fail((int)400, (String)"mode \u4e0d\u80fd\u4e3a\u7a7a");
        }
        String mode = body.getMode().trim();
        if (!SysIpAccessPolicyApi.isValidMode((String)mode)) {
            return Result.fail((int)400, (String)"mode \u987b\u4e3a off\u3001blacklist \u6216 whitelist");
        }
        long tid = this.superAdminTenantOverrideService.resolveConfigTenantId(tenantId);
        HashMap<String, String> upd = new HashMap<String, String>();
        upd.put("ipAccessPolicyMode", mode.toLowerCase());
        if (body.getBypassPaths() != null) {
            upd.put("ipAccessPolicyBypassPaths", body.getBypassPaths());
        }
        this.sysConfigService.updateConfig(tid, upd);
        this.ipAccessPolicyService.refresh();
        return Result.ok((Object)Map.of());
    }

    private static boolean isValidMode(String m) {
        return "off".equalsIgnoreCase(m) || "blacklist".equalsIgnoreCase(m) || "whitelist".equalsIgnoreCase(m);
    }

    @Generated
    public SysIpAccessPolicyApi(SysConfigService sysConfigService, IpAccessPolicyService ipAccessPolicyService, SuperAdminTenantOverrideService superAdminTenantOverrideService) {
        this.sysConfigService = sysConfigService;
        this.ipAccessPolicyService = ipAccessPolicyService;
        this.superAdminTenantOverrideService = superAdminTenantOverrideService;
    }
}

