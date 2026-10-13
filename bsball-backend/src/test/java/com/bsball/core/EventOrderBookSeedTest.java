package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysMenuApi;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysUser;
import com.bsball.repository.StadiumRepository;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("电子秩序册 种子与谓词（Issue #310）")
class EventOrderBookSeedTest {

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
    @DisplayName("API 注册幂等：两次调用仅插入 10 条（含赛事侧与球队侧路径）")
    void apiSeedIdempotent() throws Exception {
        invokeNoArg("ensureEventOrderBookApisIfNeeded");
        assertEquals(10, apiStore.size());
        invokeNoArg("ensureEventOrderBookApisIfNeeded");
        assertEquals(10, apiStore.size(), "二次调用不得重复插入");
        assertTrue(apiStore.stream().anyMatch(a -> "/event/:eventId/teams/invite".equals(a.getPath())));
        assertTrue(apiStore.stream().anyMatch(a -> "/team/:teamId/event-registrations".equals(a.getPath())));
        assertTrue(apiStore.stream().anyMatch(a ->
                "/event/:eventId/teams/:teamId/roster".equals(a.getPath()) && "PUT".equals(a.getMethod())));
    }

    @Test
    @DisplayName("按钮种子：参赛球队/赛事报名各落库一次并绑定新 API")
    void buttonsInsertedOnceWithApiLinks() throws Exception {
        menuStore.add(menu(1L, 0L, "/business", null, 1));
        menuStore.add(menu(2L, 1L, "/admin/events", null, 2));
        menuStore.add(menu(3L, 1L, "/admin/teams", null, 2));
        invokeNoArg("ensureEventOrderBookApisIfNeeded");
        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");
        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");
        Map<String, Long> counts = menuStore.stream().filter(m -> m.getPermission() != null)
                .collect(Collectors.groupingBy(SysMenu::getPermission, Collectors.counting()));
        assertEquals(1L, counts.getOrDefault("business:event:teams", 0L).longValue());
        assertEquals(1L, counts.getOrDefault("business:team:eventRegistration", 0L).longValue());
        assertEquals(11, menuApiStore.size(), "两按钮合计绑定 11 个 API（6+5）");
        Map<Long, String> pathById = apiStore.stream()
                .collect(Collectors.toMap(SysApi::getId, a -> a.getMethod() + "|" + a.getPath()));
        Set<String> bound = menuApiStore.stream().map(l -> pathById.get(l.getApiId())).collect(Collectors.toSet());
        assertEquals(Set.of(
                "GET|/event/:eventId/teams", "POST|/event/:eventId/teams/invite",
                "POST|/event/:eventId/teams/:teamId/remove", "POST|/event/:eventId/teams/:teamId/lock",
                "POST|/event/:eventId/teams/:teamId/unlock", "GET|/event/:eventId/teams/:teamId/roster",
                "GET|/team/:teamId/event-registrations", "POST|/event/:eventId/teams/:teamId/confirm",
                "POST|/event/:eventId/teams/:teamId/decline", "PUT|/event/:eventId/teams/:teamId/roster"),
                bound, "按钮-API 绑定集合须与设计一致");
    }

    @Test
    @DisplayName("预设：team_manager 含赛事报名；league_organizer 含参赛球队与赛事报名")
    void presetSetsExtended() throws Exception {
        Field f = InitDataRunner.class.getDeclaredField("PORTAL_TEAM_MANAGER_PERMISSIONS");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> tm = (List<String>) f.get(null);
        assertTrue(tm.contains("business:team:eventRegistration"));
        assertFalse(tm.contains("business:event:teams"), "赛事侧入口不授予球队管理员");

        Field f2 = InitDataRunner.class.getDeclaredField("PORTAL_LEAGUE_ORGANIZER_PERMISSIONS");
        f2.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> lo = (List<String>) f2.get(null);
        assertTrue(lo.contains("business:event:teams"));
        assertTrue(lo.contains("business:team:eventRegistration"));
    }

    @Test
    @DisplayName("谓词：球队侧自助路径（confirm/decline/roster）授球队管理员；邀请/移除/锁定不授")
    void predicatesForEventTeamPaths() throws Exception {
        Method self = InitDataRunner.class.getDeclaredMethod("isEventTeamSelfServicePath", String.class);
        self.setAccessible(true);
        assertTrue((Boolean) self.invoke(null, "/event/:eventId/teams/:teamId/confirm"));
        assertTrue((Boolean) self.invoke(null, "/event/:eventId/teams/:teamId/decline"));
        assertTrue((Boolean) self.invoke(null, "/event/:eventId/teams/:teamId/roster"));
        assertFalse((Boolean) self.invoke(null, "/event/:eventId/teams/invite"));
        assertFalse((Boolean) self.invoke(null, "/event/:eventId/teams/:teamId/remove"));
        assertFalse((Boolean) self.invoke(null, "/event/:eventId/teams/:teamId/lock"));
        assertFalse((Boolean) self.invoke(null, "/event/:eventId/teams"));

        Method tm = InitDataRunner.class.getDeclaredMethod("isApiAllowedForTeamManager", SysApi.class);
        tm.setAccessible(true);
        assertTrue((Boolean) tm.invoke(null, api(1L, "/event/:eventId/teams/:teamId/roster", "PUT")));
        assertTrue((Boolean) tm.invoke(null, api(2L, "/team/:teamId/event-registrations", "GET")));
        assertFalse((Boolean) tm.invoke(null, api(3L, "/event/:eventId/teams/invite", "POST")));
        assertFalse((Boolean) tm.invoke(null, api(4L, "/event/:eventId/teams/:teamId/remove", "POST")));
        assertFalse((Boolean) tm.invoke(null, api(5L, "/event/:eventId/teams/:teamId/lock", "POST")));
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

    private static SysUser user(long id) {
        SysUser u = new SysUser();
        u.setId(id);
        return u;
    }
}
