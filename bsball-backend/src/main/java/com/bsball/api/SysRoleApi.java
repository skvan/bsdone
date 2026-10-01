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
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysRole;
import com.bsball.service.SysRoleService;
import java.util.ArrayList;
import java.util.Collection;
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
@RequestMapping(value={"/sys/role"})
public class SysRoleApi {
    private final SysRoleService sysRoleService;

    @GetMapping(value={"/list"})
    public Result<PageResult<SysRole>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        PageResult data = this.sysRoleService.list(uid, page, pageSize, keyword);
        return Result.ok(data);
    }

    @GetMapping(value={"/assign-options"})
    public Result<PageResult<SysRole>> assignOptions(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        return Result.ok(this.sysRoleService.assignOptions(uid, page, pageSize, keyword));
    }

    @GetMapping(value={"/{id:\\d+}"})
    public Result<SysRole> get(@PathVariable Long id) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        SysRole data = this.sysRoleService.get(uid, id);
        return Result.ok(data);
    }

    @PostMapping(value={"/create"})
    public Result<Map<String, Object>> create(@RequestBody SysRole body) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        SysRole created = this.sysRoleService.create(uid, body);
        return Result.ok(Map.of("id",created.getId()));
    }

    @PutMapping(value={"/update/{id}"})
    public Result<Object> update(@PathVariable Long id, @RequestBody SysRole body) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        this.sysRoleService.update(uid, id, body);
        return Result.ok(Map.of());
    }

    @DeleteMapping(value={"/delete/{id}"})
    public Result<Object> delete(@PathVariable Long id) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        this.sysRoleService.delete(uid, id);
        return Result.ok(Map.of());
    }

    /* ----------------------------- 租户级目录覆盖（spec §8.6 / T3.15b） ----------------------------- */

    /**
     * 租户级目录覆盖端点（spec §8.6 / T3.15b）。
     * 参数位置：PUT 用 body（roleId/menuIds[/tenantId]），GET/DELETE 用 query（roleId[/tenantId]）。
     * <p>PUT body 校验：menuIds 必须为数组（元素须为整数，不可解析 → 400「menuIds 元素必须为数字」）；
     * roleId 须为整数（1.5 等非整数 → 400，不截断）。空数组 = 启用空覆盖（清空走 DELETE 或显式 []）。
     */

    @GetMapping(value={"/tenant-config"})
    public Result<Map<String, Object>> getTenantConfig(@RequestParam(required=false) Long tenantId, @RequestParam Long roleId) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        return Result.ok(this.sysRoleService.getTenantConfig(uid, tenantId, roleId));
    }

    @PutMapping(value={"/tenant-config"})
    public Result<Map<String, Object>> saveTenantConfig(@RequestBody Map<String, Object> body) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        Long roleId = SysRoleApi.asLongId(body.get("roleId"));
        Long tenantId = SysRoleApi.asLong(body.get("tenantId"));
        List<Long> menuIds = SysRoleApi.asLongList(body.get("menuIds"));
        return Result.ok(this.sysRoleService.saveTenantConfig(uid, tenantId, roleId, menuIds));
    }

    @DeleteMapping(value={"/tenant-config"})
    public Result<Object> clearTenantConfig(@RequestParam(required=false) Long tenantId, @RequestParam Long roleId) {
        Long uid = CurrentUserHolder.get();
        if (uid == null) {
            return Result.fail((int)401, "\u8bf7\u5148\u767b\u5f55");
        }
        this.sysRoleService.clearTenantConfig(uid, tenantId, roleId);
        return Result.ok(Map.of());
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return Long.valueOf(((Number)v).longValue());
        }
        try {
            return Long.valueOf(v.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /*
     * PUT body 校验收紧（批 3b T3.15b / I3）：menuIds 缺失或非数组 → 400「menuIds 必须为数组」；
     * 元素无法解析为整数 → 400「menuIds 元素必须为数字」（不再静默丢弃）。空数组 = 启用空覆盖。
     */
    private static List<Long> asLongList(Object v) {
        if (!(v instanceof Collection)) {
            throw new BusinessException(400, "menuIds 必须为数组");
        }
        ArrayList<Long> out = new ArrayList<Long>();
        for (Object o : (Collection)v) {
            Long l = SysRoleApi.asMenuIdElement(o);
            if (l == null) {
                throw new BusinessException(400, "menuIds 元素必须为数字");
            }
            out.add(l);
        }
        return out;
    }

    /*
     * roleId 校验收紧（批 3b T3.15b / I3）：非整数数字（如 1.5）→ 400「roleId 必须为整数」（不截断）；
     * 非数字仍返回 null，交由服务层以「roleId 不能为空」400 兜底（现状保持）。
     */
    private static Long asLongId(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            double d = ((Number)v).doubleValue();
            if (d != Math.rint(d) || Double.isInfinite(d)) {
                throw new BusinessException(400, "roleId 必须为整数");
            }
            return Long.valueOf(((Number)v).longValue());
        }
        try {
            return Long.valueOf(v.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long asMenuIdElement(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            double d = ((Number)v).doubleValue();
            if (d != Math.rint(d) || Double.isInfinite(d)) {
                return null;
            }
            return Long.valueOf(((Number)v).longValue());
        }
        try {
            return Long.valueOf(v.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Generated
    public SysRoleApi(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }
}

