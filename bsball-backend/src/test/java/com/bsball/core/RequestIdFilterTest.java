/*
 * RequestIdFilter 的纯单元测试：
 * ServletRequest/ServletResponse 用 Mockito mock，不启动 Spring 容器；
 * 覆盖标识生成/复用、响应头回传与 MDC 清理（含异常路径）。
 */
package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestIdFilter：请求级关联标识写入 MDC 与响应头")
class RequestIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    @AfterEach
    void tearDown() {
        // MDC 基于 ThreadLocal，测试后必须清理，避免污染其他用例
        MDC.clear();
    }

    @Test
    @DisplayName("未携带 X-Request-Id：生成 32 位十六进制标识，写入 MDC 与响应头，请求结束后清理")
    void generateWhenHeaderMissing() throws Exception {
        when(request.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).thenReturn(null);
        AtomicReference<String> idInChain = new AtomicReference<>();

        requestIdFilter.doFilter(request, response, captureInto(idInChain));

        String requestId = idInChain.get();
        assertNotNull(requestId);
        assertTrue(requestId.matches("[0-9a-f]{32}"), "应为 32 位十六进制标识，实际: " + requestId);
        verify(response).setHeader(RequestIdFilter.REQUEST_ID_HEADER, requestId);
        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_KEY), "请求结束后必须清理 MDC");
    }

    @Test
    @DisplayName("携带合法 X-Request-Id：原样复用，重放/重试归入同一标识")
    void reuseValidHeader() throws Exception {
        when(request.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).thenReturn("replay-verify-001");
        AtomicReference<String> idInChain = new AtomicReference<>();

        requestIdFilter.doFilter(request, response, captureInto(idInChain));

        assertEquals("replay-verify-001", idInChain.get());
        verify(response).setHeader(RequestIdFilter.REQUEST_ID_HEADER, "replay-verify-001");
    }

    @Test
    @DisplayName("携带非法 X-Request-Id（含换行或超长）：不复用，重新生成，防止日志注入")
    void regenerateWhenHeaderInvalid() throws Exception {
        when(request.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).thenReturn("bad\nid " + "x".repeat(80));
        AtomicReference<String> idInChain = new AtomicReference<>();

        requestIdFilter.doFilter(request, response, captureInto(idInChain));

        String requestId = idInChain.get();
        assertNotNull(requestId);
        assertTrue(requestId.matches("[0-9a-f]{32}"), "非法标识不得复用，实际: " + requestId);
    }

    @Test
    @DisplayName("过滤器链抛出异常：MDC 仍被清理，不污染线程后续日志")
    void clearMdcWhenChainThrows() {
        when(request.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).thenReturn(null);
        FilterChain chain = (req, res) -> {
            throw new ServletException("boom");
        };

        assertThrows(ServletException.class, () -> requestIdFilter.doFilter(request, response, chain));

        assertNull(MDC.get(RequestIdFilter.REQUEST_ID_KEY));
    }

    /** 在过滤器链内部捕获 MDC 中的标识，模拟被调用方读取 */
    private FilterChain captureInto(AtomicReference<String> holder) {
        return (req, res) -> holder.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY));
    }
}
