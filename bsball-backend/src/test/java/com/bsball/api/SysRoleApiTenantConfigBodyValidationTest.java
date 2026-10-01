/*
 * 账号权限重构（批次 3b，Task 3.15b / I3）：PUT /sys/role/tenant-config 的 body 校验收紧测试。
 *
 * 覆盖：
 *  - menuIds 缺失 / 非数组 → 400「menuIds 必须为数组」（不再静默当空集）；
 *  - menuIds 元素无法解析为整数 → 400「menuIds 元素必须为数字」（不再静默丢弃）；
 *  - roleId 非整数数字（1.5）→ 400「roleId 必须为整数」（不可截断）；
 *  - 校验失败一律不触碰服务层；显式空数组仍照常下传（启用空覆盖），既有语义保持。
 *
 * 风格：Mockito mock SysRoleService；仅校验 controller 层解析与下传，不启动 Spring、不连库。
 */
package com.bsball.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.service.SysRoleService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PUT /sys/role/tenant-config body 校验收紧（批次3b T3.15b / I3）")
class SysRoleApiTenantConfigBodyValidationTest {

    private static final long UID = 9L;
    private static final long TENANT_T = 10L;
    private static final long ROLE_ID = 500L;

    @Mock
    private SysRoleService sysRoleService;

    private SysRoleApi api;

    @BeforeEach
    void setUp() {
        CurrentUserHolder.set(UID, TENANT_T);
        api = new SysRoleApi(sysRoleService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    @DisplayName("menuIds 缺失/非数组/元素非数字 与 roleId 非整数 → 400 且不触碰服务；显式空数组照常下传")
    void putTenantConfig_tightensBodyValidation() {
        // menuIds 缺失 → 400「menuIds 必须为数组」
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> api.saveTenantConfig(bodyWithoutMenuIds(ROLE_ID)));
        assertEquals(400, e1.getCode());
        assertEquals("menuIds 必须为数组", e1.getMessage());

        // menuIds 非数组（字符串）→ 400「menuIds 必须为数组」
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> api.saveTenantConfig(body(ROLE_ID, "100")));
        assertEquals(400, e2.getCode());
        assertEquals("menuIds 必须为数组", e2.getMessage());

        // menuIds 元素非数字 → 400「menuIds 元素必须为数字」（不再静默丢弃）
        BusinessException e3 = assertThrows(BusinessException.class,
                () -> api.saveTenantConfig(body(ROLE_ID, List.of(100L, "abc"))));
        assertEquals(400, e3.getCode());
        assertEquals("menuIds 元素必须为数字", e3.getMessage());

        // roleId 非整数（1.5）→ 400「roleId 必须为整数」（不可截断为 1）
        BusinessException e4 = assertThrows(BusinessException.class,
                () -> api.saveTenantConfig(body(1.5d, List.of())));
        assertEquals(400, e4.getCode());
        assertEquals("roleId 必须为整数", e4.getMessage());

        // 上述校验失败均发生在服务调用之前
        verifyNoInteractions(sysRoleService);

        // 显式空数组 → 照常下传（空覆盖语义保持）
        when(sysRoleService.saveTenantConfig(UID, TENANT_T, ROLE_ID, List.of()))
                .thenReturn(Map.of("enabled", true, "menuIds", List.of()));
        Result<Map<String, Object>> ok = api.saveTenantConfig(body(ROLE_ID, List.of()));
        assertEquals(200, ok.getCode());
        verify(sysRoleService).saveTenantConfig(UID, TENANT_T, ROLE_ID, List.of());
    }

    private static Map<String, Object> body(Object roleId, Object menuIds) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("roleId", roleId);
        m.put("tenantId", TENANT_T);
        m.put("menuIds", menuIds);
        return m;
    }

    private static Map<String, Object> bodyWithoutMenuIds(Object roleId) {
        HashMap<String, Object> m = new HashMap<>();
        m.put("roleId", roleId);
        m.put("tenantId", TENANT_T);
        return m;
    }
}
