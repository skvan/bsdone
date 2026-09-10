/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.model.entity.SysDict
 *  com.bsball.repository.SysDictRepository
 *  com.bsball.service.SysDictService
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
import com.bsball.model.entity.SysDict;
import com.bsball.repository.SysDictRepository;
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
public class SysDictService {
    private final SysDictRepository sysDictRepository;

    public PageResult<SysDict> list(Integer page, Integer pageSize, String keyword) {
        Pageable p = this.buildPageable(page, pageSize);
        if (keyword == null || keyword.isBlank()) {
            Page result = this.sysDictRepository.findAll(p);
            return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
        }
        String k = "%" + keyword.toLowerCase() + "%";
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("type")), k), cb.like(cb.lower((Expression)root.get("label")), k), cb.like(cb.lower((Expression)root.get("value")), k)});
        Page result = this.sysDictRepository.findAll((Specification)spec, p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysDict create(SysDict entity) {
        return (SysDict)this.sysDictRepository.save((Object)entity);
    }

    public SysDict update(Long id, SysDict entity) {
        SysDict existing = this.sysDictRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        return (SysDict)this.sysDictRepository.save((Object)entity);
    }

    public void delete(Long id) {
        this.sysDictRepository.deleteById((Object)id);
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"type", "sort", "id"}));
    }

    @Generated
    public SysDictService(SysDictRepository sysDictRepository) {
        this.sysDictRepository = sysDictRepository;
    }
}

