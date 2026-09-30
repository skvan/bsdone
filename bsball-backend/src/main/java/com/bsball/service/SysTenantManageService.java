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
import com.bsball.model.entity.SysOperationLog;
import com.bsball.model.entity.League;
import com.bsball.model.entity.Team;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final TenantAccessGuard tenantAccessGuard;
    private final TenantResolutionService tenantResolutionService;
    private final OperationLogAsyncService operationLogAsyncService;
    @Generated
    private static final Logger log = LoggerFactory.getLogger(SysTenantManageService.class);

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
        return this.sysTenantRepository.findById(id).filter(t -> t.getDeletedAt() == null).orElse(null);
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
        return (SysTenant)this.sysTenantRepository.save(entity);
    }

    @Transactional
    public SysTenant update(long operatorUserId, Long id, SysTenant entity) {
        String newCode;
        this.requireSuperAdmin(operatorUserId);
        SysTenant existing = this.sysTenantRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BusinessException(404, "\u79df\u6237\u4e0d\u5b58\u5728");
        }
        Integer statusBefore = existing.getStatus();
        String reason = entity.getDescription();
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
        SysTenant saved = (SysTenant)this.sysTenantRepository.save(existing);
        this.applyTenantStatusSideEffects(operatorUserId, saved, statusBefore, reason);
        return saved;
    }

    @Transactional
    public void delete(long operatorUserId, Long id) {
        this.requireSuperAdmin(operatorUserId);
        if (1L == id) {
            throw new BusinessException(400, "\u4e0d\u80fd\u5220\u9664\u9ed8\u8ba4\u79df\u6237");
        }
        SysTenant t = this.sysTenantRepository.findById(id).orElse(null);
        if (t == null || t.getDeletedAt() != null) {
            throw new BusinessException(404, "\u79df\u6237\u4e0d\u5b58\u5728");
        }
        LocalDateTime now = LocalDateTime.now();
        t.setDeletedAt(now);
        t.setDeletedBy(Long.valueOf(operatorUserId));
        this.sysTenantRepository.save(t);
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
        List<League> leagues = this.leagueRepository.findByTenantIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long.valueOf(tenantId));
        List<Team> teams = this.teamRepository.findByTenantIdAndDeletedAtIsNullOrderBySortAscIdAsc(Long.valueOf(tenantId));
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

    /**
     * 退租(1→0)/续租(0→1)侧效：失效可用性缓存 + 码缓存，并按现状审计机制留痕（操作者/时间/原因）。
     */
    private void applyTenantStatusSideEffects(long operatorUserId, SysTenant tenant, Integer statusBefore, String reason) {
        Integer statusAfter = tenant.getStatus();
        boolean retired = SysTenantManageService.isStatus(statusBefore, 1) && SysTenantManageService.isStatus(statusAfter, 0);
        boolean renewed = SysTenantManageService.isStatus(statusBefore, 0) && SysTenantManageService.isStatus(statusAfter, 1);
        if (!retired && !renewed) {
            return;
        }
        this.tenantAccessGuard.evict(tenant.getId());
        this.tenantResolutionService.evictTenantByCode(tenant.getCode());
        this.recordTenantStatusChange(operatorUserId, tenant, retired, reason);
    }

    private static boolean isStatus(Integer status, int expected) {
        return status != null && status == expected;
    }

    /** 接入现状操作审计（SysOperationLog + OperationLogAsyncService），记录退租/续租操作者、时间与原因。 */
    private void recordTenantStatusChange(long operatorUserId, SysTenant tenant, boolean retired, String reason) {
        try {
            SysOperationLog entry = new SysOperationLog();
            entry.setTenantId(tenant.getId());
            entry.setUserId(Long.valueOf(operatorUserId));
            entry.setModule("\u79df\u6237\u7ba1\u7406");
            entry.setAction(retired ? "\u9000\u79df" : "\u7eed\u79df");
            entry.setTargetType("tenant");
            entry.setTargetId(tenant.getId());
            entry.setDescription(reason != null && !reason.isBlank() ? reason.trim() : (retired ? "\u79df\u6237\u5df2\u505c\u6b62\u8fd0\u8425" : "\u79df\u6237\u5df2\u6062\u590d\u8fd0\u8425"));
            entry.setCreatedAt(LocalDateTime.now());
            this.operationLogAsyncService.saveAsync(entry);
        }
        catch (Exception e) {
            log.debug("\u79df\u6237\u542f\u505c\u7559\u75d5\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    private void requireSuperAdmin(long userId) {
        if (!this.apiPermissionService.isSuperAdmin(Long.valueOf(userId))) {
            throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u53ef\u64cd\u4f5c\u79df\u6237");
        }
    }

    @Generated
    public SysTenantManageService(ApiPermissionService apiPermissionService, SysTenantRepository sysTenantRepository, LeagueRepository leagueRepository, TeamRepository teamRepository, TenantAccessGuard tenantAccessGuard, TenantResolutionService tenantResolutionService, OperationLogAsyncService operationLogAsyncService) {
        this.apiPermissionService = apiPermissionService;
        this.sysTenantRepository = sysTenantRepository;
        this.leagueRepository = leagueRepository;
        this.teamRepository = teamRepository;
        this.tenantAccessGuard = tenantAccessGuard;
        this.tenantResolutionService = tenantResolutionService;
        this.operationLogAsyncService = operationLogAsyncService;
    }
}

