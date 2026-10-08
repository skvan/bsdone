/*
 * 账号权限重构（批次 1 遗留 Minor①，随 Task 2.5 补）：TeamManagerRelationProvider 实现级单测。
 *
 * 覆盖契约：
 *  - supports：userId 为空短路（不查库）；本租户存在 active 球队关系即适用（带租户/状态限定）；
 *  - contribute：把管理的球队 ID 注入 ctx.teamIds，不注入 leagueIds，且行数据按租户限定。
 */
package com.bsball.service.scope.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.entity.TeamManager;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.service.scope.ScopeResolutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamManagerRelationProvider：球队管理员关系判定与注入")
class TeamManagerRelationProviderTest {

    private static final long USER_ID = 1L;
    private static final long TENANT_ID = 2L;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    private TeamManagerRelationProvider provider() {
        return new TeamManagerRelationProvider(teamManagerRepository);
    }

    @Test
    @DisplayName("supports：userId 为空 → false 且不查库")
    void supports_nullUser_false_noQuery() {
        assertFalse(provider().supports(new ScopeResolutionContext(null, TENANT_ID)));
        verifyNoInteractions(teamManagerRepository);
    }

    @Test
    @DisplayName("supports：本租户存在 active 球队关系 → true（带租户限定）")
    void supports_hasActiveRelation_true() {
        when(teamManagerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of(relation(100L)));

        assertTrue(provider().supports(new ScopeResolutionContext(USER_ID, TENANT_ID)));
        verify(teamManagerRepository).findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, TeamManager.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("supports：本租户无 active 球队关系 → false")
    void supports_noRelation_false() {
        when(teamManagerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of());

        assertFalse(provider().supports(new ScopeResolutionContext(USER_ID, TENANT_ID)));
    }

    @Test
    @DisplayName("contribute：注入管理球队 ID（teamIds），不注入 leagueIds，且租户限定")
    void contribute_injectsTeamIds_tenantScoped() {
        when(teamManagerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of(relation(100L), relation(101L)));
        ScopeResolutionContext ctx = new ScopeResolutionContext(USER_ID, TENANT_ID);

        provider().contribute(ctx);

        assertEquals(Set.of(100L, 101L), ctx.getTeamIds());
        assertTrue(ctx.getLeagueIds().isEmpty());
        verify(teamManagerRepository).findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, TeamManager.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("contribute：关系行 teamId 为空 → 静默忽略（addTeam null 守卫，不 NPE）")
    void contribute_nullTeamId_ignored() {
        when(teamManagerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, TeamManager.STATUS_ACTIVE))
                .thenReturn(List.of(relation(null)));
        ScopeResolutionContext ctx = new ScopeResolutionContext(USER_ID, TENANT_ID);

        provider().contribute(ctx);

        assertTrue(ctx.getTeamIds().isEmpty());
    }

    private static TeamManager relation(Long teamId) {
        TeamManager tm = new TeamManager();
        tm.setTeamId(teamId);
        tm.setUserId(USER_ID);
        tm.setTenantId(TENANT_ID);
        tm.setStatus(TeamManager.STATUS_ACTIVE);
        return tm;
    }
}
