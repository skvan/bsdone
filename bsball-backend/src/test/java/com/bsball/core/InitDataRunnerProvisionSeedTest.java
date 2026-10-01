/*
 * 账号权限重构（批次 3b，冒烟修复）：InitDataRunner 批次端点注册 ensureBatchAndTenantRoleConfigApisIfNeeded 幂等性单测。
 *
 * 覆盖（升级/首装双路径共用同一方法，故按方法行为验证）：
 *  - ① 二次调用幂等：第二次零 save；
 *  - ② 首次插入恰 4 条且 (method, path) 集合 = 预期；
 *  - ③ 预置其中 1 条时仅补其余 3 条；
 *  - ④ groupName / description（及 createdBy/updatedBy）设置正确。
 *
 * 风格：外部依赖一律 Mockito mock，不启动 Spring、不连库；被测 private 方法经反射调用（对齐 InitDataRunnerPortalRoleBindingTest）。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.model.entity.SysApi;
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
@DisplayName("InitDataRunner：批次3b端点注册 ensureBatchAndTenantRoleConfigApisIfNeeded")
class InitDataRunnerProvisionSeedTest {

    private static final long ADMIN_ID = 1L;

    /** 期望收录的 (method, path) 集合：4 条，判存主键。 */
    private static final Set<String> EXPECTED_KEYS = Set.of(
            "POST /team/batch-create",
            "GET /sys/role/tenant-config",
            "PUT /sys/role/tenant-config",
            "DELETE /sys/role/tenant-config");

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
    private final List<SysApi> apiStore = new ArrayList<>();
    private int saveCount;

    @BeforeEach
    void setUp() {
        runner = new InitDataRunner(sysMenuRepository, sysApiRepository, sysDictTypeRepository, sysDictDataRepository,
                sysConfigRepository, sysRoleRepository, sysUserRepository, sysRoleMenuRepository, sysRoleApiRepository,
                sysUserRoleRepository, stadiumRepository, tenantProperties, initSeedProperties, sysMenuApiRepository);

        apiStore.clear();
        saveCount = 0;

        lenient().when(sysUserRepository.findByUsernameAndDeletedAtIsNull("admin"))
                .thenReturn(Optional.of(admin(ADMIN_ID)));
        lenient().when(sysApiRepository.findAll()).thenAnswer(inv -> new ArrayList<>(apiStore));
        lenient().when(sysApiRepository.save(any(SysApi.class))).thenAnswer(inv -> {
            SysApi a = inv.getArgument(0);
            apiStore.add(a);
            saveCount++;
            return a;
        });
    }

    @Test
    @DisplayName("① 二次调用幂等：首次插入 4 条，第二次零 save（计数不变）")
    void secondInvocationSavesNothing() throws Exception {
        invokeEnsure();
        assertEquals(4, saveCount, "首次调用应恰好插入 4 条");
        int sizeAfterFirst = apiStore.size();
        assertEquals(4, sizeAfterFirst);

        invokeEnsure();

        assertEquals(4, saveCount, "第二次调用不得再触发 save（幂等判存）");
        assertEquals(sizeAfterFirst, apiStore.size(), "第二次调用不得新增 sys_api");
    }

    @Test
    @DisplayName("② 首次插入恰 4 条且 (method, path) 集合 = 预期")
    void firstInsertExactlyFourWithExpectedKeys() throws Exception {
        invokeEnsure();

        assertEquals(4, apiStore.size(), "首次应恰插入 4 条");
        assertEquals(EXPECTED_KEYS, keys(), "(method, path) 集合应与预期一致");
    }

    @Test
    @DisplayName("③ 预置其中 1 条（POST /team/batch-create）时仅补其余 3 条")
    void preSeededEntryIsSkipped() throws Exception {
        apiStore.add(api("POST", "/team/batch-create"));

        invokeEnsure();

        assertEquals(3, saveCount, "已存在 1 条，应仅补其余 3 条");
        assertEquals(4, apiStore.size(), "补录后总数应为 4（不重复插入已存在项）");
        assertEquals(EXPECTED_KEYS, keys(), "最终 (method, path) 集合仍为预期 4 条");
    }

    @Test
    @DisplayName("④ groupName / description（及 createdBy/updatedBy）设置正确")
    void groupAndDescriptionSet() throws Exception {
        invokeEnsure();

        Map<String, SysApi> byKey = apiStore.stream()
                .collect(Collectors.toMap(a -> a.getMethod() + " " + a.getPath(), a -> a, (x, y) -> x));

        SysApi batch = byKey.get("POST /team/batch-create");
        assertTrue(batch != null, "应存在 POST /team/batch-create");
        assertEquals("批量创建球队", batch.getDescription());
        assertEquals("球队管理", batch.getGroupName());

        SysApi get = byKey.get("GET /sys/role/tenant-config");
        assertTrue(get != null, "应存在 GET /sys/role/tenant-config");
        assertEquals("查询租户级目录覆盖", get.getDescription());
        assertEquals("权限管理", get.getGroupName());

        SysApi put = byKey.get("PUT /sys/role/tenant-config");
        assertTrue(put != null, "应存在 PUT /sys/role/tenant-config");
        assertEquals("保存租户级目录覆盖", put.getDescription());
        assertEquals("权限管理", put.getGroupName());

        SysApi del = byKey.get("DELETE /sys/role/tenant-config");
        assertTrue(del != null, "应存在 DELETE /sys/role/tenant-config");
        assertEquals("清除租户级目录覆盖", del.getDescription());
        assertEquals("权限管理", del.getGroupName());

        for (SysApi a : apiStore) {
            assertEquals(ADMIN_ID, a.getCreatedBy().longValue(), "createdBy 应为 opId");
            assertEquals(ADMIN_ID, a.getUpdatedBy().longValue(), "updatedBy 应为 opId");
        }
    }

    // ---- helpers ----

    private void invokeEnsure() throws Exception {
        Method m = InitDataRunner.class.getDeclaredMethod("ensureBatchAndTenantRoleConfigApisIfNeeded");
        m.setAccessible(true);
        m.invoke(runner);
    }

    private Set<String> keys() {
        return apiStore.stream().map(a -> a.getMethod() + " " + a.getPath()).collect(Collectors.toSet());
    }

    private static SysUser admin(long id) {
        SysUser u = new SysUser();
        u.setId(id);
        return u;
    }

    private static SysApi api(String method, String path) {
        SysApi a = new SysApi();
        a.setMethod(method);
        a.setPath(path);
        return a;
    }
}
