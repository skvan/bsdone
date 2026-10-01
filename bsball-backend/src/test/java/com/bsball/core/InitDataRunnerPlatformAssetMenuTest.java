/*
 * 账号权限重构（批次 4b，Task 4b-4 / #154）：平台资产页菜单种子 ensurePlatformAssetMenuIfNeeded 单测。
 *
 * 覆盖：
 *  - ① 首次插入恰一条：path=/admin/platform-asset；挂载于「系统管理」目录（parentId=systemRoot.id）；
 *        menuType=2（叶菜单）；permission 留空（对齐系统区既有叶菜单）；component/routeName/icon/title 正确；
 *  - ② 幂等：二次调用零新增（menuStore.size 与 save 计数均不变）；
 *  - ③ 超管绑定：admin 角色存在时补绑该菜单；二次调用不重复绑定（bindMenuToAdminIfNeeded 幂等）；
 *  - ④ 命中已存在菜单：仅确保超管绑定，零插入；
 *  - ⑤ 「系统管理」目录缺失 → 直接返回，零插入（防御：不产生悬挂菜单）；
 *  - ⑥ tenant_admin 默认集合（collectTenantAdminDefaultMenuIds）：不含平台资产菜单 id（排除结论）；
 *  - ⑦ 双分支保障（行为/反射，替代源码计数）：ensure 方法契约存在（私有无参 void）+ 首装/升级语义等价行为。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测 private 方法经反射调用（对齐既有 InitDataRunner*Test）。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysMenu;
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
@DisplayName("InitDataRunner：平台资产页菜单种子（批次 4b Task 4b-4）")
class InitDataRunnerPlatformAssetMenuTest {

    private static final long OP_ID = 1L;
    private static final long ADMIN_ROLE_ID = 900L;
    private static final String ASSET_PATH = "/admin/platform-asset";

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
    private final Map<Long, List<SysRoleMenu>> roleMenus = new HashMap<>();
    private long nextId = 1000L;
    private int menuSaveCount;

    @BeforeEach
    void setUp() {
        runner = new InitDataRunner(sysMenuRepository, sysApiRepository, sysDictTypeRepository, sysDictDataRepository,
                sysConfigRepository, sysRoleRepository, sysUserRepository, sysRoleMenuRepository, sysRoleApiRepository,
                sysUserRoleRepository, stadiumRepository, tenantProperties, initSeedProperties, sysMenuApiRepository);

        menuStore.clear();
        roleMenus.clear();
        nextId = 1000L;
        menuSaveCount = 0;

        lenient().when(sysUserRepository.findByUsernameAndDeletedAtIsNull("admin"))
                .thenReturn(Optional.of(user(OP_ID)));
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode(anyString())).thenReturn(Optional.empty());
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
        lenient().when(sysRoleMenuRepository.findByRoleId(anyLong()))
                .thenAnswer(inv -> new ArrayList<>(roleMenus.getOrDefault((Long) inv.getArgument(0), List.of())));
        lenient().when(sysRoleMenuRepository.save(any(SysRoleMenu.class))).thenAnswer(inv -> {
            SysRoleMenu rm = inv.getArgument(0);
            roleMenus.computeIfAbsent(rm.getRoleId(), k -> new ArrayList<>()).add(rm);
            return rm;
        });
    }

    @Test
    @DisplayName("① 首次插入恰一条：挂载 /system、menuType=2、permission 空、字段对齐")
    void firstInsertExactlyOneLeafUnderSystem() throws Exception {
        SysMenu systemRoot = menu(2L, 0L, "/system", "系统管理", 1, null);
        SysMenu users = menu(11L, 2L, "/admin/users", "用户管理", 2, null);
        menuStore.add(systemRoot);
        menuStore.add(users);

        invokeEnsure();

        List<SysMenu> created = menuStore.stream().filter(m -> ASSET_PATH.equals(m.getPath())).collect(Collectors.toList());
        assertEquals(1, created.size(), "应恰插入一条平台资产菜单");
        SysMenu asset = created.get(0);
        assertEquals(systemRoot.getId(), asset.getParentId(), "parentId 应指向「系统管理」目录");
        assertEquals(Integer.valueOf(2), asset.getMenuType(), "叶菜单 menuType=2");
        assertNull(asset.getPermission(), "对齐系统区既有叶菜单：permission 留空");
        assertEquals("平台资产", asset.getTitle());
        assertEquals("平台资产", asset.getName());
        assertEquals("AdminPlatformAsset", asset.getRouteName());
        assertEquals("views/admin/PlatformAsset.js", asset.getComponent());
        assertEquals("Coin", asset.getIcon());
        assertEquals(1, menuSaveCount, "首次调用应恰好 1 次 save");
        // sort 为系统目录既有子菜单最大 sort + 1（users 的 0 → 新菜单 sort=1）
        assertEquals(Integer.valueOf(1), asset.getSort(), "sort 应接在目录既有子菜单之后");
        assertEquals(OP_ID, asset.getCreatedBy().longValue());
    }

    @Test
    @DisplayName("② 幂等：二次调用零新增（size 与 save 计数不变）")
    void secondInvocationSavesNothing() throws Exception {
        menuStore.add(menu(2L, 0L, "/system", "系统管理", 1, null));

        invokeEnsure();
        int sizeAfterFirst = menuStore.size();
        int savesFirst = menuSaveCount;

        invokeEnsure();

        assertEquals(sizeAfterFirst, menuStore.size(), "第二次调用不得新增 sys_menu");
        assertEquals(savesFirst, menuSaveCount, "第二次调用不得再触发 sysMenu.save");
    }

    @Test
    @DisplayName("③ 超管绑定：admin 角色存在时补绑该菜单，二次调用不重复绑定")
    void bindsAdminIdempotently() throws Exception {
        menuStore.add(menu(2L, 0L, "/system", "系统管理", 1, null));
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("admin"))
                .thenReturn(Optional.of(role(ADMIN_ROLE_ID, "admin")));

        invokeEnsure();

        List<SysMenu> asset = menuStore.stream().filter(m -> ASSET_PATH.equals(m.getPath())).toList();
        assertEquals(1, asset.size());
        long assetId = asset.get(0).getId();
        Set<Long> bound = boundMenuIds(ADMIN_ROLE_ID);
        assertTrue(bound.contains(assetId), "超管应绑定平台资产菜单");
        assertEquals(1, roleMenus.get(ADMIN_ROLE_ID).size(), "应恰绑定一次");

        invokeEnsure();

        assertEquals(1, roleMenus.get(ADMIN_ROLE_ID).size(), "二次调用不得重复绑定（bindMenuToAdminIfNeeded 幂等）");
    }

    @Test
    @DisplayName("④ 命中已存在菜单：仅确保超管绑定，零插入")
    void existingMenuIsEnsuredNotReinserted() throws Exception {
        SysMenu existed = menu(77L, 2L, ASSET_PATH, "平台资产", 2, null);
        menuStore.add(menu(2L, 0L, "/system", "系统管理", 1, null));
        menuStore.add(existed);
        lenient().when(sysRoleRepository.findByTenantIdIsNullAndCode("admin"))
                .thenReturn(Optional.of(role(ADMIN_ROLE_ID, "admin")));

        invokeEnsure();

        assertEquals(0, menuSaveCount, "已存在时不得再插入菜单");
        assertTrue(boundMenuIds(ADMIN_ROLE_ID).contains(77L), "应补绑已存在菜单到超管");
    }

    @Test
    @DisplayName("⑤ 系统管理目录缺失 → 零插入（防御，不产生悬挂菜单）")
    void missingSystemRootInsertsNothing() throws Exception {
        menuStore.add(menu(3L, 0L, "/business", "业务管理", 1, null));

        invokeEnsure();

        assertEquals(0, menuSaveCount, "目录缺失时不得插入菜单");
        assertTrue(menuStore.stream().noneMatch(m -> ASSET_PATH.equals(m.getPath())));
    }

    @Test
    @DisplayName("⑥ tenant_admin 默认集合：不含平台资产菜单 id（排除结论）")
    void tenantAdminDefaultSetExcludesPlatformAsset() throws Exception {
        SysMenu systemRoot = menu(2L, 0L, "/system", "系统管理", 1, null);
        SysMenu dashboard = menu(10L, 0L, "/admin/dashboard", "工作台", 2, null);
        SysMenu asset = menu(77L, 2L, ASSET_PATH, "平台资产", 2, null);
        Set<Long> result = collectTenantAdminDefaultMenuIds(List.of(systemRoot, dashboard, asset));

        assertFalse(result.contains(77L), "平台资产（/system 子树）不应纳入租户管理员默认集合");
        assertTrue(result.contains(10L), "对照：工作台应纳入默认集合");
    }

    @Test
    @DisplayName("⑦ 双分支保障（行为/反射，替代源码计数）：方法契约存在 + 首装/升级语义等价行为")
    void dualBranchGuaranteeViaContractAndBehavior() throws Exception {
        // (a) 反射契约：ensure 方法存在且为私有无参 void（守卫重命名/删除/可见性漂移）。
        //     理由：run() 首装/升级两分支的统一调用方，任一分支移除调用都会使该契约失效。
        Method ensure = InitDataRunner.class.getDeclaredMethod("ensurePlatformAssetMenuIfNeeded");
        assertEquals(void.class, ensure.getReturnType(), "ensure 应为 void");
        assertEquals(0, ensure.getParameterCount(), "ensure 应无参");
        assertTrue(java.lang.reflect.Modifier.isPrivate(ensure.getModifiers()), "ensure 应为 private");
        Method run = InitDataRunner.class.getMethod("run", String[].class);
        assertTrue(java.lang.reflect.Modifier.isPublic(run.getModifiers()), "run 应为 public 入口");

        // (b) 首装分支语义等价：空库 + 「系统管理」目录存在 → ensure 恰插入一条平台资产菜单。
        menuStore.add(menu(2L, 0L, "/system", "系统管理", 1, null));
        invokeEnsure();
        assertEquals(1L, menuStore.stream().filter(m -> ASSET_PATH.equals(m.getPath())).count(),
                "首装语义：ensure 应插入平台资产菜单");
        int savesAfterFirst = menuSaveCount;

        // (c) 升级分支语义等价：再次调用（菜单已在）→ 幂等零插入（与首装后升级到新版本等价）。
        invokeEnsure();
        assertEquals(savesAfterFirst, menuSaveCount, "升级语义：ensure 对既有菜单幂等零插入");
        assertEquals(1L, menuStore.stream().filter(m -> ASSET_PATH.equals(m.getPath())).count(),
                "升级语义：不得重复插入");
    }

    // ---- helpers ----

    private void invokeEnsure() throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("ensurePlatformAssetMenuIfNeeded");
        m.setAccessible(true);
        m.invoke(runner);
    }

    @SuppressWarnings("unchecked")
    private Set<Long> collectTenantAdminDefaultMenuIds(List<SysMenu> menus) throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("collectTenantAdminDefaultMenuIds", List.class);
        m.setAccessible(true);
        return (Set<Long>) m.invoke(null, menus);
    }

    private Set<Long> boundMenuIds(long roleId) {
        return roleMenus.getOrDefault(roleId, List.of()).stream().map(SysRoleMenu::getMenuId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static SysMenu menu(long id, long parentId, String path, String title, int menuType, String permission) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setPath(path);
        m.setName(title);
        m.setTitle(title);
        m.setMenuType(menuType);
        m.setPermission(permission);
        m.setSort(0);
        return m;
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
