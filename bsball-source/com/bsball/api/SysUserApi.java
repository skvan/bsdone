/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.SysUserApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.SysUser
 *  com.bsball.service.JwtService
 *  com.bsball.service.SysUserService
 *  jakarta.servlet.http.HttpServletRequest
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
import com.bsball.model.entity.SysUser;
import com.bsball.service.JwtService;
import com.bsball.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping(value={"/sys/user"})
public class SysUserApi {
    private final SysUserService sysUserService;
    private final JwtService jwtService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysUser>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) Integer allTenants) {
        PageResult data = this.sysUserService.list(page, pageSize, keyword, allTenants != null && allTenants == 1);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/{id}"})
    public Result<SysUser> get(@PathVariable Long id) {
        SysUser data = this.sysUserService.get(id);
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysUser body) {
        SysUser created = this.sysUserService.create(body);
        return Result.ok((Object)Map.of((Object)"id", (Object)created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody SysUser body) {
        this.sysUserService.update(id, body);
        return Result.ok((Object)Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id, HttpServletRequest request) {
        Long deletedBy = this.parseUserId(request);
        this.sysUserService.delete(id, deletedBy);
        return Result.ok((Object)Map.of());
    }

    private Long parseUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        return this.jwtService.parseUserId(auth.substring(7).trim());
    }

    @Generated
    public SysUserApi(SysUserService sysUserService, JwtService jwtService) {
        this.sysUserService = sysUserService;
        this.jwtService = jwtService;
    }
}

