/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysRoleApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.entity.SysRole
 *  com.bsball.service.SysRoleService
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
import com.bsball.model.entity.SysRole;
import com.bsball.service.SysRoleService;
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
@RequestMapping(value={"/sys/role"})
public class SysRoleApi {
    private final SysRoleService sysRoleService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysRole>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        PageResult data = this.sysRoleService.list(uid, page, pageSize, keyword);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/assign-options"})
    public Result<PageResult<SysRole>> assignOptions(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        return Result.ok((Object)this.sysRoleService.assignOptions(uid, page, pageSize, keyword));
    }

    @GetMapping(value={"/{id:\\d+}"})
    public Result<SysRole> get(@PathVariable Long id) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        SysRole data = this.sysRoleService.get(uid, id);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysRole body) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        SysRole created = this.sysRoleService.create(uid, body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody SysRole body) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        this.sysRoleService.update(uid, id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        this.sysRoleService.delete(uid, id);
        return Result.ok((Object)Map.of());
    }

    @Generated
    public SysRoleApi(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }
}

