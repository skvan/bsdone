/*
 * 账号权限重构（批次 1 遗留 Minor①，随 Task 2.5 补）：LeagueOwnerRelationProvider 实现级单测。
 *
 * 覆盖契约：
 *  - supports：userId 为空短路（不查库）；本租户存在 active 联盟关系即适用（带租户/状态限定）；
 *  - contribute：把主办的联盟 ID 注入 ctx.leagueIds，**不注入 teamIds**（球队写域不归主办方）。
 */
package com.bsball.service.scope.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.model.entity.LeagueOwner;
import com.bsball.repository.LeagueOwnerRepository;
import com.bsball.service.scope.ScopeResolutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueOwnerRelationProvider：联盟主办方关系判定与注入")
class LeagueOwnerRelationProviderTest {

    private static final long USER_ID = 1L;
    private static final long TENANT_ID = 2L;

    @Mock
    private LeagueOwnerRepository leagueOwnerRepository;

    private LeagueOwnerRelationProvider provider() {
        return new LeagueOwnerRelationProvider(leagueOwnerRepository);
    }

    @Test
    @DisplayName("supports：userId 为空 → false 且不查库")
    void supports_nullUser_false_noQuery() {
        assertFalse(provider().supports(new ScopeResolutionContext(null, TENANT_ID)));
        verifyNoInteractions(leagueOwnerRepository);
    }

    @Test
    @DisplayName("supports：本租户存在 active 联盟关系 → true（带租户限定）")
    void supports_hasActiveRelation_true() {
        when(leagueOwnerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of(relation(10L)));

        assertTrue(provider().supports(new ScopeResolutionContext(USER_ID, TENANT_ID)));
        verify(leagueOwnerRepository).findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, LeagueOwner.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("supports：本租户无 active 联盟关系 → false")
    void supports_noRelation_false() {
        when(leagueOwnerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of());

        assertFalse(provider().supports(new ScopeResolutionContext(USER_ID, TENANT_ID)));
    }

    @Test
    @DisplayName("contribute：注入联盟 ID（leagueIds），且不注入 teamIds，租户限定")
    void contribute_injectsLeagueIds_neverTeamIds() {
        when(leagueOwnerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of(relation(10L), relation(11L)));
        ScopeResolutionContext ctx = new ScopeResolutionContext(USER_ID, TENANT_ID);

        provider().contribute(ctx);

        assertEquals(Set.of(10L, 11L), ctx.getLeagueIds());
        assertTrue(ctx.getTeamIds().isEmpty());
        verify(leagueOwnerRepository).findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, LeagueOwner.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("contribute：关系行 leagueId 为空 → 静默忽略（addLeague null 守卫，不 NPE）")
    void contribute_nullLeagueId_ignored() {
        when(leagueOwnerRepository.findByUserIdAndTenantIdAndStatusAndDeletedAtIsNull(
                USER_ID, TENANT_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of(relation(null)));
        ScopeResolutionContext ctx = new ScopeResolutionContext(USER_ID, TENANT_ID);

        provider().contribute(ctx);

        assertTrue(ctx.getLeagueIds().isEmpty());
    }

    private static LeagueOwner relation(Long leagueId) {
        LeagueOwner lo = new LeagueOwner();
        lo.setLeagueId(leagueId);
        lo.setUserId(USER_ID);
        lo.setTenantId(TENANT_ID);
        lo.setStatus(LeagueOwner.STATUS_ACTIVE);
        return lo;
    }
}
