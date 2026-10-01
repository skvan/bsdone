/*
 * 账号权限重构（批次 4b，Task 4b-1）：InitDataRunner 归还/下架按钮种子与联盟删除名目单测。
 *
 * 覆盖：
 *  - ① 按钮种子幂等：ensureMenuDirectoryTypesAndDefaultButtons 二次调用零新增；6 条新按钮各落库一次；
 *  - ② tenant_admin 默认集合（collectTenantAdminDefaultMenuIds）：含 5 个 business:*:return、排除 5 个 business:*:delete（含 league:delete）；
 *  - ③ 超管全量绑定（ensureAdminRoleBindsAllMenusIfNeeded）：纳入全部菜单（含 6 条新按钮）；
 *  - ④ tenant_admin 角色创建路径（ensureTenantAdminRoleIfNeeded）：落库绑定含 5 个 return、不含 5 个 delete；
 *  - ⑤ 收窄（ensureTenantAdminRoleExcludesBusinessDeleteButtons）：仅移除 delete 绑定、保留 return。
 *  - ⑥ 升级补绑（ensureTenantAdminRoleRebindsDefaultMenusIfNeeded，批次 4b-2）：角色存在缺 5 个 return → 恰补 5；
 *  - ⑦ 升级补绑幂等：二次调用零新增；⑧ 排除语义保持（不补 business:*:delete）；⑨ 角色不存在 → 零交互/零插入。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测 private 方法经反射调用（对齐既有 InitDataRunner*Test）。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysRoleMenu;
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitDataRunner：归还/下架按钮种子与联盟删除名目（批次 4b Task 4b-1）")
class InitDataRunnerReturnButtonSeedTest {

    private static final long OP_ID = 1L;
    private static final long ADMIN_ROLE_ID = 900L;

    /** 5 个 business:*:return（归还/下架）。 */
    private static final Set<String> RETURN_PERMS = Set.of(
            "business:player:return", "business:team:return", "business:event:return",
            "business:game:return", "business:league:return");

    /** 5 个 business:*:delete（历史处置权：删除仅超管；含本批补齐的 league:delete 名目）。 */
    private static final Set<String> DELETE_PERMS = Set.of(
            "business:player:delete", "business:team:delete", "business:event:delete",
            "business:game:delete", "business:league:delete");

    /** 本批新增（种子）按钮权限共 6 条：5 个 return + 1 个 league:delete。 */
    private static final Set<String> BATCH_NEW_PERMS = Stream
            .concat(RETURN_PERMS.stream(), Stream.of("business:league:delete"))
            .collect(Collectors.toSet());

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
    private final List<SysMenu> menuStore = new ArrayList<>();
    private final List<SysApi> apiStore = new ArrayList<>();
    private final Map<Long, List<SysRoleMenu>> roleMenus = new HashMap<>();
    private final Map<Long, List<SysRoleApi>> roleApis = new HashMap<>();
    private long nextId = 1000L;
    private int menuSaveCount;

    @BeforeEach
    void setUp() {
        runner = new InitDataRunner(sysMenuRepository, sysApiRepository, sysDictTypeRepository, sysDictDataRepository,
                sysConfigRepository, sysRoleRepository, sysUserRepository, sysRoleMenuRepository, sysRoleApiRepository,
                sysUserRoleRepository, stadiumRepository, tenantProperties, initSeedProperties, sysMenuApiRepository);

        menuStore.clear();
        apiStore.clear();
        roleMenus.clear();
        roleApis.clear();
        nextId = 1000L;
        menuSaveCount = 0;

        lenient().when(sysUserRepository.findByUsernameAndDeletedAtIsNull("admin"))
                .thenReturn(Optional.of(user(OP_ID)));
        lenient().when(sysMenuRepository.findAll()).thenAnswer(inv -> new ArrayList<>(menuStore));
        lenient().when(sysMenuRepository.save(any(SysMenu.class))).thenAnswer(inv -> {
            SysMenu m = inv.getArgument(0);
            if (m.getId() == null) {
                m.setId(nextId++);
            }
            menuStore.add(m);
            menuSaveCount++;
            return m;
        });
        lenient().when(sysMenuRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sysMenuApiRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(sysApiRepository.findAll()).thenAnswer(inv -> new ArrayList<>(apiStore));
        lenient().when(sysRoleRepository.save(any(SysRole.class))).thenAnswer(inv -> {
            SysRole r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(nextId++);
            }
            return r;
        });
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
    @DisplayName("① 按钮种子幂等：二次调用零新增，6 条新按钮各落库一次")
    void buttonSeedIdempotent() throws Exception {
        menuStore.addAll(pageFixtures());
        apiStore.addAll(deleteApis());

        invokeButtonSeeds();
        int sizeAfterFirst = menuStore.size();
        int savesFirst = menuSaveCount;
        assertTrue(savesFirst > 0, "首次调用应补插按钮种子");

        invokeButtonSeeds();

        assertEquals(sizeAfterFirst, menuStore.size(), "第二次调用不得新增 sys_menu（幂等判存）");
        assertEquals(savesFirst, menuSaveCount, "第二次调用不得再触发 sysMenu.save");

        Map<String, Long> counts = menuStore.stream()
                .filter(m -> m.getPermission() != null)
                .collect(Collectors.groupingBy(SysMenu::getPermission, Collectors.counting()));
        for (String perm : BATCH_NEW_PERMS) {
            assertEquals(1L, counts.getOrDefault(perm, 0L).longValue(), "应恰落库一次：" + perm);
        }
    }

    @Test
    @DisplayName("② tenant_admin 默认集合：含 5 个 return、排除 5 个 delete（含 league:delete）")
    void tenantAdminDefaultSetIncludesReturnExcludesDelete() throws Exception {
        Set<Long> result = collectTenantAdminDefaultMenuIds(tenantAdminFixture());

        for (long id : RETURN_IDS) {
            assertTrue(result.contains(id), "应纳入 return 按钮菜单 id=" + id);
        }
        for (long id : DELETE_IDS) {
            assertFalse(result.contains(id), "应排除 delete 按钮菜单 id=" + id);
        }
        // 父目录/页面随 return 按钮沿 parentId 展开纳入
        assertTrue(result.containsAll(Set.of(1L, 2L, 3L, 4L, 5L)), "应含 business 目录及各管理页面");
        assertTrue(result.contains(30L), "普通业务按钮（player:create）应纳入");
    }

    @Test
    @DisplayName("③ 超管全量绑定：ensureAdminRoleBindsAllMenusIfNeeded 纳入全部菜单（含 6 条新按钮）")
    void adminBindsAllMenusIncludingNewButtons() throws Exception {
        menuStore.addAll(pageFixtures());
        apiStore.addAll(deleteApis());
        invokeButtonSeeds();

        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("admin"))
                .thenReturn(Optional.of(role(ADMIN_ROLE_ID, "admin")));

        invokeAdminBindsAllMenus();

        Set<Long> bound = boundMenuIds(ADMIN_ROLE_ID);
        for (SysMenu m : menuStore) {
            assertTrue(bound.contains(m.getId()), "超管应绑定全部菜单 id=" + m.getId());
        }
        Set<Long> newIds = menuStore.stream()
                .filter(m -> m.getPermission() != null && BATCH_NEW_PERMS.contains(m.getPermission()))
                .map(SysMenu::getId)
                .collect(Collectors.toSet());
        assertEquals(6, newIds.size(), "应检出 6 条新按钮");
        assertTrue(bound.containsAll(newIds), "超管应绑定 6 条新按钮（含 league:delete）");
    }

    @Test
    @DisplayName("④ tenant_admin 创建路径：ensureTenantAdminRoleIfNeeded 绑定含 return、不含 delete")
    void tenantAdminCreationBindsReturnNotDelete() throws Exception {
        menuStore.addAll(tenantAdminFixture());

        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin"))
                .thenReturn(Optional.empty());

        invokeEnsureTenantAdminRole();

        assertEquals(1, roleMenus.size(), "创建路径应写入 tenant_admin 的菜单绑定");
        long roleId = roleMenus.keySet().iterator().next();
        Set<Long> bound = boundMenuIds(roleId);

        assertTrue(bound.containsAll(RETURN_IDS), "tenant_admin 应绑定 5 个 return 按钮");
        for (long id : DELETE_IDS) {
            assertFalse(bound.contains(id), "tenant_admin 不应绑定 delete 按钮 id=" + id);
        }
    }

    @Test
    @DisplayName("⑤ 收窄：ensureTenantAdminRoleExcludesBusinessDeleteButtons 仅移除 delete、保留 return")
    void excludeRemovesDeleteKeepsReturn() throws Exception {
        long roleId = 500L;
        menuStore.addAll(tenantAdminFixture());
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin"))
                .thenReturn(Optional.of(role(roleId, "tenant_admin")));

        List<SysRoleMenu> existing = new ArrayList<>();
        for (long mid : new long[]{1L, 2L, 3L, 4L, 5L, 10L, 11L, 12L, 13L, 14L, 20L, 21L, 22L, 23L, 24L}) {
            existing.add(roleMenu(roleId, mid));
        }
        roleMenus.put(roleId, existing);

        List<Long> deleted = new ArrayList<>();
        doAnswer(inv -> {
            deleted.add(((SysRoleMenu) inv.getArgument(0)).getMenuId());
            return null;
        }).when(sysRoleMenuRepository).delete(any(SysRoleMenu.class));

        invokeNoArg("ensureTenantAdminRoleExcludesBusinessDeleteButtons");

        assertEquals(DELETE_IDS, new HashSet<>(deleted), "应仅移除 5 个 delete 按钮绑定");
        assertTrue(deleted.stream().noneMatch(RETURN_IDS::contains), "不得误删 return 按钮绑定");
    }

    @Test
    @DisplayName("⑥ 升级补绑：tenant_admin 角色存在但缺 5 个 return → 恰补 5 个（其余零插入）")
    void rebindAddsOnlyMissingReturnButtons() throws Exception {
        long roleId = 500L;
        menuStore.addAll(tenantAdminFixture());
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin"))
                .thenReturn(Optional.of(role(roleId, "tenant_admin")));
        // 既有绑定：默认集合中除 5 个 return 外均已绑定
        List<SysRoleMenu> existing = new ArrayList<>();
        for (long mid : new long[]{1L, 2L, 3L, 4L, 5L, 30L}) {
            existing.add(roleMenu(roleId, mid));
        }
        roleMenus.put(roleId, existing);

        invokeRebindTenantAdminDefaults();

        List<SysRoleMenu> rows = roleMenus.get(roleId);
        assertEquals(11, rows.size(), "应恰补 5 行（既有 6 + 新增 5，零重复）");
        Set<Long> bound = boundMenuIds(roleId);
        Set<Long> newlyAdded = new HashSet<>(bound);
        newlyAdded.removeAll(Set.of(1L, 2L, 3L, 4L, 5L, 30L));
        assertEquals(new HashSet<>(RETURN_IDS), newlyAdded, "新增应恰为 5 个 return 按钮");
    }

    @Test
    @DisplayName("⑦ 升级补绑幂等：二次调用 ensureTenantAdminRoleRebindsDefaultMenusIfNeeded 零新增")
    void rebindIsIdempotent() throws Exception {
        long roleId = 500L;
        menuStore.addAll(tenantAdminFixture());
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin"))
                .thenReturn(Optional.of(role(roleId, "tenant_admin")));
        roleMenus.put(roleId, new ArrayList<>());

        invokeRebindTenantAdminDefaults();
        int sizeAfterFirst = roleMenus.get(roleId).size();
        assertTrue(sizeAfterFirst > 0, "首次调用应补绑缺失的默认节点");

        invokeRebindTenantAdminDefaults();

        assertEquals(sizeAfterFirst, roleMenus.get(roleId).size(), "二次调用不得新增任何 role_menu 行");
    }

    @Test
    @DisplayName("⑧ 排除语义保持：补绑不纳入 business:*:delete（含 league:delete）")
    void rebindExcludesBusinessDeleteButtons() throws Exception {
        long roleId = 500L;
        menuStore.addAll(tenantAdminFixture());
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin"))
                .thenReturn(Optional.of(role(roleId, "tenant_admin")));
        roleMenus.put(roleId, new ArrayList<>());

        invokeRebindTenantAdminDefaults();

        Set<Long> bound = boundMenuIds(roleId);
        assertTrue(bound.containsAll(RETURN_IDS), "补绑后应含 5 个 return");
        for (long id : DELETE_IDS) {
            assertFalse(bound.contains(id), "补绑不得纳入 delete 按钮 id=" + id);
        }
    }

    @Test
    @DisplayName("⑨ 角色不存在：ensureTenantAdminRoleRebindsDefaultMenusIfNeeded 零交互/零插入")
    void rebindNoOpWhenRoleMissing() throws Exception {
        menuStore.addAll(tenantAdminFixture());
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin"))
                .thenReturn(Optional.empty());

        invokeRebindTenantAdminDefaults();

        assertTrue(roleMenus.isEmpty(), "角色不存在时不得写入 role_menu");
        verify(sysRoleMenuRepository, never()).findByRoleId(anyLong());
        verify(sysRoleMenuRepository, never()).save(any(SysRoleMenu.class));
        verify(sysMenuRepository, never()).findAll();
        verify(sysUserRepository, never()).findByUsernameAndDeletedAtIsNull(any());
    }

    // ---- 反射调用 ----

    private void invokeButtonSeeds() throws Exception {
        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");
    }

    private void invokeAdminBindsAllMenus() throws Exception {
        invokeNoArg("ensureAdminRoleBindsAllMenusIfNeeded");
    }

    private void invokeEnsureTenantAdminRole() throws Exception {
        invokeNoArg("ensureTenantAdminRoleIfNeeded");
    }

    private void invokeRebindTenantAdminDefaults() throws Exception {
        invokeNoArg("ensureTenantAdminRoleRebindsDefaultMenusIfNeeded");
    }

    private void invokeNoArg(String name) throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(runner);
    }

    private static Set<Long> collectTenantAdminDefaultMenuIds(List<SysMenu> menus) throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("collectTenantAdminDefaultMenuIds", List.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Long> result = (Set<Long>) m.invoke(null, menus);
        return result;
    }

    // ---- fixtures / helpers ----

    private static final Set<Long> RETURN_IDS = Set.of(10L, 11L, 12L, 13L, 14L);
    private static final Set<Long> DELETE_IDS = Set.of(20L, 21L, 22L, 23L, 24L);

    private Set<Long> boundMenuIds(long roleId) {
        return roleMenus.getOrDefault(roleId, List.of()).stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static List<SysMenu> pageFixtures() {
        List<SysMenu> list = new ArrayList<>();
        list.add(menu(1L, 0L, "/business", null, 1));
        list.add(menu(2L, 1L, "/admin/events", null, 2));
        list.add(menu(3L, 1L, "/admin/teams", null, 2));
        list.add(menu(4L, 1L, "/admin/lineup-templates", null, 2));
        list.add(menu(5L, 1L, "/admin/users", null, 2));
        list.add(menu(6L, 1L, "/admin/roles", null, 2));
        list.add(menu(7L, 1L, "/admin/menus", null, 2));
        list.add(menu(8L, 1L, "/admin/players", null, 2));
        list.add(menu(9L, 1L, "/admin/leagues", null, 2));
        list.add(menu(10L, 1L, "/admin/player-claims", null, 2));
        return list;
    }

    /** business 目录 + 4 页面 + 5 return + 5 delete + 1 普通按钮（parentId 指向页面，供 collect 沿父展开）。 */
    private static List<SysMenu> tenantAdminFixture() {
        List<SysMenu> list = new ArrayList<>();
        list.add(menu(1L, 0L, "/business", null, 1));
        list.add(menu(2L, 1L, "/admin/players", null, 2));
        list.add(menu(3L, 1L, "/admin/teams", null, 2));
        list.add(menu(4L, 1L, "/admin/events", null, 2));
        list.add(menu(5L, 1L, "/admin/leagues", null, 2));
        // 5 个 return
        list.add(menu(10L, 2L, null, "business:player:return", 3));
        list.add(menu(11L, 3L, null, "business:team:return", 3));
        list.add(menu(12L, 4L, null, "business:event:return", 3));
        list.add(menu(13L, 4L, null, "business:game:return", 3));
        list.add(menu(14L, 5L, null, "business:league:return", 3));
        // 5 个 delete（应被排除）
        list.add(menu(20L, 2L, null, "business:player:delete", 3));
        list.add(menu(21L, 3L, null, "business:team:delete", 3));
        list.add(menu(22L, 4L, null, "business:event:delete", 3));
        list.add(menu(23L, 4L, null, "business:game:delete", 3));
        list.add(menu(24L, 5L, null, "business:league:delete", 3));
        // 普通按钮
        list.add(menu(30L, 2L, null, "business:player:create", 3));
        return list;
    }

    private static List<SysApi> deleteApis() {
        return List.of(
                api(2001L, "/player/delete/:id", "DELETE"),
                api(2002L, "/team/delete/:id", "DELETE"),
                api(2003L, "/event/delete/:id", "DELETE"),
                api(2004L, "/game/delete/:id", "DELETE"),
                api(2005L, "/league/delete/:id", "DELETE"));
    }

    private static SysMenu menu(long id, long parentId, String path, String permission, int menuType) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setPath(path);
        m.setPermission(permission);
        m.setMenuType(menuType);
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

    private static SysRoleMenu roleMenu(long roleId, long menuId) {
        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(roleId);
        rm.setMenuId(menuId);
        return rm;
    }

    private static SysUser user(long id) {
        SysUser u = new SysUser();
        u.setId(id);
        return u;
    }
}
