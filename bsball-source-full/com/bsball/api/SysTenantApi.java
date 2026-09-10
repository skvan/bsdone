/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysTenantApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.model.entity.SysUserTenant
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.repository.SysUserTenantRepository
 *  com.bsball.service.SysTenantManageService
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.model.entity.SysTenant;
import com.bsball.model.entity.SysUserTenant;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.service.SysTenantManageService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/tenant"})
public class SysTenantApi {
    private final SysTenantRepository sysTenantRepository;
    private final SysUserTenantRepository sysUserTenantRepository;
    private final SysTenantManageService sysTenantManageService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysTenant>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        PageResult data = this.sysTenantManageService.list(uid.longValue(), page, pageSize, keyword);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/scope-options"})
    public Result<Map<String, Object>> scopeOptions(@RequestParam Long tenantId) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        return Result.ok((Object)this.sysTenantManageService.scopeOptions(uid.longValue(), tenantId.longValue()));
    }

    @GetMapping(value={"/my-tenants"})
    public Result<List<Map<String, Object>>> myTenants() {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        List uts = this.sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(uid);
        ArrayList out = new ArrayList();
        for (SysUserTenant ut : uts) {
            SysTenant t = this.sysTenantRepository.findById((Object)ut.getTenantId()).orElse(null);
            if (t == null || t.getDeletedAt() != null) continue;
            HashMap<String, Object> row = new HashMap<String, Object>();
            row.put("id", t.getId());
            row.put("name", t.getName());
            row.put("code", t.getCode());
            out.add(row);
        }
        return Result.ok(out);
    }

    @GetMapping(value={"/{id}"})
    public Result<SysTenant> get(@PathVariable Long id) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        SysTenant t = this.sysTenantManageService.get(uid.longValue(), id);
        return Result.ok((Object)t);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysTenant body) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        SysTenant created = this.sysTenantManageService.create(uid.longValue(), body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody SysTenant body) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        this.sysTenantManageService.update(uid.longValue(), id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        this.sysTenantManageService.delete(uid.longValue(), id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysTenantApi(SysTenantRepository sysTenantRepository, SysUserTenantRepository sysUserTenantRepository, SysTenantManageService sysTenantManageService) {
        this.sysTenantRepository = sysTenantRepository;
        this.sysUserTenantRepository = sysUserTenantRepository;
        this.sysTenantManageService = sysTenantManageService;
    }
}

