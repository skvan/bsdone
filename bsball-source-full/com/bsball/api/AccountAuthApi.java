/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.AccountAuthApi
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.service.AccountRegisterService
 *  com.bsball.service.AuthCaptchaService
 *  com.bsball.service.SmsService
 *  com.bsball.service.SysConfigService
 *  com.bsball.service.TenantResolutionService
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysTenant;
import com.bsball.service.AccountRegisterService;
import com.bsball.service.AuthCaptchaService;
import com.bsball.service.SmsService;
import com.bsball.service.SysConfigService;
import com.bsball.service.TenantResolutionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/auth"})
public class AccountAuthApi {
    private final AccountRegisterService accountRegisterService;
    private final SmsService smsService;
    private final AuthCaptchaService authCaptchaService;
    private final SysConfigService sysConfigService;
    private final TenantResolutionService tenantResolutionService;

    @GetMapping(value={"/legal-docs"})
    public Result<Map<String, Object>> legalDocs() {
        return Result.ok((Object)this.accountRegisterService.legalDocs());
    }

    @PostMapping(value={"/sms/send"})
    public Result<Map<String, Object>> sendSms(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String phone = AccountAuthApi.str(body, (String)"phone");
        String scene = AccountAuthApi.str(body, (String)"scene");
        Long tenantId = AccountAuthApi.parseLong((Object)body.get("tenantId"));
        String tenantCode = AccountAuthApi.str(body, (String)"tenantCode");
        long tid = this.sysConfigService.effectiveTenantId(this.resolveTenant(tenantId, tenantCode, request));
        this.authCaptchaService.validateLoginCaptchaIfNeeded(tid, body);
        this.smsService.sendCode(phone, scene, tid, AccountAuthApi.clientIp((HttpServletRequest)request));
        return Result.ok((Object)Map.of((Object)"sent", (Object)true));
    }

    @PostMapping(value={"/register"})
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long tenantId = AccountAuthApi.parseLong((Object)body.get("tenantId"));
        String tenantCode = AccountAuthApi.str(body, (String)"tenantCode");
        long tid = this.sysConfigService.effectiveTenantId(this.resolveTenant(tenantId, tenantCode, request));
        Map data = this.accountRegisterService.register(AccountAuthApi.str(body, (String)"phone"), AccountAuthApi.str(body, (String)"smsCode"), AccountAuthApi.str(body, (String)"username"), AccountAuthApi.str(body, (String)"password"), Boolean.TRUE.equals(body.get("acceptTerms")), Boolean.TRUE.equals(body.get("acceptPrivacy")), Long.valueOf(tid), AccountAuthApi.clientIp((HttpServletRequest)request));
        return Result.ok((Object)data);
    }

    @PostMapping(value={"/login-by-phone"})
    public Result<Map<String, Object>> loginByPhone(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long tenantId = AccountAuthApi.parseLong((Object)body.get("tenantId"));
        String tenantCode = AccountAuthApi.str(body, (String)"tenantCode");
        long tid = this.sysConfigService.effectiveTenantId(this.resolveTenant(tenantId, tenantCode, request));
        Map data = this.accountRegisterService.loginByPhone(AccountAuthApi.str(body, (String)"phone"), AccountAuthApi.str(body, (String)"smsCode"), Long.valueOf(tid));
        return Result.ok((Object)data);
    }

    private Long resolveTenant(Long tenantId, String tenantCode, HttpServletRequest request) {
        String header;
        if (tenantId != null) {
            return tenantId;
        }
        if (tenantCode != null && !tenantCode.isBlank()) {
            return this.tenantResolutionService.findTenantByCodeCached(tenantCode.trim()).filter(SysTenant::isActive).map(BaseEntity::getId).orElse(null);
        }
        if (request != null && (header = request.getHeader("X-Tenant-Code")) != null && !header.isBlank()) {
            return this.tenantResolutionService.findTenantByCodeCached(header.trim()).filter(SysTenant::isActive).map(BaseEntity::getId).orElse(null);
        }
        return CurrentUserHolder.getTenantId();
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        return body.get(key).toString().trim();
    }

    private static Long parseLong(Object v) {
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

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Generated
    public AccountAuthApi(AccountRegisterService accountRegisterService, SmsService smsService, AuthCaptchaService authCaptchaService, SysConfigService sysConfigService, TenantResolutionService tenantResolutionService) {
        this.accountRegisterService = accountRegisterService;
        this.smsService = smsService;
        this.authCaptchaService = authCaptchaService;
        this.sysConfigService = sysConfigService;
        this.tenantResolutionService = tenantResolutionService;
    }
}

