/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysDataScope
 *  com.bsball.repository.SysDataScopeRepository
 *  com.bsball.repository.SysUserTenantRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.UserDataScopeManageService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysDataScope;
import com.bsball.repository.SysDataScopeRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.service.ApiPermissionService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class UserDataScopeManageService {
    private final ApiPermissionService apiPermissionService;
    private final SysDataScopeRepository sysDataScopeRepository;
    private final SysUserTenantRepository sysUserTenantRepository;

    public List<SysDataScope> list(long operatorUserId, long targetUserId, long tenantId) {
        this.requireScopeAccess(operatorUserId, targetUserId, tenantId);
        if (!this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(Long.valueOf(targetUserId), Long.valueOf(tenantId))) {
            throw new BusinessException(400, "\u8be5\u7528\u6237\u4e0d\u5c5e\u4e8e\u6b64\u79df\u6237");
        }
        return this.sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(Long.valueOf(targetUserId), Long.valueOf(tenantId));
    }

    @Transactional
    public void replace(long operatorUserId, long targetUserId, long tenantId, List<Map<String, Object>> items) {
        this.requireScopeAccess(operatorUserId, targetUserId, tenantId);
        if (!this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(Long.valueOf(targetUserId), Long.valueOf(tenantId))) {
            throw new BusinessException(400, "\u8be5\u7528\u6237\u4e0d\u5c5e\u4e8e\u6b64\u79df\u6237");
        }
        this.sysDataScopeRepository.deleteByUserIdAndTenantId(Long.valueOf(targetUserId), Long.valueOf(tenantId));
        if (items == null || items.isEmpty()) {
            return;
        }
        ArrayList<SysDataScope> save = new ArrayList<SysDataScope>();
        for (Map<String, Object> m : items) {
            if (m == null) continue;
            String type = UserDataScopeManageService.stringVal((Object)m.get("scopeType"));
            Long refId = UserDataScopeManageService.longVal((Object)m.get("refId"));
            String expansion = UserDataScopeManageService.stringVal((Object)m.get("expansion"));
            if (type == null || refId == null) {
                throw new BusinessException(400, "scopeType\u3001refId \u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (!"LEAGUE".equals(type) && !"TEAM".equals(type)) {
                throw new BusinessException(400, "scopeType \u987b\u4e3a LEAGUE \u6216 TEAM");
            }
            if (expansion == null || expansion.isBlank()) {
                expansion = "SELF";
            }
            if (!"SELF".equals(expansion) && !"INCLUDE_DESCENDANTS".equals(expansion)) {
                throw new BusinessException(400, "expansion \u987b\u4e3a SELF \u6216 INCLUDE_DESCENDANTS");
            }
            if ("TEAM".equals(type) && "INCLUDE_DESCENDANTS".equals(expansion)) {
                expansion = "SELF";
            }
            SysDataScope row = new SysDataScope();
            row.setUserId(Long.valueOf(targetUserId));
            row.setTenantId(Long.valueOf(tenantId));
            row.setScopeType(type);
            row.setRefId(refId);
            row.setExpansion(expansion);
            save.add(row);
        }
        this.sysDataScopeRepository.saveAll(save);
    }

    private static String stringVal(Object o) {
        return o == null ? null : o.toString().trim();
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

    private void requireScopeAccess(long operatorUserId, long targetUserId, long tenantId) {
        if (this.apiPermissionService.isSuperAdmin(Long.valueOf(operatorUserId))) {
            return;
        }
        if (this.apiPermissionService.isTenantAdmin(Long.valueOf(operatorUserId))) {
            Long cur = CurrentUserHolder.getTenantId();
            if (cur == null || !Objects.equals(tenantId, cur)) {
                throw new BusinessException(403, "\u79df\u6237\u7ba1\u7406\u5458\u4ec5\u80fd\u914d\u7f6e\u5f53\u524d\u79df\u6237\u4e0b\u7684\u6570\u636e\u8303\u56f4");
            }
            if (!this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(Long.valueOf(targetUserId), Long.valueOf(tenantId))) {
                throw new BusinessException(403, "\u76ee\u6807\u7528\u6237\u4e0d\u5c5e\u4e8e\u5f53\u524d\u79df\u6237");
            }
            return;
        }
        throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u6216\u79df\u6237\u7ba1\u7406\u5458\u53ef\u914d\u7f6e\u6570\u636e\u8303\u56f4");
    }

    @Generated
    public UserDataScopeManageService(ApiPermissionService apiPermissionService, SysDataScopeRepository sysDataScopeRepository, SysUserTenantRepository sysUserTenantRepository) {
        this.apiPermissionService = apiPermissionService;
        this.sysDataScopeRepository = sysDataScopeRepository;
        this.sysUserTenantRepository = sysUserTenantRepository;
    }
}

