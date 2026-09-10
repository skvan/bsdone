/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.RateLimitFilter
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  jakarta.annotation.PostConstruct
 *  jakarta.servlet.Filter
 *  jakarta.servlet.FilterChain
 *  jakarta.servlet.ServletException
 *  jakarta.servlet.ServletRequest
 *  jakarta.servlet.ServletResponse
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.core.annotation.Order
 *  org.springframework.http.HttpStatus
 *  org.springframework.stereotype.Component
 */
package com.bsball.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(value=-2147483638)
public class RateLimitFilter
implements Filter {
    @Value(value="${app.rate-limit.enabled:true}")
    private boolean enabled;
    @Value(value="${app.rate-limit.login-limit:5}")
    private int loginLimit;
    @Value(value="${app.rate-limit.druid-login-limit:5}")
    private int druidLoginLimit;
    @Value(value="${app.rate-limit.public-limit:60}")
    private int publicLimit;
    @Value(value="${app.rate-limit.admin-limit:120}")
    private int adminLimit;
    @Value(value="${app.rate-limit.window-sec:60}")
    private int windowSec;
    @Value(value="${app.rate-limit.visit-post-limit:120}")
    private int visitPostLimit;
    @Value(value="${app.rate-limit.visit-post-window-sec:60}")
    private int visitPostWindowSec;
    @Value(value="${app.rate-limit.devtools-post-limit:30}")
    private int devtoolsPostLimit;
    @Value(value="${app.rate-limit.devtools-post-window-sec:60}")
    private int devtoolsPostWindowSec;
    @Value(value="${app.rate-limit.notice-increment-limit:60}")
    private int noticeIncrementLimit;
    @Value(value="${app.rate-limit.notice-increment-window-sec:60}")
    private int noticeIncrementWindowSec;
    @Value(value="${app.rate-limit.captcha-image-limit:30}")
    private int captchaImageLimit;
    @Value(value="${app.rate-limit.captcha-image-window-sec:60}")
    private int captchaImageWindowSec;
    @Value(value="${app.rate-limit.captcha-verify-limit:60}")
    private int captchaVerifyLimit;
    @Value(value="${app.rate-limit.captcha-verify-window-sec:60}")
    private int captchaVerifyWindowSec;
    @Value(value="${app.rate-limit.sms-send-limit:5}")
    private int smsSendLimit;
    @Value(value="${app.rate-limit.sms-send-window-sec:60}")
    private int smsSendWindowSec;
    private Cache<String, AtomicInteger> visitPostCache;
    private Cache<String, AtomicInteger> devtoolsPostCache;
    private Cache<String, AtomicInteger> noticeIncrementCache;
    private Cache<String, AtomicInteger> captchaImageCache;
    private Cache<String, AtomicInteger> captchaVerifyCache;
    private Cache<String, AtomicInteger> smsSendCache;
    private final Cache<String, AtomicInteger> loginCache = Caffeine.newBuilder().expireAfterWrite(60L, TimeUnit.SECONDS).maximumSize(10000L).build();
    private final Cache<String, AtomicInteger> druidLoginCache = Caffeine.newBuilder().expireAfterWrite(60L, TimeUnit.SECONDS).maximumSize(10000L).build();
    private final Cache<String, AtomicInteger> publicCache = Caffeine.newBuilder().expireAfterWrite(60L, TimeUnit.SECONDS).maximumSize(10000L).build();
    private final Cache<String, AtomicInteger> adminCache = Caffeine.newBuilder().expireAfterWrite(60L, TimeUnit.SECONDS).maximumSize(10000L).build();

    @PostConstruct
    void initVisitAndNoticeRateCaches() {
        int vw = Math.max(10, this.visitPostWindowSec);
        int dw = Math.max(10, this.devtoolsPostWindowSec);
        int nw = Math.max(10, this.noticeIncrementWindowSec);
        int ciw = Math.max(10, this.captchaImageWindowSec);
        int cvw = Math.max(10, this.captchaVerifyWindowSec);
        int smsw = Math.max(10, this.smsSendWindowSec);
        this.visitPostCache = Caffeine.newBuilder().expireAfterWrite((long)vw, TimeUnit.SECONDS).maximumSize(50000L).build();
        this.devtoolsPostCache = Caffeine.newBuilder().expireAfterWrite((long)dw, TimeUnit.SECONDS).maximumSize(50000L).build();
        this.noticeIncrementCache = Caffeine.newBuilder().expireAfterWrite((long)nw, TimeUnit.SECONDS).maximumSize(50000L).build();
        this.captchaImageCache = Caffeine.newBuilder().expireAfterWrite((long)ciw, TimeUnit.SECONDS).maximumSize(50000L).build();
        this.captchaVerifyCache = Caffeine.newBuilder().expireAfterWrite((long)cvw, TimeUnit.SECONDS).maximumSize(50000L).build();
        this.smsSendCache = Caffeine.newBuilder().expireAfterWrite((long)smsw, TimeUnit.SECONDS).maximumSize(50000L).build();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String ip;
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
        String path = req.getRequestURI();
        if (path != null && (path.endsWith("/health") || path.contains("/health"))) {
            chain.doFilter(request, response);
            return;
        }
        String key = ip = this.getClientIp(req);
        if (path != null && path.contains("/auth/sms/send") && "POST".equalsIgnoreCase(req.getMethod())) {
            if (!this.allow(this.smsSendCache, key, this.smsSendLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && path.contains("/auth/login")) {
            if (!this.allow(this.loginCache, key, this.loginLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && path.contains("/auth/captcha/image") && "GET".equalsIgnoreCase(req.getMethod())) {
            if (!this.allow(this.captchaImageCache, key, this.captchaImageLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && (path.contains("/auth/captcha/verify-click") || path.contains("/auth/captcha/verify-drag")) && "POST".equalsIgnoreCase(req.getMethod())) {
            if (!this.allow(this.captchaVerifyCache, key, this.captchaVerifyLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && path.contains("druid/submitLogin") && "POST".equalsIgnoreCase(req.getMethod())) {
            if (!this.allow(this.druidLoginCache, key, this.druidLoginLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && path.contains("/portal/devtools") && "POST".equalsIgnoreCase(req.getMethod())) {
            if (!this.allow(this.devtoolsPostCache, key, this.devtoolsPostLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && path.contains("/portal/visit") && "POST".equalsIgnoreCase(req.getMethod())) {
            if (!this.allow(this.visitPostCache, key, this.visitPostLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && path.contains("/sys/article/increment-view") && "POST".equalsIgnoreCase(req.getMethod())) {
            if (!this.allow(this.noticeIncrementCache, key, this.noticeIncrementLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (path != null && this.isPublicPath(path)) {
            if (!this.allow(this.publicCache, key, this.publicLimit)) {
                this.sendRateLimitResponse(res);
                return;
            }
        } else if (!this.allow(this.adminCache, key, this.adminLimit)) {
            this.sendRateLimitResponse(res);
            return;
        }
        chain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse res) throws IOException {
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setContentType("application/json;charset=UTF-8");
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "*");
        res.getWriter().write("{\"code\":429,\"msg\":\"\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\"}");
    }

    private boolean allow(Cache<String, AtomicInteger> cache, String key, int limit) {
        AtomicInteger count = (AtomicInteger)cache.get((Object)key, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= limit;
    }

    private boolean isPublicPath(String path) {
        return path.contains("/league") || path.contains("/team") || path.contains("/event") || path.contains("/game") || path.contains("/stats") || path.contains("/player") || path.contains("/sys/article");
    }

    private String getClientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}

