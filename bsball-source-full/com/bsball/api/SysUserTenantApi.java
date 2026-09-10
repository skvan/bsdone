/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysUserTenantApi
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.service.SysUserTenantManageService
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
import com.bsball.core.CurrentUserHolder;
import com.bsball.service.SysUserTenantManageService;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/user-tenant"})
public class SysUserTenantApi {
    private final SysUserTenantManageService sysUserTenantManageService;

    @GetMapping(value={"/list"})
    public Result<List<Long>> list(@RequestParam Long userId) {
        Long op = CurrentUserHolder.get();
        if (op == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        return Result.ok((Object)this.sysUserTenantManageService.listTenantIds(op.longValue(), userId.longValue()));
    }

    @PutMapping(value={"/replace"})
    public Result<Object> replace(@RequestBody Map<String, Object> body) {
        Long op = CurrentUserHolder.get();
        if (op == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        Object uidObj = body.get("userId");
        List raw = (List)body.get("tenantIds");
        if (uidObj == null || raw == null) {
            return Result.fail((int)400, (String)"userId\u3001tenantIds \u4e0d\u80fd\u4e3a\u7a7a");
        }
        long userId = ((Number)uidObj).longValue();
        List tenantIds = raw.stream().map(Number::longValue).toList();
        this.sysUserTenantManageService.replace(op.longValue(), userId, tenantIds);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysUserTenantApi(SysUserTenantManageService sysUserTenantManageService) {
        this.sysUserTenantManageService = sysUserTenantManageService;
    }
}

