/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.entity.League
 *  com.bsball.repository.LeagueRepository
 *  com.bsball.service.DataScopeService
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
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.entity.League;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.DataScopeService;
import com.bsball.service.PersonnelHistoryRecorder;
import com.bsball.service.TenantQueryPolicyService;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
    private final DataScopeService dataScopeService;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<League> list(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        Page result;
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            Pageable gp = this.buildPageable(page, pageSize, sortProp, sortOrder);
            Page globalResult = this.leagueRepository.findByDeletedAtIsNull(gp);
            return PageResult.of((List)globalResult.getContent(), (long)globalResult.getTotalElements());
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (scope.isUnrestrictedInTenant()) {
            result = this.leagueRepository.findByTenantIdAndDeletedAtIsNull(Long.valueOf(tid), p);
        } else {
            if (scope.getLeagueIds().isEmpty()) {
                return PageResult.of((List)List.of(), (long)0L);
            }
            result = this.leagueRepository.findByTenantIdAndIdInAndDeletedAtIsNull(Long.valueOf(tid), (Collection)scope.getLeagueIds(), p);
        }
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public League get(Long id) {
        League league = this.leagueRepository.findById((Object)id).orElse(null);
        if (league == null || league.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(league.getTenantId(), tid)) {
            return null;
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && !scope.canReadLeague(id.longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u8054\u76df");
        }
        return league;
    }

    public League create(League entity) {
        String name;
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        entity.setTenantId(Long.valueOf(tid));
        String string = name = entity.getName() == null ? "" : entity.getName().trim();
        if (name.isEmpty()) {
            throw new BusinessException(400, "\u8054\u76df\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        entity.setName(name);
        if (this.leagueRepository.existsByTenantIdAndNameIgnoreCaseAndDeletedAtIsNull(Long.valueOf(tid), name)) {
            throw new BusinessException(400, "\u8054\u76df\u540d\u79f0\u5df2\u5b58\u5728");
        }
        return (League)this.leagueRepository.save((Object)entity);
    }

    public League update(Long id, League entity) {
        String name;
        League existing = this.leagueRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u8054\u76df");
        }
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
        League saved = (League)this.leagueRepository.save((Object)entity);
        this.personnelHistoryRecorder.afterLeagueUpdate(before, saved);
        return saved;
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        League existing = this.leagueRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u8054\u76df");
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.leagueRepository.save((Object)existing);
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
    public LeagueService(LeagueRepository leagueRepository, DataScopeService dataScopeService, PersonnelHistoryRecorder personnelHistoryRecorder, TenantQueryPolicyService tenantQueryPolicyService) {
        this.leagueRepository = leagueRepository;
        this.dataScopeService = dataScopeService;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

