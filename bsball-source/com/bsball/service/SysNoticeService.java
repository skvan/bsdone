/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.SysNoticeCreateRequest
 *  com.bsball.model.dto.SysNoticeCreateResult
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysNotice
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.repository.SysNoticeRepository
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SysNoticeService
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
import com.bsball.model.dto.SysNoticeCreateRequest;
import com.bsball.model.dto.SysNoticeCreateResult;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysNotice;
import com.bsball.model.entity.SysTenant;
import com.bsball.repository.SysNoticeRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

@Service
public class SysNoticeService {
    private final SysNoticeRepository sysNoticeRepository;
    private final SysTenantRepository sysTenantRepository;
    private final ApiPermissionService apiPermissionService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<SysNotice> list(Integer page, Integer pageSize, String sortProp, String sortOrder, String target, String keyword) {
        boolean hasKeyword;
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder);
        boolean hasTarget = target != null && !target.isBlank() && !"all".equals(target);
        boolean bl = hasKeyword = keyword != null && !keyword.isBlank();
        if (tid == null && !hasTarget && !hasKeyword) {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> cb.isNull((Expression)root.get("deletedAt"));
            Page result = this.sysNoticeRepository.findAll((Specification)spec, p);
            return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
        }
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            if (tid != null) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            if (hasTarget) {
                preds.add(cb.or((Expression)cb.equal((Expression)root.get("target"), (Object)target), (Expression)cb.equal((Expression)root.get("target"), (Object)"both")));
            }
            if (hasKeyword) {
                String k = "%" + keyword.toLowerCase() + "%";
                preds.add(cb.or((Expression)cb.like(cb.lower((Expression)root.get("title")), k), (Expression)cb.like(cb.lower((Expression)root.get("content")), k)));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.sysNoticeRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysNotice get(Long id) {
        SysNotice a = this.sysNoticeRepository.findById((Object)id).orElse(null);
        if (a == null || a.getDeletedAt() != null) {
            return null;
        }
        Long opId = CurrentUserHolder.get();
        if (opId != null && this.apiPermissionService.isSuperAdmin(opId)) {
            return a;
        }
        if (!Objects.equals(a.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            return null;
        }
        return a;
    }

    @Transactional(rollbackFor={Exception.class})
    public SysNoticeCreateResult create(SysNoticeCreateRequest req) {
        boolean superUser;
        String target;
        String string = target = req.getTarget() != null ? req.getTarget().trim() : "portal";
        if (!("admin".equals(target) || "portal".equals(target) || "both".equals(target))) {
            throw new IllegalArgumentException("target \u987b\u4e3a admin\u3001portal \u6216 both");
        }
        Long opId = CurrentUserHolder.get();
        boolean bl = superUser = opId != null && this.apiPermissionService.isSuperAdmin(opId);
        if (superUser) {
            List tids = this.resolveCreateTargetTenantIds(req.getTenantIds());
            if (tids.isEmpty()) {
                throw new BusinessException(400, "\u6ca1\u6709\u53ef\u53d1\u5e03\u7684\u542f\u7528\u79df\u6237");
            }
            long firstId = 0L;
            for (Long tid : tids) {
                SysNotice n = new SysNotice();
                n.setTenantId(tid);
                n.setTitle(req.getTitle().trim());
                n.setContent(req.getContent());
                n.setTarget(target);
                SysNotice saved = (SysNotice)this.sysNoticeRepository.save((Object)n);
                if (firstId != 0L) continue;
                firstId = saved.getId();
            }
            return new SysNoticeCreateResult(firstId, tids.size());
        }
        if (req.getTenantIds() != null && !req.getTenantIds().isEmpty()) {
            throw new BusinessException(403, "\u4ec5\u8d85\u7ea7\u7ba1\u7406\u5458\u53ef\u6307\u5b9a\u53d1\u5e03\u79df\u6237\u5217\u8868");
        }
        SysNotice entity = new SysNotice();
        entity.setTitle(req.getTitle().trim());
        entity.setContent(req.getContent());
        entity.setTarget(target);
        entity.setTenantId(Long.valueOf(this.tenantQueryPolicyService.requiredTenantId()));
        SysNotice saved = (SysNotice)this.sysNoticeRepository.save((Object)entity);
        return new SysNoticeCreateResult(saved.getId().longValue(), 1);
    }

    private List<Long> resolveCreateTargetTenantIds(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return this.listActiveTenantIds();
        }
        LinkedHashSet<Long> uniq = new LinkedHashSet<Long>();
        for (Long id : tenantIds) {
            if (id == null || id <= 0L) continue;
            uniq.add(id);
        }
        if (uniq.isEmpty()) {
            return this.listActiveTenantIds();
        }
        ArrayList<Long> out = new ArrayList<Long>();
        for (Long id : uniq) {
            SysTenant t = this.sysTenantRepository.findById((Object)id).orElse(null);
            if (t == null || t.getDeletedAt() != null) {
                throw new BusinessException(400, "\u79df\u6237\u4e0d\u5b58\u5728\u6216\u5df2\u5220\u9664\uff1a" + id);
            }
            if (t.getStatus() != null && t.getStatus() != 1) {
                throw new BusinessException(400, "\u79df\u6237\u5df2\u505c\u7528\uff0c\u65e0\u6cd5\u53d1\u5e03\uff1a" + id);
            }
            out.add(id);
        }
        out.sort(Long::compareTo);
        return out;
    }

    private List<Long> listActiveTenantIds() {
        return this.sysTenantRepository.findAll().stream().filter(t -> t.getDeletedAt() == null).filter(t -> t.getStatus() == null || t.getStatus() == 1).map(BaseEntity::getId).sorted().toList();
    }

    public SysNotice update(Long id, SysNotice entity) {
        boolean superUser;
        SysNotice existing = this.sysNoticeRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Long opId = CurrentUserHolder.get();
        boolean bl = superUser = opId != null && this.apiPermissionService.isSuperAdmin(opId);
        if (!superUser && !Objects.equals(existing.getTenantId(), tid)) {
            throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u901a\u77e5");
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        if (superUser) {
            entity.setTenantId(entity.getTenantId() != null ? entity.getTenantId() : existing.getTenantId());
        } else {
            entity.setTenantId(Long.valueOf(tid));
        }
        return (SysNotice)this.sysNoticeRepository.save((Object)entity);
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        boolean superUser;
        SysNotice existing = this.sysNoticeRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        Long opId = CurrentUserHolder.get();
        boolean bl = superUser = opId != null && this.apiPermissionService.isSuperAdmin(opId);
        if (!superUser && !Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u901a\u77e5");
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.sysNoticeRepository.save((Object)existing);
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"}));
    }

    @Generated
    public SysNoticeService(SysNoticeRepository sysNoticeRepository, SysTenantRepository sysTenantRepository, ApiPermissionService apiPermissionService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.sysNoticeRepository = sysNoticeRepository;
        this.sysTenantRepository = sysTenantRepository;
        this.apiPermissionService = apiPermissionService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

