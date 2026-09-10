/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.Result
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.IpAccessFilter
 *  com.bsball.core.ResolvedTenantHolder
 *  com.bsball.service.IpAccessPolicyService
 *  com.bsball.utils.HttpClientIpUtil
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  jakarta.servlet.Filter
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.core.annotation.Order
 *  org.springframework.http.HttpStatus
 *  org.springframework.stereotype.Component
 */
package com.bsball.core;

import com.bsball.common.Result;
import com.bsball.config.TenantProperties;
import com.bsball.core.ResolvedTenantHolder;
import com.bsball.service.IpAccessPolicyService;
import com.bsball.utils.HttpClientIpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
@Order(value=-2147483631)
public class IpAccessFilter
implements Filter {
    private final IpAccessPolicyService ipAccessPolicyService;
    private final TenantProperties tenantProperties;
    private final ObjectMapper objectMapper;
    @Value(value="${app.security.ip-access.enabled:true}")
    private boolean enabled;
    @Value(value="${app.security.ip-access.trust-x-forwarded-for:true}")
    private boolean trustXForwardedFor;
    @Value(value="${server.servlet.context-path:}")
    private String contextPath;

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!this.enabled) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse res = (HttpServletResponse)response;
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String pathNorm = this.stripContextPath(req.getRequestURI() != null ? req.getRequestURI() : "");
        String pathForCheck = IpAccessFilter.normalizePath((String)pathNorm);
        String ip = HttpClientIpUtil.getClientIp((HttpServletRequest)req, (boolean)this.trustXForwardedFor);
        Long tenantId = ResolvedTenantHolder.get();
        if (tenantId == null) {
            tenantId = this.tenantProperties.getDefaultId();
        }
        if (!this.ipAccessPolicyService.isIpBlocked(tenantId, pathForCheck, ip)) {
            chain.doFilter(request, response);
            return;
        }
        IpAccessFilter.applyCorsHeaders((HttpServletRequest)req, (HttpServletResponse)res);
        res.setStatus(HttpStatus.FORBIDDEN.value());
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            res.getWriter().write(this.objectMapper.writeValueAsString((Object)Result.fail((int)403, (String)"\u5f53\u524d IP \u4e0d\u5728\u5141\u8bb8\u8303\u56f4\u5185")));
        }
        catch (Exception e) {
            res.getWriter().write("{\"code\":403,\"msg\":\"\u5f53\u524d IP \u4e0d\u5728\u5141\u8bb8\u8303\u56f4\u5185\"}");
        }
    }

    private String stripContextPath(String path) {
        Object prefix;
        if (this.contextPath == null || this.contextPath.isEmpty() || "/".equals(this.contextPath)) {
            return path;
        }
        Object object = prefix = this.contextPath.endsWith("/") ? this.contextPath : this.contextPath + "/";
        if (path.startsWith((String)prefix)) {
            String rest = path.substring(((String)prefix).length());
            return rest.isEmpty() ? "/" : (rest.startsWith("/") ? rest : "/" + rest);
        }
        if (path.equals(this.contextPath)) {
            return "/";
        }
        return path;
    }

    private static String normalizePath(String path) {
        Object p;
        if (path == null || path.isEmpty()) {
            return "/";
        }
        Object object = p = path.startsWith("/") ? path : "/" + path;
        if (((String)p).startsWith("/api/")) {
            return ((String)p).substring(4);
        }
        if (((String)p).startsWith("/server/")) {
            return ((String)p).substring(8);
        }
        return p;
    }

    private static void applyCorsHeaders(HttpServletRequest req, HttpServletResponse res) {
        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            res.setHeader("Access-Control-Allow-Origin", origin);
            res.setHeader("Access-Control-Allow-Credentials", "true");
            res.addHeader("Vary", "Origin");
        }
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, X-Tenant-Id, X-Tenant-Code");
    }

    @Generated
    public IpAccessFilter(IpAccessPolicyService ipAccessPolicyService, TenantProperties tenantProperties, ObjectMapper objectMapper) {
        this.ipAccessPolicyService = ipAccessPolicyService;
        this.tenantProperties = tenantProperties;
        this.objectMapper = objectMapper;
    }
}

