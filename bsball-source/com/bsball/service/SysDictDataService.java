/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.model.entity.SysDictData
 *  com.bsball.repository.SysDictDataRepository
 *  com.bsball.service.SysDictDataService
 *  org.springframework.cache.Cache
 *  org.springframework.cache.CacheManager
 *  org.springframework.cache.annotation.CacheEvict
 *  org.springframework.cache.annotation.Cacheable
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
import com.bsball.model.entity.SysDictData;
import com.bsball.repository.SysDictDataRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class SysDictDataService {
    private final SysDictDataRepository sysDictDataRepository;
    private final Optional<CacheManager> cacheManager;

    public SysDictDataService(SysDictDataRepository sysDictDataRepository, Optional<CacheManager> cacheManager) {
        this.sysDictDataRepository = sysDictDataRepository;
        this.cacheManager = cacheManager != null ? cacheManager : Optional.empty();
    }

    public PageResult<SysDictData> list(Integer page, Integer pageSize, Long dictTypeId) {
        int total;
        int ps;
        if (dictTypeId == null) {
            Pageable p = this.buildPageable(page, pageSize);
            Page result = this.sysDictDataRepository.findAll(p);
            return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
        }
        List all = this.getListByDictTypeId(dictTypeId);
        int p = page != null && page > 0 ? page : 1;
        int from = (p - 1) * (ps = PaginationSupport.resolvePageSize((Integer)pageSize));
        if (from >= (total = all.size())) {
            return PageResult.of((List)List.of(), (long)total);
        }
        int to = Math.min(from + ps, total);
        return PageResult.of(all.subList(from, to), (long)total);
    }

    @Cacheable(value={"sys_dict"}, key="#dictTypeId", unless="#result == null || #result.isEmpty()")
    public List<SysDictData> getListByDictTypeId(Long dictTypeId) {
        if (dictTypeId == null) {
            return List.of();
        }
        return this.sysDictDataRepository.findByDictTypeIdOrderBySortAscIdAsc(dictTypeId);
    }

    @CacheEvict(value={"sys_dict"}, key="#entity.dictTypeId")
    public SysDictData create(SysDictData entity) {
        return (SysDictData)this.sysDictDataRepository.save((Object)entity);
    }

    @CacheEvict(value={"sys_dict"}, key="#entity.dictTypeId")
    public SysDictData update(Long id, SysDictData entity) {
        SysDictData existing = this.sysDictDataRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        return (SysDictData)this.sysDictDataRepository.save((Object)entity);
    }

    public void delete(Long id) {
        SysDictData one = this.sysDictDataRepository.findById((Object)id).orElse(null);
        Long dictTypeId = one != null ? one.getDictTypeId() : null;
        this.sysDictDataRepository.deleteById((Object)id);
        if (dictTypeId != null) {
            this.cacheManager.ifPresent(cm -> {
                Cache cache = cm.getCache("sys_dict");
                if (cache != null) {
                    cache.evict((Object)String.valueOf(dictTypeId));
                }
            });
        }
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.ASC, (String[])new String[]{"dictTypeId", "sort", "id"}));
    }
}

