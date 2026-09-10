/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.CoachOptionDto
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.entity.Coach
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.CoachRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.CoachService
 *  com.bsball.service.DataScopeService
 *  com.bsball.service.PersonnelHistoryRecorder
 *  com.bsball.service.TenantQueryPolicyService
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.CoachOptionDto;
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.entity.Coach;
import com.bsball.model.entity.Team;
import com.bsball.repository.CoachRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.DataScopeService;
import com.bsball.service.PersonnelHistoryRecorder;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class CoachService {
    private final CoachRepository coachRepository;
    private final TeamRepository teamRepository;
    private final DataScopeService dataScopeService;
    private final PersonnelHistoryRecorder personnelHistoryRecorder;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public List<CoachOptionDto> listForSelect() {
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            return this.coachRepository.findAllForSelect();
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (scope.isUnrestrictedInTenant()) {
            return this.coachRepository.findAllForSelectByTenantId(tid);
        }
        if (scope.getTeamIds().isEmpty()) {
            return this.coachRepository.findAllForSelectByTenantIdAndTeamIdIsNull(tid);
        }
        return this.coachRepository.findAllForSelectByTenantIdAndFreeOrTeamIdIn(tid, (Collection)scope.getTeamIds());
    }

    public PageResult<Coach> list(Integer page, Integer pageSize, String keyword, Long teamId) {
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            Pageable gp = this.buildPageable(page, pageSize, null, null);
            Specification & Serializable globalSpec = (Specification & Serializable)(root, q, cb) -> {
                ArrayList<Predicate> preds = new ArrayList<Predicate>();
                preds.add(cb.isNull((Expression)root.get("deletedAt")));
                if (keyword != null && !keyword.isBlank()) {
                    preds.add(cb.like(cb.lower((Expression)root.get("name")), "%" + keyword.toLowerCase() + "%"));
                }
                if (teamId != null) {
                    preds.add(cb.equal((Expression)root.get("teamId"), (Object)teamId));
                }
                return cb.and(preds.toArray(new Predicate[0]));
            };
            Page globalResult = this.coachRepository.findAll((Specification)globalSpec, gp);
            return PageResult.of((List)globalResult.getContent(), (long)globalResult.getTotalElements());
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!(scope.isUnrestrictedInTenant() || teamId == null || scope.getTeamIds().isEmpty() || scope.getTeamIds().contains(teamId))) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        Pageable p = this.buildPageable(page, pageSize, null, null);
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            if (!scope.isUnrestrictedInTenant()) {
                if (scope.getTeamIds().isEmpty()) {
                    preds.add(cb.isNull((Expression)root.get("teamId")));
                } else {
                    preds.add(cb.or((Expression)cb.isNull((Expression)root.get("teamId")), (Expression)root.get("teamId").in((Collection)scope.getTeamIds())));
                }
            }
            if (keyword != null && !keyword.isBlank()) {
                preds.add(cb.like(cb.lower((Expression)root.get("name")), "%" + keyword.toLowerCase() + "%"));
            }
            if (teamId != null) {
                preds.add(cb.equal((Expression)root.get("teamId"), (Object)teamId));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.coachRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public Coach get(Long id) {
        Coach c = this.coachRepository.findById((Object)id).orElse(null);
        if (c == null || c.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(c.getTenantId(), tid)) {
            return null;
        }
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant()) {
            Long coachTeam = c.getTeamId();
            if (coachTeam == null) {
                return c;
            }
            if (scope.getTeamIds().isEmpty() || !scope.getTeamIds().contains(coachTeam)) {
                throw new BusinessException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u6559\u7ec3");
            }
        }
        return c;
    }

    public Coach create(Coach entity) {
        CoachService.normalizeCoachTeamId((Coach)entity);
        this.applyTenantFromTeam(entity);
        this.validateTeamId(entity.getTeamId());
        Coach saved = (Coach)this.coachRepository.save((Object)entity);
        this.personnelHistoryRecorder.afterCoachCreate(saved);
        return saved;
    }

    public Coach update(Long id, Coach entity) {
        Coach existing = this.coachRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6559\u7ec3");
        }
        CoachService.normalizeCoachTeamId((Coach)entity);
        this.applyTenantFromTeam(entity);
        this.validateTeamId(entity.getTeamId());
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        Coach before = PersonnelHistoryRecorder.snapshotCoach((Coach)existing);
        Coach saved = (Coach)this.coachRepository.save((Object)entity);
        this.personnelHistoryRecorder.afterCoachUpdate(before, saved);
        return saved;
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        Coach existing = this.coachRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        if (!Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u6559\u7ec3");
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.coachRepository.save((Object)existing);
    }

    private void applyTenantFromTeam(Coach entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (entity.getTeamId() != null && entity.getTeamId() > 0L) {
            Team team = this.teamRepository.findById((Object)entity.getTeamId()).orElse(null);
            if (team == null) {
                throw new BusinessException(400, "\u7403\u961f\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u521b\u5efa\u7403\u961f");
            }
            if (!Objects.equals(team.getTenantId(), tid)) {
                throw new BusinessException(400, "\u7403\u961f\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
            entity.setTenantId(team.getTenantId());
        } else {
            entity.setTenantId(Long.valueOf(tid));
        }
    }

    private void validateTeamId(Long teamId) {
        if (teamId != null && teamId > 0L && !this.teamRepository.existsById((Object)teamId)) {
            throw new BusinessException(400, "\u7403\u961f\u4e0d\u5b58\u5728\uff0c\u8bf7\u5148\u521b\u5efa\u7403\u961f");
        }
    }

    private static void normalizeCoachTeamId(Coach entity) {
        if (entity == null) {
            return;
        }
        Long id = entity.getTeamId();
        if (id == null || id == 0L) {
            entity.setTeamId(null);
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
    public CoachService(CoachRepository coachRepository, TeamRepository teamRepository, DataScopeService dataScopeService, PersonnelHistoryRecorder personnelHistoryRecorder, TenantQueryPolicyService tenantQueryPolicyService) {
        this.coachRepository = coachRepository;
        this.teamRepository = teamRepository;
        this.dataScopeService = dataScopeService;
        this.personnelHistoryRecorder = personnelHistoryRecorder;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

