/*
 * 参考样例：Service 层的纯单元测试写法。
 * 外部依赖（Repository、其他 Service）一律用 Mockito mock，
 * 不启动 Spring 容器、不连接数据库、不访问网络。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.core.GuestPublicApiHolder;
import com.bsball.model.dto.EffectiveDataScope;
import com.bsball.model.entity.SysDataScope;
import com.bsball.repository.SysDataScopeRepository;
import com.bsball.repository.TeamRepository;
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
@DisplayName("DataScopeService：按用户与租户解析有效数据范围")
class DataScopeServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 9L;

    @Mock
    private ApiPermissionService apiPermissionService;

    @Mock
    private SysDataScopeRepository sysDataScopeRepository;

    @Mock
    private TeamRepository teamRepository;

    private TenantProperties tenantProperties;

    private DataScopeService dataScopeService;

    @BeforeEach
    void setUp() {
        tenantProperties = new TenantProperties();
        dataScopeService = new DataScopeService(
                apiPermissionService, tenantProperties, sysDataScopeRepository, teamRepository);
    }

    @AfterEach
    void tearDown() {
        // GuestPublicApiHolder 基于 ThreadLocal，测试后必须清理，避免污染其他用例
        GuestPublicApiHolder.clear();
    }

    @Test
    @DisplayName("用户ID为空：不受限，且不查询数据范围")
    void resolve_nullUserId_returnsUnrestricted() {
        EffectiveDataScope scope = dataScopeService.resolve(null, TENANT_ID);

        assertTrue(scope.isUnrestrictedInTenant());
        verifyNoInteractions(sysDataScopeRepository, teamRepository);
    }

    @Test
    @DisplayName("访客只读上下文：不受限，且不校验权限、不查询数据范围")
    void resolve_guestLikeRead_returnsUnrestricted() {
        GuestPublicApiHolder.setGuestLikeRead(true);

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertTrue(scope.isUnrestrictedInTenant());
        verifyNoInteractions(apiPermissionService, sysDataScopeRepository, teamRepository);
    }

    @Test
    @DisplayName("超级管理员：不受限，且不查询数据范围")
    void resolve_superAdmin_returnsUnrestricted() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(true);

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertTrue(scope.isUnrestrictedInTenant());
        verifyNoInteractions(sysDataScopeRepository);
    }

    @Test
    @DisplayName("无数据范围配置、非严格模式：不受限")
    void resolve_noScopeRowsAndNotStrict_returnsUnrestricted() {
        tenantProperties.setStrictDataScope(false);
        givenScopeRows();

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertTrue(scope.isUnrestrictedInTenant());
    }

    @Test
    @DisplayName("无数据范围配置、严格模式：返回空范围（什么都不放行）")
    void resolve_noScopeRowsAndStrict_returnsEmpty() {
        tenantProperties.setStrictDataScope(true);
        givenScopeRows();

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertFalse(scope.isUnrestrictedInTenant());
        assertTrue(scope.getLeagueIds().isEmpty());
        assertTrue(scope.getTeamIds().isEmpty());
    }

    @Test
    @DisplayName("TEAM 类型配置：收集球队ID，并按ID判定可读性")
    void resolve_teamScope_collectsTeamIds() {
        givenScopeRows(teamScope(11L), teamScope(12L));

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertFalse(scope.isUnrestrictedInTenant());
        assertEquals(Set.of(11L, 12L), scope.getTeamIds());
        assertTrue(scope.getLeagueIds().isEmpty());
        assertTrue(scope.canReadTeam(11L));
        assertFalse(scope.canReadTeam(99L));
    }

    @Test
    @DisplayName("LEAGUE + INCLUDE_DESCENDANTS：同时展开联盟下的球队")
    void resolve_leagueWithDescendants_expandsTeamIds() {
        givenScopeRows(leagueScope(7L, SysDataScope.EXP_INCLUDE_DESCENDANTS));
        when(teamRepository.findIdsByLeagueIdAndTenantId(7L, TENANT_ID)).thenReturn(List.of(101L, 102L));

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertEquals(Set.of(7L), scope.getLeagueIds());
        assertEquals(Set.of(101L, 102L), scope.getTeamIds());
        assertTrue(scope.canReadLeague(7L));
        assertTrue(scope.canReadTeam(101L));
    }

    @Test
    @DisplayName("LEAGUE + SELF：只放行联盟本身，不展开球队")
    void resolve_leagueWithoutExpansion_doesNotExpand() {
        givenScopeRows(leagueScope(7L, SysDataScope.EXP_SELF));

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertEquals(Set.of(7L), scope.getLeagueIds());
        assertTrue(scope.getTeamIds().isEmpty());
        verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("未知 scopeType：忽略该行且不报错")
    void resolve_unknownScopeType_isIgnored() {
        SysDataScope unknown = new SysDataScope();
        unknown.setScopeType("OTHER");
        unknown.setRefId(3L);
        givenScopeRows(unknown);

        EffectiveDataScope scope = dataScopeService.resolve(USER_ID, TENANT_ID);

        assertFalse(scope.isUnrestrictedInTenant());
        assertTrue(scope.getLeagueIds().isEmpty());
        assertTrue(scope.getTeamIds().isEmpty());
    }

    private void givenScopeRows(SysDataScope... rows) {
        when(sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(USER_ID, TENANT_ID))
                .thenReturn(List.of(rows));
    }

    private static SysDataScope teamScope(long teamId) {
        SysDataScope row = new SysDataScope();
        row.setScopeType(SysDataScope.TYPE_TEAM);
        row.setRefId(teamId);
        return row;
    }

    private static SysDataScope leagueScope(long leagueId, String expansion) {
        SysDataScope row = new SysDataScope();
        row.setScopeType(SysDataScope.TYPE_LEAGUE);
        row.setRefId(leagueId);
        row.setExpansion(expansion);
        return row;
    }
}
