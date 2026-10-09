package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysMenuApi;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysUser;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysConfigRepository;
import com.bsball.repository.SysDictDataRepository;
import com.bsball.repository.SysDictTypeRepository;
import com.bsball.repository.SysMenuApiRepository;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.repository.StadiumRepository;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("球员移除/恢复 种子与回收（2026-10-09）")
class PlayerRemoveRestoreSeedTest {

    private static final long OP_ID = 1L;

    @Mock private SysMenuRepository sysMenuRepository;
    @Mock private SysApiRepository sysApiRepository;
    @Mock private SysDictTypeRepository sysDictTypeRepository;
    @Mock private SysDictDataRepository sysDictDataRepository;
    @Mock private SysConfigRepository sysConfigRepository;
    @Mock private SysRoleRepository sysRoleRepository;
    @Mock private SysUserRepository sysUserRepository;
    @Mock private SysRoleMenuRepository sysRoleMenuRepository;
    @Mock private SysRoleApiRepository sysRoleApiRepository;
    @Mock private SysUserRoleRepository sysUserRoleRepository;
    @Mock private StadiumRepository stadiumRepository;
    @Mock private TenantProperties tenantProperties;
    @Mock private InitSeedProperties initSeedProperties;
    @Mock private SysMenuApiRepository sysMenuApiRepository;

    private InitDataRunner runner;
    private final List<SysMenu> menuStore = new ArrayList<>();
    private final List<SysApi> apiStore = new ArrayList<>();
    private final List<SysMenuApi> menuApiStore = new ArrayList<>();
    private final Map<Long, List<SysRoleApi>> roleApis = new HashMap<>();
    private long nextId = 1000L;

    @BeforeEach
    void setUp() {
        runner = new InitDataRunner(sysMenuRepository, sysApiRepository, sysDictTypeRepository, sysDictDataRepository,
                sysConfigRepository, sysRoleRepository, sysUserRepository, sysRoleMenuRepository, sysRoleApiRepository,
                sysUserRoleRepository, stadiumRepository, tenantProperties, initSeedProperties, sysMenuApiRepository);
        menuStore.clear();
        apiStore.clear();
        menuApiStore.clear();
        roleApis.clear();
        nextId = 1000L;
        lenient().when(sysUserRepository.findByUsernameAndDeletedAtIsNull("admin"))
                .thenReturn(Optional.of(user(OP_ID)));
        lenient().when(sysMenuRepository.findAll()).thenAnswer(inv -> new ArrayList<>(menuStore));
        lenient().when(sysMenuRepository.save(any(SysMenu.class))).thenAnswer(inv -> {
            SysMenu m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(nextId++);
            }
            menuStore.add(m);
            return m;
        });
        lenient().when(sysMenuRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sysMenuApiRepository.save(any())).thenAnswer(inv -> {
            menuApiStore.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        lenient().when(sysApiRepository.findAll()).thenAnswer(inv -> new ArrayList<>(apiStore));
        lenient().when(sysApiRepository.save(any(SysApi.class))).thenAnswer(inv -> {
            SysApi a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(nextId++);
            }
            apiStore.add(a);
            return a;
        });
        lenient().when(sysRoleApiRepository.findByRoleId(anyLong()))
                .thenAnswer(inv -> new ArrayList<>(roleApis.getOrDefault((Long) inv.getArgument(0), List.of())));
    }

    @Test
    @DisplayName("API 注册幂等：两次调用仅插入 2 条")
    void apiSeedIdempotent() throws Exception {
        invokeNoArg("ensurePlayerRemoveRestoreApisIfNeeded");
        assertEquals(2, apiStore.size());
        invokeNoArg("ensurePlayerRemoveRestoreApisIfNeeded");
        assertEquals(2, apiStore.size(), "二次调用不得重复插入");
        assertTrue(apiStore.stream().anyMatch(a -> "/player/remove-from-team/:id".equals(a.getPath())));
        assertTrue(apiStore.stream().anyMatch(a -> "/player/restore/:id".equals(a.getPath())));
    }

    @Test
    @DisplayName("按钮种子：移除出球队/恢复球员各落库一次且绑定新 API")
    void buttonsInsertedOnceWithApiLinks() throws Exception {
        menuStore.add(menu(1L, 0L, "/business", null, 1));
        menuStore.add(menu(2L, 1L, "/admin/players", null, 2));
        invokeNoArg("ensurePlayerRemoveRestoreApisIfNeeded");
        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");
        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");
        Map<String, Long> counts = menuStore.stream().filter(m -> m.getPermission() != null)
                .collect(Collectors.groupingBy(SysMenu::getPermission, Collectors.counting()));
        assertEquals(1L, counts.getOrDefault("business:player:removeFromTeam", 0L).longValue());
        assertEquals(1L, counts.getOrDefault("business:player:restore", 0L).longValue());
        assertEquals(2, menuApiStore.size(), "两个按钮各绑定 1 个 API");
    }

    @Test
    @DisplayName("回收幂等：team_manager/league_organizer 的球员删除/批量删除 role_api 被移除，他角色不动")
    void revokeLegacyDeleteApis_idempotent() throws Exception {
        apiStore.add(api(99L, "/player/delete/:id", "DELETE"));
        apiStore.add(api(205L, "/player/delete-batch", "POST"));
        apiStore.add(api(94L, "/player/list", "GET"));
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("team_manager"))
                .thenReturn(Optional.of(role(600L, "team_manager")));
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("league_organizer"))
                .thenReturn(Optional.of(role(601L, "league_organizer")));
        roleApis.put(600L, new ArrayList<>(List.of(roleApi(600L, 99L), roleApi(600L, 205L), roleApi(600L, 94L))));
        roleApis.put(601L, new ArrayList<>(List.of(roleApi(601L, 99L), roleApi(601L, 94L))));
        List<Long> deleted = new ArrayList<>();
        doAnswer(inv -> {
            SysRoleApi ra = inv.getArgument(0);
            deleted.add(ra.getApiId());
            for (List<SysRoleApi> list : roleApis.values()) {
                list.remove(ra);
            }
            return null;
        }).when(sysRoleApiRepository).delete(any(SysRoleApi.class));

        invokeNoArg("ensurePortalRolesRevokePlayerDeleteApisIfNeeded");
        invokeNoArg("ensurePortalRolesRevokePlayerDeleteApisIfNeeded");

        assertEquals(List.of(99L, 205L, 99L), deleted, "应恰好移除 3 条（tm 2 条 + lo 1 条），/player/list 保留，二次调用零删除");
    }

    @Test
    @DisplayName("预设：team_manager/league_organizer 预设含 removeFromTeam 不含 player:delete")
    void presetSetsTightened() throws Exception {
        Field f = InitDataRunner.class.getDeclaredField("PORTAL_TEAM_MANAGER_PERMISSIONS");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> tm = (List<String>) f.get(null);
        assertTrue(tm.contains("business:player:removeFromTeam"));
        assertFalse(tm.contains("business:player:delete"));
        Field f2 = InitDataRunner.class.getDeclaredField("PORTAL_LEAGUE_ORGANIZER_PERMISSIONS");
        f2.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> lo = (List<String>) f2.get(null);
        assertTrue(lo.contains("business:player:removeFromTeam"));
        assertFalse(lo.contains("business:player:delete"));
    }

    private void invokeNoArg(String name) throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(runner);
    }

    private static SysMenu menu(long id, long parentId, String path, String permission, int type) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setPath(path);
        m.setPermission(permission);
        m.setMenuType(type);
        return m;
    }

    private static SysApi api(long id, String path, String method) {
        SysApi a = new SysApi();
        a.setId(id);
        a.setPath(path);
        a.setMethod(method);
        return a;
    }

    private static SysRole role(long id, String code) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    private static SysRoleApi roleApi(long roleId, long apiId) {
        SysRoleApi ra = new SysRoleApi();
        ra.setRoleId(roleId);
        ra.setApiId(apiId);
        return ra;
    }

    private static SysUser user(long id) {
        SysUser u = new SysUser();
        u.setId(id);
        return u;
    }
}
