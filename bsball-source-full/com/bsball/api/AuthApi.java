/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.AuthApi
 *  com.bsball.common.Result
 *  com.bsball.config.InitSeedProperties
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.exception.UnauthorizedException
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.service.AuthCaptchaService
 *  com.bsball.service.AuthService
 *  com.bsball.service.JwtService
 *  com.bsball.service.SysConfigService
 *  com.bsball.service.SysLoginLogService
 *  com.bsball.service.TenantResolutionService
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PatchMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.exception.UnauthorizedException;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysTenant;
import com.bsball.service.AuthCaptchaService;
import com.bsball.service.AuthService;
import com.bsball.service.JwtService;
import com.bsball.service.SysConfigService;
import com.bsball.service.SysLoginLogService;
import com.bsball.service.TenantResolutionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/auth"})
public class AuthApi {
    private final AuthService authService;
    private final SysLoginLogService sysLoginLogService;
    private final InitSeedProperties initSeedProperties;
    private final AuthCaptchaService authCaptchaService;
    private final SysConfigService sysConfigService;
    private final JwtService jwtService;
    private final TenantResolutionService tenantResolutionService;
    private final TenantProperties tenantProperties;

    @PostMapping(value={"/login"})
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String username = body != null && body.get("username") != null ? body.get("username").toString() : null;
        String password = body != null && body.get("password") != null ? body.get("password").toString() : null;
        String ip = AuthApi.getClientIp((HttpServletRequest)request);
        String deviceInfo = AuthApi.getDeviceInfo((HttpServletRequest)request);
        Long requestedTenant = null;
        try {
            String tokenPrefix;
            boolean superAdmin;
            Long l;
            Object v;
            Long userId;
            Object v2;
            requestedTenant = this.resolveLoginTenantRequest(body, request);
            long tid = this.sysConfigService.effectiveTenantId(requestedTenant);
            this.authCaptchaService.validateLoginCaptchaIfNeeded(tid, body);
            Map data = this.authService.login(username != null ? username : "", password != null ? password : "", requestedTenant);
            this.authCaptchaService.consumeCaptchaAfterLoginSuccess(tid, body);
            Map user = (Map)data.get("user");
            if (user != null && (v2 = user.get("id")) instanceof Number) {
                Number n = (Number)v2;
                v0 = n.longValue();
            } else {
                v0 = userId = null;
            }
            if (user != null && (v = user.get("tenantId")) instanceof Number) {
                Number n = (Number)v;
                l = n.longValue();
            } else {
                l = null;
            }
            Long tenantId = l;
            boolean bl = superAdmin = user != null && Boolean.TRUE.equals(user.get("superAdmin"));
            Long logTenantId = superAdmin ? 0L : (tenantId != null && tenantId > 0L ? tenantId : (requestedTenant != null && requestedTenant > 0L ? requestedTenant : tenantId));
            String token = data.get("token") != null ? data.get("token").toString() : null;
            String string = tokenPrefix = token != null && token.length() > 12 ? token.substring(0, 12) : token;
            if (this.initSeedProperties.isRecordLoginLog()) {
                this.sysLoginLogService.recordLogin(userId, username, ip, deviceInfo, "success", null, tokenPrefix, logTenantId);
            }
            return Result.ok((Object)data);
        }
        catch (BusinessException | UnauthorizedException e) {
            Long failTid = requestedTenant != null ? requestedTenant : (CurrentUserHolder.getTenantId() != null ? CurrentUserHolder.getTenantId().longValue() : this.tenantProperties.getDefaultId());
            if (this.initSeedProperties.isRecordLoginLog()) {
                this.sysLoginLogService.recordLogin(null, username, ip, deviceInfo, "fail", e.getMessage(), null, failTid);
            }
            throw e;
        }
    }

    private Long resolveLoginTenantRequest(Map<String, Object> body, HttpServletRequest request) {
        String headerCode;
        if (body != null) {
            Long tid = AuthApi.parseOptionalLong((Object)body.get("tenantId"));
            if (tid != null) {
                return tid;
            }
            Object tc = body.get("tenantCode");
            if (tc != null && !tc.toString().isBlank()) {
                String code = tc.toString().trim();
                return this.tenantResolutionService.findTenantByCodeCached(code).filter(SysTenant::isActive).map(BaseEntity::getId).orElse(null);
            }
        }
        String string = headerCode = request != null ? request.getHeader("X-Tenant-Code") : null;
        if (headerCode != null && !headerCode.isBlank()) {
            String code = headerCode.trim();
            return this.tenantResolutionService.findTenantByCodeCached(code).filter(SysTenant::isActive).map(BaseEntity::getId).orElse(null);
        }
        return CurrentUserHolder.getTenantId();
    }

    private static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String getDeviceInfo(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ua = request.getHeader("User-Agent");
        return ua != null && !ua.isBlank() ? ua : null;
    }

    private static Long parseOptionalLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            Number n = (Number)v;
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString().trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    private Long resolveTenantForCaptcha(Long tenantId, String tenantCode, HttpServletRequest request) {
        String headerCode;
        if (tenantId != null) {
            return tenantId;
        }
        if (tenantCode != null && !tenantCode.isBlank()) {
            return this.tenantResolutionService.findTenantByCodeCached(tenantCode.trim()).filter(SysTenant::isActive).map(BaseEntity::getId).orElseThrow(() -> new UnauthorizedException("\u79df\u6237\u65e0\u6548\u6216\u5df2\u505c\u7528"));
        }
        if (request != null && (headerCode = request.getHeader("X-Tenant-Code")) != null && !headerCode.isBlank()) {
            return this.tenantResolutionService.findTenantByCodeCached(headerCode.trim()).filter(SysTenant::isActive).map(BaseEntity::getId).orElseThrow(() -> new UnauthorizedException("\u79df\u6237\u65e0\u6548\u6216\u5df2\u505c\u7528"));
        }
        return CurrentUserHolder.getTenantId();
    }

    @GetMapping(value={"/me"})
    public Result<Map<String, Object>> me(@RequestHeader(value="Authorization", required=false) String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Map data = this.authService.me(token);
        return Result.ok((Object)data);
    }

    @GetMapping(value={"/captcha/options"})
    public Result<Map<String, Object>> captchaOptions(@RequestParam(value="tenantId", required=false) Long tenantId, @RequestParam(value="tenantCode", required=false) String tenantCode, HttpServletRequest request) {
        long tid = this.sysConfigService.effectiveTenantId(this.resolveTenantForCaptcha(tenantId, tenantCode, request));
        return Result.ok((Object)this.authCaptchaService.getOptions(tid));
    }

    @GetMapping(value={"/captcha/image"})
    public Result<Map<String, Object>> captchaImage(@RequestParam(value="tenantId", required=false) Long tenantId, @RequestParam(value="tenantCode", required=false) String tenantCode, HttpServletRequest request) {
        long tid = this.sysConfigService.effectiveTenantId(this.resolveTenantForCaptcha(tenantId, tenantCode, request));
        return Result.ok((Object)this.authCaptchaService.createImageCaptcha(tid));
    }

    @PostMapping(value={"/captcha/verify-click"})
    public Result<Map<String, Object>> verifyClickCaptcha(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long tenantId = AuthApi.parseOptionalLong(body != null ? body.get("tenantId") : null);
        String tenantCode = body != null && body.get("tenantCode") != null ? body.get("tenantCode").toString() : null;
        long tid = this.sysConfigService.effectiveTenantId(this.resolveTenantForCaptcha(tenantId, tenantCode, request));
        return Result.ok((Object)this.authCaptchaService.verifyClickCaptcha(tid, body));
    }

    @PostMapping(value={"/captcha/verify-drag"})
    public Result<Map<String, Object>> verifyDragCaptcha(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long tenantId = AuthApi.parseOptionalLong(body != null ? body.get("tenantId") : null);
        String tenantCode = body != null && body.get("tenantCode") != null ? body.get("tenantCode").toString() : null;
        long tid = this.sysConfigService.effectiveTenantId(this.resolveTenantForCaptcha(tenantId, tenantCode, request));
        return Result.ok((Object)this.authCaptchaService.verifyDragCaptcha(tid, body));
    }

    @PostMapping(value={"/switch-tenant"})
    public Result<Map<String, Object>> switchTenant(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Map<String, Object> body) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Long userId = this.jwtService.parseUserId(token);
        if (userId == null) {
            return Result.fail((String)"\u8bf7\u5148\u767b\u5f55");
        }
        Long tenantId = AuthApi.parseOptionalLong(body != null ? body.get("tenantId") : null);
        if (tenantId == null) {
            return Result.fail((String)"tenantId \u4e0d\u80fd\u4e3a\u7a7a");
        }
        Map data = this.authService.switchTenant(userId, tenantId);
        return Result.ok((Object)data);
    }

    @PatchMapping(value={"/change-password"})
    public Result<Object> changePassword(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Map<String, String> body) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Long userId = this.jwtService.parseUserId(token);
        if (userId == null) {
            return Result.fail((String)"\u8bf7\u5148\u767b\u5f55");
        }
        String oldPassword = body != null ? body.get("oldPassword") : null;
        String newPassword = body != null ? body.get("newPassword") : null;
        this.authService.changePassword(userId, oldPassword != null ? oldPassword : "", newPassword != null ? newPassword : "");
        return Result.ok((Object)Map.of());
    }

    @Generated
    public AuthApi(AuthService authService, SysLoginLogService sysLoginLogService, InitSeedProperties initSeedProperties, AuthCaptchaService authCaptchaService, SysConfigService sysConfigService, JwtService jwtService, TenantResolutionService tenantResolutionService, TenantProperties tenantProperties) {
        this.authService = authService;
        this.sysLoginLogService = sysLoginLogService;
        this.initSeedProperties = initSeedProperties;
        this.authCaptchaService = authCaptchaService;
        this.sysConfigService = sysConfigService;
        this.jwtService = jwtService;
        this.tenantResolutionService = tenantResolutionService;
        this.tenantProperties = tenantProperties;
    }
}

