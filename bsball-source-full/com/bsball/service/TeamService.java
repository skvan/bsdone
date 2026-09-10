/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.dto.TeamOptionDto
 *  com.bsball.model.entity.League
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.LeagueRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.DataScopeService
 *  com.bsball.service.PersonnelHistoryRecorder
 *  com.bsball.service.TeamService
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
import com.bsball.model.dto.TeamOptionDto;
import com.bsball.model.entity.League;
import com.bsball.model.entity.Team;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.TeamRepository;
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

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final DataScopeService dataScopeService;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<Team> list(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        Page result;
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            Pageable gp = this.buildPageable(page, pageSize, sortProp, sortOrder);
            Page globalResult = this.teamRepository.findByDeletedAtIsNull(gp);
            return PageResult.of((List)globalResult.getContent(), (long)globalResult.getTotalElements());
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (scope.isUnrestrictedInTenant()) {
            result = this.teamRepository.findByTenantIdAndDeletedAtIsNull(Long.valueOf(tid), p);
        } else {
            if (scope.getTeamIds().isEmpty()) {
                return PageResult.of((List)List.of(), (long)0L);
            }
            result = this.teamRepository.findByTenantIdAndIdInAndDeletedAtIsNull(Long.valueOf(tid), (Collection)scope.getTeamIds(), p);
        }
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public List<TeamOptionDto> listForSelect() {
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            return this.teamRepository.findAllForSelect();
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (scope.isUnrestrictedInTenant()) {
            return this.teamRepository.findForSelectByTenantId(Long.valueOf(tid));
        }
        if (scope.getTeamIds().isEmpty()) {
            return List.of();
        }
        return this.teamRepository.findForSelectByTenantIdAndIdIn(Long.valueOf(tid), (Collection)scope.getTeamIds());
    }

    public Team get(Long id) {
        Team t = this.teamRepository.findById((Object)id).orElse(null);
        if (t == null || t.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(t.getTenantId(), tid)) {
            return null;
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && !scope.canReadTeam(id.longValue())) {
            throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u7403\u961f");
        }
        return t;
    }

    public Team create(Team entity) {
        TeamService.normalizeBlankStringsToNull((Team)entity);
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        entity.setTenantId(Long.valueOf(tid));
        if (entity.getLeagueId() != null) {
            League league = this.leagueRepository.findById((Object)entity.getLeagueId()).orElse(null);
            if (league == null || league.getDeletedAt() != null) {
                throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728");
            }
            if (!Objects.equals(league.getTenantId(), tid)) {
                throw new BusinessException(400, "\u8054\u76df\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
        }
        return (Team)this.teamRepository.save((Object)entity);
    }

    public Team update(Long id, Team entity) {
        Team existing = this.teamRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u7403\u961f");
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setTenantId(Long.valueOf(tid));
        TeamService.normalizeBlankStringsToNull((Team)entity);
        if (entity.getLeagueId() != null) {
            League league = this.leagueRepository.findById((Object)entity.getLeagueId()).orElse(null);
            if (league == null || league.getDeletedAt() != null) {
                throw new BusinessException(400, "\u8054\u76df\u4e0d\u5b58\u5728");
            }
            if (!Objects.equals(league.getTenantId(), tid)) {
                throw new BusinessException(400, "\u8054\u76df\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
        }
        Team before = PersonnelHistoryRecorder.snapshotTeam((Team)existing);
        Team saved = (Team)this.teamRepository.save((Object)entity);
        this.personnelHistoryRecorder.afterTeamUpdate(before, saved);
        return saved;
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Team existing = this.teamRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u7403\u961f");
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.teamRepository.save((Object)existing);
    }

    private static void normalizeBlankStringsToNull(Team t) {
        if (t == null) {
            return;
        }
        if (t.getName() != null && t.getName().isBlank()) {
            t.setName(null);
        }
        if (t.getNameEn() != null && t.getNameEn().isBlank()) {
            t.setNameEn(null);
        }
        if (t.getShortName() != null && t.getShortName().isBlank()) {
            t.setShortName(null);
        }
        if (t.getLogo() != null && t.getLogo().isBlank()) {
            t.setLogo(null);
        }
        if (t.getWordmark() != null && t.getWordmark().isBlank()) {
            t.setWordmark(null);
        }
        if (t.getBgImage() != null && t.getBgImage().isBlank()) {
            t.setBgImage(null);
        }
        if (t.getCity() != null && t.getCity().isBlank()) {
            t.setCity(null);
        }
        if (t.getStadium() != null && t.getStadium().isBlank()) {
            t.setStadium(null);
        }
        if (t.getDescription() != null && t.getDescription().isBlank()) {
            t.setDescription(null);
        }
        if (t.getContactPhone() != null && t.getContactPhone().isBlank()) {
            t.setContactPhone(null);
        }
        if (t.getContactEmail() != null && t.getContactEmail().isBlank()) {
            t.setContactEmail(null);
        }
        if (t.getContactPerson() != null && t.getContactPerson().isBlank()) {
            t.setContactPerson(null);
        }
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
    public TeamService(TeamRepository teamRepository, LeagueRepository leagueRepository, DataScopeService dataScopeService, PersonnelHistoryRecorder personnelHistoryRecorder, TenantQueryPolicyService tenantQueryPolicyService) {
        this.teamRepository = teamRepository;
        this.leagueRepository = leagueRepository;
        this.dataScopeService = dataScopeService;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

