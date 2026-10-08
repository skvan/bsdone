/*
 * 账号权限重构（批次 3a，评审修复 I2/I3/I6 + M2）：LeagueOwnerAssignService 单元测试（Mockito 手工装配）。
 *
 * 覆盖：
 *  - assignInternal：幂等 no-op（exists → 不 save、仍 evict）/ 插入成功（active / GRANT 常量 / evict）；
 *  - assign / revoke / handover：401 / 403、跨租户 403；
 *  - revoke：软删 + inactive + evict；
 *  - handover：from == to → no-op（M2）；from 无 active 行 = 纯指派；
 *  - listOwners（I2）：守卫 401/403、跨租户 403、超管跨租户放行、DTO 字段集精确。
 *
 * evict 后置：经 accountScopeService.evictUserScopeCacheAfterCommit（后置提交）；本测试验证接线调用，
 * 提交/回滚即时性由 AccountScopeServiceTest 覆盖。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.LeagueOwnerDto;
import com.bsball.model.entity.League;
import com.bsball.model.entity.LeagueOwner;
import com.bsball.repository.LeagueOwnerRepository;
import com.bsball.repository.LeagueRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueOwnerAssignService：主办方指派/交接/撤回 + owners 守卫（#154）")
class LeagueOwnerAssignServiceTest {

    private static final long TENANT_ID = 10L;
    private static final long OTHER_TENANT = 99L;
    private static final long LEAGUE_ID = 88L;

    @Mock
    private LeagueOwnerRepository leagueOwnerRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private ApiPermissionService apiPermissionService;

    @Mock
    private AccountScopeService accountScopeService;

    private LeagueOwnerAssignService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new LeagueOwnerAssignService(leagueOwnerRepository, leagueRepository, apiPermissionService,
                accountScopeService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------- assignInternal

    @Test
    @DisplayName("assignInternal 插入成功：active + SELF_CREATE + evict")
    void assignInternal_insert_success() {
        when(leagueOwnerRepository.existsByLeagueIdAndUserIdAndDeletedAtIsNull(LEAGUE_ID, 7L)).thenReturn(false);
        when(leagueOwnerRepository.save(any(LeagueOwner.class))).thenAnswer(inv -> {
            LeagueOwner o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        LeagueOwner saved = service.assignInternal(7L, TENANT_ID, LEAGUE_ID, LeagueOwner.GRANT_SELF_CREATE);

        assertNotNull(saved);
        ArgumentCaptor<LeagueOwner> captor = ArgumentCaptor.forClass(LeagueOwner.class);
        verify(leagueOwnerRepository).save(captor.capture());
        LeagueOwner entity = captor.getValue();
        assertEquals(TENANT_ID, entity.getTenantId());
        assertEquals(LEAGUE_ID, entity.getLeagueId());
        assertEquals(7L, entity.getUserId());
        assertEquals(LeagueOwner.STATUS_ACTIVE, entity.getStatus());
        assertEquals(LeagueOwner.GRANT_SELF_CREATE, entity.getGrantSource());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(7L);
    }

    @Test
    @DisplayName("assignInternal 幂等 no-op：exists → 不 save、仍 evict")
    void assignInternal_exists_noop_stillEvict() {
        when(leagueOwnerRepository.existsByLeagueIdAndUserIdAndDeletedAtIsNull(LEAGUE_ID, 7L)).thenReturn(true);

        LeagueOwner result = service.assignInternal(7L, TENANT_ID, LEAGUE_ID, LeagueOwner.GRANT_SELF_CREATE);

        assertNull(result);
        verify(leagueOwnerRepository, never()).save(any());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(7L);
    }

    @Test
    @DisplayName("assignInternal 入参缺失：直接 no-op（不查库、不 evict）")
    void assignInternal_nullArgs_noop() {
        assertNull(service.assignInternal(null, TENANT_ID, LEAGUE_ID, LeagueOwner.GRANT_ADMIN_ASSIGN));

        verifyNoInteractions(leagueOwnerRepository);
        verifyNoInteractions(accountScopeService);
    }

    // ------------------------------------------------------------- assign 守卫

    @Test
    @DisplayName("assign 未登录：401「请先登录」")
    void assign_unauthorized_401() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(null, LEAGUE_ID, 7L));
        assertEquals(401, ex.getCode());
        verifyNoInteractions(leagueRepository);
    }

    @Test
    @DisplayName("assign 非管理员：403「仅管理员可管理联盟主办方」")
    void assign_nonAdmin_403() {
        CurrentUserHolder.set(2L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(2L, LEAGUE_ID, 7L));
        assertEquals(403, ex.getCode());
        assertEquals("仅管理员可管理联盟主办方", ex.getMessage());
        verifyNoInteractions(leagueRepository);
    }

    @Test
    @DisplayName("assign 租管跨租户：403「无权管理该联盟」")
    void assign_crossTenant_403() {
        CurrentUserHolder.set(2L, OTHER_TENANT);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(true);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.assign(2L, LEAGUE_ID, 7L));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该联盟", ex.getMessage());
        verify(leagueOwnerRepository, never()).save(any());
    }

    // ------------------------------------------------------------- revoke

    @Test
    @DisplayName("revoke 成功：active 行置 inactive + 软删 + evict")
    void revoke_success_softDeleteAndInactive() {
        CurrentUserHolder.set(1L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));
        LeagueOwner active = owner(200L, 7L, TENANT_ID);
        when(leagueOwnerRepository.findByLeagueIdAndStatusAndDeletedAtIsNull(LEAGUE_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of(active));

        service.revoke(1L, LEAGUE_ID, 7L);

        assertEquals(LeagueOwner.STATUS_INACTIVE, active.getStatus());
        assertNotNull(active.getDeletedAt());
        assertEquals(1L, active.getDeletedBy());
        verify(leagueOwnerRepository).save(active);
        verify(accountScopeService).evictUserScopeCacheAfterCommit(7L);
    }

    @Test
    @DisplayName("revoke 非管理员：403")
    void revoke_nonAdmin_403() {
        CurrentUserHolder.set(2L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.revoke(2L, LEAGUE_ID, 7L));
        assertEquals(403, ex.getCode());
        verifyNoInteractions(leagueOwnerRepository);
    }

    // ------------------------------------------------------------- handover

    @Test
    @DisplayName("handover 非管理员：403")
    void handover_nonAdmin_403() {
        CurrentUserHolder.set(2L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.handover(2L, LEAGUE_ID, 7L, 8L));
        assertEquals(403, ex.getCode());
        verifyNoInteractions(leagueOwnerRepository);
    }

    @Test
    @DisplayName("handover from == to：直接 no-op（不 revoke / 不 assign / 不 evict）（M2）")
    void handover_fromEqualsTo_noop() {
        CurrentUserHolder.set(1L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));

        service.handover(1L, LEAGUE_ID, 7L, 7L);

        verifyNoInteractions(leagueOwnerRepository);
        verifyNoInteractions(accountScopeService);
    }

    @Test
    @DisplayName("handover from 无 active 行：退化为纯指派 to + evict from/to")
    void handover_fromNoActiveRow_pureAssign() {
        CurrentUserHolder.set(1L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));
        when(leagueOwnerRepository.findByLeagueIdAndStatusAndDeletedAtIsNull(LEAGUE_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of());
        when(leagueOwnerRepository.existsByLeagueIdAndUserIdAndDeletedAtIsNull(LEAGUE_ID, 8L)).thenReturn(false);
        when(leagueOwnerRepository.save(any(LeagueOwner.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handover(1L, LEAGUE_ID, 7L, 8L);

        ArgumentCaptor<LeagueOwner> captor = ArgumentCaptor.forClass(LeagueOwner.class);
        verify(leagueOwnerRepository).save(captor.capture());
        assertEquals(8L, captor.getValue().getUserId());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(7L);
        verify(accountScopeService).evictUserScopeCacheAfterCommit(8L);
    }

    @Test
    @DisplayName("handover 成功：revoke(from) + assign(to, ADMIN_ASSIGN) + evict 双方")
    void handover_success_revokeAndAssign() {
        CurrentUserHolder.set(1L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));
        LeagueOwner fromActive = owner(200L, 7L, TENANT_ID);
        when(leagueOwnerRepository.findByLeagueIdAndStatusAndDeletedAtIsNull(LEAGUE_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of(fromActive));
        when(leagueOwnerRepository.existsByLeagueIdAndUserIdAndDeletedAtIsNull(LEAGUE_ID, 8L)).thenReturn(false);
        when(leagueOwnerRepository.save(any(LeagueOwner.class))).thenAnswer(inv -> inv.getArgument(0));

        service.handover(1L, LEAGUE_ID, 7L, 8L);

        assertEquals(LeagueOwner.STATUS_INACTIVE, fromActive.getStatus());
        assertNotNull(fromActive.getDeletedAt());
        ArgumentCaptor<LeagueOwner> captor = ArgumentCaptor.forClass(LeagueOwner.class);
        verify(leagueOwnerRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(8L, captor.getAllValues().get(1).getUserId());
        verify(accountScopeService).evictUserScopeCacheAfterCommit(7L);
        verify(accountScopeService).evictUserScopeCacheAfterCommit(8L);
    }

    // ------------------------------------------------------------- listOwners（I2）

    @Test
    @DisplayName("listOwners 未登录：401")
    void listOwners_unauthorized_401() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.listOwners(null, LEAGUE_ID));
        assertEquals(401, ex.getCode());
        verifyNoInteractions(leagueOwnerRepository);
    }

    @Test
    @DisplayName("listOwners 非管理员：403")
    void listOwners_nonAdmin_403() {
        CurrentUserHolder.set(2L, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.listOwners(2L, LEAGUE_ID));
        assertEquals(403, ex.getCode());
        verifyNoInteractions(leagueOwnerRepository);
    }

    @Test
    @DisplayName("listOwners 租管跨租户：403（可跨租户枚举被阻断）")
    void listOwners_crossTenantTenantAdmin_403() {
        CurrentUserHolder.set(2L, OTHER_TENANT);
        when(apiPermissionService.isSuperAdmin(2L)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(2L)).thenReturn(true);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.listOwners(2L, LEAGUE_ID));
        assertEquals(403, ex.getCode());
        assertEquals("无权管理该联盟", ex.getMessage());
        verifyNoInteractions(leagueOwnerRepository);
    }

    @Test
    @DisplayName("listOwners 超管跨租户放行：返回精简 DTO（字段集精确）")
    void listOwners_superAdminCrossTenant_dtoFieldsExact() {
        CurrentUserHolder.set(1L, 0L);
        when(apiPermissionService.isSuperAdmin(1L)).thenReturn(true);
        when(leagueRepository.findById(LEAGUE_ID)).thenReturn(Optional.of(league(LEAGUE_ID, TENANT_ID)));
        LeagueOwner a = owner(200L, 7L, TENANT_ID);
        a.setGrantSource(LeagueOwner.GRANT_SELF_CREATE);
        a.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(leagueOwnerRepository.findByLeagueIdAndStatusAndDeletedAtIsNull(LEAGUE_ID, LeagueOwner.STATUS_ACTIVE))
                .thenReturn(List.of(a));

        List<LeagueOwnerDto> out = service.listOwners(1L, LEAGUE_ID);

        assertEquals(1, out.size());
        LeagueOwnerDto dto = out.get(0);
        assertEquals(7L, dto.userId());
        assertEquals(LeagueOwner.GRANT_SELF_CREATE, dto.grantSource());
        assertEquals(LeagueOwner.STATUS_ACTIVE, dto.status());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), dto.createdAt());

        // 字段集精确：仅 userId / grantSource / status / createdAt（无审计 / 租户字段）
        assertEquals(4, LeagueOwnerDto.class.getRecordComponents().length);
        Set<String> names = Arrays.stream(LeagueOwnerDto.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(Collectors.toSet());
        assertEquals(Set.of("userId", "grantSource", "status", "createdAt"), names);
    }

    // ------------------------------------------------------------- helpers

    private static League league(long id, long tenantId) {
        League l = new League();
        l.setId(id);
        l.setTenantId(tenantId);
        l.setName("联盟" + id);
        return l;
    }

    private static LeagueOwner owner(long id, long userId, long tenantId) {
        LeagueOwner o = new LeagueOwner();
        o.setId(id);
        o.setUserId(userId);
        o.setLeagueId(LEAGUE_ID);
        o.setTenantId(tenantId);
        o.setStatus(LeagueOwner.STATUS_ACTIVE);
        return o;
    }
}
