/*
 * 账号权限重构（批次 3b 收敛，批 2 沉淀⑪）：SysUserService 角色授予/回收后范围缓存失效测试。
 *
 * 背景：scope resolveCore 依赖角色/管理员判定（isSuperAdmin/isTenantAdmin/门户角色），
 * 既有 SysUserService 角色写入未失效 AccountScopeService 的用户范围缓存（最长 30s 窗口）。
 * 本测试断言：授予角色（create）与角色变更（update）后调用 evictUserScopeCacheAfterCommit(userId)。
 *
 * 风格：Mockito mock 外部依赖，不启动 Spring、不连库；服务手工 new，操作者保持未登录（op=null）以走最简路径。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.core.CurrentUserHolder;
import com.bsball.model.entity.SysUser;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.repository.SysUserTenantRepository;
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
@DisplayName("SysUserService：角色写入后范围缓存失效（批 2 沉淀⑪）")
class SysUserServiceScopeEvictTest {

    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysUserRoleRepository sysUserRoleRepository;
    @Mock
    private SysUserTenantRepository sysUserTenantRepository;
    @Mock
    private SysTenantRepository sysTenantRepository;
    @Mock
    private SysUserTenantManageService sysUserTenantManageService;
    @Mock
    private ApiPermissionService apiPermissionService;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private AccountScopeService accountScopeService;

    private SysUserService service;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.clear();
        service = new SysUserService(sysUserRepository, sysUserRoleRepository, sysUserTenantRepository,
                sysTenantRepository, sysUserTenantManageService, apiPermissionService, sysRoleRepository,
                accountScopeService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("create 授予角色：失效该用户范围缓存")
    void create_roleGranted_evictsScopeCache() {
        String username = "user01";
        when(sysUserRepository.findByUsernameAndDeletedAtIsNull(username)).thenReturn(Optional.empty());
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(88L);
            return u;
        });
        when(sysRoleRepository.findByTenantIdIsNullAndCode("admin")).thenReturn(Optional.empty());
        when(sysTenantRepository.findFirstByDeletedAtIsNullOrderByIdAsc()).thenReturn(Optional.empty());
        when(sysUserRoleRepository.findByUserId(88L)).thenReturn(List.of());
        when(sysUserTenantRepository.findByUserIdInAndDeletedAtIsNull(any())).thenReturn(List.of());

        SysUser entity = new SysUser();
        entity.setUsername(username);
        entity.setRoleIds(List.of(3L));

        SysUser saved = service.create(entity);

        assertNotNull(saved);
        verify(accountScopeService).evictUserScopeCacheAfterCommit(88L);
    }

    @Test
    @DisplayName("update 角色变更：失效该用户范围缓存")
    void update_roleChanged_evictsScopeCache() {
        long id = 77L;
        SysUser existing = new SysUser();
        existing.setId(id);
        when(sysUserRepository.findById(id)).thenReturn(Optional.of(existing));
        when(sysUserRoleRepository.findByUserId(id)).thenReturn(List.of());
        when(sysRoleRepository.findByTenantIdIsNullAndCode("admin")).thenReturn(Optional.empty());
        when(sysUserRepository.save(any(SysUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sysUserTenantRepository.findByUserIdInAndDeletedAtIsNull(any())).thenReturn(List.of());

        SysUser entity = new SysUser();
        entity.setRoleIds(List.of(2L));

        SysUser saved = service.update(id, entity);

        assertNotNull(saved);
        verify(accountScopeService).evictUserScopeCacheAfterCommit(id);
    }
}
