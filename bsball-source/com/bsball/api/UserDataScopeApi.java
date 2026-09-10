/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.UserDataScopeApi
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.entity.SysDataScope
 *  com.bsball.service.UserDataScopeManageService
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
import com.bsball.model.entity.SysDataScope;
import com.bsball.service.UserDataScopeManageService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
@RequestMapping(value={"/sys/user-data-scope"})
public class UserDataScopeApi {
    private final UserDataScopeManageService userDataScopeManageService;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam Long userId, @RequestParam Long tenantId) {
        Long op = CurrentUserHolder.get();
        if (op == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        List rows = this.userDataScopeManageService.list(op.longValue(), userId.longValue(), tenantId.longValue());
        List out = rows.stream().map(UserDataScopeApi::toRow).collect(Collectors.toList());
        return Result.ok(out);
    }

    @PutMapping
    public Result<Object> replace(@RequestBody Map<String, Object> body) {
        Long op = CurrentUserHolder.get();
        if (op == null) {
            return Result.fail((int)401, (String)"\u8bf7\u5148\u767b\u5f55");
        }
        Long userId = UserDataScopeApi.longVal((Object)body.get("userId"));
        Long tenantId = UserDataScopeApi.longVal((Object)body.get("tenantId"));
        List scopes = (List)body.get("scopes");
        if (userId == null || tenantId == null) {
            return Result.fail((int)400, (String)"userId\u3001tenantId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        this.userDataScopeManageService.replace(op.longValue(), userId.longValue(), tenantId.longValue(), scopes);
        return Result.ok((Object)Map.of());
    }

    private static Map<String, Object> toRow(SysDataScope s) {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("id", s.getId());
        m.put("userId", s.getUserId());
        m.put("tenantId", s.getTenantId());
        m.put("scopeType", s.getScopeType());
        m.put("refId", s.getRefId());
        m.put("expansion", s.getExpansion());
        return m;
    }

    private static Long longVal(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            Number n = (Number)o;
            return n.longValue();
        }
        try {
            return Long.parseLong(o.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    @Generated
    public UserDataScopeApi(UserDataScopeManageService userDataScopeManageService) {
        this.userDataScopeManageService = userDataScopeManageService;
    }
}

