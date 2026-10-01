/*
 * 账号权限重构（批次 b5，Task 2）：租户不可用拒绝响应的唯一码头 X-Tenant-Unavailable 单元测试。
 *
 * 说明（filter 级测试的必要性）：
 *  该头唯一写入点为 ApiPermissionFilter.writeTenantReject（private），其调用方 rejectIfUnusableTenantCodeHeader
 *  / rejectIfUnusableRequestTenant 亦为 private。故只能经 doFilter 的租户门禁路径触达：
 *    - 码通路（X-Tenant-Code 不可用）→ 404「租户不存在」（本次唯一 404 来源）；
 *    - ID 头通路（X-Tenant-Id 不可用且非超管）→ 403「租户已停止运营」。
 *  两条通路共用 writeTenantReject，故两用例分别断言该头存在，覆盖 404 与 403 两类终态。
 *  Decision 级（TenantAccessGuard.decideCodeHeader / decideStrictTenant）只产出码与文案、不写响应头，
 *  无法断言头，故不采用；此处选择 filter 级 mock 装配（不启动 Spring 容器）。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bsball.config.TenantProperties;
import com.bsball.service.AccountScopeService;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.JwtService;
import com.bsball.service.TenantAccessGuard;
import com.bsball.service.TenantResolutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiPermissionFilter：租户不可用拒绝统一置 X-Tenant-Unavailable 头")
class ApiPermissionFilterTenantRejectHeaderTest {

    @Mock
    private ApiPermissionService apiPermissionService;

    @Mock
    private JwtService jwtService;

    @Mock
    private TenantProperties tenantProperties;

    @Mock
    private TenantResolutionService tenantResolutionService;

    @Mock
    private AccountScopeService accountScopeService;

    @Mock
    private TenantAccessGuard tenantAccessGuard;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private ApiPermissionFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiPermissionFilter(apiPermissionService, new ObjectMapper(), jwtService, tenantProperties,
                tenantResolutionService, accountScopeService, tenantAccessGuard);
        // @Value 注入字段在手工构造时为空，显式开启门禁；contextPath 留空即根路径。
        ReflectionTestUtils.setField(filter, "enabled", true);
        // 匿名请求（无 userId / 无 rejectMessage）→ rejectAsUnauthorized=false，进入租户门禁。
        when(jwtService.authenticateBearerToken(any())).thenReturn(new JwtService.TokenAuth(null, null, null, null));
    }

    @Test
    @DisplayName("码通路：X-Tenant-Code 不可用 → 404 + X-Tenant-Unavailable=1 + 终态不进入回退链")
    void codeHeaderUnusable_404_setsHeader() throws Exception {
        // getHeader 会被多次以不同头名调用（Authorization/Origin/X-Tenant-Code…），故用 lenient 避免严格校验误报。
        lenient().when(request.getHeader("X-Tenant-Code")).thenReturn("ghost");
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        lenient().when(request.getHeader("X-Tenant-Id")).thenReturn(null);
        when(tenantResolutionService.findTenantByCodeCached("ghost")).thenReturn(Optional.empty());
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("租户不可用应终态拒绝，不得进入过滤器链");
        });

        verify(response).setStatus(404);
        verify(response).setHeader("X-Tenant-Unavailable", "1");
        assertTrue(body.toString().contains("租户不存在"), "响应体应含 404 文案，实际: " + body);
    }

    @Test
    @DisplayName("ID 头通路：非超管且 X-Tenant-Id 不可用 → 403 + X-Tenant-Unavailable=1")
    void idHeaderUnusable_403_setsHeader() throws Exception {
        lenient().when(request.getHeader("X-Tenant-Code")).thenReturn(null);
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        lenient().when(request.getHeader("X-Tenant-Id")).thenReturn("77");
        when(tenantAccessGuard.isActive(77L)).thenReturn(false);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("租户不可用应终态拒绝，不得进入过滤器链");
        });

        verify(response).setStatus(403);
        verify(response).setHeader("X-Tenant-Unavailable", "1");
        assertTrue(body.toString().contains("租户已停止运营"), "响应体应含 403 文案，实际: " + body);
    }
}
