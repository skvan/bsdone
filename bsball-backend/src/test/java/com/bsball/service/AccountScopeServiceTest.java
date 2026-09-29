/*
 * 账号权限重构（批次 1）：统一范围中枢 AccountScopeService 的解析矩阵单测。
 *
 * 覆盖语义铁律：
 *  - 匿名 / 门户只读放行仅授予读语义，绝不授予管理权；
 *  - 门户角色（member / team_manager / league_organizer）无归属即空域，绝不回退不受限；
 *  - 关系驱动 Provider 仅注入自有集合（主办方不注入球队）；
 *  - 遗留 sys_data_scope 行按行受限，无行时 legacy strict 决定空域 / 不受限；
 *  - 驱逐用户范围缓存后关系变更可重算。
 *
 * 风格参照 DataScopeServiceTest：外部依赖一律 Mockito mock，不启动 Spring、不连库。
 * ScopeRelationProvider 非函数式接口（supports + contribute），故用 mock 逐方法打桩。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.core.GuestPublicApiHolder;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.SysDataScope;
import com.bsball.repository.SysDataScopeRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.scope.ScopeRelationProvider;
import com.bsball.service.scope.ScopeResolutionContext;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountScopeService：账号有效范围解析矩阵")
class AccountScopeServiceTest {

    private static final long USER_ID = 1L;
    private static final long TENANT_ID = 2L;

    @Mock
    private ApiPermissionService apiPermissionService;

    @Mock
    private TenantProperties tenantProperties;

    @Mock
    private SysDataScopeRepository sysDataScopeRepository;

    @Mock
    private TeamRepository teamRepository;

    @BeforeEach
    void setUp() {
        // GuestPublicApiHolder / REQ_CACHE 均为 ThreadLocal，用例起点必须清空
        GuestPublicApiHolder.clear();
    }

    @AfterEach
    void tearDown() {
        GuestPublicApiHolder.clear();
    }

    // ------------------------------------------------------------------ 用例 1

    @Test
    @DisplayName("匿名（userId=null）：门户只读放行，绝不授予管理权")
    void anonymous_returnsGuestLikeReadOnly() {
        GuestPublicApiHolder.setGuestLikeRead(true);
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(null, TENANT_ID);

        assertTrue(s.isGuestLikeRead());
        assertTrue(s.canReadLeague(1L));
        assertTrue(s.canReadTeam(1L));
        assertFalse(s.canManageLeague(1L));
        assertFalse(s.canManageTeam(1L));
        assertTrue(s.isManageEmpty());
        verifyNoInteractions(apiPermissionService, sysDataScopeRepository, teamRepository);
    }

    // ------------------------------------------------------------------ 用例 2

    @Test
    @DisplayName("超级管理员：租户内不受限")
    void superAdmin_isUnrestricted() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(true);
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertTrue(s.isUnrestrictedInTenant());
        assertFalse(s.isManageEmpty());
        assertTrue(s.canManageLeague(1L));
        assertTrue(s.canManageTeam(1L));
    }

    // ------------------------------------------------------------------ 用例 3

    @Test
    @DisplayName("租户管理员：租户内不受限")
    void tenantAdmin_isUnrestricted() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(USER_ID)).thenReturn(true);
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertTrue(s.isUnrestrictedInTenant());
        assertFalse(s.isManageEmpty());
    }

    // ------------------------------------------------------------------ 用例 4

    @Test
    @DisplayName("球队管理员：仅可管理自有球队（100 放行 / 101 拒绝）")
    void teamManager_withRelation_managesOwnTeams() {
        givenPortalAccountWithoutLegacyRows();
        AccountScopeService service = newService(teamProvider(100L));

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertFalse(s.isUnrestrictedInTenant());
        assertEquals(Set.of(100L), s.getTeamIds());
        assertTrue(s.canManageTeam(100L));
        assertFalse(s.canManageTeam(101L));
    }

    // ------------------------------------------------------------------ 用例 5

    @Test
    @DisplayName("主办方：仅可管理自有联盟，且不注入球队")
    void organizer_withLeagueRelation_managesOnlyOwnedLeague() {
        givenPortalAccountWithoutLegacyRows();
        AccountScopeService service = newService(leagueProvider(10L));

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertEquals(Set.of(10L), s.getLeagueIds());
        assertTrue(s.canManageLeague(10L));
        assertFalse(s.canManageLeague(11L));
        assertTrue(s.getTeamIds().isEmpty());
        assertFalse(s.canManageTeam(10L));
        assertFalse(s.canManageTeam(100L));
    }

    // ------------------------------------------------------------------ 用例 6

    @Test
    @DisplayName("门户角色无归属：返回空域（绝不回退不受限）")
    void portalWithoutRelations_getsEmptyScope() {
        givenPortalAccountWithoutLegacyRows();
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertTrue(s.isManageEmpty());
        assertFalse(s.isUnrestrictedInTenant());
        assertFalse(s.canManageLeague(1L));
        assertFalse(s.canManageTeam(1L));
    }

    // ------------------------------------------------------------------ 用例 7

    @Test
    @DisplayName("门户 member 只读上下文：可读不可管，管理域为空")
    void member_guestLikeRead_canReadButNotManage() {
        givenPortalAccountWithoutLegacyRows();
        GuestPublicApiHolder.setGuestLikeRead(true);
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertTrue(s.canReadLeague(1L));
        assertTrue(s.canReadTeam(1L));
        assertFalse(s.canManageLeague(1L));
        assertFalse(s.canManageTeam(1L));
        assertTrue(s.isManageEmpty());
    }

    // ------------------------------------------------------------------ 用例 8

    @Test
    @DisplayName("球员身份：仅标记不注入集合，管理域为空")
    void playerIdentity_doesNotGrantManage() {
        givenPortalAccountWithoutLegacyRows();
        AccountScopeService service = newService(identityOnlyProvider());

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertTrue(s.isManageEmpty());
        assertFalse(s.canManageTeam(100L));
        assertFalse(s.canManageLeague(10L));
    }

    // ------------------------------------------------------------------ 用例 9

    @Test
    @DisplayName("遗留自定义角色：按 sys_data_scope 行受限（LEAGUE 展开 + TEAM 自身）")
    void legacyCustomAccount_withScopeRows_restrictedByRows() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(USER_ID)).thenReturn(false);
        when(sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER_ID, TENANT_ID))
                .thenReturn(List.of(
                        leagueScope(10L, SysDataScope.EXP_INCLUDE_DESCENDANTS),
                        teamScope(200L)));
        when(teamRepository.findIdsByLeagueIdAndTenantId(10L, TENANT_ID)).thenReturn(List.of(100L, 101L));
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertFalse(s.isUnrestrictedInTenant());
        assertTrue(s.canManageLeague(10L));
        assertFalse(s.canManageLeague(11L));
        assertTrue(s.canManageTeam(100L));
        assertTrue(s.canManageTeam(101L));
        assertTrue(s.canManageTeam(200L));
    }

    // ------------------------------------------------------------------ 用例 10

    @Test
    @DisplayName("遗留自定义角色、无行、非严格：租户内不受限")
    void legacyCustomAccount_noRows_strictFalse_isUnrestricted() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(USER_ID)).thenReturn(false);
        when(sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER_ID, TENANT_ID))
                .thenReturn(List.of());
        when(apiPermissionService.hasAnyRoleCode(USER_ID, "member", "team_manager", "league_organizer"))
                .thenReturn(false);
        when(tenantProperties.isStrictDataScope()).thenReturn(false);
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertTrue(s.isUnrestrictedInTenant());
        assertFalse(s.isManageEmpty());
    }

    // ------------------------------------------------------------------ 用例 11

    @Test
    @DisplayName("遗留自定义角色、无行、严格：返回空域")
    void legacyCustomAccount_noRows_strictTrue_isEmpty() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(USER_ID)).thenReturn(false);
        when(sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER_ID, TENANT_ID))
                .thenReturn(List.of());
        when(apiPermissionService.hasAnyRoleCode(USER_ID, "member", "team_manager", "league_organizer"))
                .thenReturn(false);
        when(tenantProperties.isStrictDataScope()).thenReturn(true);
        AccountScopeService service = newService();

        EffectiveScope s = service.resolve(USER_ID, TENANT_ID);

        assertTrue(s.isManageEmpty());
        assertFalse(s.isUnrestrictedInTenant());
    }

    // ------------------------------------------------------------------ 用例 12

    @Test
    @DisplayName("驱逐用户范围缓存后：关系变更可重算出新范围")
    void evictUserScopeCache_recomputesAfterRelationChange() {
        givenPortalAccountWithoutLegacyRows();
        // 可变桩：contribute 注入的 teamId 可随测试推进而变更
        long[] teamId = {100L};
        ScopeRelationProvider provider = mock(ScopeRelationProvider.class);
        when(provider.supports(any(ScopeResolutionContext.class))).thenReturn(true);
        doAnswer(inv -> {
            ((ScopeResolutionContext) inv.getArgument(0)).addTeam(teamId[0]);
            return null;
        }).when(provider).contribute(any(ScopeResolutionContext.class));
        AccountScopeService service = newService(provider);

        EffectiveScope first = service.resolve(USER_ID, TENANT_ID);
        assertTrue(first.canManageTeam(100L));
        assertFalse(first.canManageTeam(200L));

        teamId[0] = 200L;

        // 模拟新请求（清请求级缓存）：核心范围仍命中 Caffeine → 仍旧值 100
        service.clearRequestScopeCache();
        EffectiveScope cached = service.resolve(USER_ID, TENANT_ID);
        assertTrue(cached.canManageTeam(100L));
        assertFalse(cached.canManageTeam(200L));

        // 驱逐用户范围缓存 + 新请求 → 重算 → 新值 200
        service.evictUserScopeCache(USER_ID);
        service.clearRequestScopeCache();
        EffectiveScope recomputed = service.resolve(USER_ID, TENANT_ID);
        assertTrue(recomputed.canManageTeam(200L));
        assertFalse(recomputed.canManageTeam(100L));
    }

    // ------------------------------------------------------------------ 辅助

    /** 构造被测服务：@Generated 构造器 + 手动初始化缓存（不启动 Spring）。 */
    private AccountScopeService newService(ScopeRelationProvider... providers) {
        AccountScopeService service = new AccountScopeService(
                apiPermissionService, tenantProperties, List.of(providers),
                sysDataScopeRepository, teamRepository);
        service.initScopeCache();
        service.clearRequestScopeCache();
        return service;
    }

    /** 门户角色账号基础桩：非超管 / 非租管、无遗留行、具门户角色。 */
    private void givenPortalAccountWithoutLegacyRows() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(USER_ID)).thenReturn(false);
        when(sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER_ID, TENANT_ID))
                .thenReturn(List.of());
        when(apiPermissionService.hasAnyRoleCode(USER_ID, "member", "team_manager", "league_organizer"))
                .thenReturn(true);
    }

    /** 桩 Provider：适用并注入一个球队 ID。 */
    private ScopeRelationProvider teamProvider(long teamId) {
        ScopeRelationProvider p = mock(ScopeRelationProvider.class);
        when(p.supports(any(ScopeResolutionContext.class))).thenReturn(true);
        doAnswer(inv -> {
            ((ScopeResolutionContext) inv.getArgument(0)).addTeam(teamId);
            return null;
        }).when(p).contribute(any(ScopeResolutionContext.class));
        return p;
    }

    /** 桩 Provider：适用并注入一个联盟 ID。 */
    private ScopeRelationProvider leagueProvider(long leagueId) {
        ScopeRelationProvider p = mock(ScopeRelationProvider.class);
        when(p.supports(any(ScopeResolutionContext.class))).thenReturn(true);
        doAnswer(inv -> {
            ((ScopeResolutionContext) inv.getArgument(0)).addLeague(leagueId);
            return null;
        }).when(p).contribute(any(ScopeResolutionContext.class));
        return p;
    }

    /** 桩 Provider：仅身份、不注入任何集合（对照 PlayerRelationProvider 语义）。 */
    private ScopeRelationProvider identityOnlyProvider() {
        ScopeRelationProvider p = mock(ScopeRelationProvider.class);
        when(p.supports(any(ScopeResolutionContext.class))).thenReturn(true);
        // contribute 有意留空：身份标记不注入 leagueIds / teamIds
        return p;
    }

    private static SysDataScope leagueScope(long leagueId, String expansion) {
        SysDataScope row = new SysDataScope();
        row.setScopeType(SysDataScope.TYPE_LEAGUE);
        row.setRefId(leagueId);
        row.setExpansion(expansion);
        return row;
    }

    private static SysDataScope teamScope(long teamId) {
        SysDataScope row = new SysDataScope();
        row.setScopeType(SysDataScope.TYPE_TEAM);
        row.setRefId(teamId);
        return row;
    }
}
