/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysMenu
 *  com.bsball.repository.SysMenuRepository
 *  com.bsball.service.MenuExpansionHelper
 *  lombok.Generated
 *  org.springframework.stereotype.Component
 */
package com.bsball.service;

import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysMenu;
import com.bsball.repository.SysMenuRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.stereotype.Component;

@Component
public class MenuExpansionHelper {
    private final SysMenuRepository sysMenuRepository;

    public Set<Long> expandWithAncestors(Collection<Long> seedIds) {
        if (seedIds == null || seedIds.isEmpty()) {
            return Set.of();
        }
        List all = this.sysMenuRepository.findAll();
        Map<Long, SysMenu> byId = all.stream().filter(m -> m.getId() != null).collect(Collectors.toMap(BaseEntity::getId, m -> m, (a, b) -> a));
        HashSet<Long> out = new HashSet<Long>();
        block0: for (Long id : seedIds) {
            if (id == null) continue;
            Long cur = id;
            while (cur != null && cur > 0L) {
                Long p;
                out.add(cur);
                SysMenu m2 = byId.get(cur);
                if (m2 == null || (p = m2.getParentId()) == null || p == 0L) continue block0;
                cur = p;
            }
        }
        return out;
    }

    @Generated
    public MenuExpansionHelper(SysMenuRepository sysMenuRepository) {
        this.sysMenuRepository = sysMenuRepository;
    }
}

