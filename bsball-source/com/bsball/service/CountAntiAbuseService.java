/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.CountAntiAbuseService
 *  com.bsball.utils.PortalVisitPathUtil
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  jakarta.annotation.PostConstruct
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.utils.PortalVisitPathUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class CountAntiAbuseService {
    @Value(value="${app.count-abuse.enabled:true}")
    private boolean enabled;
    @Value(value="${app.count-abuse.portal-pv-dedup-sec:20}")
    private int portalPvDedupSec;
    @Value(value="${app.count-abuse.notice-view-dedup-sec:300}")
    private int noticeViewDedupSec;
    @Value(value="${app.count-abuse.devtools-report-dedup-sec:600}")
    private int devtoolsReportDedupSec;
    @Value(value="${app.count-abuse.portal-feedback-dedup-sec:45}")
    private int portalFeedbackDedupSec;
    @Value(value="${app.count-abuse.portal-feedback-captcha-dedup-sec:3}")
    private int portalFeedbackCaptchaDedupSec;
    private Cache<String, Boolean> portalPvDedup;
    private Cache<String, Boolean> noticeViewDedup;
    private Cache<String, Boolean> devtoolsReportDedup;
    private Cache<String, Boolean> portalFeedbackDedup;
    private Cache<String, Boolean> portalFeedbackCaptchaDedup;

    @PostConstruct
    void init() {
        int pvSec = Math.max(5, this.portalPvDedupSec);
        int nvSec = Math.max(30, this.noticeViewDedupSec);
        int dtSec = Math.max(60, this.devtoolsReportDedupSec);
        int fbSec = Math.max(15, this.portalFeedbackDedupSec);
        int fbCaptchaSec = Math.max(1, this.portalFeedbackCaptchaDedupSec);
        this.portalPvDedup = Caffeine.newBuilder().expireAfterWrite((long)pvSec, TimeUnit.SECONDS).maximumSize(200000L).build();
        this.noticeViewDedup = Caffeine.newBuilder().expireAfterWrite((long)nvSec, TimeUnit.SECONDS).maximumSize(200000L).build();
        this.devtoolsReportDedup = Caffeine.newBuilder().expireAfterWrite((long)dtSec, TimeUnit.SECONDS).maximumSize(100000L).build();
        this.portalFeedbackDedup = Caffeine.newBuilder().expireAfterWrite((long)fbSec, TimeUnit.SECONDS).maximumSize(100000L).build();
        this.portalFeedbackCaptchaDedup = Caffeine.newBuilder().expireAfterWrite((long)fbCaptchaSec, TimeUnit.SECONDS).maximumSize(100000L).build();
    }

    public boolean acquirePortalPv(String visitorId, String clientIp, String path) {
        if (!this.enabled) {
            return true;
        }
        String p = CountAntiAbuseService.normalizePath((String)path);
        String key = visitorId != null && !visitorId.isBlank() ? "v:" + visitorId + ":" + p : "ip:" + CountAntiAbuseService.safeIp((String)clientIp) + ":" + p;
        return this.portalPvDedup.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    public boolean acquireNoticeView(String clientIp, Long noticeId) {
        if (!this.enabled) {
            return true;
        }
        if (noticeId == null) {
            return false;
        }
        String key = CountAntiAbuseService.safeIp((String)clientIp) + ":n:" + noticeId;
        return this.noticeViewDedup.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    public boolean acquireDevtoolsReport(String visitorId, String clientIp) {
        if (!this.enabled) {
            return true;
        }
        String key = visitorId != null && !visitorId.isBlank() ? "devtools:v:" + visitorId : "devtools:ip:" + CountAntiAbuseService.safeIp((String)clientIp);
        return this.devtoolsReportDedup.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    public boolean acquirePortalFeedback(String clientIp, long tenantId) {
        if (!this.enabled) {
            return true;
        }
        String key = "fb:" + tenantId + ":" + CountAntiAbuseService.safeIp((String)clientIp);
        return this.portalFeedbackDedup.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    public boolean acquirePortalFeedbackCaptcha(String clientIp, long tenantId) {
        if (!this.enabled) {
            return true;
        }
        String key = "fb-captcha:" + tenantId + ":" + CountAntiAbuseService.safeIp((String)clientIp);
        return this.portalFeedbackCaptchaDedup.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    private static String safeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.length() > 128 ? ip.substring(0, 128) : ip;
    }

    private static String normalizePath(String path) {
        String p = PortalVisitPathUtil.normalizeForPv((String)path);
        if (p == null || p.isBlank()) {
            return "/";
        }
        return p;
    }
}

