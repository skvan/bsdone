/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.entity.HistoryRecord
 *  com.bsball.model.entity.Team
 *  com.bsball.repository.HistoryRecordRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.DataScopeService
 *  com.bsball.service.HistoryRecordService
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
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.entity.HistoryRecord;
import com.bsball.model.entity.Team;
import com.bsball.repository.HistoryRecordRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.DataScopeService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class HistoryRecordService {
    private final HistoryRecordRepository historyRecordRepository;
    private final TeamRepository teamRepository;
    private final DataScopeService dataScopeService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<HistoryRecord> list(Integer page, Integer pageSize, String sortProp, String sortOrder, String targetType, Long targetId, String relatedObjectType, Long relatedObjectId, String type, String recordType, String dateFrom, String dateTo) {
        boolean global = this.tenantQueryPolicyService.isGlobalQueryMode();
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        EffectiveDataScope scope = this.dataScopeService.resolve(CurrentUserHolder.get(), tid);
        if (!scope.isUnrestrictedInTenant() && scope.getLeagueIds().isEmpty() && scope.getTeamIds().isEmpty()) {
            return PageResult.of((List)List.of(), (long)0L);
        }
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        boolean hasFilter = targetType != null && !targetType.isBlank() || targetId != null || relatedObjectId != null || relatedObjectType != null && !relatedObjectType.isBlank() || type != null && !type.isBlank() || recordType != null && !recordType.isBlank() || dateFrom != null && !dateFrom.isBlank() || dateTo != null && !dateTo.isBlank();
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (!global) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            if (hasFilter) {
                if (targetType != null && !targetType.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("targetType"), (Object)targetType));
                }
                if (targetId != null) {
                    preds.add(cb.equal((Expression)root.get("targetId"), (Object)targetId));
                }
                if (relatedObjectType != null && !relatedObjectType.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("relatedObjectType"), (Object)relatedObjectType));
                }
                if (relatedObjectId != null) {
                    preds.add(cb.equal((Expression)root.get("relatedObjectId"), (Object)relatedObjectId));
                }
                if (type != null && !type.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("type"), (Object)type));
                }
                if (recordType != null && !recordType.isBlank()) {
                    preds.add(cb.equal((Expression)root.get("recordType"), (Object)recordType));
                }
                if (dateFrom != null && !dateFrom.isBlank()) {
                    preds.add(cb.greaterThanOrEqualTo((Expression)root.get("changeDate"), (Comparable)((Object)dateFrom)));
                }
                if (dateTo != null && !dateTo.isBlank()) {
                    preds.add(cb.lessThanOrEqualTo((Expression)root.get("changeDate"), (Comparable)((Object)dateTo)));
                }
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.historyRecordRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public HistoryRecord create(HistoryRecord entity) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if ("team".equals(entity.getRelatedObjectType()) && entity.getRelatedObjectId() != null && entity.getRelatedObjectId() > 0L) {
            Team team = this.teamRepository.findById((Object)entity.getRelatedObjectId()).orElse(null);
            if (team == null) {
                throw new BusinessException(400, "\u7403\u961f\u4e0d\u5b58\u5728");
            }
            if (!Objects.equals(team.getTenantId(), tid)) {
                throw new BusinessException(400, "\u7403\u961f\u4e0e\u5f53\u524d\u79df\u6237\u4e0d\u4e00\u81f4");
            }
            entity.setTenantId(team.getTenantId());
        } else {
            entity.setTenantId(Long.valueOf(tid));
        }
        return (HistoryRecord)this.historyRecordRepository.save((Object)entity);
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"changeDate", "id"}));
    }

    @Generated
    public HistoryRecordService(HistoryRecordRepository historyRecordRepository, TeamRepository teamRepository, DataScopeService dataScopeService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.historyRecordRepository = historyRecordRepository;
        this.teamRepository = teamRepository;
        this.dataScopeService = dataScopeService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

