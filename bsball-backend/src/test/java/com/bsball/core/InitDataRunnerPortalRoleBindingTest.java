/*
 * 账号权限重构（批次 3a，Task 3.8）：InitDataRunner 门户角色绑定/端点种子 幂等性单测。
 *
 * 覆盖：
 *  - ensurePortalRoleBindingsIfNeeded：第二次调用零 insert（role_menu / role_api 均判存跳过）；
 *  - team_manager / league_organizer / member 关键绑定项存在断言（fixture 化菜单/API 列表）；
 *  - 敏感端点不授门户角色：/team/delete、/league/*owner*、/league/create-request/*。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测 private 方法经反射调用。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysRoleMenu;
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
@DisplayName("InitDataRunner：门户角色绑定/按钮/端点种子（批次 3a Task 3.8）")
class InitDataRunnerPortalRoleBindingTest {

    private static final long OP_ID = 1L;
    private static final long TEAM_MANAGER_ROLE_ID = 100L;
    private static final long LEAGUE_ORGANIZER_ROLE_ID = 101L;
    private static final long MEMBER_ROLE_ID = 102L;

    @Mock
    private SysMenuRepository sysMenuRepository;
    @Mock
    private SysApiRepository sysApiRepository;
    @Mock
    private SysDictTypeRepository sysDictTypeRepository;
    @Mock
    private SysDictDataRepository sysDictDataRepository;
    @Mock
    private SysConfigRepository sysConfigRepository;
    @Mock
    private SysRoleRepository sysRoleRepository;
    @Mock
    private SysUserRepository sysUserRepository;
    @Mock
    private SysRoleMenuRepository sysRoleMenuRepository;
    @Mock
    private SysRoleApiRepository sysRoleApiRepository;
    @Mock
    private SysUserRoleRepository sysUserRoleRepository;
    @Mock
    private StadiumRepository stadiumRepository;
    @Mock
    private TenantProperties tenantProperties;
    @Mock
    private InitSeedProperties initSeedProperties;
    @Mock
    private SysMenuApiRepository sysMenuApiRepository;

    private InitDataRunner runner;
    private final Map<Long, List<SysRoleMenu>> roleMenus = new HashMap<>();
    private final Map<Long, List<SysRoleApi>> roleApis = new HashMap<>();
    private final List<SysMenu> menuStore = new ArrayList<>();
    private final List<SysApi> apiStore = new ArrayList<>();

    @BeforeEach
    void setUp() {
        runner = new InitDataRunner(sysMenuRepository, sysApiRepository, sysDictTypeRepository, sysDictDataRepository,
                sysConfigRepository, sysRoleRepository, sysUserRepository, sysRoleMenuRepository, sysRoleApiRepository,
                sysUserRoleRepository, stadiumRepository, tenantProperties, initSeedProperties, sysMenuApiRepository);

        roleMenus.clear();
        roleApis.clear();
        menuStore.clear();
        menuStore.addAll(fixtureMenus());
        apiStore.clear();
        apiStore.addAll(fixtureApis());

        lenient().when(sysMenuRepository.findAll()).thenAnswer(inv -> new ArrayList<>(menuStore));
        lenient().when(sysMenuRepository.save(any(SysMenu.class))).thenAnswer(inv -> {
            SysMenu m = inv.getArgument(0);
            menuStore.add(m);
            return m;
        });
        lenient().when(sysMenuRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sysApiRepository.findAll()).thenAnswer(inv -> new ArrayList<>(apiStore));
        lenient().when(sysApiRepository.save(any(SysApi.class))).thenAnswer(inv -> {
            SysApi a = inv.getArgument(0);
            apiStore.add(a);
            return a;
        });
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("team_manager"))
                .thenReturn(Optional.of(role(TEAM_MANAGER_ROLE_ID, "team_manager")));
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("league_organizer"))
                .thenReturn(Optional.of(role(LEAGUE_ORGANIZER_ROLE_ID, "league_organizer")));
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("member"))
                .thenReturn(Optional.of(role(MEMBER_ROLE_ID, "member")));
        lenient().when(sysRoleMenuRepository.findByRoleId(anyLong()))
                .thenAnswer(inv -> new ArrayList<>(roleMenus.getOrDefault((Long) inv.getArgument(0), List.of())));
        lenient().when(sysRoleMenuRepository.save(any(SysRoleMenu.class))).thenAnswer(inv -> {
            SysRoleMenu rm = inv.getArgument(0);
            roleMenus.computeIfAbsent(rm.getRoleId(), k -> new ArrayList<>()).add(rm);
            return rm;
        });
        lenient().when(sysRoleApiRepository.findByRoleId(anyLong()))
                .thenAnswer(inv -> new ArrayList<>(roleApis.getOrDefault((Long) inv.getArgument(0), List.of())));
        lenient().when(sysRoleApiRepository.save(any(SysRoleApi.class))).thenAnswer(inv -> {
            SysRoleApi ra = inv.getArgument(0);
            roleApis.computeIfAbsent(ra.getRoleId(), k -> new ArrayList<>()).add(ra);
            return ra;
        });
    }

    @Test
    @DisplayName("第二次调用零 insert：role_menu 与 role_api 均判存跳过（仅补绑不清理）")
    void secondInvocationInsertsNothing() throws Exception {
        invokeBindings();
        int menusAfterFirst = totalMenus();
        int apisAfterFirst = totalApis();
        assertTrue(menusAfterFirst > 0, "首次调用应产生 role_menu 绑定");
        assertTrue(apisAfterFirst > 0, "首次调用应产生 role_api 绑定");

        invokeBindings();

        assertEquals(menusAfterFirst, totalMenus(), "第二次调用不得新增 sys_role_menu");
        assertEquals(apisAfterFirst, totalApis(), "第二次调用不得新增 sys_role_api");
    }

    @Test
    @DisplayName("team_manager：关键菜单/按钮绑定存在，且不授 /team/delete、/league/*、/player/create")
    void teamManagerKeyBindings() throws Exception {
        invokeBindings();

        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 8L, 9L, 10L, 11L), menuIdsOf(TEAM_MANAGER_ROLE_ID),
                "team_manager 精确菜单/id 集：/business、/admin/teams、/admin/players、/admin/player-claims、/admin/lineup-templates 及对应按钮");

        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 10L, 17L, 19L, 20L), apiIdsOf(TEAM_MANAGER_ROLE_ID),
                "team_manager 精确 API/id 集：member 自助集 + /player/list + /player/team-options + /team/*（非删除）+ 阵容模板 + 认领审核面；不含 /team/delete、/league/list、/player/create");
    }

    @Test
    @DisplayName("member：仅自助端点（/account/player-profile* 与 /portal/*）")
    void memberSelfServiceOnly() throws Exception {
        invokeBindings();

        assertEquals(Set.of(1L, 2L, 3L, 17L), apiIdsOf(MEMBER_ROLE_ID),
                "member 仅应获 /account/player-profile(GET/POST/:playerId) 与 /portal/*");
        assertTrue(menuIdsOf(MEMBER_ROLE_ID).isEmpty(), "member 本任务不补绑菜单/按钮");
    }

    @Test
    @DisplayName("league_organizer：获联盟/赛事/比赛；排除 /league/*owner* 与 /league/create-request/*")
    void leagueOrganizerExcludesSensitive() throws Exception {
        invokeBindings();

        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L), menuIdsOf(LEAGUE_ORGANIZER_ROLE_ID),
                "league_organizer 精确菜单/id 集：team_manager 菜单集 + /admin/leagues、/admin/events 及赛事按钮");

        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 10L, 11L, 12L, 15L, 16L, 17L, 19L, 20L),
                apiIdsOf(LEAGUE_ORGANIZER_ROLE_ID),
                "league_organizer 精确 API/id 集：含 /league/*、/event/*、/game/* 与认领审核面；不含 /team/delete、/league/:id/owners、/league/create-request/*");
    }

    @Test
    @DisplayName("review-path 谓词：member 不含认领审核面，team_manager / league_organizer 含")
    void playerClaimReviewPathBinding() throws Exception {
        invokeBindings();

        Set<Long> memberApis = apiIdsOf(MEMBER_ROLE_ID);
        assertFalse(memberApis.contains(19L), "member 不得获 POST /account/player-claims/:id/approve");
        assertFalse(memberApis.contains(20L), "member 不得获 POST /account/player-claims/:id/reject");

        assertTrue(apiIdsOf(TEAM_MANAGER_ROLE_ID).containsAll(Set.of(19L, 20L)), "team_manager 应获认领审核面");
        assertTrue(apiIdsOf(LEAGUE_ORGANIZER_ROLE_ID).containsAll(Set.of(19L, 20L)), "league_organizer 应获认领审核面");
    }

    @Test
    @DisplayName("端点种子：ensurePortalProvisionApisIfNeeded 首次补录，第二次零 insert（计数不变）")
    void portalProvisionApisIdempotent() throws Exception {
        invokePortalProvisionApis();
        int afterFirst = apiStore.size();
        assertTrue(afterFirst > 20, "首次调用应补录门户建联盟端点");
        assertTrue(apiStore.stream().anyMatch(a -> "/league/:id/owner/assign".equals(a.getPath())),
                "应补录 /league/:id/owner/assign");

        invokePortalProvisionApis();
        assertEquals(afterFirst, apiStore.size(), "第二次调用不得新增 sys_api（幂等判存）");
    }

    @Test
    @DisplayName("按钮种子：6 条新按钮（球员增删改导入/建联盟/认领审核）各落库一次")
    void portalButtonSeedsInserted() throws Exception {
        invokeButtonSeeds();

        Map<String, Long> counts = menuStore.stream()
                .filter(m -> m.getPermission() != null)
                .collect(Collectors.groupingBy(SysMenu::getPermission, Collectors.counting()));
        for (String perm : List.of("business:player:create", "business:player:edit", "business:player:delete",
                "business:player:import", "business:league:create", "business:claim:review")) {
            assertEquals(1L, counts.getOrDefault(perm, 0L).longValue(), "应补插按钮且仅一次：" + perm);
        }
    }

    // ---- helpers ----

    private void invokeBindings() throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("ensurePortalRoleBindingsIfNeeded", long.class);
        m.setAccessible(true);
        m.invoke(runner, OP_ID);
    }

    private void invokePortalProvisionApis() throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("ensurePortalProvisionApisIfNeeded");
        m.setAccessible(true);
        m.invoke(runner);
    }

    private void invokeButtonSeeds() throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("ensureMenuDirectoryTypesAndDefaultButtons");
        m.setAccessible(true);
        m.invoke(runner);
    }

    private int totalMenus() {
        return roleMenus.values().stream().mapToInt(List::size).sum();
    }

    private int totalApis() {
        return roleApis.values().stream().mapToInt(List::size).sum();
    }

    private Set<Long> menuIdsOf(long roleId) {
        return roleMenus.getOrDefault(roleId, List.of()).stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
    }

    private Set<Long> apiIdsOf(long roleId) {
        return roleApis.getOrDefault(roleId, List.of()).stream().map(SysRoleApi::getApiId).collect(Collectors.toSet());
    }

    private static SysRole role(long id, String code) {
        SysRole r = new SysRole();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    private static SysMenu menu(long id, long parentId, String path, String permission) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setPath(path);
        m.setPermission(permission);
        return m;
    }

    private static List<SysMenu> fixtureMenus() {
        List<SysMenu> list = new ArrayList<>();
        list.add(menu(1L, 0L, "/business", null));
        list.add(menu(2L, 1L, "/admin/teams", null));
        list.add(menu(3L, 1L, "/admin/players", null));
        list.add(menu(4L, 1L, "/admin/player-claims", null));
        list.add(menu(5L, 1L, "/admin/lineup-templates", null));
        list.add(menu(6L, 1L, "/admin/leagues", null));
        list.add(menu(7L, 1L, "/admin/events", null));
        list.add(menu(8L, 2L, null, "business:team:create"));
        list.add(menu(9L, 2L, null, "business:team:edit"));
        list.add(menu(10L, 2L, null, "business:team:managePlayers"));
        list.add(menu(11L, 5L, null, "business:lineup-template:manage"));
        list.add(menu(12L, 7L, null, "business:event:create"));
        return list;
    }

    private static SysApi api(long id, String path, String method) {
        SysApi a = new SysApi();
        a.setId(id);
        a.setPath(path);
        a.setMethod(method);
        return a;
    }

    private static List<SysApi> fixtureApis() {
        List<SysApi> list = new ArrayList<>();
        list.add(api(1L, "/account/player-profile", "GET"));
        list.add(api(2L, "/account/player-profile", "POST"));
        list.add(api(3L, "/account/player-profile/:playerId", "PUT"));
        list.add(api(4L, "/player/list", "GET"));
        list.add(api(5L, "/player/team-options", "GET"));
        list.add(api(6L, "/team/list", "GET"));
        list.add(api(7L, "/team/create", "POST"));
        list.add(api(8L, "/team/update/:id", "PUT"));
        list.add(api(9L, "/team/delete/:id", "DELETE"));
        list.add(api(10L, "/lineup-template/list", "GET"));
        list.add(api(11L, "/league/list", "GET"));
        list.add(api(12L, "/league/create", "POST"));
        list.add(api(13L, "/league/:id/owners", "GET"));
        list.add(api(14L, "/league/create-request/list", "GET"));
        list.add(api(15L, "/event/create", "POST"));
        list.add(api(16L, "/game/create", "POST"));
        list.add(api(17L, "/portal/feedback/submit", "POST"));
        list.add(api(18L, "/player/create", "POST"));
        list.add(api(19L, "/account/player-claims/:id/approve", "POST"));
        list.add(api(20L, "/account/player-claims/:id/reject", "POST"));
        return list;
    }
}
