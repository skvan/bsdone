/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.AccountProfileApi
 *  com.bsball.common.Result
 *  com.bsball.config.TenantProperties
 *  com.bsball.exception.UnauthorizedException
 *  com.bsball.service.AccountProfileService
 *  com.bsball.service.EmailService
 *  com.bsball.service.JwtService
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestHeader
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.config.TenantProperties;
import com.bsball.exception.UnauthorizedException;
import com.bsball.service.AccountProfileService;
import com.bsball.service.EmailService;
import com.bsball.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
public class AccountProfileApi {
    private final AccountProfileService accountProfileService;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final TenantProperties tenantProperties;

    @GetMapping(value={"/account/profile"})
    public Result<Map<String, Object>> getProfile(@RequestHeader(value="Authorization", required=false) String auth) {
        return Result.ok((Object)this.accountProfileService.getProfile(this.requireUserId(auth)));
    }

    @PutMapping(value={"/account/profile"})
    public Result<Map<String, Object>> updateProfile(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody(required=false) Map<String, Object> body) {
        return Result.ok((Object)this.accountProfileService.updateProfile(this.requireUserId(auth), body == null ? Map.of() : body));
    }

    @PostMapping(value={"/account/email/send-code"})
    public Result<Map<String, Object>> sendEmailCode(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        this.requireUserId(auth);
        this.emailService.sendCode(AccountProfileApi.str(body, (String)"email"), "bind_email", this.tenantProperties.getDefaultId(), AccountProfileApi.clientIp((HttpServletRequest)request));
        return Result.ok((Object)Map.of((Object)"sent", (Object)true));
    }

    @PostMapping(value={"/account/email/bind"})
    public Result<Map<String, Object>> bindEmail(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Map<String, Object> body) {
        return Result.ok((Object)this.accountProfileService.bindEmail(this.requireUserId(auth), AccountProfileApi.str(body, (String)"email"), AccountProfileApi.str(body, (String)"code")));
    }

    @PostMapping(value={"/account/phone/change"})
    public Result<Map<String, Object>> changePhone(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Map<String, Object> body) {
        return Result.ok((Object)this.accountProfileService.changePhone(this.requireUserId(auth), AccountProfileApi.str(body, (String)"phone"), AccountProfileApi.str(body, (String)"smsCode")));
    }

    @PostMapping(value={"/auth/password/forgot"})
    public Result<Map<String, Object>> forgotSendCode(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        this.accountProfileService.forgotSendCode(AccountProfileApi.str(body, (String)"channel"), AccountProfileApi.str(body, (String)"account"), this.tenantProperties.getDefaultId(), AccountProfileApi.clientIp((HttpServletRequest)request));
        return Result.ok((Object)Map.of((Object)"sent", (Object)true));
    }

    @PostMapping(value={"/auth/password/reset"})
    public Result<Map<String, Object>> resetPassword(@RequestBody Map<String, Object> body) {
        this.accountProfileService.resetPassword(AccountProfileApi.str(body, (String)"channel"), AccountProfileApi.str(body, (String)"account"), AccountProfileApi.str(body, (String)"code"), AccountProfileApi.str(body, (String)"newPassword"));
        return Result.ok((Object)Map.of((Object)"reset", (Object)true));
    }

    private Long requireUserId(String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7).trim() : null;
        Long userId = this.jwtService.parseUserId(token);
        if (userId == null) {
            throw new UnauthorizedException("\u8bf7\u5148\u767b\u5f55");
        }
        return userId;
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return null;
        }
        return body.get(key).toString().trim();
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
    public AccountProfileApi(AccountProfileService accountProfileService, EmailService emailService, JwtService jwtService, TenantProperties tenantProperties) {
        this.accountProfileService = accountProfileService;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.tenantProperties = tenantProperties;
    }
}

