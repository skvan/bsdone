/*
 * 账号权限重构（批次 7 / #154）：InitDataRunner 门户角色菜单「预设收敛」单测。
 *
 * 覆盖：
 *  ① 超集输入 → 仅保留设计集（列出被删项示例断言）；
 *  ② 设计集内保留；
 *  ③ 其它角色 / 租户级覆盖表零交互（never）；
 *  ④ 幂等：二跑零删除；
 *  ⑤ 设计集解析与补绑同源（收敛保留集 == 补绑产出集）；
 *  ⑥ 空设计集防全删：设计解析为空 → 零删除、绑定原样。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测 private 方法经反射调用。
 * 说明：租户级覆盖表落到独立实体/仓储（bs_tenant_role_menu / bs_tenant_role_menu_config），
 *       InitDataRunner 未注入其仓储——以 verifyNoInteractions 断言「结构性零触碰」。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
import com.bsball.repository.TenantRoleMenuConfigRepository;
import com.bsball.repository.TenantRoleMenuRepository;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
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
@DisplayName("InitDataRunner：门户角色菜单预设收敛（批次 7 / #154）")
class InitDataRunnerPortalRoleMenuPruneTest {

    private static final long TEAM_MANAGER_ROLE_ID = 100L;
    private static final long LEAGUE_ORGANIZER_ROLE_ID = 101L;
    private static final long MEMBER_ROLE_ID = 102L;
    private static final long OTHER_TENANT_ROLE_ID = 200L;

    /* 设计集（与补绑产出集同源；见 InitDataRunnerPortalRoleBindingTest 精确断言）。 */
    private static final Set<Long> DESIGN_TEAM_MANAGER = Set.of(1L, 2L, 3L, 4L, 5L, 8L, 9L, 10L, 11L);
    private static final Set<Long> DESIGN_LEAGUE_ORGANIZER =
            Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);

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
    /* 租户级覆盖表仓储：InitDataRunner 未注入，收敛必须零触碰（结构性保证）。 */
    @Mock
    private TenantRoleMenuRepository tenantRoleMenuRepository;
    @Mock
    private TenantRoleMenuConfigRepository tenantRoleMenuConfigRepository;

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
        lenient().when(sysApiRepository.findAll()).thenAnswer(inv -> new ArrayList<>(apiStore));
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
        lenient().doAnswer(inv -> {
            Iterable<SysRoleMenu> toRemove = inv.getArgument(0);
            for (SysRoleMenu rm : toRemove) {
                List<SysRoleMenu> list = roleMenus.get(rm.getRoleId());
                if (list != null) {
                    list.removeIf(x -> java.util.Objects.equals(x.getMenuId(), rm.getMenuId()));
                }
            }
            return null;
        }).when(sysRoleMenuRepository).deleteAllInBatch(anyIterable());
        lenient().when(sysRoleApiRepository.findByRoleId(anyLong()))
                .thenAnswer(inv -> new ArrayList<>(roleApis.getOrDefault((Long) inv.getArgument(0), List.of())));
        lenient().when(sysRoleApiRepository.save(any(SysRoleApi.class))).thenAnswer(inv -> {
            SysRoleApi ra = inv.getArgument(0);
            roleApis.computeIfAbsent(ra.getRoleId(), k -> new ArrayList<>()).add(ra);
            return ra;
        });
    }

    @Test
    @DisplayName("① 超集输入 → 仅保留设计集（示例：/admin/leagues、/admin/events、事件按钮、幻影 id 被删）")
    void supersetIsPrunedToDesignSet() throws Exception {
        // 两角色均预置「设计集 + 历史超集」：team_manager 混入 6/7/12/999；league_organizer 混入 999。
        preBind(TEAM_MANAGER_ROLE_ID, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 999L);
        preBind(LEAGUE_ORGANIZER_ROLE_ID, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 999L);

        invokePrune();

        Set<Long> tm = menuIdsOf(TEAM_MANAGER_ROLE_ID);
        assertEquals(DESIGN_TEAM_MANAGER, tm, "team_manager 收敛后应恰为设计集");
        assertFalse(tm.contains(6L), "示例：/admin/leagues(6) 超出球队负责人预设，应被移除");
        assertFalse(tm.contains(7L), "示例：/admin/events(7) 超出球队负责人预设，应被移除");
        assertFalse(tm.contains(12L), "示例：事件按钮(12) 超出球队负责人预设，应被移除");
        assertFalse(tm.contains(999L), "示例：幻影 id(999) 应被移除");

        Set<Long> lo = menuIdsOf(LEAGUE_ORGANIZER_ROLE_ID);
        assertEquals(DESIGN_LEAGUE_ORGANIZER, lo, "league_organizer 收敛后应恰为设计集");
        assertFalse(lo.contains(999L), "示例：幻影 id(999) 应被移除");
    }

    @Test
    @DisplayName("② 设计集内保留：设计集内的绑定行一条不删")
    void designSetIsRetained() throws Exception {
        preBind(TEAM_MANAGER_ROLE_ID, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 999L);

        invokePrune();

        assertTrue(menuIdsOf(TEAM_MANAGER_ROLE_ID).containsAll(DESIGN_TEAM_MANAGER),
                "设计集内节点（含父级目录 /business）必须全部保留");
    }

    @Test
    @DisplayName("③ 零误伤：其它角色绑定不动，租户级覆盖表零交互（never）")
    void otherRolesAndTenantTablesUntouched() throws Exception {
        // 其它角色预置绑定，收敛不得触碰。
        preBind(MEMBER_ROLE_ID, 999L);
        preBind(OTHER_TENANT_ROLE_ID, 999L);
        // 门户角色预置超集以触发删除路径。
        preBind(TEAM_MANAGER_ROLE_ID, 6L);

        invokePrune();

        assertEquals(Set.of(999L), menuIdsOf(MEMBER_ROLE_ID), "member 绑定不得被收敛触碰");
        assertEquals(Set.of(999L), menuIdsOf(OTHER_TENANT_ROLE_ID), "其它（租户级）角色绑定不得被收敛触碰");
        // 收敛仅以两门户角色为入口，绝不查询其它角色。
        verify(sysRoleMenuRepository, never()).findByRoleId(MEMBER_ROLE_ID);
        verify(sysRoleMenuRepository, never()).findByRoleId(OTHER_TENANT_ROLE_ID);
        // 租户级覆盖表仓储未被注入 / 未交互（bs_tenant_role_menu / bs_tenant_role_menu_config 零触碰）。
        verifyNoInteractions(tenantRoleMenuRepository, tenantRoleMenuConfigRepository);
    }

    @Test
    @DisplayName("④ 幂等：首跑删除、二跑零删除（deleteAll 总调用次数不增）")
    void secondRunDeletesNothing() throws Exception {
        // 两角色均预置「设计集 + 超集」：首跑应恰好删除超集部分（各 1 次 deleteAll），二跑零删除。
        preBind(TEAM_MANAGER_ROLE_ID, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L);
        preBind(LEAGUE_ORGANIZER_ROLE_ID, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 999L);

        invokePrune();
        verify(sysRoleMenuRepository, times(2)).deleteAllInBatch(anyIterable());
        assertEquals(DESIGN_TEAM_MANAGER, menuIdsOf(TEAM_MANAGER_ROLE_ID));
        assertEquals(DESIGN_LEAGUE_ORGANIZER, menuIdsOf(LEAGUE_ORGANIZER_ROLE_ID));

        invokePrune();
        verify(sysRoleMenuRepository, times(2)).deleteAllInBatch(anyIterable());
        assertEquals(DESIGN_TEAM_MANAGER, menuIdsOf(TEAM_MANAGER_ROLE_ID), "二跑后 team_manager 稳定为设计集");
        assertEquals(DESIGN_LEAGUE_ORGANIZER, menuIdsOf(LEAGUE_ORGANIZER_ROLE_ID), "二跑后 league_organizer 稳定为设计集");
    }

    @Test
    @DisplayName("⑤ 同源：收敛保留集 == 补绑产出集（同一设计集解析口径）")
    void pruneRetainsExactlyWhatBindProduces() throws Exception {
        // 干净角色上跑补绑，产出即设计集。
        invokeBindings();
        Set<Long> bindTm = menuIdsOf(TEAM_MANAGER_ROLE_ID);
        Set<Long> bindLo = menuIdsOf(LEAGUE_ORGANIZER_ROLE_ID);
        assertEquals(DESIGN_TEAM_MANAGER, bindTm, "补绑产出应等于 team_manager 设计集");
        assertEquals(DESIGN_LEAGUE_ORGANIZER, bindLo, "补绑产出应等于 league_organizer 设计集");

        // 重置为「补绑产出 + 超集」，再收敛：保留集必须逐字等于补绑产出集 → 同源。
        roleMenus.clear();
        Set<Long> superTm = new java.util.HashSet<>(bindTm);
        superTm.add(6L);
        superTm.add(999L);
        Set<Long> superLo = new java.util.HashSet<>(bindLo);
        superLo.add(999L);
        preBindSet(TEAM_MANAGER_ROLE_ID, superTm);
        preBindSet(LEAGUE_ORGANIZER_ROLE_ID, superLo);

        invokePrune();

        assertEquals(bindTm, menuIdsOf(TEAM_MANAGER_ROLE_ID), "收敛保留集应与补绑产出集同源");
        assertEquals(bindLo, menuIdsOf(LEAGUE_ORGANIZER_ROLE_ID), "收敛保留集应与补绑产出集同源");
    }

    @Test
    @DisplayName("⑥ 空设计集防全删：sys_menu 全空 → 设计解析为空 → 零删除、绑定原样")
    void emptyDesignSetDeletesNothing() throws Exception {
        // 构造：全部 role_menu 已存在（含超集与幻影 id），但 sys_menu 全空 → 设计集解析为空。
        preBind(TEAM_MANAGER_ROLE_ID, 1L, 6L, 999L);
        preBind(LEAGUE_ORGANIZER_ROLE_ID, 7L, 999L);
        menuStore.clear();

        invokePrune();

        // 守卫：设计集为空时跳过收敛，绝不触发批量删除。
        verify(sysRoleMenuRepository, never()).deleteAllInBatch(anyIterable());
        // 绑定原样：一条不删（防止种子漂移把该角色全部绑定误删）。
        assertEquals(Set.of(1L, 6L, 999L), menuIdsOf(TEAM_MANAGER_ROLE_ID), "空设计集下 team_manager 绑定须原样保留");
        assertEquals(Set.of(7L, 999L), menuIdsOf(LEAGUE_ORGANIZER_ROLE_ID), "空设计集下 league_organizer 绑定须原样保留");
    }

    // ---- helpers ----

    private void invokePrune() throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("ensurePortalRolesPruneExtraMenusIfNeeded");
        m.setAccessible(true);
        m.invoke(runner);
    }

    private void invokeBindings() throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("ensurePortalRoleBindingsIfNeeded", long.class);
        m.setAccessible(true);
        m.invoke(runner, 1L);
    }

    private void preBind(long roleId, Long... menuIds) {
        for (Long mid : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(mid);
            roleMenus.computeIfAbsent(roleId, k -> new ArrayList<>()).add(rm);
        }
    }

    private void preBindSet(long roleId, Collection<Long> menuIds) {
        preBind(roleId, menuIds.toArray(new Long[0]));
    }

    private Set<Long> menuIdsOf(long roleId) {
        return roleMenus.getOrDefault(roleId, List.of()).stream()
                .map(SysRoleMenu::getMenuId).collect(Collectors.toSet());
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
