/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.model.entity.SysDictType
 *  com.bsball.repository.SysDictTypeRepository
 *  com.bsball.service.SysDictTypeService
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.model.entity.SysDictType;
import com.bsball.repository.SysDictTypeRepository;
import java.util.List;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SysDictTypeService {
    private final SysDictTypeRepository sysDictTypeRepository;

    public PageResult<SysDictType> list(Integer page, Integer pageSize) {
        Pageable p = this.buildPageable(page, pageSize);
        Page result = this.sysDictTypeRepository.findAll(p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysDictType create(SysDictType entity) {
        return (SysDictType)this.sysDictTypeRepository.save((Object)entity);
    }

    public SysDictType update(Long id, SysDictType entity) {
        SysDictType existing = this.sysDictTypeRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        return (SysDictType)this.sysDictTypeRepository.save((Object)entity);
    }

    public void delete(Long id) {
        this.sysDictTypeRepository.deleteById((Object)id);
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"id"}));
    }

    @Generated
    public SysDictTypeService(SysDictTypeRepository sysDictTypeRepository) {
        this.sysDictTypeRepository = sysDictTypeRepository;
    }
}

