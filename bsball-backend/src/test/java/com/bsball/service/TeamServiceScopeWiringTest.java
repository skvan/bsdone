/*
 * 账号权限重构（批次 2，Task 2.5）：TeamService 范围收口与写保护接线测试。
 *
 * 目的（对照 spec §12.2 越权矩阵）：
 *  - 读：管理上下文窄域 → 收窄查询；空域 → 空页且不查库；宽读 → 跳过过滤；
 *  - 读：他域 get → 403「无权查看该球队」；跨租户 → 租户校验分支返回 null；
 *  - 写：他队 update/delete → ResourceGuard 403；跨租户 → 租户校验分支 403 且守卫不被触达。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；TeamService 手工 new（@Generated 构造器）。
 * ScopeQuerySupport / ResourceGuard 以桩打接口，验证「接线」而非其内部语义（后者另测）。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.Team;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.TeamManagerRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService：范围收口与写保护接线（越权矩阵·球队）")
class TeamServiceScopeWiringTest {

    private static final long TENANT_ID = 10L;
    private static final long OTHER_TENANT = 99L;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private ScopeQuerySupport scopeQuerySupport;

    @Mock
    private ResourceGuard resourceGuard;

    @Mock
    private PersonnelHistoryRecorder personnelHistoryRecorder;

    @Mock
    private TenantQueryPolicyService tenantQueryPolicyService;

    @Mock
    private TeamManagerRepository teamManagerRepository;

    @Mock
    private ApiPermissionService apiPermissionService;

    private TeamService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new TeamService(teamRepository, leagueRepository, accountScopeService, scopeQuerySupport,
                resourceGuard, personnelHistoryRecorder, tenantQueryPolicyService, teamManagerRepository,
                apiPermissionService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ 读：list

    @Test
    @DisplayName("list 管理上下文窄域：按可见球队集合收窄查询")
    void list_manageRestricted_narrowQuery() {
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(100L));
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(100L));
        when(teamRepository.findByTenantIdAndIdInAndDeletedAtIsNull(eq(TENANT_ID), eq(List.of(100L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(team(100L, TENANT_ID))));

        PageResult<Team> result = service.list(1, 10, null, null);

        assertEquals(1, result.getList().size());
        assertEquals(1L, result.getTotal());
        verify(teamRepository).findByTenantIdAndIdInAndDeletedAtIsNull(eq(TENANT_ID), eq(List.of(100L)), any(Pageable.class));
        verify(teamRepository, never()).findByTenantIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    @DisplayName("list 空域：返回空页且不查库（空集不查库）")
    void list_emptyScope_emptyPage_noQuery() {
        EffectiveScope scope = EffectiveScope.empty();
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of());

        PageResult<Team> result = service.list(1, 10, null, null);

        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        verifyNoInteractions(teamRepository);
    }

    @Test
    @DisplayName("list 宽读（不受限）：跳过过滤谓词，按租户全量分页")
    void list_wideRead_queriesTenant() {
        EffectiveScope scope = EffectiveScope.unrestricted();
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(null);
        when(teamRepository.findByTenantIdAndDeletedAtIsNull(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(team(1L, TENANT_ID), team(2L, TENANT_ID))));

        PageResult<Team> result = service.list(1, 10, null, null);

        assertEquals(2, result.getList().size());
        verify(teamRepository).findByTenantIdAndDeletedAtIsNull(eq(TENANT_ID), any(Pageable.class));
        verify(teamRepository, never()).findByTenantIdAndIdInAndDeletedAtIsNull(any(), any(), any());
    }

    // ------------------------------------------------------------------ 读：get

    @Test
    @DisplayName("get 本域命中：返回球队")
    void get_ownTeam_returns() {
        Team t = team(100L, TENANT_ID);
        when(teamRepository.findById(100L)).thenReturn(java.util.Optional.of(t));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(100L));
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(100L));

        assertEquals(t, service.get(100L));
    }

    @Test
    @DisplayName("get 他域：403「无权查看该球队」")
    void get_otherDomain_forbidden() {
        when(teamRepository.findById(101L)).thenReturn(java.util.Optional.of(team(101L, TENANT_ID)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of(100L));
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleTeamIds(scope, TENANT_ID)).thenReturn(List.of(100L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(101L));
        assertEquals(403, ex.getCode());
        assertEquals("无权查看该球队", ex.getMessage());
    }

    @Test
    @DisplayName("get 跨租户：租户校验分支返回 null（不进入范围判定）")
    void get_crossTenant_null() {
        when(teamRepository.findById(100L)).thenReturn(java.util.Optional.of(team(100L, OTHER_TENANT)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        assertNull(service.get(100L));
        verifyNoInteractions(scopeQuerySupport);
    }

    // ------------------------------------------------------------------ 写：update / delete

    @Test
    @DisplayName("update 他队：ResourceGuard 403（越权写）")
    void update_otherTeam_guardForbidden() {
        when(teamRepository.findById(101L)).thenReturn(java.util.Optional.of(team(101L, TENANT_ID)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        doThrow(new BusinessException(403, "无权管理该球队")).when(resourceGuard).assertCanManageTeam(101L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(101L, new Team()));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该球队", ex.getMessage());
        verify(resourceGuard).assertCanManageTeam(101L);
    }

    @Test
    @DisplayName("update 跨租户：租户校验先行 403，守卫不被触达")
    void update_crossTenant_forbiddenBeforeGuard() {
        when(teamRepository.findById(101L)).thenReturn(java.util.Optional.of(team(101L, OTHER_TENANT)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(101L, new Team()));
        assertEquals(403, ex.getCode());
        assertEquals("无权修改该球队", ex.getMessage());
        verify(resourceGuard, never()).assertCanManageTeam(any());
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("update 本队：守卫放行并落库（接线到 guard + save）")
    void update_ownTeam_guardCalledAndSaved() {
        when(teamRepository.findById(100L)).thenReturn(java.util.Optional.of(team(100L, TENANT_ID)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        Team updated = new Team();
        updated.setName("新队名");
        when(teamRepository.save(updated)).thenReturn(updated);

        Team saved = service.update(100L, updated);

        assertEquals(updated, saved);
        verify(resourceGuard).assertCanManageTeam(100L);
        verify(teamRepository).save(updated);
    }

    @Test
    @DisplayName("delete 跨租户：403「无权删除该球队」")
    void delete_crossTenant_forbidden() {
        when(teamRepository.findById(101L)).thenReturn(java.util.Optional.of(team(101L, OTHER_TENANT)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(101L));
        assertEquals(403, ex.getCode());
        assertEquals("无权删除该球队", ex.getMessage());
        verify(resourceGuard, never()).assertCanManageTeam(any());
    }

    private static Team team(long id, long tenantId) {
        Team t = new Team();
        t.setId(id);
        t.setTenantId(tenantId);
        t.setName("球队" + id);
        return t;
    }
}
