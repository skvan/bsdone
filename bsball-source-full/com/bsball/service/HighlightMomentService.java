/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.HighlightMoment
 *  com.bsball.repository.HighlightMomentRepository
 *  com.bsball.service.HighlightMomentService
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
import com.bsball.model.entity.HighlightMoment;
import com.bsball.repository.HighlightMomentRepository;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
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
public class HighlightMomentService {
    private final HighlightMomentRepository highlightMomentRepository;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<HighlightMoment> list(Integer page, Integer pageSize, String sortProp, String sortOrder, String subjectType, Long subjectId, String mediaType, String status) {
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            if (tid != null) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (subjectType != null && !subjectType.isBlank()) {
                preds.add(cb.equal((Expression)root.get("subjectType"), (Object)subjectType));
            }
            if (subjectId != null) {
                preds.add(cb.equal((Expression)root.get("subjectId"), (Object)subjectId));
            }
            if (mediaType != null && !mediaType.isBlank()) {
                preds.add(cb.equal((Expression)root.get("mediaType"), (Object)mediaType));
            }
            if (status != null && !status.isBlank()) {
                preds.add(cb.equal((Expression)root.get("status"), (Object)status));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.highlightMomentRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public HighlightMoment create(HighlightMoment body) {
        body.setTenantId(Long.valueOf(this.tenantQueryPolicyService.requiredTenantId()));
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus("published");
        }
        return (HighlightMoment)this.highlightMomentRepository.save((Object)body);
    }

    public HighlightMoment update(Long id, HighlightMoment body) {
        HighlightMoment existing = this.highlightMomentRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new BusinessException(404, "\u9ad8\u5149\u65f6\u523b\u4e0d\u5b58\u5728");
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u9ad8\u5149\u65f6\u523b");
        }
        body.setId(id);
        body.setTenantId(Long.valueOf(tid));
        body.setCreatedAt(existing.getCreatedAt());
        body.setCreatedBy(existing.getCreatedBy());
        body.setDeletedAt(existing.getDeletedAt());
        body.setDeletedBy(existing.getDeletedBy());
        if (body.getStatus() == null || body.getStatus().isBlank()) {
            body.setStatus(existing.getStatus());
        }
        return (HighlightMoment)this.highlightMomentRepository.save((Object)body);
    }

    public void delete(Long id) {
        HighlightMoment existing = this.highlightMomentRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (!Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u9ad8\u5149\u65f6\u523b");
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.highlightMomentRepository.save((Object)existing);
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"pinned", "sortWeight", "happenedAt", "id"}));
    }

    @Generated
    public HighlightMomentService(HighlightMomentRepository highlightMomentRepository, TenantQueryPolicyService tenantQueryPolicyService) {
        this.highlightMomentRepository = highlightMomentRepository;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

