/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.model.entity.SysApi
 *  com.bsball.repository.SysApiRepository
 *  com.bsball.service.SysApiService
 *  com.bsball.utils.ApiGroupUtil
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
import com.bsball.model.entity.SysApi;
import com.bsball.repository.SysApiRepository;
import com.bsball.utils.ApiGroupUtil;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.List;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SysApiService {
    private final SysApiRepository sysApiRepository;

    public PageResult<SysApi> list(Integer page, Integer pageSize, String keyword) {
        Pageable p = this.buildPageable(page, pageSize);
        if (keyword == null || keyword.isBlank()) {
            Page result = this.sysApiRepository.findAll(p);
            return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
        }
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            String k = "%" + keyword.trim() + "%";
            Predicate path = cb.like((Expression)root.get("path"), k);
            Predicate method = cb.like((Expression)root.get("method"), k);
            Predicate desc = cb.like((Expression)root.get("description"), k);
            Predicate group = cb.like((Expression)root.get("groupName"), k);
            return cb.or(new Predicate[]{path, method, desc, group});
        };
        Page result = this.sysApiRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public List<SysApi> listAll() {
        return this.sysApiRepository.findAll(Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"groupName", "path", "id"}));
    }

    public SysApi create(SysApi entity) {
        this.ensureGroup(entity);
        return (SysApi)this.sysApiRepository.save((Object)entity);
    }

    public SysApi update(Long id, SysApi entity) {
        SysApi existing = this.sysApiRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        this.ensureGroup(entity);
        return (SysApi)this.sysApiRepository.save((Object)entity);
    }

    public void delete(Long id) {
        this.sysApiRepository.deleteById((Object)id);
    }

    private void ensureGroup(SysApi entity) {
        if (entity.getGroupName() == null || entity.getGroupName().isBlank()) {
            entity.setGroupName(ApiGroupUtil.inferGroup((String)entity.getPath()));
        }
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"groupName", "path", "id"}));
    }

    @Generated
    public SysApiService(SysApiRepository sysApiRepository) {
        this.sysApiRepository = sysApiRepository;
    }
}

