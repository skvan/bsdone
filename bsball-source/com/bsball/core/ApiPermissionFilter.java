/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.Result
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.ApiPermissionFilter
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.core.GuestPublicApiHolder
 *  com.bsball.core.ResolvedTenantHolder
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.JwtService
 *  com.bsball.service.JwtService$TokenAuth
 *  com.bsball.service.TenantResolutionService
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
import com.bsball.core.CurrentUserHolder;
import com.bsball.core.GuestPublicApiHolder;
import com.bsball.core.ResolvedTenantHolder;
import com.bsball.model.entity.SysTenant;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.JwtService;
import com.bsball.service.TenantResolutionService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
@Order(value=-2147483628)
public class ApiPermissionFilter
implements Filter {
    private final ApiPermissionService apiPermissionService;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final TenantProperties tenantProperties;
    private final TenantResolutionService tenantResolutionService;
    @Value(value="${app.api.guest-check.enabled:true}")
    private boolean enabled;
    @Value(value="${app.api.guest-whitelist:/auth/login,/auth/captcha,/health,/files,/portal/feedback}")
    private String whitelistStr;
    @Value(value="${server.servlet.context-path:}")
    private String contextPath;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
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
        String auth = req.getHeader("Authorization");
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        JwtService.TokenAuth tokenAuth = this.jwtService.authenticateBearerToken(token);
        if (tokenAuth.rejectAsUnauthorized()) {
            ApiPermissionFilter.applyCorsHeaders((HttpServletRequest)req, (HttpServletResponse)res);
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.setContentType("application/json");
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try {
                res.getWriter().write(this.objectMapper.writeValueAsString((Object)Result.fail((int)401, (String)tokenAuth.rejectMessage(), (String)tokenAuth.rejectDetail())));
            }
            catch (Exception e) {
                res.getWriter().write("{\"code\":401,\"msg\":\"\u767b\u5f55\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\"}");
            }
            return;
        }
        String pathNormPre = this.stripContextPath(req.getRequestURI() != null ? req.getRequestURI() : "");
        String pathForEarly = ApiPermissionFilter.normalizePath((String)pathNormPre);
        if (!pathForEarly.startsWith("/portal/tenant/check") && this.rejectIfInactiveTenantCodeHeader(req, res)) {
            return;
        }
        Long userId = tokenAuth.userId();
        if (userId != null && tokenAuth.tenantId() == null) {
            ApiPermissionFilter.applyCorsHeaders((HttpServletRequest)req, (HttpServletResponse)res);
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.setContentType("application/json");
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            res.getWriter().write("{\"code\":401,\"msg\":\"\u767b\u5f55\u6001\u7f3a\u5c11\u79df\u6237\u4fe1\u606f\uff0c\u8bf7\u91cd\u65b0\u767b\u5f55\"}");
            return;
        }
        long tenantId = this.resolveEffectiveTenantId(req, tokenAuth, userId, pathForEarly, req.getMethod());
        CurrentUserHolder.set((Long)userId, (Long)tenantId);
        try {
            String path = req.getRequestURI();
            if (path == null) {
                path = "";
            }
            Long jwtTid = tokenAuth.tenantId();
            boolean crossTenantBrowsing = userId != null && jwtTid != null && tenantId != jwtTid;
            boolean readLike = ApiPermissionFilter.isReadLikeHttpMethod((String)req.getMethod());
            boolean guestAllowed = this.apiPermissionService.isGuestAllowed(path, req.getMethod());
            GuestPublicApiHolder.setGuestLikeRead((userId != null && (guestAllowed || crossTenantBrowsing && readLike) ? 1 : 0) != 0);
            String pathNorm = this.stripContextPath(path);
            String pathForCheck = ApiPermissionFilter.normalizePath((String)pathNorm);
            if (this.isWhitelisted(pathForCheck)) {
                chain.doFilter(request, response);
                return;
            }
            if (userId != null) {
                boolean allowed;
                if (ApiPermissionFilter.isAuthSelfPath((String)pathForCheck)) {
                    chain.doFilter(request, response);
                    return;
                }
                if (this.apiPermissionService.isSuperAdmin(userId)) {
                    chain.doFilter(request, response);
                    return;
                }
                if (jwtTid != null && tenantId != jwtTid && pathForCheck.startsWith("/sys/") && !this.apiPermissionService.isGuestAllowed(path, req.getMethod())) {
                    ApiPermissionFilter.applyCorsHeaders((HttpServletRequest)req, (HttpServletResponse)res);
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType("application/json");
                    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    res.getWriter().write("{\"code\":403,\"msg\":\"\u8bf7\u4f7f\u7528\u5f53\u524d\u8d26\u53f7\u6240\u5c5e\u79df\u6237\u7684\u540e\u53f0\u5165\u53e3\u8bbf\u95ee\"}");
                    return;
                }
                boolean bl = allowed = this.apiPermissionService.canUserAccessApi(userId, path, req.getMethod()) || guestAllowed;
                if (!allowed) {
                    ApiPermissionFilter.applyCorsHeaders((HttpServletRequest)req, (HttpServletResponse)res);
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType("application/json");
                    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    res.getWriter().write("{\"code\":403,\"msg\":\"\u6ca1\u6709\u6743\u9650\u8bbf\u95ee\u8be5\u63a5\u53e3\"}");
                    return;
                }
                chain.doFilter(request, response);
                return;
            }
            boolean allowed = this.apiPermissionService.isGuestAllowed(path, req.getMethod());
            if (!allowed) {
                ApiPermissionFilter.applyCorsHeaders((HttpServletRequest)req, (HttpServletResponse)res);
                res.setStatus(HttpStatus.FORBIDDEN.value());
                res.setContentType("application/json");
                res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                res.getWriter().write("{\"code\":403,\"msg\":\"\u65e0\u6743\u9650\u8bbf\u95ee\u8be5\u63a5\u53e3\uff0c\u8bf7\u767b\u5f55\u6216\u4f7f\u7528\u6709\u6743\u9650\u7684\u8d26\u53f7\"}");
                return;
            }
            chain.doFilter(request, response);
        }
        finally {
            CurrentUserHolder.clear();
            GuestPublicApiHolder.clear();
            ApiPermissionService.clearRequestCache();
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

    private boolean isWhitelisted(String path) {
        if (path == null || this.whitelistStr == null || this.whitelistStr.isBlank()) {
            return false;
        }
        List prefixes = Arrays.stream(this.whitelistStr.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        for (String prefix : prefixes) {
            if (!path.equals(prefix) && !path.startsWith(prefix + "/")) continue;
            return true;
        }
        return false;
    }

    private static boolean isAuthSelfPath(String pathNorm) {
        if (pathNorm == null) {
            return false;
        }
        return pathNorm.endsWith("/auth/me") || pathNorm.contains("/auth/change-password") || pathNorm.endsWith("/auth/switch-tenant") || pathNorm.endsWith("/sys/menu/list");
    }

    private long resolveEffectiveTenantId(HttpServletRequest req, JwtService.TokenAuth tokenAuth, Long userId, String pathForCheck, String method) {
        boolean superAdminGlobalToken;
        boolean bl = superAdminGlobalToken = userId != null && tokenAuth.tenantId() != null && tokenAuth.tenantId() == 0L && this.apiPermissionService.isSuperAdmin(userId);
        if (superAdminGlobalToken && (pathForCheck == null || !this.apiPermissionService.isGuestAllowed(pathForCheck, method))) {
            return 0L;
        }
        Long fromHeader = ApiPermissionFilter.parseXTenantId((HttpServletRequest)req);
        if (fromHeader != null) {
            return fromHeader;
        }
        Long tid = ResolvedTenantHolder.get();
        if (tid == null) {
            tid = this.tenantResolutionService.resolve(req);
        }
        if (tid == null) {
            tid = this.tenantResolutionService.resolveFromRefererHeader(req);
        }
        if (tid != null) {
            return tid;
        }
        if (userId != null) {
            Long jwtTid = tokenAuth.tenantId();
            return jwtTid != null ? jwtTid.longValue() : this.tenantProperties.getDefaultId();
        }
        return this.tenantProperties.getDefaultId();
    }

    private static boolean isReadLikeHttpMethod(String method) {
        if (method == null) {
            return true;
        }
        String m = method.trim().toUpperCase();
        return "GET".equals(m) || "HEAD".equals(m) || "OPTIONS".equals(m);
    }

    private boolean rejectIfInactiveTenantCodeHeader(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String code = req.getHeader("X-Tenant-Code");
        if (code == null || code.isBlank()) {
            return false;
        }
        Optional opt = this.tenantResolutionService.findTenantByCodeCached(code.trim());
        if (opt.isEmpty() || ((SysTenant)opt.get()).isActive()) {
            return false;
        }
        ApiPermissionFilter.applyCorsHeaders((HttpServletRequest)req, (HttpServletResponse)res);
        res.setStatus(HttpStatus.FORBIDDEN.value());
        res.setContentType("application/json");
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(this.objectMapper.writeValueAsString((Object)Result.fail((int)403, (String)"\u79df\u6237\u5df2\u505c\u7528")));
        return true;
    }

    private static Long parseXTenantId(HttpServletRequest req) {
        String h = req.getHeader("X-Tenant-Id");
        if (h == null || h.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(h.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
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
    public ApiPermissionFilter(ApiPermissionService apiPermissionService, ObjectMapper objectMapper, JwtService jwtService, TenantProperties tenantProperties, TenantResolutionService tenantResolutionService) {
        this.apiPermissionService = apiPermissionService;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
        this.tenantProperties = tenantProperties;
        this.tenantResolutionService = tenantResolutionService;
    }
}

