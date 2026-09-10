/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysMediaGalleryItem
 *  com.bsball.repository.SysMediaGalleryItemRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SysMediaGalleryItemService
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
import com.bsball.model.entity.SysMediaGalleryItem;
import com.bsball.repository.SysMediaGalleryItemRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysMediaGalleryItemService {
    private final SysMediaGalleryItemRepository repository;
    private final ApiPermissionService apiPermissionService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    private boolean superAdmin() {
        Long uid = CurrentUserHolder.get();
        return uid != null && this.apiPermissionService.isSuperAdmin(uid);
    }

    public PageResult<SysMediaGalleryItem> list(Integer page, Integer pageSize, String keyword, String tag) {
        PageRequest p = PageRequest.of((int)Math.max((page != null ? page : 1) - 1, 0), (int)PaginationSupport.resolvePageSize((Integer)pageSize), (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"id"}));
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        boolean kw = keyword != null && !keyword.isBlank();
        boolean tagFilter = tag != null && !tag.isBlank();
        String tagLower = tagFilter ? tag.trim().toLowerCase() : "";
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList ps = new ArrayList();
            ps.add(cb.isNull((Expression)root.get("deletedAt")));
            if (tid != null) {
                ps.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            if (kw) {
                String k = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("name")), k), cb.like(cb.lower((Expression)root.get("title")), k), cb.like(cb.lower((Expression)root.get("description")), k), cb.like(cb.lower((Expression)root.get("tags")), k), cb.like(cb.lower((Expression)root.get("imageUrl")), k)}));
            }
            if (tagFilter) {
                ps.add(cb.like(cb.lower((Expression)root.get("tags")), "%" + tagLower + "%"));
            }
            return cb.and((Predicate[])ps.toArray(Predicate[]::new));
        };
        Page result = this.repository.findAll((Specification)spec, (Pageable)p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysMediaGalleryItem get(Long id) {
        SysMediaGalleryItem e = (SysMediaGalleryItem)this.repository.findById((Object)id).orElseThrow(() -> new BusinessException(404, "\u56fe\u7247\u4e0d\u5b58\u5728"));
        this.assertTenant(e);
        return e;
    }

    private void assertTenant(SysMediaGalleryItem e) {
        if (this.superAdmin()) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (e.getTenantId() == null || e.getTenantId() != tid) {
            throw new BusinessException(403, "\u65e0\u6743\u8bbf\u95ee\u8be5\u56fe\u7247");
        }
    }

    @Transactional
    public SysMediaGalleryItem create(SysMediaGalleryItem body) {
        if (body.getImageUrl() == null || body.getImageUrl().isBlank()) {
            throw new BusinessException(400, "\u56fe\u7247\u5730\u5740\u4e0d\u80fd\u4e3a\u7a7a");
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        SysMediaGalleryItem e = new SysMediaGalleryItem();
        e.setTenantId(Long.valueOf(tid));
        e.setImageUrl(body.getImageUrl().trim());
        e.setTags((String)Optional.ofNullable(body.getTags()).map(String::trim).filter(s -> !s.isEmpty()).orElse(null));
        String displayName = Optional.ofNullable(body.getName()).map(String::trim).filter(s -> !s.isEmpty()).orElse(Optional.ofNullable(body.getTitle()).map(String::trim).filter(s -> !s.isEmpty()).orElse("\u672a\u547d\u540d"));
        e.setName(displayName);
        e.setTitle(displayName);
        e.setDescription((String)Optional.ofNullable(body.getDescription()).map(String::trim).filter(s -> !s.isEmpty()).orElse(null));
        e.setSortOrder(Integer.valueOf(body.getSortOrder() != null ? body.getSortOrder() : 0));
        return (SysMediaGalleryItem)this.repository.save((Object)e);
    }

    @Transactional
    public SysMediaGalleryItem update(Long id, SysMediaGalleryItem body) {
        SysMediaGalleryItem e = this.get(id);
        if (body.getImageUrl() != null) {
            if (body.getImageUrl().isBlank()) {
                throw new BusinessException(400, "\u56fe\u7247\u5730\u5740\u4e0d\u80fd\u4e3a\u7a7a");
            }
            e.setImageUrl(body.getImageUrl().trim());
        }
        if (body.getTags() != null) {
            e.setTags(body.getTags().trim().isEmpty() ? null : body.getTags().trim());
        }
        if (body.getName() != null) {
            String n = body.getName().trim();
            e.setName(n.isEmpty() ? "\u672a\u547d\u540d" : n);
            e.setTitle(e.getName());
        } else if (body.getTitle() != null) {
            String t = body.getTitle().trim();
            e.setTitle(t.isEmpty() ? null : t);
            if (e.getName() == null || e.getName().isBlank()) {
                e.setName(t.isEmpty() ? "\u672a\u547d\u540d" : t);
            }
        }
        if (body.getDescription() != null) {
            e.setDescription(body.getDescription().trim().isEmpty() ? null : body.getDescription().trim());
        }
        if (body.getSortOrder() != null) {
            e.setSortOrder(body.getSortOrder());
        }
        return (SysMediaGalleryItem)this.repository.save((Object)e);
    }

    @Transactional
    public void delete(Long id) {
        SysMediaGalleryItem e = this.get(id);
        Long uid = CurrentUserHolder.get();
        e.setDeletedAt(LocalDateTime.now());
        e.setDeletedBy(uid);
        this.repository.save((Object)e);
    }

    @Generated
    public SysMediaGalleryItemService(SysMediaGalleryItemRepository repository, ApiPermissionService apiPermissionService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.repository = repository;
        this.apiPermissionService = apiPermissionService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

