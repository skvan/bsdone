/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalVisitApi
 *  com.bsball.common.Result
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.service.CountAntiAbuseService
 *  com.bsball.service.PortalVisitService
 *  com.bsball.utils.HttpClientIpUtil
 *  com.bsball.utils.PortalVisitPathUtil
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
import com.bsball.service.CountAntiAbuseService;
import com.bsball.service.PortalVisitService;
import com.bsball.utils.HttpClientIpUtil;
import com.bsball.utils.PortalVisitPathUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.Generated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/portal/visit"})
public class PortalVisitApi {
    private static final String VISITOR_COOKIE = "portal_vid";
    private static final int COOKIE_MAX_AGE = 31536000;
    private static final Pattern ADMIN_IN_PATH = Pattern.compile("/admin(/|$|\\?|#)");
    private final PortalVisitService portalVisitService;
    private final CountAntiAbuseService countAntiAbuseService;
    private final TenantProperties tenantProperties;

    @PostMapping(value={"/record"})
    public Result<Map<String, Object>> record(HttpServletRequest request, HttpServletResponse response, @RequestBody(required=false) Map<String, String> body) {
        String ip;
        boolean recorded;
        String rawPath;
        String string = rawPath = body != null ? body.get("path") : null;
        if (!PortalVisitApi.shouldRecordPortalVisitPv((String)rawPath)) {
            return Result.ok((Object)Map.of((Object)"ok", (Object)true, (Object)"visitorNew", (Object)false, (Object)"recorded", (Object)false));
        }
        String path = PortalVisitPathUtil.normalizeForPv((String)rawPath);
        if (path == null || path.isBlank()) {
            return Result.ok((Object)Map.of((Object)"ok", (Object)true, (Object)"visitorNew", (Object)false, (Object)"recorded", (Object)false));
        }
        String visitorId = PortalVisitApi.readVisitorCookie((HttpServletRequest)request);
        boolean fresh = false;
        if (visitorId == null || visitorId.isBlank()) {
            visitorId = UUID.randomUUID().toString().replace("-", "");
            fresh = true;
            Cookie c = new Cookie("portal_vid", visitorId);
            c.setPath("/");
            c.setMaxAge(31536000);
            c.setHttpOnly(true);
            c.setAttribute("SameSite", "Lax");
            response.addCookie(c);
        }
        if (recorded = this.countAntiAbuseService.acquirePortalPv(visitorId, ip = HttpClientIpUtil.getClientIp((HttpServletRequest)request), path)) {
            Long tid = CurrentUserHolder.getTenantId();
            long tenantId = tid != null ? tid.longValue() : this.tenantProperties.getDefaultId();
            String ua = request.getHeader("User-Agent");
            this.portalVisitService.recordAsync(visitorId, path, ip, tenantId, ua);
        }
        return Result.ok((Object)Map.of((Object)"ok", (Object)true, (Object)"visitorNew", (Object)fresh, (Object)"recorded", (Object)recorded));
    }

    private static boolean shouldRecordPortalVisitPv(String path) {
        Object p;
        if (path == null || path.isBlank()) {
            return false;
        }
        Object object = p = path.startsWith("/") ? path : "/" + path;
        if (((String)p).contains("/admin/login")) {
            return false;
        }
        if (ADMIN_IN_PATH.matcher((CharSequence)p).find()) {
            return false;
        }
        String normalized = PortalVisitPathUtil.normalizeForPv((String)p);
        return !PortalVisitApi.isExcludedPortalPvPath((String)normalized);
    }

    private static boolean isExcludedPortalPvPath(String normalizedPath) {
        if (normalizedPath == null || normalizedPath.isBlank()) {
            return true;
        }
        String p = normalizedPath.replaceAll("/+$", "");
        if (p.isEmpty()) {
            p = "/";
        }
        return "/404".equals(p) || "/service-unavailable".equals(p);
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
    public PortalVisitApi(PortalVisitService portalVisitService, CountAntiAbuseService countAntiAbuseService, TenantProperties tenantProperties) {
        this.portalVisitService = portalVisitService;
        this.countAntiAbuseService = countAntiAbuseService;
        this.tenantProperties = tenantProperties;
    }
}

