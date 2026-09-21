package com.bsball.core;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求级关联标识过滤器。
 *
 * <p>作为最早的请求入口，为每个 HTTP 请求确定一个关联标识并写入 MDC（键 {@code requestId}），
 * 使同一请求在各层产生的日志（含异常日志）可通过 logback 模式中的 {@code %X{requestId}}
 * 串联检索；标识同时通过响应头 {@code X-Request-Id} 回传，便于按调用方反馈的值定位日志。</p>
 *
 * <p>调用方已携带合法 {@code X-Request-Id} 时直接复用（重试/重放仍归入同一标识），
 * 否则生成新的 32 位十六进制标识。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    /** MDC 键名，与 logback-spring.xml 中的 %X{requestId} 对应 */
    public static final String REQUEST_ID_KEY = "requestId";
    /** 请求与响应共用的头名称 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    /** 可复用标识的合法格式：1-64 位字母、数字、点、下划线或连字符（防止换行等造成日志注入） */
    private static final Pattern REUSABLE_ID_PATTERN = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = this.resolveRequestId(request);
        MDC.put(REQUEST_ID_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(REQUEST_ID_KEY);
        }
    }

    /** 复用调用方传入的合法标识，缺失或不合法时生成新的标识 */
    private String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader(REQUEST_ID_HEADER);
        if (header != null && REUSABLE_ID_PATTERN.matcher(header).matches()) {
            return header;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
