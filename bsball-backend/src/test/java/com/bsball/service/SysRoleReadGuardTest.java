/*
 * #154 修复：租户管理员「租户目录」抽屉 403 —— GET /sys/role/:id 只读语义放宽回归测试。
 *
 * 覆盖：
 *  - 租户管理员 GET 平台级角色（tenant_id NULL）→ OK（只读放行，抽屉初始化全局预设所需）；
 *  - 租户管理员 GET 本租户角色 → OK（现状保持）；
 *  - 租户管理员 GET 他租户角色 → 403「无权查看该角色」（越权读取仍拒）；
 *  - 租户管理员 update 平台级角色 → 仍 403「无权操作系统级角色」（写路径零改动回归）；
 *  - 其它角色（member：非超管/非租管）GET → 403「无权查看角色」（不变）。
 *
 * 风格：Mockito mock 外部依赖，不启动 Spring、不连库；服务手工 new；CurrentUserHolder（ThreadLocal）逐用例清理。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysRole;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("#154 租户管理员「租户目录」抽屉 403 —— GET /sys/role/:id 只读语义放宽")
class SysRoleReadGuardTest {

    private static final long OPERATOR_ID = 9L;
    private static final long TENANT_ID = 10L;
    private static final long OTHER_TENANT_ID = 20L;
    private static final long ROLE_ID = 6L;

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

    @Test
    @DisplayName("只读放行：租户管理员 GET 平台级角色（tenant_id NULL）→ OK")
    void tenantAdminReadsPlatformRole_allowed() {
        stubTenantAdmin();
        SysRole platform = role(ROLE_ID, "team_manager", null);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(platform));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of());
        when(sysRoleApiRepository.findByRoleIdIn(any())).thenReturn(List.of());

        SysRole result = service.get(OPERATOR_ID, ROLE_ID);

        assertSame(platform, result, "租户管理员应可只读平台级角色详情");
    }

    @Test
    @DisplayName("现状保持：租户管理员 GET 本租户角色 → OK")
    void tenantAdminReadsOwnTenantRole_allowed() {
        stubTenantAdmin();
        SysRole own = role(ROLE_ID, "team_admin", TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(own));
        when(sysRoleMenuRepository.findByRoleIdIn(any())).thenReturn(List.of());
        when(sysRoleApiRepository.findByRoleIdIn(any())).thenReturn(List.of());

        SysRole result = service.get(OPERATOR_ID, ROLE_ID);

        assertSame(own, result, "租户管理员应可读本租户角色");
    }

    @Test
    @DisplayName("越权读取仍拒：租户管理员 GET 他租户角色 → 403「无权查看该角色」")
    void tenantAdminReadsOtherTenantRole_forbidden() {
        stubTenantAdmin();
        SysRole other = role(ROLE_ID, "team_admin", OTHER_TENANT_ID);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(other));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(OPERATOR_ID, ROLE_ID));

        assertEquals(403, ex.getCode());
        assertEquals("无权查看该角色", ex.getMessage());
    }

    @Test
    @DisplayName("写路径零改动回归：租户管理员 update 平台级角色 → 仍 403「无权操作系统级角色」")
    void tenantAdminUpdatesPlatformRole_stillForbidden() {
        stubTenantAdmin();
        SysRole platform = role(ROLE_ID, "team_manager", null);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(platform));

        SysRole body = new SysRole();
        body.setName("篡改");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.update(OPERATOR_ID, ROLE_ID, body));

        assertEquals(403, ex.getCode());
        assertEquals("无权操作系统级角色", ex.getMessage());
        verify(sysRoleRepository, never()).save(any(SysRole.class));
    }

    @Test
    @DisplayName("其它角色不变：member GET 角色 → 403「无权查看角色」")
    void memberReadsRole_forbidden() {
        CurrentUserHolder.set(OPERATOR_ID, TENANT_ID);
        when(apiPermissionService.isSuperAdmin(OPERATOR_ID)).thenReturn(false);
        when(apiPermissionService.isTenantAdmin(OPERATOR_ID)).thenReturn(false);
        SysRole platform = role(ROLE_ID, "team_manager", null);
        when(sysRoleRepository.findById(ROLE_ID)).thenReturn(Optional.of(platform));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.get(OPERATOR_ID, ROLE_ID));

        assertEquals(403, ex.getCode());
        assertEquals("无权查看角色", ex.getMessage());
    }

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
}
