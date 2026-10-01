/*
 * 账号权限重构（批次 3b，Task 3.15 / spec §8.6）：角色目录配置能力与越权配置防护单测。
 *
 * 覆盖：
 *  - 可授予集合：租户管理员为本租户角色增授普通业务菜单 → 通过且绑定落库（role_menu.saveAll 收到该菜单）；
 *  - 越权配置防护：租户管理员授予平台级菜单（「租户管理」/admin/tenants）→ 403「无权授予平台级菜单」；
 *  - 越权矩阵：租户管理员修改他租户角色 → 403；系统保留编码（admin）不可改/删（现状复核）；
 *  - 超管不受限：可授予平台级菜单（且不触达平台级集合查询）；
 *  - 配置生效链：菜单/API 绑定变更后 clearUserRoleCache 被调用（缓存失效）。
 *
 * 风格：Mockito mock 外部依赖，不启动 Spring、不连库；服务手工 new；CurrentUserHolder（ThreadLocal）逐用例清理。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysRoleMenu;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRoleRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("角色目录配置能力与越权配置防护（批次 3b T3.15 / spec §8.6）")
class RoleMenuConfigGuardTest {

    private static final long OPERATOR_ID = 9L;
    private static final long SUPER_ID = 1L;
    private static final long TENANT_ID = 10L;
    private static final long OTHER_TENANT_ID = 20L;
    private static final long ROLE_ID = 500L;
    private static final long BUSINESS_MENU_ID = 100L;
    private static final long PLATFORM_MENU_ID = 200L;
    private static final long BUSINESS_API_ID = 300L;
    private static final long PLATFORM_API_ID = 400L;

    @Mock
    private ApiPermissionService apiPermissionService;
    @Mock
    private MenuExpansionHelper menuExpansionHelper;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private SysRoleMenuRepository sysRoleMenuRepository;
    @Mock
    private SysRoleApiRepository sysRoleApiRepository;
    @Mock
    private SysUserRoleRepository sysUserRoleRepository;
    @Mock
    private SysTenantRepository sysTenantRepository;
    @Mock
    private SysMenuRepository sysMenuRepository;
    @Mock
    private SysApiRepository sysApiRepository;
    @Mock
    private TenantRoleConfigService tenantRoleConfigService;

    private SysRoleService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new SysRoleService(apiPermissionService, menuExpansionHelper, sysRoleRepository,
                sysRoleMenuRepository, sysRoleApiRepository, sysUserRoleRepository, sysTenantRepository,
                sysMenuRepository, sysApiRepository, tenantRoleConfigService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    // ------------------------------------------------------------ 可授予集合

    @Test
    @DisplayName("可授予集合：租户管理员为本租户角色增授普通业务菜单 → 通过且绑定落库")
    void tenantAdminGrantsBusinessMenu_persists() {
        stubTenantAdmin();
        SysRole existing = role(ROLE_ID, "team_admin", TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(sysMenuRepository.findAll()).thenReturn(menus());
        when(menuExpansionHelper.expandWithAncestors(any())).thenReturn(new HashSet<>(List.of(BUSINESS_MENU_ID)));
        when(sysRoleApiRepository.findByRoleIdIn(any())).thenReturn(List.of());
        List<SysRoleMenu> saved = new ArrayList<>();
        when(sysRoleMenuRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<SysRoleMenu> arg = inv.getArgument(0);
            arg.forEach(saved::add);
            return arg;
        });
        when(sysRoleRepository.save(any(SysRole.class))).thenAnswer(inv -> inv.getArgument(0));

        SysRole body = new SysRole();
        body.setMenuIds(new ArrayList<>(List.of(BUSINESS_MENU_ID)));
        SysRole result = service.update(OPERATOR_ID, ROLE_ID, body);

        assertNotNull(result);
        assertTrue(saved.stream().anyMatch(rm -> Objects.equals(rm.getMenuId(), BUSINESS_MENU_ID)),
                "普通业务菜单应落库到 role_menu");
        verify(apiPermissionService).clearUserRoleCache();
    }

    @Test
    @DisplayName("越权配置防护：租户管理员授予平台级菜单（租户管理）→ 403「无权授予平台级菜单」")
    void tenantAdminGrantsPlatformMenu_forbidden() {
        stubTenantAdmin();
        SysRole existing = role(ROLE_ID, "team_admin", TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(sysMenuRepository.findAll()).thenReturn(menus());

        SysRole body = new SysRole();
        body.setMenuIds(new ArrayList<>(List.of(PLATFORM_MENU_ID)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(OPERATOR_ID, ROLE_ID, body));

        assertEquals(403, ex.getCode());
        assertEquals("无权授予平台级菜单", ex.getMessage());
        verify(sysRoleMenuRepository, never()).saveAll(any());
        verify(sysRoleMenuRepository, never()).deleteByRoleId(any());
    }

    // ------------------------------------------------------------ 越权矩阵

    @Test
    @DisplayName("越权矩阵：租户管理员修改他租户角色 → 403")
    void tenantAdminModifiesOtherTenantRole_forbidden() {
        stubTenantAdmin();
        SysRole other = role(ROLE_ID, "team_admin", OTHER_TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(other));

        SysRole body = new SysRole();
        body.setMenuIds(new ArrayList<>(List.of(BUSINESS_MENU_ID)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(OPERATOR_ID, ROLE_ID, body));

        assertEquals(403, ex.getCode());
        assertEquals("无权操作该角色", ex.getMessage());
    }

    @Test
    @DisplayName("现状复核：超管不可修改/删除系统保留编码角色（admin）")
    void reservedCode_notModifiable_notDeletable() {
        CurrentUserHolder.set(SUPER_ID, 0L);
        when(apiPermissionService.isSuperAdmin(SUPER_ID)).thenReturn(true);
        SysRole adminRole = role(1L, "admin", null);
        when(sysRoleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        SysRole body = new SysRole();
        body.setName("篡改");
        BusinessException updateEx = assertThrows(BusinessException.class, () -> service.update(SUPER_ID, 1L, body));
        assertEquals(403, updateEx.getCode());
        assertEquals("超级管理员角色不可修改", updateEx.getMessage());

        BusinessException deleteEx = assertThrows(BusinessException.class, () -> service.delete(SUPER_ID, 1L));
        assertEquals(400, deleteEx.getCode());
        assertEquals("系统内置角色不可删除", deleteEx.getMessage());
    }

    @Test
    @DisplayName("越权矩阵：租户管理员操作平台级系统角色（tenant_id NULL）→ 403")
    void tenantAdminModifiesSystemRole_forbidden() {
        stubTenantAdmin();
        SysRole platform = role(ROLE_ID, "tenant_admin", null);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(platform));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(OPERATOR_ID, ROLE_ID));

        assertEquals(403, ex.getCode());
        assertEquals("无权操作系统级角色", ex.getMessage());
    }

    // ------------------------------------------------------------ 超管不受限

    @Test
    @DisplayName("超管不受限：可授予平台级菜单（租户管理），且不触达平台级集合查询")
    void superAdminCanGrantPlatformMenu() {
        CurrentUserHolder.set(SUPER_ID, 0L);
        when(apiPermissionService.isSuperAdmin(SUPER_ID)).thenReturn(true);
        SysRole existing = role(ROLE_ID, "ops_custom", null);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(menuExpansionHelper.expandWithAncestors(any())).thenReturn(new HashSet<>(List.of(PLATFORM_MENU_ID)));
        when(sysRoleApiRepository.findByRoleIdIn(any())).thenReturn(List.of());
        List<SysRoleMenu> saved = new ArrayList<>();
        when(sysRoleMenuRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<SysRoleMenu> arg = inv.getArgument(0);
            arg.forEach(saved::add);
            return arg;
        });
        when(sysRoleRepository.save(any(SysRole.class))).thenAnswer(inv -> inv.getArgument(0));

        SysRole body = new SysRole();
        body.setMenuIds(new ArrayList<>(List.of(PLATFORM_MENU_ID)));
        SysRole result = service.update(SUPER_ID, ROLE_ID, body);

        assertNotNull(result);
        assertTrue(saved.stream().anyMatch(rm -> Objects.equals(rm.getMenuId(), PLATFORM_MENU_ID)),
                "超管应可授予平台级菜单");
        verify(apiPermissionService).clearUserRoleCache();
        verify(sysMenuRepository, never()).findAll();
    }

    // ------------------------------------------------------------ 配置生效链

    @Test
    @DisplayName("配置生效链：仅 API 绑定变更亦触发 userRoleCache 失效")
    void apiBindingChange_evictsUserRoleCache() {
        stubTenantAdmin();
        SysRole existing = role(ROLE_ID, "team_admin", TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of());
        when(sysRoleApiRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sysRoleRepository.save(any(SysRole.class))).thenAnswer(inv -> inv.getArgument(0));

        SysRole body = new SysRole();
        body.setApiIds(new ArrayList<>(List.of(11L, 12L)));
        service.update(OPERATOR_ID, ROLE_ID, body);

        verify(sysRoleApiRepository).deleteByRoleId(ROLE_ID);
        verify(sysRoleApiRepository).saveAll(any());
        verify(apiPermissionService).clearUserRoleCache();
    }

    // ------------------------------------------------------------ apiIds 越权防护（T3.15b）

    @Test
    @DisplayName("apiIds 防护：租户管理员绑定平台级接口（/sys/tenant/*）→ 403「无权授予平台级接口」")
    void tenantAdminGrantsPlatformApi_forbidden() {
        stubTenantAdmin();
        SysRole existing = role(ROLE_ID, "team_admin", TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(sysApiRepository.findAll()).thenReturn(apis());

        SysRole body = new SysRole();
        body.setApiIds(new ArrayList<>(List.of(PLATFORM_API_ID)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(OPERATOR_ID, ROLE_ID, body));

        assertEquals(403, ex.getCode());
        assertEquals("无权授予平台级接口", ex.getMessage());
        verify(sysRoleApiRepository, never()).saveAll(any());
        verify(sysRoleApiRepository, never()).deleteByRoleId(any());
    }

    @Test
    @DisplayName("apiIds 防护：租户管理员绑定业务接口 → 通过且落库")
    void tenantAdminGrantsBusinessApi_persists() {
        stubTenantAdmin();
        SysRole existing = role(ROLE_ID, "team_admin", TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(existing));
        when(sysApiRepository.findAll()).thenReturn(apis());
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of());
        List<SysRoleApi> saved = new ArrayList<>();
        when(sysRoleApiRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<SysRoleApi> arg = inv.getArgument(0);
            arg.forEach(saved::add);
            return arg;
        });
        when(sysRoleRepository.save(any(SysRole.class))).thenAnswer(inv -> inv.getArgument(0));

        SysRole body = new SysRole();
        body.setApiIds(new ArrayList<>(List.of(BUSINESS_API_ID)));
        SysRole result = service.update(OPERATOR_ID, ROLE_ID, body);

        assertNotNull(result);
        assertTrue(saved.stream().anyMatch(ra -> Objects.equals(ra.getApiId(), BUSINESS_API_ID)),
                "业务接口应落库到 role_api");
        verify(apiPermissionService).clearUserRoleCache();
    }

    // ------------------------------------------------------------ helpers

    private void stubTenantAdmin() {
        CurrentUserHolder.set(OPERATOR_ID, TENANT_ID);
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

    private static SysMenu menu(long id, String path) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setPath(path);
        return m;
    }

    private static List<SysMenu> menus() {
        return List.of(menu(BUSINESS_MENU_ID, "/admin/teams"), menu(PLATFORM_MENU_ID, "/admin/tenants"));
    }

    private static SysApi api(long id, String path, String method) {
        SysApi a = new SysApi();
        a.setId(id);
        a.setPath(path);
        a.setMethod(method);
        return a;
    }

    private static List<SysApi> apis() {
        return List.of(api(BUSINESS_API_ID, "/sys/user/list", "GET"), api(PLATFORM_API_ID, "/sys/tenant/page", "GET"));
    }
}
