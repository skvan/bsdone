/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.League
 *  com.bsball.repository.LeagueRepository
 *  com.bsball.service.LeagueService
 *  com.bsball.service.PersonnelHistoryRecorder
 *  com.bsball.service.TenantQueryPolicyService
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
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.League;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueService {
    private final LeagueRepository leagueRepository;
    private final AccountScopeService accountScopeService;
    private final ScopeQuerySupport scopeQuerySupport;
    private final ResourceGuard resourceGuard;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final TenantQueryPolicyService tenantQueryPolicyService;
    private final LeagueProvisionService leagueProvisionService;
    private final ApiPermissionService apiPermissionService;

    public PageResult<League> list(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        Page result;
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            Pageable gp = this.buildPageable(page, pageSize, sortProp, sortOrder);
            Page globalResult = this.leagueRepository.findByDeletedAtIsNull(gp);
            return PageResult.of((List)globalResult.getContent(), (long)globalResult.getTotalElements());
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleLeagueIds = this.scopeQuerySupport.visibleLeagueIds(scope);
        if (visibleLeagueIds != null && visibleLeagueIds.isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        if (visibleLeagueIds != null) {
            result = this.leagueRepository.findByTenantIdAndIdInAndDeletedAtIsNull(Long.valueOf(tid), visibleLeagueIds, p);
        } else {
            result = this.leagueRepository.findByTenantIdAndDeletedAtIsNull(Long.valueOf(tid), p);
        }
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public League get(Long id) {
        League league = this.leagueRepository.findById(id).orElse(null);
        if (league == null || league.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(league.getTenantId(), tid)) {
            return null;
        }
        EffectiveScope scope = this.accountScopeService.resolveCurrent();
        List<Long> visibleLeagueIds = this.scopeQuerySupport.visibleLeagueIds(scope);
        if (visibleLeagueIds != null && !visibleLeagueIds.contains(id)) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u8054\u76df");
        }
        return league;
    }

    public League create(League entity) {
        return this.createInternal(entity);
    }

    /**
     * 门户自助建联盟入口（批次 3a，Task 3.3）：按当前登录用户角色分派。
     * 门户角色（member / team_manager / league_organizer）且非超管 / 租管 → 走审批链路
     * （LeagueProvisionService.submitOrCreate，按租户开关落申请或直建）；
     * 其余（超管 / 租管 / 未登录 / 无门户角色）→ 直建并返回。
     */
    public Map<String, Object> createForCurrentUser(League body) {
        Long uid = CurrentUserHolder.get();
        if (uid != null
                && !this.apiPermissionService.isSuperAdmin(uid)
                && !this.apiPermissionService.isTenantAdmin(uid)
                && this.apiPermissionService.hasAnyRoleCode(uid, "member", "team_manager", "league_organizer")) {
            return this.leagueProvisionService.submitOrCreate(uid, body);
        }
        League created = this.createInternal(body);
        return Map.of("pending", false, "id", created.getId());
    }

    /** 供批次 3 自助建联盟流程复用（本批仅拆分，行为不变）：租户取自当前请求上下文。 */
    public League createInternal(League entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        return this.createInternalForTenant(entity, tid);
    }

    /**
     * 指定租户建联盟（批次 3a 评审修复 I1）：供跨租户审批（LeagueProvisionService.approve）复用，
     * 确保联盟落库租户 == 申请租户，而非审核者上下文租户（超管全局 token 下上下文租户为 0）。
     * createInternal 保持原语义（上下文租户）；本方法仅新增显式租户入参，不改变原路径行为。
     */
    public League createInternalForTenant(League entity, long tenantId) {
        entity.setTenantId(Long.valueOf(tenantId));
        String name = entity.getName() == null ? "" : entity.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException(400, "\u8054\u76df\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        entity.setName(name);
        if (this.leagueRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(Long.valueOf(tenantId), name)) {
            throw new BusinessException(400, "\u8054\u76df\u540d\u79f0\u5df2\u5b58\u5728");
        }
        return (League)this.leagueRepository.save(entity);
    }

    public League update(Long id, League entity) {
        String name;
        League existing = this.leagueRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u8054\u76df");
        }
        this.resourceGuard.assertCanManageLeague(id);
        String string = name = entity.getName() == null ? "" : entity.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException(400, "\u8054\u76df\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        entity.setName(name);
        if (this.leagueRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(Long.valueOf(tid), name, id)) {
            throw new BusinessException(400, "\u8054\u76df\u540d\u79f0\u5df2\u5b58\u5728");
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        League before = PersonnelHistoryRecorder.snapshotLeague((League)existing);
        League saved = (League)this.leagueRepository.save(entity);
        this.personnelHistoryRecorder.afterLeagueUpdate(before, saved);
        return saved;
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        League existing = this.leagueRepository.findById(id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u8054\u76df");
        }
        this.resourceGuard.assertCanManageLeague(id);
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.leagueRepository.save(existing);
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps);
    }

    @Generated
    public LeagueService(LeagueRepository leagueRepository, AccountScopeService accountScopeService, ScopeQuerySupport scopeQuerySupport, ResourceGuard resourceGuard, PersonnelHistoryRecorder personnelHistoryRecorder, TenantQueryPolicyService tenantQueryPolicyService, LeagueProvisionService leagueProvisionService, ApiPermissionService apiPermissionService) {
        this.leagueRepository = leagueRepository;
        this.accountScopeService = accountScopeService;
        this.scopeQuerySupport = scopeQuerySupport;
        this.resourceGuard = resourceGuard;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
        this.leagueProvisionService = leagueProvisionService;
        this.apiPermissionService = apiPermissionService;
    }
}

