/*
 * 账号权限重构（批次 3b，Task 3.12）：租户退租封禁落地测试（spec §6.8）。
 *
 * 覆盖：
 *  - 四通路封禁：login 停用/软删、loginWithoutPassword 停用、switchTenant 非超管停用 → 403「租户已停止运营」；
 *    码通路带码但不可用（停用/解析不到）→ 404「租户不存在」（终态，不落默认回退）；
 *  - 超管豁免：ID 头 / JWT 通路超管一律放行（含指向停用/软删租户）；switchTenant 超管路径维持现状；
 *  - 续租恢复：status 0→1 + evict 后可重新登录；requireActive 缓存语义（二次不查库、evict 后重查）；
 *  - 退租/续租侧效：管理更新 status 1→0 / 0→1 失效可用性与码缓存并留痕；仅改名不留痕不失效。
 *
 * 风格：外部依赖 Mockito mock；TenantAccessGuard 用真实实例（sysTenantRepository mock 驱动），
 * 以真实执行缓存读写与 requireActive；不启动 Spring、不连库。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.exception.BusinessException;
import com.bsball.exception.UnauthorizedException;
import com.bsball.model.entity.SysOperationLog;
import com.bsball.model.entity.SysTenant;
import com.bsball.model.entity.SysUser;
import com.bsball.model.entity.SysUserTenant;
import com.bsball.repository.LeagueRepository;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.repository.TeamRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("租户退租封禁（批次3b Task3.12 / spec §6.8）")
class TenantRetireGuardTest {

    private static final long USER_ID = 7L;
    private static final long OPERATOR_ID = 1L;
    private static final long TENANT_ID = 10L;

    @Mock
    private SysTenantRepository sysTenantRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysUserTenantRepository sysUserTenantRepository;
    @Mock
    private SysUserRoleRepository sysUserRoleRepository;
    @Mock
    private SysRoleMenuRepository sysRoleMenuRepository;
    @Mock
    private SysMenuRepository sysMenuRepository;
    @Mock
    private ApiPermissionService apiPermissionService;
    @Mock
    private JwtService jwtService;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private TenantResolutionService tenantResolutionService;
    @Mock
    private OperationLogAsyncService operationLogAsyncService;
    @Mock
    private IpLocationCacheService ipLocationCacheService;
    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private TeamRepository teamRepository;

    private TenantAccessGuard tenantAccessGuard;
    private AuthService authService;
    private SysTenantManageService tenantManageService;

    @BeforeEach
    void setUp() {
        tenantAccessGuard = new TenantAccessGuard(sysTenantRepository);
        tenantAccessGuard.initTenantAccessCache();
        authService = new AuthService(apiPermissionService, jwtService, tenantProperties,
                sysUserRepository, sysUserRoleRepository, sysRoleMenuRepository, sysMenuRepository,
                sysUserTenantRepository, sysTenantRepository, tenantAccessGuard);
        tenantManageService = new SysTenantManageService(apiPermissionService, sysTenantRepository,
                leagueRepository, teamRepository, tenantAccessGuard, tenantResolutionService,
                operationLogAsyncService, sysUserRepository, ipLocationCacheService);
    }

    // ---------------------------------------------------------------- 登录/切换封禁

    @Test
    @DisplayName("登录：requestedTenantId 指向停用租户 → 403「租户已停止运营」，不发 token")
    void login_disabledTenant_rejected() {
        when(sysUserRepository.findByUsernameAndDeletedAtIsNull("coach")).thenReturn(Optional.of(user(USER_ID, 1)));
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(List.of(userTenant(USER_ID, TENANT_ID)));
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("coach", "pwd", TENANT_ID));

        assertEquals(403, ex.getCode());
        assertEquals("租户已停止运营", ex.getMessage());
        verify(jwtService, never()).createToken(any(), any());
    }

    @Test
    @DisplayName("登录：requestedTenantId 指向软删租户 → 403「租户已停止运营」")
    void login_softDeletedTenant_rejected() {
        when(sysUserRepository.findByUsernameAndDeletedAtIsNull("coach")).thenReturn(Optional.of(user(USER_ID, 1)));
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(List.of(userTenant(USER_ID, TENANT_ID)));
        when(sysTenantRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(tenant(TENANT_ID, 1, LocalDateTime.now())));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login("coach", "pwd", TENANT_ID));

        assertEquals(403, ex.getCode());
        assertEquals("租户已停止运营", ex.getMessage());
    }

    @Test
    @DisplayName("免密登录：requestedTenantId 指向停用租户 → 403「租户已停止运营」")
    void loginWithoutPassword_disabledTenant_rejected() {
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(List.of(userTenant(USER_ID, TENANT_ID)));
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.loginWithoutPassword(user(USER_ID, 1), TENANT_ID));

        assertEquals(403, ex.getCode());
        assertEquals("租户已停止运营", ex.getMessage());
    }

    @Test
    @DisplayName("切换租户：非超管切到停用租户 → 403「租户已停止运营」")
    void switchTenant_nonSuper_disabledTenant_rejected() {
        when(sysUserRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, 1)));
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(sysUserTenantRepository.existsByUserIdAndTenantIdAndDeletedAtIsNull(USER_ID, TENANT_ID)).thenReturn(true);
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.switchTenant(USER_ID, TENANT_ID));

        assertEquals(403, ex.getCode());
        assertEquals("租户已停止运营", ex.getMessage());
    }

    @Test
    @DisplayName("切换租户：超管路径维持现状（切到停用租户 → 403「无权切换到该租户」）")
    void switchTenant_superAdmin_disabledTenant_unchanged() {
        when(sysUserRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID, 1)));
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(true);
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.switchTenant(USER_ID, TENANT_ID));

        assertEquals(403, ex.getCode());
        assertEquals("无权切换到该租户", ex.getMessage());
    }

    @Test
    @DisplayName("登录：非成员 + 停用租户 → 401（归属优先，不暴露停运）")
    void login_nonMemberDisabledTenant_membershipFirst401() {
        when(sysUserRepository.findByUsernameAndDeletedAtIsNull("coach")).thenReturn(Optional.of(user(USER_ID, 1)));
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        // 仅归属其它租户 → 对请求租户 TENANT_ID 非成员
        when(sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(List.of(userTenant(USER_ID, 99L)));
        // 目标租户已停用（停运信息不该被读取/暴露）
        lenient().when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));

        // UnauthorizedException 由 GlobalExceptionHandler 映射为 401
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login("coach", "pwd", TENANT_ID));

        assertEquals("用户名或密码错误", ex.getMessage());
        verify(sysTenantRepository, never()).findById(TENANT_ID);
    }

    // ---------------------------------------------------------------- 三类请求级校验（纯函数）

    @Test
    @DisplayName("请求级·码通路：带码但不可用 → 404「租户不存在」（终态，不落默认回退）")
    void codeHeader_unusable_terminalNotFound() {
        assertEquals(TenantAccessGuard.Decision.TENANT_NOT_FOUND,
                TenantAccessGuard.decideCodeHeader("acme", Optional.of(tenant(TENANT_ID, 0, null))));
        assertEquals(TenantAccessGuard.Decision.TENANT_NOT_FOUND,
                TenantAccessGuard.decideCodeHeader("gone", Optional.empty()));
        assertTrue(TenantAccessGuard.Decision.TENANT_NOT_FOUND.rejected());
        assertEquals(404, TenantAccessGuard.Decision.TENANT_NOT_FOUND.code());
        assertEquals("租户不存在", TenantAccessGuard.Decision.TENANT_NOT_FOUND.msg());
        // 可用租户 / 无码 → 放行（继续既有解析链）
        assertEquals(TenantAccessGuard.Decision.ALLOW,
                TenantAccessGuard.decideCodeHeader("acme", Optional.of(tenant(TENANT_ID, 1, null))));
        assertEquals(TenantAccessGuard.Decision.ALLOW, TenantAccessGuard.decideCodeHeader(null, Optional.empty()));
    }

    @Test
    @DisplayName("请求级·ID头/JWT：非超管不可用 → 403「租户已停止运营」；超管豁免")
    void strictTenant_nonSuper403_superAdminExempt() {
        assertEquals(TenantAccessGuard.Decision.TENANT_STOPPED, TenantAccessGuard.decideStrictTenant(false, false));
        assertEquals(TenantAccessGuard.Decision.ALLOW, TenantAccessGuard.decideStrictTenant(false, true));
        // 超管豁免：指向停用/软删租户仍放行（归档查看可用）
        assertEquals(TenantAccessGuard.Decision.ALLOW, TenantAccessGuard.decideStrictTenant(true, false));
        assertEquals(403, TenantAccessGuard.Decision.TENANT_STOPPED.code());
        assertEquals("租户已停止运营", TenantAccessGuard.Decision.TENANT_STOPPED.msg());
    }

    // ---------------------------------------------------------------- 续租恢复 + 缓存语义

    @Test
    @DisplayName("续租恢复：status 0→1 + evict 后可重新登录")
    void renew_recovery_loginSucceedsAfterEvict() {
        when(sysUserRepository.findByUsernameAndDeletedAtIsNull("coach")).thenReturn(Optional.of(user(USER_ID, 1)));
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(USER_ID))
                .thenReturn(List.of(userTenant(USER_ID, TENANT_ID)));
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));
        when(jwtService.createToken(USER_ID, TENANT_ID)).thenReturn("token-ok");

        // 退租中：拒绝
        assertThrows(BusinessException.class, () -> authService.login("coach", "pwd", TENANT_ID));

        // 续租：库恢复启用 + 失效缓存
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 1, null)));
        tenantAccessGuard.evict(TENANT_ID);

        Map<String, Object> res = authService.login("coach", "pwd", TENANT_ID);
        assertEquals("token-ok", res.get("token"));
    }

    @Test
    @DisplayName("requireActive 缓存：二次调用不查库；evict 后重查")
    void requireActive_cacheThenEvictReloads() {
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 1, null)));

        tenantAccessGuard.requireActive(TENANT_ID);
        tenantAccessGuard.requireActive(TENANT_ID);
        verify(sysTenantRepository, times(1)).findById(TENANT_ID);

        // 库变更但缓存未失效：仍视为可用（不再查库）
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));
        tenantAccessGuard.requireActive(TENANT_ID);
        verify(sysTenantRepository, times(1)).findById(TENANT_ID);

        // evict 后重查 → 反映停用
        tenantAccessGuard.evict(TENANT_ID);
        assertThrows(BusinessException.class, () -> tenantAccessGuard.requireActive(TENANT_ID));
        verify(sysTenantRepository, times(2)).findById(TENANT_ID);
    }

    // ---------------------------------------------------------------- 退租/续租侧效

    @Test
    @DisplayName("管理更新：退租(1→0) 失效可用性+码缓存 + 留痕；evict 后立即封禁")
    void update_retire_evictsAndAudits() {
        when(apiPermissionService.isSuperAdmin(OPERATOR_ID)).thenReturn(true);
        SysTenant existing = tenant(TENANT_ID, 1, null);
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(existing));
        when(sysTenantRepository.save(any(SysTenant.class))).thenAnswer(inv -> inv.getArgument(0));

        // 预置可用性缓存 = true
        tenantAccessGuard.requireActive(TENANT_ID);

        SysTenant body = new SysTenant();
        body.setStatus(0);
        body.setDescription("合同到期，退租");
        tenantManageService.update(OPERATOR_ID, TENANT_ID, body);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogAsyncService).saveAsync(captor.capture());
        assertEquals("退租", captor.getValue().getAction());
        assertEquals("合同到期，退租", captor.getValue().getDescription());
        assertEquals(Long.valueOf(OPERATOR_ID), captor.getValue().getUserId());
        verify(tenantResolutionService).evictTenantByCode("t" + TENANT_ID);

        // 缓存已失效 → 库改为停用后立即封禁
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));
        assertThrows(BusinessException.class, () -> tenantAccessGuard.requireActive(TENANT_ID));
    }

    @Test
    @DisplayName("管理更新：续租(0→1) 失效缓存 + 留痕「续租」")
    void update_renew_evictsAndAudits() {
        when(apiPermissionService.isSuperAdmin(OPERATOR_ID)).thenReturn(true);
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 0, null)));
        when(sysTenantRepository.save(any(SysTenant.class))).thenAnswer(inv -> inv.getArgument(0));

        SysTenant body = new SysTenant();
        body.setStatus(1);
        tenantManageService.update(OPERATOR_ID, TENANT_ID, body);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(operationLogAsyncService).saveAsync(captor.capture());
        assertEquals("续租", captor.getValue().getAction());
        verify(tenantResolutionService).evictTenantByCode("t" + TENANT_ID);
    }

    @Test
    @DisplayName("管理更新：仅改名（status 不变）→ 不失效缓存、不留痕")
    void update_nameOnly_noSideEffects() {
        when(apiPermissionService.isSuperAdmin(OPERATOR_ID)).thenReturn(true);
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant(TENANT_ID, 1, null)));
        when(sysTenantRepository.save(any(SysTenant.class))).thenAnswer(inv -> inv.getArgument(0));

        SysTenant body = new SysTenant();
        body.setName("新名字");
        tenantManageService.update(OPERATOR_ID, TENANT_ID, body);

        verifyNoInteractions(operationLogAsyncService);
        verifyNoInteractions(tenantResolutionService);
    }

    // ---------------------------------------------------------------- 新建/改码缓存失效（I2）

    @Test
    @DisplayName("管理新建：保存后失效码缓存与可用性缓存（消除新建后按码解析 404/负缓存窗口）")
    void create_evictsCodeAndAccessCaches() {
        when(apiPermissionService.isSuperAdmin(OPERATOR_ID)).thenReturn(true);
        when(sysTenantRepository.existsByCodeAndDeletedAtIsNull("newcode")).thenReturn(false);
        when(sysTenantRepository.save(any(SysTenant.class))).thenAnswer(inv -> {
            SysTenant t = inv.getArgument(0);
            t.setId(88L);
            return t;
        });

        // 预置守卫负缓存：库中尚无该租户 → 判定不可用
        assertThrows(BusinessException.class, () -> tenantAccessGuard.requireActive(88L));

        SysTenant body = new SysTenant();
        body.setCode("newcode");
        body.setName("新租户");
        SysTenant saved = tenantManageService.create(OPERATOR_ID, body);
        assertEquals(Long.valueOf(88L), saved.getId());

        // 码缓存失效（负缓存窗口消除）
        verify(tenantResolutionService).evictTenantByCode("newcode");

        // 守卫缓存防御性失效：库出现该启用租户后，下次判定即刻放行（未失效则命中旧 false）
        when(sysTenantRepository.findById(88L)).thenReturn(Optional.of(tenant(88L, 1, null)));
        tenantAccessGuard.requireActive(88L);
    }

    @Test
    @DisplayName("管理改码：旧/新 code 缓存均失效；无状态变更不落留痕")
    void update_codeChange_evictsBothCodes() {
        when(apiPermissionService.isSuperAdmin(OPERATOR_ID)).thenReturn(true);
        SysTenant existing = tenant(TENANT_ID, 1, null);
        when(sysTenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(existing));
        when(sysTenantRepository.existsByCodeAndDeletedAtIsNullAndIdNot("t10-new", TENANT_ID)).thenReturn(false);
        when(sysTenantRepository.save(any(SysTenant.class))).thenAnswer(inv -> inv.getArgument(0));

        SysTenant body = new SysTenant();
        body.setCode("t10-new");
        tenantManageService.update(OPERATOR_ID, TENANT_ID, body);

        verify(tenantResolutionService).evictTenantByCode("t10");
        verify(tenantResolutionService).evictTenantByCode("t10-new");
        // status 未变 → 不落退租/续租留痕
        verifyNoInteractions(operationLogAsyncService);
    }

    // ---------------------------------------------------------------- helpers

    private static SysUser user(long id, int status) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername("coach");
        u.setStatus(status);
        u.setPassword("pwd");
        return u;
    }

    private static SysUserTenant userTenant(long userId, long tenantId) {
        SysUserTenant ut = new SysUserTenant();
        ut.setUserId(userId);
        ut.setTenantId(tenantId);
        return ut;
    }

    private static SysTenant tenant(long id, int status, LocalDateTime deletedAt) {
        SysTenant t = new SysTenant();
        t.setId(id);
        t.setCode("t" + id);
        t.setName("租户" + id);
        t.setStatus(status);
        t.setDeletedAt(deletedAt);
        return t;
    }
}
