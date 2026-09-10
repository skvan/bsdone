/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalDevtoolsApi
 *  com.bsball.common.Result
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.CountAntiAbuseService
 *  com.bsball.service.PortalDevtoolsReportService
 *  com.bsball.utils.HttpClientIpUtil
 *  com.bsball.utils.PortalVisitPathUtil
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  jakarta.servlet.http.Cookie
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.CountAntiAbuseService;
import com.bsball.service.PortalDevtoolsReportService;
import com.bsball.utils.HttpClientIpUtil;
import com.bsball.utils.PortalVisitPathUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/portal/devtools"})
public class PortalDevtoolsApi {
    private static final String VISITOR_COOKIE = "portal_vid";
    private static final int COOKIE_MAX_AGE = 31536000;
    private final PortalDevtoolsReportService portalDevtoolsReportService;
    private final CountAntiAbuseService countAntiAbuseService;
    private final TenantProperties tenantProperties;
    private final ObjectMapper objectMapper;
    private final ApiPermissionService apiPermissionService;

    @PostMapping(value={"/report"})
    public Result<Map<String, Object>> report(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) Map<String, Object> body) {
        Long uid;
        String visitorId = PortalDevtoolsApi.readVisitorCookie((HttpServletRequest)request);
        boolean freshVisitor = false;
        if (visitorId == null || visitorId.isBlank()) {
            visitorId = UUID.randomUUID().toString().replace("-", "");
            freshVisitor = true;
            Cookie c = new Cookie("portal_vid", visitorId);
            c.setPath("/");
            c.setMaxAge(31536000);
            c.setHttpOnly(true);
            c.setAttribute("SameSite", "Lax");
            response.addCookie(c);
        }
        if ((uid = CurrentUserHolder.get()) != null && this.apiPermissionService.isSuperAdmin(uid)) {
            return Result.ok((Object)Map.of((Object)"ok", (Object)true, (Object)"visitorNew", (Object)freshVisitor, (Object)"recorded", (Object)false));
        }
        String ip = HttpClientIpUtil.getClientIp((HttpServletRequest)request);
        boolean acquired = this.countAntiAbuseService.acquireDevtoolsReport(visitorId, ip);
        if (acquired) {
            Long tid = CurrentUserHolder.getTenantId();
            long tenantId = tid != null ? tid.longValue() : this.tenantProperties.getDefaultId();
            String routeFullPath = PortalVisitPathUtil.normalizeForPv((String)PortalDevtoolsApi.extractRouteFullPath(body));
            String routeName = body != null && body.get("routeName") != null ? String.valueOf(body.get("routeName")) : null;
            String ua = request.getHeader("User-Agent");
            if (ua == null || ua.isBlank()) {
                ua = body != null && body.get("userAgent") != null ? String.valueOf(body.get("userAgent")) : null;
            }
            String metaJson = this.buildClientMetaJson(body);
            this.portalDevtoolsReportService.recordAsync(visitorId, tenantId, ip, routeFullPath, routeName, ua, metaJson);
        }
        return Result.ok((Object)Map.of((Object)"ok", (Object)true, (Object)"visitorNew", (Object)freshVisitor, (Object)"recorded", (Object)acquired));
    }

    private static String extractRouteFullPath(Map<String, Object> body) {
        String s;
        if (body == null) {
            return null;
        }
        if (body.get("routeFullPath") != null && !(s = String.valueOf(body.get("routeFullPath"))).isBlank() && !"null".equals(s)) {
            return s;
        }
        if (body.get("path") != null) {
            return String.valueOf(body.get("path"));
        }
        return null;
    }

    private String buildClientMetaJson(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        LinkedHashMap<String, Object> meta = new LinkedHashMap<String, Object>();
        for (String key : new String[]{"language", "platform", "screenWidth", "screenHeight", "viewportWidth", "viewportHeight", "timezone", "devicePixelRatio", "hardwareConcurrency", "maxTouchPoints", "appVersion", "routeTitle"}) {
            if (!body.containsKey(key) || body.get(key) == null) continue;
            meta.put(key, body.get(key));
        }
        if (meta.isEmpty()) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(meta);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static String readVisitorCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (!"portal_vid".equals(c.getName())) continue;
            return c.getValue();
        }
        return null;
    }

    @Generated
    public PortalDevtoolsApi(PortalDevtoolsReportService portalDevtoolsReportService, CountAntiAbuseService countAntiAbuseService, TenantProperties tenantProperties, ObjectMapper objectMapper, ApiPermissionService apiPermissionService) {
        this.portalDevtoolsReportService = portalDevtoolsReportService;
        this.countAntiAbuseService = countAntiAbuseService;
        this.tenantProperties = tenantProperties;
        this.objectMapper = objectMapper;
        this.apiPermissionService = apiPermissionService;
    }
}

