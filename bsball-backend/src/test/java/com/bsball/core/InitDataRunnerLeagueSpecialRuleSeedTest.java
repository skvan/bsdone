/*
 * 联赛特殊规则（Issue #275，设计稿 §3.2）种子单测：按钮权限点 business:league:specialRule 与两端点注册。
 *
 * 覆盖：
 *  - ① API 注册幂等：ensureLeagueSpecialRuleApisIfNeeded 恰插入 GET/PUT /league/:id/special-rules 两条
 *    （分组「联盟管理」），二次调用零新增；
 *  - ② 按钮种子幂等 + menu_api 绑定：business:league:specialRule 落库一次，且恰绑定上述两 API
 *    （apiBinding 模板与 ApiDef 注册逐字一致，直查命中）；
 *  - ③ 租管默认集合（collectTenantAdminDefaultMenuIds）：含该按钮（未被排除清单拦截），delete / 门户动作按钮仍排除；
 *  - ④ 升级补绑（ensureTenantAdminRoleRebindsDefaultMenusIfNeeded）：存量角色缺该按钮 → 补齐；二次调用零新增；
 *  - ⑤ 租管 API 白名单谓词（isApiAllowedForTenantAdmin）：GET/PUT 两端点均命中（存量角色补绑 API 依据）。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测 private 方法经反射调用（对齐既有 InitDataRunner*Test）。
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
@DisplayName("InitDataRunner：联赛特殊规则按钮与接口种子（Issue #275）")
class InitDataRunnerLeagueSpecialRuleSeedTest {

    private static final long OP_ID = 1L;
    private static final long TENANT_ADMIN_ROLE_ID = 500L;
    private static final String PERM = "business:league:specialRule";
    private static final String API_PATH = "/league/:id/special-rules";
    private static final long SPECIAL_RULE_BUTTON_ID = 10L;
    private static final long DELETE_BUTTON_ID = 20L;
    private static final long APPLY_BUTTON_ID = 40L;

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
        lenient().when(sysApiRepository.save(any(SysApi.class))).thenAnswer(inv -> {
            SysApi a = inv.getArgument(0);
            if (a.getId() == null) {
                a.setId(nextId++);
            }
            apiStore.add(a);
            return a;
        });
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("admin"))
                .thenReturn(Optional.empty());
        lenient().when(sysRoleMenuRepository.findByRoleId(anyLong()))
                .thenAnswer(inv -> new ArrayList<>(roleMenus.getOrDefault((Long) inv.getArgument(0), List.of())));
        lenient().when(sysRoleMenuRepository.save(any(SysRoleMenu.class))).thenAnswer(inv -> {
            SysRoleMenu rm = inv.getArgument(0);
            roleMenus.computeIfAbsent(rm.getRoleId(), k -> new ArrayList<>()).add(rm);
            return rm;
        });
    }

    @Test
    @DisplayName("① API 注册幂等：恰插入 GET/PUT 两端点（分组「联盟管理」），二次调用零新增")
    void apiSeedIdempotent() throws Exception {
        invokeNoArg("ensureLeagueSpecialRuleApisIfNeeded");
        assertEquals(2, apiStore.size(), "首次调用应恰插入 2 条（GET + PUT）");

        invokeNoArg("ensureLeagueSpecialRuleApisIfNeeded");
        assertEquals(2, apiStore.size(), "二次调用不得重复插入");

        SysApi get = apiByMethod("GET");
        SysApi put = apiByMethod("PUT");
        assertEquals(API_PATH, get.getPath());
        assertEquals(API_PATH, put.getPath());
        assertEquals("查询联赛特殊规则", get.getDescription());
        assertEquals("更新联赛特殊规则", put.getDescription());
        assertEquals("联盟管理", get.getGroupName());
        assertEquals("联盟管理", put.getGroupName());
        assertEquals(Long.valueOf(OP_ID), get.getCreatedBy());
        assertEquals(Long.valueOf(OP_ID), put.getUpdatedBy());
    }

    @Test
    @DisplayName("② 按钮种子幂等：business:league:specialRule 落库一次且恰绑定两端点")
    void buttonSeedBindsRegisteredApis() throws Exception {
        menuStore.addAll(pagesOnly());
        invokeNoArg("ensureLeagueSpecialRuleApisIfNeeded");
        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");
        int sizeAfterFirst = menuStore.size();
        int savesFirst = menuSaveCount;
        assertTrue(savesFirst > 0, "首次调用应插入按钮种子（联赛页既有条目 + 本任务新增）");

        invokeNoArg("ensureMenuDirectoryTypesAndDefaultButtons");

        assertEquals(sizeAfterFirst, menuStore.size(), "第二次调用不得新增 sys_menu（幂等判存）");
        assertEquals(savesFirst, menuSaveCount, "第二次调用不得再触发 sysMenu.save");

        List<SysMenu> buttons = menuStore.stream().filter(m -> PERM.equals(m.getPermission())).toList();
        assertEquals(1, buttons.size(), "specialRule 按钮应恰落库一次");
        SysMenu btn = buttons.get(0);
        assertEquals(Integer.valueOf(3), btn.getMenuType(), "按钮 menu_type=3");
        assertEquals("特殊规则", btn.getTitle(), "按钮标题对齐设计「特殊规则」");
        assertEquals(Long.valueOf(2L), btn.getParentId(), "按钮应挂在联赛页（/admin/leagues）下");
        assertEquals(Integer.valueOf(8), btn.getSort(), "排序紧随既有联赛按钮（1-7）");

        Map<Long, String> keyById = apiStore.stream()
                .collect(Collectors.toMap(SysApi::getId, a -> a.getMethod() + "|" + a.getPath()));
        Set<String> bound = menuApiStore.stream()
                .filter(x -> btn.getId().equals(x.getMenuId()))
                .map(x -> keyById.get(x.getApiId()))
                .collect(Collectors.toSet());
        assertEquals(Set.of("GET|" + API_PATH, "PUT|" + API_PATH), bound,
                "按钮应恰绑定 GET/PUT 两端点（apiBinding 直查命中）");
    }

    @Test
    @DisplayName("③ 租管默认集合：含 specialRule 按钮（未入排除清单），delete / 门户动作按钮仍排除")
    void tenantAdminDefaultSetIncludesSpecialRuleButton() throws Exception {
        Set<Long> defaults = collectTenantAdminDefaultMenuIds(tenantAdminFixture());

        assertTrue(defaults.contains(SPECIAL_RULE_BUTTON_ID), "默认集合应纳入 specialRule 按钮");
        assertTrue(defaults.containsAll(Set.of(1L, 2L)), "应含 business 目录 + 联赛页（沿 parentId 展开）");
        assertFalse(defaults.contains(DELETE_BUTTON_ID), "delete 类按钮仍排除");
        assertFalse(defaults.contains(APPLY_BUTTON_ID), "门户动作按钮（apply）仍排除");

        assertFalse(isTenantAdminExcludedPermission(PERM), "specialRule 不应命中租管排除清单");
    }

    @Test
    @DisplayName("④ 升级补绑：存量角色缺 specialRule 按钮 → 恰补齐一次（二次调用幂等零新增）")
    void rebindCoversSpecialRuleButton() throws Exception {
        menuStore.addAll(tenantAdminFixture());
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin"))
                .thenReturn(Optional.of(role(TENANT_ADMIN_ROLE_ID, "tenant_admin")));
        roleMenus.put(TENANT_ADMIN_ROLE_ID, new ArrayList<>(List.of(
                roleMenu(TENANT_ADMIN_ROLE_ID, 1L), roleMenu(TENANT_ADMIN_ROLE_ID, 2L))));

        invokeNoArg("ensureTenantAdminRoleRebindsDefaultMenusIfNeeded");

        assertTrue(boundMenuIds(TENANT_ADMIN_ROLE_ID).contains(SPECIAL_RULE_BUTTON_ID),
                "存量租管角色应补绑 specialRule 按钮");
        int sizeAfterFirst = roleMenus.get(TENANT_ADMIN_ROLE_ID).size();

        invokeNoArg("ensureTenantAdminRoleRebindsDefaultMenusIfNeeded");

        assertEquals(sizeAfterFirst, roleMenus.get(TENANT_ADMIN_ROLE_ID).size(),
                "二次调用不得新增任何 role_menu 行（幂等判存）");
    }

    @Test
    @DisplayName("⑤ 租管 API 白名单：GET/PUT special-rules 两端点均命中（存量角色补绑 API 依据）")
    void tenantAdminApiWhitelistCoversSpecialRuleEndpoints() throws Exception {
        assertTrue(isApiAllowedForTenantAdmin(api(3001L, API_PATH, "GET")), "GET 端点应入租管白名单");
        assertTrue(isApiAllowedForTenantAdmin(api(3002L, API_PATH, "PUT")), "PUT 端点应入租管白名单");
    }

    // ---- 反射调用 ----

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

    private static boolean isTenantAdminExcludedPermission(String permission) throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("isTenantAdminExcludedPermission", String.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(null, permission);
    }

    private static boolean isApiAllowedForTenantAdmin(SysApi api) throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("isApiAllowedForTenantAdmin", SysApi.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(null, api);
    }

    // ---- fixtures / helpers ----

    private SysApi apiByMethod(String method) {
        return apiStore.stream()
                .filter(a -> method.equalsIgnoreCase(a.getMethod()))
                .findFirst().orElseThrow();
    }

    private Set<Long> boundMenuIds(long roleId) {
        return roleMenus.getOrDefault(roleId, List.of()).stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /** 仅 business 目录 + 联赛页（其余页面缺失 → 其按钮种子被 parent==null 跳过）。 */
    private static List<SysMenu> pagesOnly() {
        List<SysMenu> list = new ArrayList<>();
        list.add(menu(1L, 0L, "/business", null, 1));
        list.add(menu(2L, 1L, "/admin/leagues", null, 2));
        return list;
    }

    /** business 目录 + 联赛页 + specialRule 按钮 + delete 按钮（排除）+ apply 按钮（排除）。 */
    private static List<SysMenu> tenantAdminFixture() {
        List<SysMenu> list = new ArrayList<>(pagesOnly());
        list.add(menu(SPECIAL_RULE_BUTTON_ID, 2L, null, PERM, 3));
        list.add(menu(DELETE_BUTTON_ID, 2L, null, "business:league:delete", 3));
        list.add(menu(APPLY_BUTTON_ID, 2L, null, "business:league:apply", 3));
        return list;
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
