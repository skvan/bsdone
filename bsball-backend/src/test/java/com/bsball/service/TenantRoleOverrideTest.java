/*
 * 账号权限重构（批次 3b，Task 3.15b / spec §8.6）：门户角色租户级目录覆盖层测试。
 *
 * 覆盖：
 *  - 生效合并：存在覆盖行 → 覆盖集合（可空）替换全局；无覆盖 → 回落全局；非门户角色照旧全局；
 *  - 租户隔离：T1 覆盖不影响 T2；
 *  - 全量替换 + 全局零污染：saveOverride 仅写覆盖两表，绝不触碰全局角色/绑定；
 *  - 读路径接入：AuthService 派生（经租户配置服务）与 ApiPermissionService 的 API 集合派生；
 *  - 执行者矩阵（服务层）：租管 own ✓ / 他租户 ✗ / 平台菜单 ✗403 / 超管任意 ✓；仅平台门户角色可覆盖；
 *  - list() 标记；缓存失效 verify。
 *
 * 风格：Mockito mock 外部依赖；TenantRoleConfigService / ApiPermissionService 用真实实例驱动合并与 API 派生；
 * SysRoleService 以 mock 权限判定驱动越权矩阵；不启动 Spring、不连库；CurrentUserHolder（ThreadLocal）逐用例清理。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.common.PageResult;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysMenuApi;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysRoleMenu;
import com.bsball.model.entity.SysTenant;
import com.bsball.model.entity.SysUser;
import com.bsball.model.entity.SysUserRole;
import com.bsball.model.entity.TenantRoleMenu;
import com.bsball.model.entity.TenantRoleMenuConfig;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysMenuApiRepository;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.repository.TenantRoleMenuConfigRepository;
import com.bsball.repository.TenantRoleMenuRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("门户角色租户级目录覆盖层（批次 3b T3.15b / spec §8.6）")
class TenantRoleOverrideTest {

    private static final long USER_ID = 7L;
    private static final long OPERATOR_ID = 9L;
    private static final long SUPER_ID = 1L;
    private static final long TENANT_T = 10L;
    private static final long TENANT_T2 = 20L;
    private static final long TM_ROLE_ID = 500L;
    private static final long LO_ROLE_ID = 501L;
    private static final long TENANT_ADMIN_ROLE_ID = 600L;
    private static final long MENU_A = 100L;
    private static final long MENU_B = 101L;
    private static final long PLATFORM_MENU_ID = 200L;
    private static final long API_A = 300L;
    private static final long API_B = 301L;
    // 与任意菜单无关的「基线功能 API」（role_api / 预设能力）：覆盖不应剔除它。
    private static final long BIZ_BASE_API = 350L;

    @Mock
    private TenantRoleMenuConfigRepository configRepository;
    @Mock
    private TenantRoleMenuRepository tenantMenuRepository;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private SysRoleMenuRepository sysRoleMenuRepository;
    @Mock
    private SysRoleApiRepository sysRoleApiRepository;
    @Mock
    private SysMenuApiRepository sysMenuApiRepository;
    @Mock
    private SysApiRepository sysApiRepository;
    @Mock
    private SysUserRoleRepository sysUserRoleRepository;
    @Mock
    private SysTenantRepository sysTenantRepository;
    @Mock
    private SysMenuRepository sysMenuRepository;
    @Mock
    private MenuExpansionHelper menuExpansionHelper;
    @Mock
    private ApiPermissionService apiPermissionService;
    @Mock
    private JwtService jwtService;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysUserTenantRepository sysUserTenantRepository;
    @Mock
    private TenantAccessGuard tenantAccessGuard;

    private TenantRoleConfigService tenantRoleConfigService;
    private ApiPermissionService apiDerivationService;
    private SysRoleService sysRoleService;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        ApiPermissionService.clearRequestCache();
        tenantRoleConfigService = new TenantRoleConfigService(configRepository, tenantMenuRepository,
                sysRoleRepository, sysRoleMenuRepository);
        sysRoleService = new SysRoleService(apiPermissionService, menuExpansionHelper, sysRoleRepository,
                sysRoleMenuRepository, sysRoleApiRepository, sysUserRoleRepository, sysTenantRepository,
                sysMenuRepository, sysApiRepository, tenantRoleConfigService);
        apiDerivationService = new ApiPermissionService(sysRoleRepository, sysRoleApiRepository,
                sysMenuApiRepository, sysApiRepository, sysUserRoleRepository, tenantRoleConfigService);
        apiDerivationService.initGuestPermissionCache();
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
        ApiPermissionService.clearRequestCache();
    }

    // ------------------------------------------------------------ 生效合并

    @Test
    @DisplayName("覆盖生效：存在覆盖行 → 覆盖集合替换全局；无覆盖 → 回落全局")
    void overrideReplacesGlobal_orFallsBack() {
        when(sysRoleRepository.findAllById(any())).thenReturn(List.of(role(TM_ROLE_ID, "team_manager", null)));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of(rm(TM_ROLE_ID, MENU_A)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T), any()))
                .thenReturn(List.of(config(1L, TENANT_T, TM_ROLE_ID)));
        when(tenantMenuRepository.findByConfigIdIn(List.of(1L))).thenReturn(List.of(trow(1L, MENU_B)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T2), any())).thenReturn(List.of());

        Set<Long> withOverride = tenantRoleConfigService.resolveEffectiveMenuIds(TENANT_T, List.of(TM_ROLE_ID));
        assertEquals(Set.of(MENU_B), withOverride, "存在覆盖行 → 用覆盖集合（不含全局 A）");

        Set<Long> fallback = tenantRoleConfigService.resolveEffectiveMenuIds(TENANT_T2, List.of(TM_ROLE_ID));
        assertEquals(Set.of(MENU_A), fallback, "无覆盖行 → 回落全局绑定");
    }

    @Test
    @DisplayName("空覆盖：config 行存在但明细为空 → 生效集合为空（该租户该角色无菜单）")
    void emptyOverride_yieldsEmpty() {
        when(sysRoleRepository.findAllById(any())).thenReturn(List.of(role(TM_ROLE_ID, "team_manager", null)));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of(rm(TM_ROLE_ID, MENU_A)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T), any()))
                .thenReturn(List.of(config(1L, TENANT_T, TM_ROLE_ID)));
        when(tenantMenuRepository.findByConfigIdIn(List.of(1L))).thenReturn(List.of());

        Set<Long> effective = tenantRoleConfigService.resolveEffectiveMenuIds(TENANT_T, List.of(TM_ROLE_ID));
        assertTrue(effective.isEmpty(), "空覆盖 → 生效集合为空（不回落全局）");
    }

    @Test
    @DisplayName("租户隔离 + 非门户角色全局：T1 覆盖生效、T2 不受影响；非门户角色照旧全局")
    void tenantIsolation_andNonPortalGlobal() {
        when(sysRoleRepository.findAllById(any())).thenReturn(List.of(
                role(TM_ROLE_ID, "team_manager", null), role(TENANT_ADMIN_ROLE_ID, "tenant_admin", TENANT_T)));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of(
                rm(TM_ROLE_ID, MENU_A), rm(TENANT_ADMIN_ROLE_ID, MENU_A)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T), any()))
                .thenReturn(List.of(config(1L, TENANT_T, TM_ROLE_ID)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T2), any())).thenReturn(List.of());
        when(tenantMenuRepository.findByConfigIdIn(List.of(1L))).thenReturn(List.of(trow(1L, MENU_B)));

        Set<Long> t1 = tenantRoleConfigService.resolveEffectiveMenuIds(TENANT_T,
                List.of(TM_ROLE_ID, TENANT_ADMIN_ROLE_ID));
        assertEquals(Set.of(MENU_A, MENU_B), t1, "T1：tm 走覆盖 B；tenant_admin 走全局 A");

        Set<Long> t2 = tenantRoleConfigService.resolveEffectiveMenuIds(TENANT_T2,
                List.of(TM_ROLE_ID, TENANT_ADMIN_ROLE_ID));
        assertEquals(Set.of(MENU_A), t2, "T2 无覆盖 → 全部回落全局 A（T1 覆盖不影响 T2）");
    }

    // ------------------------------------------------------------ 全量替换 + 全局零污染

    @Test
    @DisplayName("保存覆盖：upsert config 头 + 全量替换明细；绝不触碰全局角色/绑定")
    void saveOverride_fullReplace_neverTouchesGlobal() {
        when(configRepository.findByTenantIdAndRoleId(TENANT_T, TM_ROLE_ID)).thenReturn(Optional.empty());
        when(configRepository.save(any(TenantRoleMenuConfig.class))).thenAnswer(inv -> {
            TenantRoleMenuConfig c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(1L);
            }
            return c;
        });
        List<TenantRoleMenu> savedRows = new ArrayList<>();
        when(tenantMenuRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<TenantRoleMenu> arg = inv.getArgument(0);
            arg.forEach(savedRows::add);
            return arg;
        });

        TenantRoleMenuConfig cfg = tenantRoleConfigService.saveOverride(TENANT_T, TM_ROLE_ID, List.of(MENU_B), OPERATOR_ID);

        assertEquals(Long.valueOf(1L), cfg.getId());
        verify(tenantMenuRepository).deleteByConfigId(1L);
        assertTrue(savedRows.stream().anyMatch(r -> Objects.equals(r.getMenuId(), MENU_B)), "明细应落库");
        verify(sysRoleMenuRepository, never()).saveAll(any());
        verify(sysRoleMenuRepository, never()).deleteByRoleId(any());
        verify(sysRoleRepository, never()).save(any());
        verify(sysRoleApiRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("移除覆盖：clearOverride 删除 config 头与明细（仅覆盖两表）")
    void clearOverride_onlyTouchesOverrideTables() {
        when(configRepository.findByTenantIdAndRoleId(TENANT_T, TM_ROLE_ID))
                .thenReturn(Optional.of(config(1L, TENANT_T, TM_ROLE_ID)));

        tenantRoleConfigService.clearOverride(TENANT_T, TM_ROLE_ID);

        verify(tenantMenuRepository).deleteByConfigId(1L);
        verify(configRepository).deleteById(1L);
        verify(sysRoleMenuRepository, never()).deleteByRoleId(any());
        verify(sysRoleRepository, never()).deleteById(any());
    }

    // ------------------------------------------------------------ 读路径接入

    @Test
    @DisplayName("API 派生接入覆盖：覆盖调整菜单派生 API；保留基线 role_api")
    void apiDerivation_respectsOverride() {
        when(sysUserRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(ur(USER_ID, TM_ROLE_ID)));
        when(sysRoleRepository.findAllById(any())).thenReturn(List.of(role(TM_ROLE_ID, "team_manager", null)));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of(rm(TM_ROLE_ID, MENU_A)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T), any()))
                .thenReturn(List.of(config(1L, TENANT_T, TM_ROLE_ID)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T2), any())).thenReturn(List.of());
        when(tenantMenuRepository.findByConfigIdIn(List.of(1L))).thenReturn(List.of(trow(1L, MENU_B)));
        // 真实非空的 role_api：基线（预设）能力 BIZ_BASE_API，与任意菜单无关——覆盖不应剔除它。
        when(sysRoleApiRepository.findByRoleIdIn(any())).thenReturn(List.of(ra(TM_ROLE_ID, BIZ_BASE_API)));
        when(sysMenuApiRepository.findByMenuIdIn(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            List<SysMenuApi> out = new ArrayList<>();
            if (ids.contains(MENU_A)) {
                out.add(ma(MENU_A, API_A));
            }
            if (ids.contains(MENU_B)) {
                out.add(ma(MENU_B, API_B));
            }
            return out;
        });
        when(sysApiRepository.findAll()).thenReturn(List.of(
                api(API_A, "/biz/a", "GET"), api(API_B, "/biz/b", "GET"), api(BIZ_BASE_API, "/biz/base", "GET")));

        // 覆盖后（TENANT_T）：授予 = role_api ∪ menu_api(生效菜单 B)。
        CurrentUserHolder.set(USER_ID, TENANT_T);
        assertTrue(apiDerivationService.canUserAccessApi(USER_ID, "/biz/b", "GET"),
                "覆盖菜单 B 的 menu_api 派生 API 应放行");
        assertFalse(apiDerivationService.canUserAccessApi(USER_ID, "/biz/a", "GET"),
                "全局菜单 A 的 menu_api 派生 API 应被覆盖剔除");
        assertTrue(apiDerivationService.canUserAccessApi(USER_ID, "/biz/base", "GET"),
                "基线 role_api（BIZ_BASE_API）不受覆盖影响，应始终放行");

        // 无覆盖（TENANT_T2）：回落全局菜单 A，A 的派生 API 放行；基线仍放行。
        CurrentUserHolder.set(USER_ID, TENANT_T2);
        assertTrue(apiDerivationService.canUserAccessApi(USER_ID, "/biz/a", "GET"),
                "无覆盖 → 回落全局菜单 A，其 menu_api 派生 API 应放行");
        assertTrue(apiDerivationService.canUserAccessApi(USER_ID, "/biz/base", "GET"),
                "无覆盖 → 基线 role_api 仍放行");
    }

    // ------------------------------------------------------------ 端到端：AuthService.toAuthUser（真实合并服务驱动）

    @Test
    @DisplayName("端到端：真实 TenantRoleConfigService 驱动 AuthService.toAuthUser——有覆盖→覆盖集合且剔除全局；无覆盖→回落全局")
    void toAuthUser_endToEnd_overrideThenFallback() {
        // 本用例选 TenantRoleOverrideTest：它已装配真实 TenantRoleConfigService（mock 其仓储），
        // 只需补建真实 AuthService；与 TenantRetireGuardTest（真实 AuthService 但 TenantRoleConfigService 为 mock）相比，
        // 这里能真正经「覆盖两表读取 → 生效合并」驱动 toAuthUser，故为 I4 首选。
        AuthService authService = new AuthService(apiPermissionService, jwtService, tenantProperties,
                sysUserRepository, sysUserRoleRepository, tenantRoleConfigService, sysMenuRepository,
                sysUserTenantRepository, sysTenantRepository, tenantAccessGuard);

        when(sysUserRepository.findById(USER_ID)).thenReturn(Optional.of(user(USER_ID)));
        when(sysUserRoleRepository.findByUserId(USER_ID)).thenReturn(List.of(ur(USER_ID, TM_ROLE_ID)));
        when(sysRoleRepository.findAllById(any())).thenReturn(List.of(role(TM_ROLE_ID, "team_manager", null)));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of(rm(TM_ROLE_ID, MENU_A)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T), any()))
                .thenReturn(List.of(config(1L, TENANT_T, TM_ROLE_ID)));
        when(configRepository.findByTenantIdAndRoleIdIn(eq(TENANT_T2), any())).thenReturn(List.of());
        when(tenantMenuRepository.findByConfigIdIn(List.of(1L))).thenReturn(List.of(trow(1L, MENU_B)));
        when(sysMenuRepository.findAllById(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            List<SysMenu> out = new ArrayList<>();
            if (ids.contains(MENU_A)) {
                out.add(menuWithPerm(MENU_A, "/admin/a", "biz:a"));
            }
            if (ids.contains(MENU_B)) {
                out.add(menuWithPerm(MENU_B, "/admin/b", "biz:b"));
            }
            return out;
        });
        when(tenantProperties.isStrictDataScope()).thenReturn(false);
        when(apiPermissionService.isSuperAdmin(USER_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(USER_ID)).thenReturn(false);
        when(sysTenantRepository.findById(TENANT_T)).thenReturn(Optional.of(tenant(TENANT_T)));
        when(sysTenantRepository.findById(TENANT_T2)).thenReturn(Optional.of(tenant(TENANT_T2)));
        when(sysUserTenantRepository.findByUserIdAndDeletedAtIsNull(USER_ID)).thenReturn(List.of());
        when(jwtService.authenticateBearerToken("tok-T"))
                .thenReturn(new JwtService.TokenAuth(USER_ID, TENANT_T, null, null));
        when(jwtService.authenticateBearerToken("tok-T2"))
                .thenReturn(new JwtService.TokenAuth(USER_ID, TENANT_T2, null, null));

        Map<String, Object> withOverride = authUser(authService.me("tok-T"));
        assertEquals(List.of(MENU_B), withOverride.get("menuIds"), "有覆盖 → menuIds 为覆盖集合");
        assertEquals(List.of("/admin/b"), withOverride.get("menuPaths"), "menuPaths 应反映覆盖菜单 B");
        assertEquals(List.of("biz:b"), withOverride.get("perms"), "perms 应反映覆盖菜单 B");
        assertFalse(((List<?>) withOverride.get("menuPaths")).contains("/admin/a"), "被替换掉的全局菜单 A 不应出现");
        assertFalse(((List<?>) withOverride.get("perms")).contains("biz:a"), "被替换掉的全局权限不应出现");

        Map<String, Object> fallback = authUser(authService.me("tok-T2"));
        assertEquals(List.of(MENU_A), fallback.get("menuIds"), "无覆盖 → 回落全局 menuIds");
        assertEquals(List.of("/admin/a"), fallback.get("menuPaths"), "无覆盖 → 回落全局 menuPaths");
        assertEquals(List.of("biz:a"), fallback.get("perms"), "无覆盖 → 回落全局 perms");
    }

    // ------------------------------------------------------------ 执行者矩阵（服务层）

    @Test
    @DisplayName("执行者矩阵：租管 own 保存门户角色覆盖 → 成功 + 缓存失效 + 全局零污染")
    void tenantAdminSavesOwnOverride() {
        stubTenantAdmin();
        when(sysRoleRepository.findById(TM_ROLE_ID)).thenReturn(Optional.of(role(TM_ROLE_ID, "team_manager", null)));
        when(sysMenuRepository.findAll()).thenReturn(List.of(menu(MENU_B, "/admin/teams")));
        when(configRepository.findByTenantIdAndRoleId(TENANT_T, TM_ROLE_ID)).thenReturn(Optional.empty());
        when(configRepository.save(any(TenantRoleMenuConfig.class))).thenAnswer(inv -> {
            TenantRoleMenuConfig c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(tenantMenuRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantMenuRepository.findByConfigId(1L)).thenReturn(List.of(trow(1L, MENU_B)));

        Map<String, Object> res = sysRoleService.saveTenantConfig(OPERATOR_ID, null, TM_ROLE_ID, List.of(MENU_B));

        assertEquals(Boolean.TRUE, res.get("enabled"));
        assertEquals(Long.valueOf(TENANT_T), res.get("tenantId"));
        verify(apiPermissionService).clearUserRoleCache();
        verify(sysRoleMenuRepository, never()).saveAll(any());
        verify(sysRoleMenuRepository, never()).deleteByRoleId(any());
    }

    @Test
    @DisplayName("执行者矩阵：租管指定他租户 → 403「无权配置他租户目录」")
    void tenantAdminOtherTenant_forbidden() {
        stubTenantAdmin();
        when(sysRoleRepository.findById(TM_ROLE_ID)).thenReturn(Optional.of(role(TM_ROLE_ID, "team_manager", null)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.saveTenantConfig(OPERATOR_ID, TENANT_T2, TM_ROLE_ID, List.of(MENU_B)));

        assertEquals(403, ex.getCode());
        assertEquals("无权配置他租户目录", ex.getMessage());
    }

    @Test
    @DisplayName("执行者矩阵：租管覆盖含平台级菜单 → 403「无权授予平台级菜单」")
    void tenantAdminOverridePlatformMenu_forbidden() {
        stubTenantAdmin();
        when(sysRoleRepository.findById(TM_ROLE_ID)).thenReturn(Optional.of(role(TM_ROLE_ID, "team_manager", null)));
        when(sysMenuRepository.findAll()).thenReturn(List.of(menu(PLATFORM_MENU_ID, "/admin/tenants")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.saveTenantConfig(OPERATOR_ID, null, TM_ROLE_ID, List.of(PLATFORM_MENU_ID)));

        assertEquals(403, ex.getCode());
        assertEquals("无权授予平台级菜单", ex.getMessage());
        verify(configRepository, never()).save(any());
        verify(apiPermissionService, never()).clearUserRoleCache();
    }

    @Test
    @DisplayName("执行者矩阵：超管任意租户保存覆盖 → 成功")
    void superAdminSavesArbitraryTenant() {
        CurrentUserHolder.set(SUPER_ID, 0L);
        when(apiPermissionService.isSuperAdmin(SUPER_ID)).thenReturn(true);
        when(sysRoleRepository.findById(TM_ROLE_ID)).thenReturn(Optional.of(role(TM_ROLE_ID, "team_manager", null)));
        when(configRepository.findByTenantIdAndRoleId(TENANT_T2, TM_ROLE_ID)).thenReturn(Optional.empty());
        when(configRepository.save(any(TenantRoleMenuConfig.class))).thenAnswer(inv -> {
            TenantRoleMenuConfig c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });
        when(tenantMenuRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tenantMenuRepository.findByConfigId(2L)).thenReturn(List.of(trow(2L, MENU_B)));

        Map<String, Object> res = sysRoleService.saveTenantConfig(SUPER_ID, TENANT_T2, TM_ROLE_ID, List.of(MENU_B));

        assertEquals(Boolean.TRUE, res.get("enabled"));
        assertEquals(Long.valueOf(TENANT_T2), res.get("tenantId"));
        verify(apiPermissionService).clearUserRoleCache();
    }

    @Test
    @DisplayName("仅平台门户角色可覆盖：租管对本租户角色尝试覆盖 → 403「仅平台级门户角色支持租户级目录覆盖」")
    void nonPortalRole_overrideUnsupported() {
        stubTenantAdmin();
        when(sysRoleRepository.findById(TENANT_ADMIN_ROLE_ID))
                .thenReturn(Optional.of(role(TENANT_ADMIN_ROLE_ID, "tenant_admin", TENANT_T)));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sysRoleService.saveTenantConfig(OPERATOR_ID, null, TENANT_ADMIN_ROLE_ID, List.of(MENU_B)));

        assertEquals(403, ex.getCode());
        assertEquals("仅平台级门户角色支持租户级目录覆盖", ex.getMessage());
    }

    @Test
    @DisplayName("查询/移除覆盖：清空后 GET → enabled=false（回落全局）")
    void clearThenGet_disabled() {
        stubTenantAdmin();
        when(sysRoleRepository.findById(TM_ROLE_ID)).thenReturn(Optional.of(role(TM_ROLE_ID, "team_manager", null)));
        when(configRepository.findByTenantIdAndRoleId(TENANT_T, TM_ROLE_ID))
                .thenReturn(Optional.of(config(1L, TENANT_T, TM_ROLE_ID)))
                .thenReturn(Optional.empty());

        sysRoleService.clearTenantConfig(OPERATOR_ID, null, TM_ROLE_ID);
        verify(tenantMenuRepository).deleteByConfigId(1L);
        verify(configRepository).deleteById(1L);
        verify(apiPermissionService).clearUserRoleCache();

        Map<String, Object> res = sysRoleService.getTenantConfig(OPERATOR_ID, null, TM_ROLE_ID);
        assertEquals(Boolean.FALSE, res.get("enabled"));
        assertEquals(List.of(), res.get("menuIds"));
    }

    @Test
    @DisplayName("list 标记：租管可见平台门户角色行 platform=true / hasTenantOverride 正确")
    void listMarksPlatformAndOverride() {
        stubTenantAdmin();
        SysRole own = role(TENANT_ADMIN_ROLE_ID, "tenant_admin", TENANT_T);
        SysRole portal = role(TM_ROLE_ID, "team_manager", null);
        Page<SysRole> page = new PageImpl<>(List.of(own, portal));
        when(sysRoleRepository.findForAssignOptions(eq(TENANT_T), any(), any(), any())).thenReturn(page);
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of());
        when(sysRoleApiRepository.findByRoleIdIn(any())).thenReturn(List.of());
        when(configRepository.findByTenantIdAndRoleId(TENANT_T, TM_ROLE_ID))
                .thenReturn(Optional.of(config(1L, TENANT_T, TM_ROLE_ID)));

        PageResult<SysRole> result = sysRoleService.list(OPERATOR_ID, 1, 10, null);
        List<SysRole> roles = result.getList();

        SysRole portalOut = roles.stream().filter(r -> Objects.equals(r.getId(), TM_ROLE_ID)).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, portalOut.getPlatform());
        assertEquals(Boolean.TRUE, portalOut.getHasTenantOverride());

        SysRole ownOut = roles.stream().filter(r -> Objects.equals(r.getId(), TENANT_ADMIN_ROLE_ID)).findFirst().orElseThrow();
        assertEquals(Boolean.FALSE, ownOut.getPlatform());
        assertNull(ownOut.getHasTenantOverride());
    }

    // ------------------------------------------------------------ helpers

    private void stubTenantAdmin() {
        CurrentUserHolder.set(OPERATOR_ID, TENANT_T);
        when(apiPermissionService.isSuperAdmin(OPERATOR_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(OPERATOR_ID)).thenReturn(true);
    }

    private static SysRole role(long id, String code, Long tenantId) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setCode(code);
        r.setTenantId(tenantId);
        return r;
    }

    private static SysRoleMenu rm(long roleId, long menuId) {
        SysRoleMenu m = new SysRoleMenu();
        m.setRoleId(roleId);
        m.setMenuId(menuId);
        return m;
    }

    private static TenantRoleMenuConfig config(long id, long tenantId, long roleId) {
        TenantRoleMenuConfig c = new TenantRoleMenuConfig();
        c.setId(id);
        c.setTenantId(tenantId);
        c.setRoleId(roleId);
        return c;
    }

    private static TenantRoleMenu trow(long configId, long menuId) {
        TenantRoleMenu t = new TenantRoleMenu();
        t.setConfigId(configId);
        t.setMenuId(menuId);
        return t;
    }

    private static SysMenu menu(long id, String path) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setPath(path);
        return m;
    }

    private static SysApi api(long id, String path, String method) {
        SysApi a = new SysApi();
        a.setId(id);
        a.setPath(path);
        a.setMethod(method);
        return a;
    }

    private static SysMenuApi ma(long menuId, long apiId) {
        SysMenuApi m = new SysMenuApi();
        m.setMenuId(menuId);
        m.setApiId(apiId);
        return m;
    }

    private static SysRoleApi ra(long roleId, long apiId) {
        SysRoleApi r = new SysRoleApi();
        r.setRoleId(roleId);
        r.setApiId(apiId);
        return r;
    }

    private static SysUserRole ur(long userId, long roleId) {
        SysUserRole u = new SysUserRole();
        u.setUserId(userId);
        u.setRoleId(roleId);
        return u;
    }

    private static SysUser user(long id) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setUsername("coach");
        u.setNickname("教练");
        return u;
    }

    private static SysTenant tenant(long id) {
        SysTenant t = new SysTenant();
        t.setId(id);
        t.setCode("t" + id);
        t.setName("租户" + id);
        return t;
    }

    private static SysMenu menuWithPerm(long id, String path, String permission) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setPath(path);
        m.setPermission(permission);
        return m;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> authUser(Map<String, Object> meResult) {
        return (Map<String, Object>) meResult.get("user");
    }
}
