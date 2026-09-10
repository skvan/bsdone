/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysArticle
 *  com.bsball.repository.SysArticleRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.CountAntiAbuseService
 *  com.bsball.service.SysArticleService
 *  com.bsball.service.TenantQueryPolicyService
 *  jakarta.persistence.criteria.CriteriaBuilder
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.persistence.criteria.Root
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.domain.Sort$Order
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysArticle;
import com.bsball.repository.SysArticleRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.CountAntiAbuseService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
public class SysArticleService {
    private final SysArticleRepository sysArticleRepository;
    private final CountAntiAbuseService countAntiAbuseService;
    private final ApiPermissionService apiPermissionService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    public PageResult<SysArticle> list(Integer page, Integer pageSize, String sortProp, String sortOrder, String publishTarget, String type, String keyword, Integer showInCarousel, String adminScope) {
        boolean hasCarousel;
        Long uid;
        if (adminScope != null && "platform".equalsIgnoreCase(adminScope.trim()) && ((uid = CurrentUserHolder.get()) == null || !this.apiPermissionService.isSuperAdmin(uid))) {
            return PageResult.of(Collections.emptyList(), (long)0L);
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder, showInCarousel);
        boolean hasTarget = publishTarget != null && !publishTarget.isBlank() && !"all".equals(publishTarget);
        boolean hasType = type != null && !type.isBlank() && !"all".equals(type);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean bl = hasCarousel = showInCarousel != null && showInCarousel == 1;
        if (!(hasTarget || hasType || hasKeyword || hasCarousel)) {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> cb.and((Expression)cb.isNull((Expression)root.get("deletedAt")), (Expression)this.tenantPredicateForAdminList(adminScope, root, cb));
            Page result = this.sysArticleRepository.findAll((Specification)spec, p);
            return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
        }
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            preds.add(this.tenantPredicateForAdminList(adminScope, root, cb));
            if (hasTarget) {
                if ("portal".equals(publishTarget)) {
                    preds.add(cb.or((Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"portal"), (Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"both")));
                } else if ("admin".equals(publishTarget)) {
                    preds.add(cb.or((Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"admin"), (Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"both")));
                }
            }
            if (hasType) {
                preds.add(cb.equal((Expression)root.get("type"), (Object)type));
            }
            if (hasKeyword) {
                String k = "%" + keyword.toLowerCase() + "%";
                preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("title")), k), cb.like(cb.lower((Expression)root.get("content")), k), cb.like(cb.lower((Expression)root.get("summary")), k), cb.like(cb.lower((Expression)root.get("type")), k)}));
            }
            if (hasCarousel) {
                preds.add(cb.equal((Expression)root.get("showInCarousel"), (Object)1));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.sysArticleRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    private Predicate tenantScopePredicate(Root<SysArticle> root, CriteriaBuilder cb) {
        if (this.tenantQueryPolicyService.isGlobalQueryMode()) {
            return cb.isNotNull((Expression)root.get("id"));
        }
        return cb.equal((Expression)root.get("tenantId"), (Object)this.tenantQueryPolicyService.requiredTenantId());
    }

    private Predicate tenantPredicateForAdminList(String adminScope, Root<SysArticle> root, CriteriaBuilder cb) {
        if (adminScope == null || adminScope.isBlank()) {
            return this.tenantScopePredicate(root, cb);
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Long uid = CurrentUserHolder.get();
        String s = adminScope.trim();
        if ("business".equalsIgnoreCase(s)) {
            return cb.and((Expression)cb.isNotNull((Expression)root.get("tenantId")), (Expression)cb.equal((Expression)root.get("tenantId"), (Object)tid));
        }
        if ("platform".equalsIgnoreCase(s)) {
            if (uid == null || !this.apiPermissionService.isSuperAdmin(uid)) {
                return cb.equal((Expression)root.get("id"), (Object)-1L);
            }
            return cb.isNull((Expression)root.get("tenantId"));
        }
        return this.tenantScopePredicate(root, cb);
    }

    public PageResult<SysArticle> listPortal(Integer page, Integer pageSize, String sortProp, String sortOrder, String publishTarget, String type, String keyword, Integer showInCarousel) {
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder, showInCarousel);
        boolean hasTarget = publishTarget != null && !publishTarget.isBlank() && !"all".equals(publishTarget);
        boolean hasType = type != null && !type.isBlank() && !"all".equals(type);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCarousel = showInCarousel != null && showInCarousel == 1;
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            preds.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            if (hasTarget) {
                if ("portal".equals(publishTarget)) {
                    preds.add(cb.or((Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"portal"), (Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"both")));
                } else if ("admin".equals(publishTarget)) {
                    preds.add(cb.or((Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"admin"), (Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"both")));
                }
            }
            if (hasType) {
                preds.add(cb.equal((Expression)root.get("type"), (Object)type));
            }
            if (hasKeyword) {
                String k = "%" + keyword.toLowerCase() + "%";
                preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("title")), k), cb.like(cb.lower((Expression)root.get("content")), k), cb.like(cb.lower((Expression)root.get("summary")), k), cb.like(cb.lower((Expression)root.get("type")), k)}));
            }
            if (hasCarousel) {
                preds.add(cb.equal((Expression)root.get("showInCarousel"), (Object)1));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.sysArticleRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public PageResult<SysArticle> listPlatformPortal(Integer page, Integer pageSize, String sortProp, String sortOrder, String publishTarget, String type, String keyword, Integer showInCarousel) {
        Pageable p = this.buildPageable(page, pageSize, sortProp, sortOrder, showInCarousel);
        boolean hasType = type != null && !type.isBlank() && !"all".equals(type);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCarousel = showInCarousel != null && showInCarousel == 1;
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            preds.add(cb.isNull((Expression)root.get("deletedAt")));
            preds.add(cb.isNull((Expression)root.get("tenantId")));
            preds.add(cb.or((Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"portal"), (Expression)cb.equal((Expression)root.get("publishTarget"), (Object)"both")));
            if (hasType) {
                preds.add(cb.equal((Expression)root.get("type"), (Object)type));
            }
            if (hasKeyword) {
                String k = "%" + keyword.toLowerCase() + "%";
                preds.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("title")), k), cb.like(cb.lower((Expression)root.get("content")), k), cb.like(cb.lower((Expression)root.get("summary")), k), cb.like(cb.lower((Expression)root.get("type")), k)}));
            }
            if (hasCarousel) {
                preds.add(cb.equal((Expression)root.get("showInCarousel"), (Object)1));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page result = this.sysArticleRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysArticle get(Long id) {
        SysArticle a = this.sysArticleRepository.findById((Object)id).orElse(null);
        if (a == null || a.getDeletedAt() != null) {
            return null;
        }
        Long uid = CurrentUserHolder.get();
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (a.getTenantId() == null) {
            if (uid != null && this.apiPermissionService.isSuperAdmin(uid)) {
                return a;
            }
            if (a.getStatus() != null && a.getStatus() == 0) {
                return null;
            }
            return a;
        }
        if (Objects.equals(a.getTenantId(), tid)) {
            return a;
        }
        return null;
    }

    @Transactional
    public Optional<Long> incrementViewCount(Long id, String clientIp) {
        if (id == null) {
            return Optional.empty();
        }
        SysArticle existing = this.sysArticleRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return Optional.empty();
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (existing.getTenantId() != null && !Objects.equals(existing.getTenantId(), tid)) {
            return Optional.empty();
        }
        if (!this.countAntiAbuseService.acquireNoticeView(clientIp, id)) {
            return Optional.of(existing.getViewCount());
        }
        int updated = this.sysArticleRepository.incrementViewCountById(id);
        if (updated == 0) {
            return Optional.empty();
        }
        return this.sysArticleRepository.findById((Object)id).map(SysArticle::getViewCount);
    }

    public SysArticle create(SysArticle entity, String clientIp, String submitRegionText) {
        Long uid;
        entity.setSubmitIp(null);
        entity.setSubmitIpRegion(null);
        if (entity.getViewCount() < 0L) {
            entity.setViewCount(0L);
        }
        if ((uid = CurrentUserHolder.get()) == null || !this.apiPermissionService.isSuperAdmin(uid)) {
            entity.setTenantId(Long.valueOf(this.tenantQueryPolicyService.requiredTenantId()));
        }
        SysArticleService.applySubmitMeta((SysArticle)entity, (String)clientIp, (String)submitRegionText);
        return (SysArticle)this.sysArticleRepository.save((Object)entity);
    }

    public SysArticle update(Long id, SysArticle entity, String clientIp, String submitRegionText) {
        SysArticle existing = this.sysArticleRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return null;
        }
        entity.setSubmitIp(null);
        entity.setSubmitIpRegion(null);
        Long uid = CurrentUserHolder.get();
        if (uid == null || !this.apiPermissionService.isSuperAdmin(uid)) {
            if (!Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
                throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6587\u7ae0");
            }
            entity.setTenantId(existing.getTenantId());
        } else {
            if (!this.tenantQueryPolicyService.isGlobalQueryMode() && existing.getTenantId() != null && !Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
                throw new BusinessException(403, "\u65e0\u6743\u4fee\u6539\u8be5\u6587\u7ae0");
            }
            entity.setTenantId(existing.getTenantId());
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setViewCount(existing.getViewCount());
        SysArticleService.applySubmitMeta((SysArticle)entity, (String)clientIp, (String)submitRegionText);
        return (SysArticle)this.sysArticleRepository.save((Object)entity);
    }

    private static void applySubmitMeta(SysArticle e, String clientIp, String submitRegionText) {
        if (clientIp != null) {
            String ip = clientIp.trim();
            if (ip.length() > 128) {
                ip = ip.substring(0, 128);
            }
            e.setSubmitIp(ip.isEmpty() ? null : ip);
        } else {
            e.setSubmitIp(null);
        }
        if (submitRegionText != null) {
            String r = submitRegionText.trim();
            if (r.length() > 512) {
                r = r.substring(0, 512);
            }
            e.setSubmitIpRegion(r.isEmpty() ? null : r);
        } else {
            e.setSubmitIpRegion(null);
        }
    }

    @Transactional(rollbackFor={Exception.class})
    public void delete(Long id) {
        SysArticle existing = this.sysArticleRepository.findById((Object)id).orElse(null);
        if (existing == null || existing.getDeletedAt() != null) {
            return;
        }
        Long uid = CurrentUserHolder.get();
        if (uid == null || !this.apiPermissionService.isSuperAdmin(uid) ? !Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId()) : !this.tenantQueryPolicyService.isGlobalQueryMode() && existing.getTenantId() != null && !Objects.equals(existing.getTenantId(), this.tenantQueryPolicyService.requiredTenantId())) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u6587\u7ae0");
        }
        existing.setDeletedAt(LocalDateTime.now());
        existing.setDeletedBy(CurrentUserHolder.get());
        this.sysArticleRepository.save((Object)existing);
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortProp, String sortOrder, Integer showInCarousel) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        if (showInCarousel != null && showInCarousel == 1) {
            return PageRequest.of((int)0, (int)7, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"carouselOrder"}));
        }
        if (sortProp != null && !sortProp.isEmpty()) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)dir, (String[])new String[]{sortProp}));
        }
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Order[])new Sort.Order[]{Sort.Order.desc((String)"isPinned"), Sort.Order.desc((String)"createdAt")}));
    }

    @Generated
    public SysArticleService(SysArticleRepository sysArticleRepository, CountAntiAbuseService countAntiAbuseService, ApiPermissionService apiPermissionService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.sysArticleRepository = sysArticleRepository;
        this.countAntiAbuseService = countAntiAbuseService;
        this.apiPermissionService = apiPermissionService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

