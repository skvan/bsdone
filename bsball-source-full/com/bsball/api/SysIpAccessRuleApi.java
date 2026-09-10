/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysIpAccessRuleApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.IpAccessRulePayload
 *  com.bsball.model.entity.SysIpAccessRule
 *  com.bsball.service.SysIpAccessRuleService
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
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.IpAccessRulePayload;
import com.bsball.model.entity.SysIpAccessRule;
import com.bsball.service.SysIpAccessRuleService;
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
@RequestMapping(value={"/sys/ip-access-rule"})
public class SysIpAccessRuleApi {
    private final SysIpAccessRuleService sysIpAccessRuleService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysIpAccessRule>> list(@RequestParam(required=false) Long tenantId) {
        List list = this.sysIpAccessRuleService.list(tenantId);
        return Result.ok((Object)PageResult.of((List)list, (long)list.size()));
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestParam(required=false) Long tenantId, @RequestBody IpAccessRulePayload body) {
        try {
            SysIpAccessRule saved = this.sysIpAccessRuleService.create(tenantId, body);
            return Result.ok((Object)Map.of((Object)"id", (Object)saved.getId()));
        }
        catch (IllegalArgumentException e) {
            return Result.fail((int)400, (String)e.getMessage());
        }
        catch (BusinessException e) {
            return Result.fail((int)e.getCode(), (String)e.getMessage());
        }
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Map<String, Object>> update(@PathVariable long id, @RequestBody IpAccessRulePayload body) {
        try {
            this.sysIpAccessRuleService.update(id, body);
            return Result.ok((Object)Map.of());
        }
        catch (IllegalArgumentException e) {
            return Result.fail((int)400, (String)e.getMessage());
        }
        catch (BusinessException e) {
            return Result.fail((int)e.getCode(), (String)e.getMessage());
        }
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Map<String, Object>> delete(@PathVariable long id) {
        try {
            this.sysIpAccessRuleService.delete(id);
            return Result.ok((Object)Map.of());
        }
        catch (IllegalArgumentException e) {
            return Result.fail((int)400, (String)e.getMessage());
        }
        catch (BusinessException e) {
            return Result.fail((int)e.getCode(), (String)e.getMessage());
        }
    }

    @Generated
    public SysIpAccessRuleApi(SysIpAccessRuleService sysIpAccessRuleService) {
        this.sysIpAccessRuleService = sysIpAccessRuleService;
    }
}

