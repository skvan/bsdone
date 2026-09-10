/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysUserTenant
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.repository.SysUserTenantRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SysUserTenantManageService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysUserTenant;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.service.ApiPermissionService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysUserTenantManageService {
    private final ApiPermissionService apiPermissionService;
    private final SysUserTenantRepository sysUserTenantRepository;
    private final SysTenantRepository sysTenantRepository;

    public List<Long> listTenantIds(long operatorUserId, long userId) {
        if (this.apiPermissionService.isSuperAdmin(Long.valueOf(operatorUserId))) {
            return this.sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(Long.valueOf(userId)).stream().map(SysUserTenant::getTenantId).collect(Collectors.toList());
        }
        if (this.apiPermissionService.isTenantAdmin(Long.valueOf(operatorUserId))) {
            Long tid = CurrentUserHolder.getTenantId();
            if (tid == null) {
                throw new BusinessException(403, "\u7f3a\u5c11\u79df\u6237\u4e0a\u4e0b\u6587");
            }
            if (!this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(Long.valueOf(userId), tid)) {
                throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7528\u6237\u7684\u79df\u6237\u6210\u5458");
            }
            return this.sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(Long.valueOf(userId)).stream().map(SysUserTenant::getTenantId).collect(Collectors.toList());
        }
        throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u6216\u79df\u6237\u7ba1\u7406\u5458\u53ef\u67e5\u770b\u7528\u6237\u79df\u6237");
    }

    @Transactional
    public void replace(long operatorUserId, long userId, List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            throw new BusinessException(400, "\u81f3\u5c11\u4fdd\u7559\u4e00\u4e2a\u79df\u6237");
        }
        List distinct = tenantIds.stream().distinct().toList();
        for (Long tid : distinct) {
            if (this.sysTenantRepository.findById((Object)tid).filter(t -> t.getDeletedAt() == null).isPresent()) continue;
            throw new BusinessException(400, "\u79df\u6237\u4e0d\u5b58\u5728: " + tid);
        }
        if (this.apiPermissionService.isSuperAdmin(Long.valueOf(operatorUserId))) {
            this.applyReplace(userId, distinct);
            return;
        }
        if (this.apiPermissionService.isTenantAdmin(Long.valueOf(operatorUserId))) {
            Long cur = CurrentUserHolder.getTenantId();
            if (cur == null) {
                throw new BusinessException(403, "\u7f3a\u5c11\u79df\u6237\u4e0a\u4e0b\u6587");
            }
            if (!this.sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(Long.valueOf(userId), cur)) {
                throw new BusinessException(403, "\u53ea\u80fd\u7ba1\u7406\u672c\u79df\u6237\u5185\u7684\u7528\u6237");
            }
            if (distinct.size() != 1 || !((Long)distinct.get(0)).equals(cur)) {
                throw new BusinessException(403, "\u79df\u6237\u7ba1\u7406\u5458\u4ec5\u53ef\u5c06\u7528\u6237\u7ed1\u5b9a\u5230\u5f53\u524d\u79df\u6237");
            }
            this.applyReplace(userId, distinct);
            return;
        }
        throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u6216\u79df\u6237\u7ba1\u7406\u5458\u53ef\u914d\u7f6e\u7528\u6237\u79df\u6237");
    }

    private void applyReplace(long userId, List<Long> distinct) {
        this.sysUserTenantRepository.deleteByUserIdHard(Long.valueOf(userId));
        ArrayList<SysUserTenant> save = new ArrayList<SysUserTenant>();
        for (Long tid : distinct) {
            SysUserTenant ut = new SysUserTenant();
            ut.setUserId(Long.valueOf(userId));
            ut.setTenantId(tid);
            save.add(ut);
        }
        this.sysUserTenantRepository.saveAll(save);
    }

    @Generated
    public SysUserTenantManageService(ApiPermissionService apiPermissionService, SysUserTenantRepository sysUserTenantRepository, SysTenantRepository sysTenantRepository) {
        this.apiPermissionService = apiPermissionService;
        this.sysUserTenantRepository = sysUserTenantRepository;
        this.sysTenantRepository = sysTenantRepository;
    }
}

