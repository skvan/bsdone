/*
 * 账号权限重构（批次 2，Task 2.5）：LeagueService 范围收口与写保护接线测试（补 §12.2 点名场景）。
 *
 * 目的：
 *  - 「主办方在 /admin 列表看不到其他联盟（manage 头生效）」→ 直接用例；
 *  - 空域管理读 → 空页且不查库；漏带头（宽读）→ 跳过过滤；
 *  - 他域 get → 403；跨租户 → 租户校验分支；写 → ResourceGuard 403。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.bsball.model.entity.League;
import com.bsball.repository.LeagueRepository;
import com.bsball.service.query.ScopeQuerySupport;
import java.util.List;
import java.util.Optional;
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
@DisplayName("LeagueService：范围收口与写保护接线（越权矩阵·联盟）")
class LeagueServiceScopeWiringTest {

    private static final long TENANT_ID = 10L;
    private static final long OTHER_TENANT = 99L;

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

    private LeagueService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new LeagueService(leagueRepository, accountScopeService, scopeQuerySupport, resourceGuard,
                personnelHistoryRecorder, tenantQueryPolicyService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------------ 读：list（manage 头生效）

    @Test
    @DisplayName("list 主办方 manage 窄读：只看得到本联盟，看不到其他联盟")
    void list_organizerSeesOwnLeagueOnly() {
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(10L), Set.of());
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleLeagueIds(scope)).thenReturn(List.of(10L));
        when(leagueRepository.findByTenantIdAndIdInAndDeletedAtIsNull(eq(TENANT_ID), eq(List.of(10L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(league(10L, TENANT_ID))));

        PageResult<League> result = service.list(1, 10, null, null);

        assertEquals(1, result.getList().size());
        assertEquals(10L, result.getList().get(0).getId().longValue());
        verify(leagueRepository).findByTenantIdAndIdInAndDeletedAtIsNull(eq(TENANT_ID), eq(List.of(10L)), any(Pageable.class));
        // 绝不回退全租户宽读
        verify(leagueRepository, never()).findByTenantIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    @DisplayName("list 空域：空页且不查库")
    void list_emptyScope_emptyPage_noQuery() {
        EffectiveScope scope = EffectiveScope.empty();
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleLeagueIds(scope)).thenReturn(List.of());

        PageResult<League> result = service.list(1, 10, null, null);

        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
        verifyNoInteractions(leagueRepository);
    }

    @Test
    @DisplayName("list 漏带头（宽读）：跳过过滤，按租户全量分页")
    void list_wideRead_queriesTenant() {
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(), Set.of());
        when(tenantQueryPolicyService.isGlobalQueryMode()).thenReturn(false);
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleLeagueIds(scope)).thenReturn(null);
        when(leagueRepository.findByTenantIdAndDeletedAtIsNull(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(league(1L, TENANT_ID), league(2L, TENANT_ID))));

        PageResult<League> result = service.list(1, 10, null, null);

        assertEquals(2, result.getList().size());
        verify(leagueRepository).findByTenantIdAndDeletedAtIsNull(eq(TENANT_ID), any(Pageable.class));
        verify(leagueRepository, never()).findByTenantIdAndIdInAndDeletedAtIsNull(any(), any(), any());
    }

    // ------------------------------------------------------------------ 读：get

    @Test
    @DisplayName("get 本联盟命中：返回联盟")
    void get_ownLeague_returns() {
        League l = league(10L, TENANT_ID);
        when(leagueRepository.findById(10L)).thenReturn(Optional.of(l));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(10L), Set.of());
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleLeagueIds(scope)).thenReturn(List.of(10L));

        assertEquals(l, service.get(10L));
    }

    @Test
    @DisplayName("get 他域：403「无权查看该联盟」")
    void get_otherDomain_forbidden() {
        when(leagueRepository.findById(11L)).thenReturn(Optional.of(league(11L, TENANT_ID)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        EffectiveScope scope = EffectiveScope.restricted(false, Set.of(10L), Set.of());
        when(accountScopeService.resolveCurrent()).thenReturn(scope);
        when(scopeQuerySupport.visibleLeagueIds(scope)).thenReturn(List.of(10L));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(11L));
        assertEquals(403, ex.getCode());
        assertEquals("无权查看该联盟", ex.getMessage());
    }

    @Test
    @DisplayName("get 跨租户：租户校验分支返回 null")
    void get_crossTenant_null() {
        when(leagueRepository.findById(10L)).thenReturn(Optional.of(league(10L, OTHER_TENANT)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        assertNull(service.get(10L));
        verifyNoInteractions(scopeQuerySupport);
    }

    // ------------------------------------------------------------------ 写：update

    @Test
    @DisplayName("update 他联盟：ResourceGuard 403")
    void update_otherLeague_guardForbidden() {
        League updated = new League();
        updated.setName("改名");
        when(leagueRepository.findById(11L)).thenReturn(Optional.of(league(11L, TENANT_ID)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        doThrow(new BusinessException(403, "无权管理该联盟")).when(resourceGuard).assertCanManageLeague(11L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(11L, updated));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该联盟", ex.getMessage());
        verify(resourceGuard).assertCanManageLeague(11L);
    }

    @Test
    @DisplayName("update 跨租户：租户校验先行 403，守卫不被触达")
    void update_crossTenant_forbiddenBeforeGuard() {
        when(leagueRepository.findById(11L)).thenReturn(Optional.of(league(11L, OTHER_TENANT)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(11L, new League()));
        assertEquals(403, ex.getCode());
        assertEquals("无权修改该联盟", ex.getMessage());
        verify(resourceGuard, never()).assertCanManageLeague(any());
        verify(leagueRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ 写：delete

    @Test
    @DisplayName("delete 他联盟：ResourceGuard 403（越权写）")
    void delete_otherLeague_guardForbidden() {
        when(leagueRepository.findById(11L)).thenReturn(Optional.of(league(11L, TENANT_ID)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);
        doThrow(new BusinessException(403, "无权管理该联盟")).when(resourceGuard).assertCanManageLeague(11L);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(11L));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该联盟", ex.getMessage());
        verify(resourceGuard).assertCanManageLeague(11L);
        verify(leagueRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete 跨租户：租户校验先行 403，守卫不被触达")
    void delete_crossTenant_forbiddenBeforeGuard() {
        when(leagueRepository.findById(11L)).thenReturn(Optional.of(league(11L, OTHER_TENANT)));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(11L));
        assertEquals(403, ex.getCode());
        assertEquals("无权删除该联盟", ex.getMessage());
        verify(resourceGuard, never()).assertCanManageLeague(any());
        verify(leagueRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete 本联盟：守卫放行并落库（接线到 guard + 软删）")
    void delete_ownLeague_guardCalledAndSoftDeleted() {
        League existing = league(10L, TENANT_ID);
        when(leagueRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(tenantQueryPolicyService.requiredTenantId()).thenReturn(TENANT_ID);

        service.delete(10L);

        verify(resourceGuard).assertCanManageLeague(10L);
        verify(leagueRepository).save(existing);
        assertNotNull(existing.getDeletedAt());
    }

    private static League league(long id, long tenantId) {
        League l = new League();
        l.setId(id);
        l.setTenantId(tenantId);
        l.setName("联盟" + id);
        return l;
    }
}
