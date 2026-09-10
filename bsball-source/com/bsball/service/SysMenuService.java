/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysMenu
 *  com.bsball.model.entity.SysMenuApi
 *  com.bsball.repository.SysMenuApiRepository
 *  com.bsball.repository.SysMenuRepository
 *  com.bsball.service.SysMenuService
 *  lombok.Generated
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Order
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysMenuApi;
import com.bsball.repository.SysMenuApiRepository;
import com.bsball.repository.SysMenuRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SysMenuService {
    private final SysMenuRepository sysMenuRepository;
    private final SysMenuApiRepository sysMenuApiRepository;

    public PageResult<SysMenu> list() {
        List all = this.sysMenuRepository.findAll(Sort.by((Sort.Order[])new Sort.Order[]{Sort.Order.asc((String)"sort"), Sort.Order.asc((String)"id")}));
        Map apiMap = this.loadMenuApiMap();
        List tree = this.buildMenuTree(all, Long.valueOf(0L));
        this.attachApiIds(tree, apiMap);
        return PageResult.of((List)tree, (long)tree.size());
    }

    private Map<Long, List<Long>> loadMenuApiMap() {
        return this.sysMenuApiRepository.findAll().stream().collect(Collectors.groupingBy(SysMenuApi::getMenuId, Collectors.mapping(SysMenuApi::getApiId, Collectors.toList())));
    }

    private void attachApiIds(List<SysMenu> nodes, Map<Long, List<Long>> apiMap) {
        if (nodes == null) {
            return;
        }
        for (SysMenu m : nodes) {
            m.setApiIds(apiMap.getOrDefault(m.getId(), List.of()));
            this.attachApiIds(m.getChildren(), apiMap);
        }
    }

    private List<SysMenu> buildMenuTree(List<SysMenu> flat, Long parentId) {
        return flat.stream().filter(m -> m.getParentId() == null && parentId == 0L || m.getParentId() != null && m.getParentId().equals(parentId)).sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(BaseEntity::getId)).peek(m -> m.setChildren(this.buildMenuTree(flat, m.getId()))).collect(Collectors.toList());
    }

    @Transactional
    public SysMenu create(SysMenu entity) {
        if (entity.getMenuType() == null) {
            entity.setMenuType(Integer.valueOf(2));
        }
        List apiIds = entity.getApiIds();
        entity.setApiIds(null);
        SysMenu saved = (SysMenu)this.sysMenuRepository.save((Object)entity);
        this.replaceMenuApis(saved.getId(), apiIds);
        saved.setApiIds(apiIds != null ? apiIds : List.of());
        return saved;
    }

    @Transactional
    public SysMenu update(Long id, SysMenu entity) {
        SysMenu existing = this.sysMenuRepository.findById((Object)id).orElse(null);
        if (existing == null) {
            return null;
        }
        entity.setId(id);
        entity.setCreatedAt(existing.getCreatedAt());
        if (entity.getMenuType() == null) {
            entity.setMenuType(Integer.valueOf(existing.getMenuType() != null ? existing.getMenuType() : 2));
        }
        List apiIds = entity.getApiIds();
        entity.setApiIds(null);
        SysMenu saved = (SysMenu)this.sysMenuRepository.save((Object)entity);
        if (apiIds != null) {
            this.replaceMenuApis(saved.getId(), apiIds);
            saved.setApiIds(apiIds);
        } else {
            saved.setApiIds(this.sysMenuApiRepository.findByMenuId(saved.getId()).stream().map(SysMenuApi::getApiId).toList());
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        this.sysMenuApiRepository.deleteByMenuId(id);
        this.sysMenuRepository.deleteById((Object)id);
    }

    private void replaceMenuApis(Long menuId, List<Long> apiIds) {
        this.sysMenuApiRepository.deleteByMenuId(menuId);
        this.sysMenuApiRepository.flush();
        if (apiIds == null || apiIds.isEmpty()) {
            return;
        }
        for (Long aid : apiIds.stream().filter(x -> x != null).distinct().toList()) {
            SysMenuApi row = new SysMenuApi();
            row.setMenuId(menuId);
            row.setApiId(aid);
            this.sysMenuApiRepository.save((Object)row);
        }
    }

    @Generated
    public SysMenuService(SysMenuRepository sysMenuRepository, SysMenuApiRepository sysMenuApiRepository) {
        this.sysMenuRepository = sysMenuRepository;
        this.sysMenuApiRepository = sysMenuApiRepository;
    }
}

