/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.entity.Event
 *  com.bsball.model.entity.League
 *  com.bsball.repository.EventRepository
 *  com.bsball.repository.LeagueRepository
 *  com.bsball.service.DataScopeService
 *  com.bsball.service.EventService
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
import com.bsball.model.entity.Event;
import com.bsball.model.entity.League;
import com.bsball.repository.EventRepository;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.DataScopeService;
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
public class EventService {
    private final EventRepository eventRepository;
    private final LeagueRepository leagueRepository;
    private final DataScopeService dataScopeService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<Event> list(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        Page result;
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            Pageable gp = this.buildPageable(page, pageSize, sortProp, sortOrder);
            Page globalResult = this.eventRepository.findByDeletedAtIsNull(gp);
            return PageResult.of((List)globalResult.getContent(), (long)globalResult.getTotalElements());
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (scope.isUnrestrictedInTenant()) {
            result = this.eventRepository.findByTenantIdAndDeletedAtIsNull(Long.valueOf(tid), p);
        } else {
            if (scope.getLeagueIds().isEmpty()) {
                return PageResult.of((List)List.of(), (long)0L);
            }
            result = this.eventRepository.findByTenantIdAndLeagueIdInAndDeletedAtIsNull(Long.valueOf(tid), (Collection)scope.getLeagueIds(), p);
        }
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public Event get(Long id) {
        Event entity = this.eventRepository.findById((Object)id).orElse(null);
        if (entity == null || entity.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(entity.getTenantId(), tid)) {
            return null;
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && entity.getLeagueId() != null && !scope.canReadLeague(entity.getLeagueId().longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u8d5b\u4e8b");
        }
        return entity;
    }

    public Event create(Event entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (entity.getLeagueId() != null) {
            League league = this.leagueRepository.findById((Object)entity.getLeagueId()).orElse(null);
            if (league == null || league.getDeletedAt() != null || !Objects.equals(league.getTenantId(), tid)) {
                throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
            }
            EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
            if (!scope.isUnrestrictedInTenant() && !scope.canReadLeague(entity.getLeagueId().longValue())) {
                throw new BusinessException(403, "\u65e0\u6743\u5728\u8be5\u8054\u76df\u4e0b\u521b\u5efa\u8d5b\u4e8b");
            }
        }
        entity.setTenantId(Long.valueOf(tid));
        return (Event)this.eventRepository.save((Object)entity);
    }

    public Event update(Long id, Event entity) {
        League league;
        Long leagueId;
        Event existing = this.eventRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u8d5b\u4e8b");
        }
        Long l = leagueId = entity.getLeagueId() != null ? entity.getLeagueId() : existing.getLeagueId();
        if (!(leagueId == null || (league = (League)this.leagueRepository.findById((Object)leagueId).orElse(null)) != null && league.getDeletedAt() == null && Objects.equals(league.getTenantId(), tid))) {
            throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728\u6216\u65e0\u6743\u4f7f\u7528");
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant()) {
            boolean newOk;
            boolean oldOk = existing.getLeagueId() == null || scope.canReadLeague(existing.getLeagueId().longValue());
            boolean bl = newOk = leagueId == null || scope.canReadLeague(leagueId.longValue());
            if (!oldOk || !newOk) {
                throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u8d5b\u4e8b");
            }
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        return (Event)this.eventRepository.save((Object)entity);
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Event existing = this.eventRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u8d5b\u4e8b");
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && existing.getLeagueId() != null && !scope.canReadLeague(existing.getLeagueId().longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u8d5b\u4e8b");
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.eventRepository.save((Object)existing);
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
    public EventService(EventRepository eventRepository, LeagueRepository leagueRepository, DataScopeService dataScopeService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.eventRepository = eventRepository;
        this.leagueRepository = leagueRepository;
        this.dataScopeService = dataScopeService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

