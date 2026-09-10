/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.core.PortalVisitIpLocationBackfillRunner
 *  com.bsball.repository.PortalVisitHitRepository
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.LbsIpLocationService
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.core.annotation.Order
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Component
 */
package com.bsball.core;

import com.bsball.config.OpenPlatformProperties;
import com.bsball.repository.PortalVisitHitRepository;
import com.bsball.service.IpLocationCacheService;
import com.bsball.service.LbsIpLocationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
@Order(value=500)
@ConditionalOnProperty(prefix="app.portal-visit", name={"ip-backfill-schedule-enabled"}, havingValue="true", matchIfMissing=true)
public class PortalVisitIpLocationBackfillRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(PortalVisitIpLocationBackfillRunner.class);
    private final PortalVisitHitRepository portalVisitHitRepository;
    private final IpLocationCacheService ipLocationCacheService;
    private final LbsIpLocationService lbsIpLocationService;
    private final OpenPlatformProperties openPlatformProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron="${app.portal-visit.ip-backfill-cron:0 20 5 * * *}")
    public void scheduledBackfill() {
        if (!this.running.compareAndSet(false, true)) {
            log.info("\u95e8\u6237\u6253\u70b9 IP \u5f52\u5c5e\u5730\u56de\u586b\u4efb\u52a1\u4ecd\u5728\u6267\u884c\uff0c\u8df3\u8fc7\u672c\u8f6e\u8c03\u5ea6");
            return;
        }
        try {
            this.backfill();
        }
        finally {
            this.running.set(false);
        }
    }

    private void backfill() {
        long startedAt = System.currentTimeMillis();
        if (!this.openPlatformProperties.anyProviderConfigured()) {
            log.debug("\u672a\u914d\u7f6e IP \u5b9a\u4f4d\u5bc6\u94a5\uff0c\u8df3\u8fc7\u95e8\u6237\u6253\u70b9 IP \u5f52\u5c5e\u5730\u56de\u586b");
            return;
        }
        try {
            int staleDays = this.openPlatformProperties.regionStaleDays();
            LocalDateTime staleThreshold = LocalDateTime.now().minusDays(staleDays);
            List missing = this.portalVisitHitRepository.findDistinctIpsWithoutValidCache(LocalDate.now().minusDays(30L), staleThreshold);
            if (missing == null || missing.isEmpty()) {
                long elapsedMs = System.currentTimeMillis() - startedAt;
                log.info("\u95e8\u6237\u6253\u70b9 IP \u5f52\u5c5e\u5730\u56de\u586b\u5b8c\u6210\uff1a\u5f85\u56de\u586b 0\uff0c\u6210\u529f\u56de\u586b 0\uff0c\u5269\u4f59\u672a\u56de\u586b 0\uff0c\u8017\u65f6 {}", (Object)PortalVisitIpLocationBackfillRunner.formatElapsed((long)elapsedMs));
                return;
            }
            int totalDistinct = missing.size();
            int chunk = this.openPlatformProperties.maxBatchIps();
            int chunks = (missing.size() + chunk - 1) / chunk;
            log.info("\u95e8\u6237\u6253\u70b9 IP \u5f52\u5c5e\u5730\u56de\u586b\uff1a\u5f85\u8865\u5168 {} \u4e2a\uff0c\u5206 {} \u6279\uff08\u6bcf\u6279\u81f3\u591a {}\uff09", new Object[]{missing.size(), chunks, chunk});
            for (int i = 0; i < missing.size(); i += chunk) {
                int end = Math.min(i + chunk, missing.size());
                this.lbsIpLocationService.locateBatch(missing.subList(i, end));
                if (end >= missing.size()) continue;
                try {
                    Thread.sleep(1000L);
                    continue;
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            Map afterRegions = this.ipLocationCacheService.findValidRegionsByIps((Collection)missing, staleDays);
            int unresolvedAfter = missing.size() - afterRegions.size();
            int resolvedNow = Math.max(0, missing.size() - unresolvedAfter);
            List stillMissing = missing.stream().filter(ip -> !afterRegions.containsKey(ip)).toList();
            String unresolvedBreakdown = PortalVisitIpLocationBackfillRunner.formatUnresolvedBreakdown((List)stillMissing);
            log.info("\u95e8\u6237\u6253\u70b9 IP \u5f52\u5c5e\u5730\u56de\u586b\u5b8c\u6210\uff1a\u5f85\u56de\u586b {}\uff0c\u6210\u529f\u56de\u586b {}\uff0c\u5269\u4f59\u672a\u56de\u586b {}\uff0c\u8017\u65f6 {}{}", new Object[]{missing.size(), resolvedNow, unresolvedAfter, PortalVisitIpLocationBackfillRunner.formatElapsed((long)(System.currentTimeMillis() - startedAt)), unresolvedBreakdown});
        }
        catch (Exception e) {
            log.warn("\u95e8\u6237\u6253\u70b9 IP \u5f52\u5c5e\u5730\u56de\u586b\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    private static String formatElapsed(long elapsedMs) {
        long ms = Math.max(0L, elapsedMs);
        long totalSeconds = ms / 1000L;
        long millis = ms % 1000L;
        if (totalSeconds < 60L) {
            if (totalSeconds == 0L) {
                return millis + "\u6beb\u79d2";
            }
            if (millis == 0L) {
                return totalSeconds + "\u79d2";
            }
            return totalSeconds + "\u79d2" + millis + "\u6beb\u79d2";
        }
        long totalMinutes = totalSeconds / 60L;
        if (totalMinutes < 60L) {
            long sec = totalSeconds % 60L;
            return totalMinutes + "\u5206" + sec + "\u79d2";
        }
        long totalHours = totalMinutes / 60L;
        if (totalHours < 24L) {
            long min = totalMinutes % 60L;
            long sec = totalSeconds % 60L;
            return totalHours + "\u5c0f\u65f6" + min + "\u5206" + sec + "\u79d2";
        }
        long days = totalHours / 24L;
        long hour = totalHours % 24L;
        long min = totalMinutes % 60L;
        return days + "\u5929" + hour + "\u5c0f\u65f6" + min + "\u5206";
    }

    private static String formatUnresolvedBreakdown(List<String> stillMissing) {
        if (stillMissing.isEmpty()) {
            return "";
        }
        int internalOrNonRoutable = 0;
        ArrayList<String> publicStill = new ArrayList<String>();
        for (String ip : stillMissing) {
            if (!LbsIpLocationService.isPublicRoutableIp((String)ip)) {
                ++internalOrNonRoutable;
                continue;
            }
            publicStill.add(ip);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\uff1b\u672a\u56de\u586b\u6784\u6210\uff1a\u5185\u7f51/\u4e0d\u53ef\u8def\u7531 ").append(internalOrNonRoutable);
        sb.append("\uff0c\u516c\u7f51\u5404\u6e90\u4ecd\u672a\u89e3\u6790 ").append(publicStill.size());
        if (!publicStill.isEmpty()) {
            int cap = Math.min(publicStill.size(), 12);
            sb.append("\uff08\u793a\u4f8b IP\uff1a");
            for (int i = 0; i < cap; ++i) {
                if (i > 0) {
                    sb.append("\uff0c");
                }
                sb.append((String)publicStill.get(i));
            }
            if (publicStill.size() > cap) {
                sb.append("\u2026\u5171").append(publicStill.size()).append("\u4e2a");
            }
            sb.append("\uff09");
        }
        return sb.toString();
    }

    @Generated
    public PortalVisitIpLocationBackfillRunner(PortalVisitHitRepository portalVisitHitRepository, IpLocationCacheService ipLocationCacheService, LbsIpLocationService lbsIpLocationService, OpenPlatformProperties openPlatformProperties) {
        this.portalVisitHitRepository = portalVisitHitRepository;
        this.ipLocationCacheService = ipLocationCacheService;
        this.lbsIpLocationService = lbsIpLocationService;
        this.openPlatformProperties = openPlatformProperties;
    }
}

