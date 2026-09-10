/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.core.OperationLogAspect
 *  com.bsball.model.entity.SysOperationLog
 *  com.bsball.model.entity.SysUser
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.service.JwtService
 *  com.bsball.service.OperationLogAsyncService
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.aspectj.lang.ProceedingJoinPoint
 *  org.aspectj.lang.annotation.Around
 *  org.aspectj.lang.annotation.Aspect
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.stereotype.Component
 *  org.springframework.web.context.request.RequestContextHolder
 *  org.springframework.web.context.request.ServletRequestAttributes
 */
package com.bsball.core;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.model.entity.SysOperationLog;
import com.bsball.model.entity.SysUser;
import com.bsball.repository.SysUserRepository;
import com.bsball.service.JwtService;
import com.bsball.service.OperationLogAsyncService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import lombok.Generated;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/*
 * Exception performing whole class analysis ignored.
 */
@Aspect
@Component
@ConditionalOnProperty(prefix="app.operation-log", name={"enabled"}, havingValue="true")
public class OperationLogAspect {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    @Value(value="${spring.application.name}")
    private String serviceName;
    private final OperationLogAsyncService operationLogAsyncService;
    private final SysUserRepository sysUserRepository;
    private final JwtService jwtService;
    private final TenantProperties tenantProperties;

    @Around(value="execution(* com.bsball.api..*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        Object result = pjp.proceed();
        try {
            HttpServletRequest request = this.getRequest();
            if (request == null) {
                return result;
            }
            String method = request.getMethod();
            if ("GET".equalsIgnoreCase(method)) {
                return result;
            }
            String path = request.getRequestURI();
            if (path == null) {
                path = "";
            }
            if (path.contains("/increment-view/")) {
                return result;
            }
            if (!this.shouldLog(path)) {
                return result;
            }
            Long userId = this.parseUserId(request);
            String username = userId != null ? (String)this.sysUserRepository.findById((Object)userId).map(SysUser::getUsername).orElse(null) : null;
            String module = this.inferModule(path);
            String action = this.inferAction(method, path);
            String ip = this.getClientIp(request);
            SysOperationLog logEntity = new SysOperationLog();
            Long tid = CurrentUserHolder.getTenantId();
            logEntity.setTenantId(Long.valueOf(tid != null ? tid.longValue() : this.tenantProperties.getDefaultId()));
            logEntity.setUserId(userId);
            logEntity.setUsername(username);
            logEntity.setModule(module);
            logEntity.setAction(action);
            logEntity.setIp(ip);
            logEntity.setDescription(path);
            logEntity.setCreatedAt(LocalDateTime.now());
            this.operationLogAsyncService.saveAsync(logEntity);
        }
        catch (Exception e) {
            log.debug("\u64cd\u4f5c\u65e5\u5fd7\u8bb0\u5f55\u5931\u8d25: {}", (Object)e.getMessage());
        }
        return result;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private Long parseUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        return this.jwtService.parseUserId(auth.substring(7).trim());
    }

    private boolean shouldLog(String path) {
        if (path.contains("/sys/") || path.contains("change-password")) {
            return true;
        }
        return path.contains("/league/") || path.contains("/team/") || path.contains("/coach/") || path.contains("/player/") || path.contains("/event/") || path.contains("/game/") || path.contains("/personnel-change/");
    }

    private String inferModule(String path) {
        if (path.contains("/sys/user")) {
            return "\u7528\u6237\u7ba1\u7406";
        }
        if (path.contains("/sys/role")) {
            return "\u89d2\u8272\u7ba1\u7406";
        }
        if (path.contains("/sys/menu")) {
            return "\u83dc\u5355\u7ba1\u7406";
        }
        if (path.contains("/sys/api")) {
            return "API\u7ba1\u7406";
        }
        if (path.contains("/sys/dict")) {
            return "\u5b57\u5178\u7ba1\u7406";
        }
        if (path.contains("/sys/article")) {
            return "\u6587\u7ae0\u7ba1\u7406";
        }
        if (path.contains("/sys/notice")) {
            return "\u901a\u77e5\u7ba1\u7406";
        }
        if (path.contains("/sys/config")) {
            return "\u914d\u7f6e\u7ba1\u7406";
        }
        if (path.contains("/sys/resource")) {
            return "\u8d44\u6e90\u6587\u4ef6";
        }
        if (path.contains("change-password")) {
            return "\u8ba4\u8bc1";
        }
        if (path.contains("/league")) {
            return "\u8054\u76df\u7ba1\u7406";
        }
        if (path.contains("/team")) {
            return "\u7403\u961f\u7ba1\u7406";
        }
        if (path.contains("/coach")) {
            return "\u6559\u7ec3\u7ba1\u7406";
        }
        if (path.contains("/player")) {
            return "\u7403\u5458\u7ba1\u7406";
        }
        if (path.contains("/event")) {
            return "\u8d5b\u4e8b\u7ba1\u7406";
        }
        if (path.contains("/game")) {
            return "\u6bd4\u8d5b\u7ba1\u7406";
        }
        if (path.contains("/personnel-change") || path.contains("/history-record")) {
            return "\u6cbf\u9769\u8bb0\u5f55";
        }
        return "\u7cfb\u7edf";
    }

    private String inferAction(String method, String path) {
        if ("PUT".equalsIgnoreCase(method)) {
            return "update";
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            return "delete";
        }
        if ("POST".equalsIgnoreCase(method)) {
            if (OperationLogAspect.postPathMeansUpdate((String)path)) {
                return "update";
            }
            return "create";
        }
        return method != null ? method.toLowerCase() : "";
    }

    private static boolean postPathMeansUpdate(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        return path.contains("/game/") && (path.contains("/save-result") || path.contains("/save-live") || path.contains("/live-snapshot"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Generated
    public OperationLogAspect(OperationLogAsyncService operationLogAsyncService, SysUserRepository sysUserRepository, JwtService jwtService, TenantProperties tenantProperties) {
        this.operationLogAsyncService = operationLogAsyncService;
        this.sysUserRepository = sysUserRepository;
        this.jwtService = jwtService;
        this.tenantProperties = tenantProperties;
    }
}

