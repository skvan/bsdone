/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.repository.LeagueRepository
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SysTenantManageService
 *  com.bsball.util.TenantCodeValidator
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysTenant;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.util.TenantCodeValidator;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysTenantManageService {
    public static final long DEFAULT_TENANT_ID = 1L;
    private final ApiPermissionService apiPermissionService;
    private final SysTenantRepository sysTenantRepository;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;

    public PageResult<SysTenant> list(long operatorUserId, Integer page, Integer pageSize, String keyword) {
        Page result;
        this.requireSuperAdmin(operatorUserId);
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        PageRequest pg = PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"sort"}).and(Sort.by((String[])new String[]{"id"})));
        if (keyword == null || keyword.isBlank()) {
            result = this.sysTenantRepository.findByDeletedAtIsNull((Pageable)pg);
        } else {
            String kw = keyword.trim();
            result = this.sysTenantRepository.findByDeletedAtIsNullAndKeyword(kw, (Pageable)pg);
        }
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysTenant get(long operatorUserId, Long id) {
        this.requireSuperAdmin(operatorUserId);
        return this.sysTenantRepository.findById((Object)id).filter(t -> t.getDeletedAt() == null).orElse(null);
    }

    @Transactional
    public SysTenant create(long operatorUserId, SysTenant entity) {
        this.requireSuperAdmin(operatorUserId);
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            throw new BusinessException(400, "\u79df\u6237\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (entity.getName() == null || entity.getName().isBlank()) {
            throw new BusinessException(400, "\u79df\u6237\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String code = entity.getCode().trim();
        TenantCodeValidator.validateNewCode((String)code);
        if (this.sysTenantRepository.existsByCodeAndDeletedAtIsNull(code)) {
            throw new BusinessException(400, "\u79df\u6237\u7f16\u7801\u5df2\u5b58\u5728");
        }
        entity.setId(null);
        entity.setCode(code);
        entity.setName(entity.getName().trim());
        if (entity.getStatus() == null) {
            entity.setStatus(Integer.valueOf(1));
        }
        if (entity.getSort() == null) {
            entity.setSort(Integer.valueOf(0));
        }
        if (entity.getDescription() != null) {
            String d = entity.getDescription().trim();
            entity.setDescription(d.isEmpty() ? null : d);
        }
        return (SysTenant)this.sysTenantRepository.save((Object)entity);
    }

    @Transactional
    public SysTenant update(long operatorUserId, Long id, SysTenant entity) {
        String newCode;
        this.requireSuperAdmin(operatorUserId);
        SysTenant existing = this.sysTenantRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BusinessException(404, "\u79df\u6237\u4e0d\u5b58\u5728");
        }
        if (entity.getName() != null && !entity.getName().isBlank()) {
            existing.setName(entity.getName().trim());
        }
        if (entity.getStatus() != null) {
            existing.setStatus(entity.getStatus());
        }
        if (entity.getSort() != null) {
            existing.setSort(entity.getSort());
        }
        if (entity.getCode() != null && !entity.getCode().isBlank() && !(newCode = entity.getCode().trim()).equals(existing.getCode())) {
            TenantCodeValidator.validateNewCode((String)newCode);
            if (this.sysTenantRepository.existsByCodeAndDeletedAtIsNullAndIdNot(newCode, id)) {
                throw new BusinessException(400, "\u79df\u6237\u7f16\u7801\u5df2\u5b58\u5728");
            }
            existing.setCode(newCode);
        }
        if (entity.getDescription() != null) {
            String d = entity.getDescription().trim();
            existing.setDescription(d.isEmpty() ? null : d);
        }
        existing.setLeaseStartDate(entity.getLeaseStartDate());
        existing.setLeaseEndDate(entity.getLeaseEndDate());
        return (SysTenant)this.sysTenantRepository.save((Object)existing);
    }

    @Transactional
    public void delete(long operatorUserId, Long id) {
        this.requireSuperAdmin(operatorUserId);
        if (1L == id) {
            throw new BusinessException(400, "\u4e0d\u80fd\u5220\u9664\u9ed8\u8ba4\u79df\u6237");
        }
        SysTenant t = this.sysTenantRepository.findById((Object)id).orElse(null);
        if (t == null || t.getDeletedAt() != null) {
            throw new BusinessException(404, "\u79df\u6237\u4e0d\u5b58\u5728");
        }
        LocalDateTime now = LocalDateTime.now();
        t.setDeletedAt(now);
        t.setDeletedBy(Long.valueOf(operatorUserId));
        this.sysTenantRepository.save((Object)t);
    }

    public Map<String, Object> scopeOptions(long operatorUserId, long tenantId) {
        if (!this.apiPermissionService.isSuperAdmin(Long.valueOf(operatorUserId))) {
            if (!this.apiPermissionService.isTenantAdmin(Long.valueOf(operatorUserId))) {
                throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u6216\u79df\u6237\u7ba1\u7406\u5458\u53ef\u67e5\u770b\u6570\u636e\u8303\u56f4\u9009\u9879");
            }
            Long cur = CurrentUserHolder.getTenantId();
            if (cur == null || !Objects.equals(tenantId, cur)) {
                throw new BusinessException(403, "\u79df\u6237\u7ba1\u7406\u5458\u4ec5\u80fd\u67e5\u770b\u5f53\u524d\u79df\u6237\u7684\u6570\u636e\u8303\u56f4\u9009\u9879");
            }
        }
        List leagues = this.leagueRepository.findByTenantIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long.valueOf(tenantId));
        List teams = this.teamRepository.findByTenantIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long.valueOf(tenantId));
        List leagueRows = leagues.stream().map(l -> {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("id", l.getId());
            m.put("name", l.getName());
            return m;
        }).collect(Collectors.toList());
        List teamRows = teams.stream().map(t -> {
            HashMap<String, Object> m = new HashMap<String, Object>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("leagueId", t.getLeagueId());
            return m;
        }).collect(Collectors.toList());
        HashMap<String, Object> out = new HashMap<String, Object>();
        out.put("leagues", leagueRows);
        out.put("teams", teamRows);
        return out;
    }

    private void requireSuperAdmin(long userId) {
        if (!this.apiPermissionService.isSuperAdmin(Long.valueOf(userId))) {
            throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u53ef\u64cd\u4f5c\u79df\u6237");
        }
    }

    @Generated
    public SysTenantManageService(ApiPermissionService apiPermissionService, SysTenantRepository sysTenantRepository, LeagueRepository leagueRepository, TeamRepository teamRepository) {
        this.apiPermissionService = apiPermissionService;
        this.sysTenantRepository = sysTenantRepository;
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
    }
}

