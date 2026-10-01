/*
 * 账号权限重构（批次 5，Task ⑭）：InitDataRunner 其它 delete 复刻——按钮种子与名目矩阵单测。
 *
 * 覆盖（对齐既有 InitDataRunnerReturnButtonSeedTest，独立 fixture 含本批新增三页面）：
 *  - ① 按钮种子幂等：ensureMenuDirectoryTypesAndDefaultButtons 二次调用零新增；6 条新按钮
 *    （coach/highlight-moment/stadium 各 delete+return）各落库一次；
 *  - ② API 绑定矩阵：6 条新按钮的 menu_api 链接均命中既有 DELETE ApiDef
 *    （/coach/delete/:id、/highlight-moment/delete/{id}、/stadium/delete/:id）；
 *  - ③ tenant_admin 默认集合（collectTenantAdminDefaultMenuIds）：含 3 个 business:*:return、
 *    排除 3 个 business:*:delete，页面与 business 目录沿 parentId 展开纳入；
 *  - ④ 超管全量绑定（ensureAdminRoleBindsAllMenusIfNeeded）：纳入全部菜单（含 6 条新按钮）。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测 private 方法经反射调用（对齐既有
 * InitDataRunner*Test）。fixture 仅含 /business 目录 + 本批三页面（其余页面缺失 → 其种子被
 * parent==null 跳过，不影响本类断言）。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysMenuApi;
import com.bsball.model.entity.SysRole;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitDataRunner：其它 delete 复刻按钮种子与名目矩阵（批次 5 Task ⑭）")
class InitDataRunnerMoreReturnButtonSeedTest {

    private static final long OP_ID = 1L;
    private static final long ADMIN_ROLE_ID = 900L;

    /** 本批新增 3 个 business:*:return（自动入租管默认集合）。 */
    private static final Set<String> NEW_RETURN_PERMS = Set.of(
            "business:coach:return", "business:highlight-moment:return", "business:stadium:return");

    /** 本批新增 3 个 business:*:delete（租管默认集合排除）。 */
    private static final Set<String> NEW_DELETE_PERMS = Set.of(
            "business:coach:delete", "business:highlight-moment:delete", "business:stadium:delete");

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
    private final List<SysMenuApi> menuApiStore = new ArrayList<>();
    private final Map<Long, List<SysRoleMenu>> roleMenus = new HashMap<>();
    private long nextId = 1000L;
    private int menuSaveCount;

    @BeforeEach
    void setUp() {
        runner = new InitDataRunner(sysMenuRepository, sysApiRepository, sysDictTypeRepository, sysDictDataRepository,
                sysConfigRepository, sysRoleRepository, sysUserRepository, sysRoleMenuRepository, sysRoleApiRepository,
                sysUserRoleRepository, stadiumRepository, tenantProperties, initSeedProperties, sysMenuApiRepository);

        menuStore.clear();
        apiStore.clear();
        menuApiStore.clear();
        roleMenus.clear();
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
        lenient().when(sysMenuApiRepository.save(any(SysMenuApi.class))).thenAnswer(inv -> {
            SysMenuApi link = inv.getArgument(0);
            menuApiStore.add(link);
            return link;
        });
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
    }

    @Test
    @DisplayName("① 按钮种子幂等：二次调用零新增，6 条新按钮各落库一次")
    void buttonSeedIdempotent() throws Exception {
        menuStore.addAll(pagesOnly());
        apiStore.addAll(deleteApis());

        invokeButtonSeeds();
        int sizeAfterFirst = menuStore.size();
        int savesFirst = menuSaveCount;
        assertEquals(6, savesFirst, "首次调用应恰补插 6 条新按钮（本批三页面各 delete+return）");

        invokeButtonSeeds();

        assertEquals(sizeAfterFirst, menuStore.size(), "第二次调用不得新增 sys_menu（幂等判存）");
        assertEquals(savesFirst, menuSaveCount, "第二次调用不得再触发 sysMenu.save");

        Map<String, Long> counts = menuStore.stream()
                .filter(m -> m.getPermission() != null)
                .collect(Collectors.groupingBy(SysMenu::getPermission, Collectors.counting()));
        for (String perm : Set.of("business:coach:delete", "business:coach:return",
                "business:highlight-moment:delete", "business:highlight-moment:return",
                "business:stadium:delete", "business:stadium:return")) {
            assertEquals(1L, counts.getOrDefault(perm, 0L).longValue(), "应恰落库一次：" + perm);
        }
    }

    @Test
    @DisplayName("② API 绑定矩阵：6 条新按钮 menu_api 均命中既有 DELETE ApiDef")
    void newButtonsBindDeleteApis() throws Exception {
        menuStore.addAll(pagesOnly());
        apiStore.addAll(deleteApis());

        invokeButtonSeeds();

        // 6 条新按钮各绑定 1 个 API，且 apiId 非空
        assertEquals(6, menuApiStore.size(), "6 条新按钮各绑定 1 个 API");
        assertTrue(menuApiStore.stream().allMatch(l -> l.getApiId() != null), "所有绑定 apiId 应解析成功");
        // 权限 → 期望 apiId（coach=3001 / highlight-moment=3002 / stadium=3003；delete 与 return 共用同一 DELETE 端点）
        Map<String, Long> expected = Map.of(
                "business:coach:delete", 3001L,
                "business:coach:return", 3001L,
                "business:highlight-moment:delete", 3002L,
                "business:highlight-moment:return", 3002L,
                "business:stadium:delete", 3003L,
                "business:stadium:return", 3003L);
        for (SysMenuApi link : menuApiStore) {
            SysMenu owner = menuStore.stream()
                    .filter(m -> m.getId().equals(link.getMenuId()))
                    .findFirst().orElseThrow();
            assertEquals(expected.get(owner.getPermission()), link.getApiId(),
                    "绑定 apiId 应命中 " + owner.getPermission());
        }
        Set<Long> boundApiIds = menuApiStore.stream().map(SysMenuApi::getApiId).collect(Collectors.toSet());
        assertEquals(Set.of(3001L, 3002L, 3003L), boundApiIds, "应仅绑定三个 DELETE ApiDef");
    }

    @Test
    @DisplayName("③ tenant_admin 默认集合：含 3 个 return、排除 3 个 delete（页面沿父展开）")
    void tenantAdminDefaultSetIncludesReturnExcludesDelete() throws Exception {
        Set<Long> result = collectTenantAdminDefaultMenuIds(withButtons());

        for (long id : RETURN_IDS) {
            assertTrue(result.contains(id), "应纳入 return 按钮菜单 id=" + id);
        }
        for (long id : DELETE_IDS) {
            assertFalse(result.contains(id), "应排除 delete 按钮菜单 id=" + id);
        }
        assertTrue(result.containsAll(Set.of(1L, 2L, 3L, 4L)), "应含 business 目录 + 三页面（沿 parentId 展开）");
    }

    @Test
    @DisplayName("④ 超管全量绑定：ensureAdminRoleBindsAllMenusIfNeeded 纳入全部菜单（含 6 条新按钮）")
    void adminBindsAllMenusIncludingNewButtons() throws Exception {
        menuStore.addAll(pagesOnly());
        apiStore.addAll(deleteApis());
        invokeButtonSeeds();

        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("admin"))
                .thenReturn(Optional.of(role(ADMIN_ROLE_ID, "admin")));

        invokeNoArg("ensureAdminRoleBindsAllMenusIfNeeded");

        Set<Long> bound = boundMenuIds(ADMIN_ROLE_ID);
        for (SysMenu m : menuStore) {
            assertTrue(bound.contains(m.getId()), "超管应绑定全部菜单 id=" + m.getId());
        }
        assertEquals(6, menuStore.stream()
                .filter(m -> m.getPermission() != null && (NEW_RETURN_PERMS.contains(m.getPermission())
                        || NEW_DELETE_PERMS.contains(m.getPermission())))
                .count(), "应检出 6 条新按钮");
    }

    // ---- 反射调用 ----

    private void invokeButtonSeeds() throws Exception {
        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");
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

    private static final Set<Long> RETURN_IDS = Set.of(10L, 11L, 12L);
    private static final Set<Long> DELETE_IDS = Set.of(20L, 21L, 22L);

    private Set<Long> boundMenuIds(long roleId) {
        return roleMenus.getOrDefault(roleId, List.of()).stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** 仅 business 目录 + 本批三页面（无按钮）：供种子插入/绑定/超管绑定用例。 */
    private static List<SysMenu> pagesOnly() {
        List<SysMenu> list = new ArrayList<>();
        list.add(menu(1L, 0L, "/business", null, 1));
        list.add(menu(2L, 1L, "/admin/coaches", null, 2));
        list.add(menu(3L, 1L, "/admin/highlight-moments", null, 2));
        list.add(menu(4L, 1L, "/admin/stadiums", null, 2));
        return list;
    }

    /** 仅 business 目录 + 本批三页面 + 3 return + 3 delete（parentId 指向页面，供 collect 沿父展开）。 */
    private static List<SysMenu> withButtons() {
        List<SysMenu> list = new ArrayList<>(pagesOnly());
        list.add(menu(10L, 2L, null, "business:coach:return", 3));
        list.add(menu(11L, 3L, null, "business:highlight-moment:return", 3));
        list.add(menu(12L, 4L, null, "business:stadium:return", 3));
        list.add(menu(20L, 2L, null, "business:coach:delete", 3));
        list.add(menu(21L, 3L, null, "business:highlight-moment:delete", 3));
        list.add(menu(22L, 4L, null, "business:stadium:delete", 3));
        return list;
    }

    private static List<SysApi> deleteApis() {
        return List.of(
                api(3001L, "/coach/delete/:id", "DELETE"),
                api(3002L, "/highlight-moment/delete/{id}", "DELETE"),
                api(3003L, "/stadium/delete/:id", "DELETE"));
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

    private static SysUser user(long id) {
        SysUser u = new SysUser();
        u.setId(id);
        return u;
    }
}
