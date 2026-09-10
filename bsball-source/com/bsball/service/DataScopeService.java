/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.GuestPublicApiHolder
 *  com.bsball.model.dto.EffectiveDataScope
 *  com.bsball.model.entity.SysDataScope
 *  com.bsball.repository.SysDataScopeRepository
 *  com.bsball.repository.TeamRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.DataScopeService
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.core.GuestPublicApiHolder;
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.entity.SysDataScope;
import com.bsball.repository.SysDataScopeRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.ApiPermissionService;
import java.util.HashSet;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;

@Service
public class DataScopeService {
    private final ApiPermissionService apiPermissionService;
    private final TenantProperties tenantProperties;
    private final SysDataScopeRepository sysDataScopeRepository;
    private final TeamRepository teamRepository;

    public EffectiveDataScope resolve(Long userId, long tenantId) {
        if (userId == null || GuestPublicApiHolder.isGuestLikeRead()) {
            return EffectiveDataScope.unrestricted();
        }
        if (this.apiPermissionService.isSuperAdmin(userId)) {
            return EffectiveDataScope.unrestricted();
        }
        List rows = this.sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(userId, Long.valueOf(tenantId));
        if (rows.isEmpty()) {
            if (this.tenantProperties.isStrictDataScope()) {
                return EffectiveDataScope.empty();
            }
            return EffectiveDataScope.unrestricted();
        }
        HashSet<Long> leagueIds = new HashSet<Long>();
        HashSet<Long> teamIds = new HashSet<Long>();
        for (SysDataScope row : rows) {
            if ("TEAM".equals(row.getScopeType())) {
                teamIds.add(row.getRefId());
                continue;
            }
            if (!"LEAGUE".equals(row.getScopeType())) continue;
            leagueIds.add(row.getRefId());
            if (!"INCLUDE_DESCENDANTS".equals(row.getExpansion())) continue;
            teamIds.addAll(this.teamRepository.findIdsByLeagueIdAndTenantId(row.getRefId(), Long.valueOf(tenantId)));
        }
        return EffectiveDataScope.restricted(leagueIds, teamIds);
    }

    @Generated
    public DataScopeService(ApiPermissionService apiPermissionService, TenantProperties tenantProperties, SysDataScopeRepository sysDataScopeRepository, TeamRepository teamRepository) {
        this.apiPermissionService = apiPermissionService;
        this.tenantProperties = tenantProperties;
        this.sysDataScopeRepository = sysDataScopeRepository;
        this.teamRepository = teamRepository;
    }
}

