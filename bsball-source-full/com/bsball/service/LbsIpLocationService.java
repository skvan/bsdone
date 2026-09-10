/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.OpenPlatformProperties
 *  com.bsball.config.OpenPlatformProperties$Provider
 *  com.bsball.model.dto.IpLocationBatchDto
 *  com.bsball.model.dto.IpLocationDto
 *  com.bsball.model.dto.LbsProviderOptionDto
 *  com.bsball.model.enums.LbsProvider
 *  com.bsball.service.IpLocationCacheService
 *  com.bsball.service.LbsIpLocationService
 *  com.bsball.service.LbsIpLocationService$1
 *  com.bsball.service.LbsIpLocationService$AttemptResult
 *  com.bsball.service.LbsIpLocationService$BatchResolveResult
 *  com.bsball.service.iplocation.AmapIpLocationProvider
 *  com.bsball.service.iplocation.BaiduIpLocationProvider
 *  com.bsball.service.iplocation.InterfaceBoxIpLocationProvider
 *  com.bsball.service.iplocation.IpLocationDetail
 *  com.bsball.service.iplocation.IpLocationProvider
 *  com.bsball.service.iplocation.TencentIpLocationProvider
 *  java.lang.MatchException
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.OpenPlatformProperties;
import com.bsball.model.dto.IpLocationBatchDto;
import com.bsball.model.dto.IpLocationDto;
import com.bsball.model.dto.LbsProviderOptionDto;
import com.bsball.model.enums.LbsProvider;
import com.bsball.service.IpLocationCacheService;
import com.bsball.service.LbsIpLocationService;
import com.bsball.service.iplocation.AmapIpLocationProvider;
import com.bsball.service.iplocation.BaiduIpLocationProvider;
import com.bsball.service.iplocation.InterfaceBoxIpLocationProvider;
import com.bsball.service.iplocation.IpLocationDetail;
import com.bsball.service.iplocation.IpLocationProvider;
import com.bsball.service.iplocation.TencentIpLocationProvider;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class LbsIpLocationService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(LbsIpLocationService.class);
    private final OpenPlatformProperties openPlatformProperties;
    private final IpLocationCacheService ipLocationCacheService;
    private final List<IpLocationProvider> providerChain;

    public LbsIpLocationService(OpenPlatformProperties openPlatformProperties, IpLocationCacheService ipLocationCacheService, AmapIpLocationProvider amapIpLocationProvider, BaiduIpLocationProvider baiduIpLocationProvider, TencentIpLocationProvider tencentIpLocationProvider, InterfaceBoxIpLocationProvider interfaceBoxIpLocationProvider) {
        this.openPlatformProperties = openPlatformProperties;
        this.ipLocationCacheService = ipLocationCacheService;
        this.providerChain = List.of((Object)amapIpLocationProvider, (Object)tencentIpLocationProvider, (Object)baiduIpLocationProvider, (Object)interfaceBoxIpLocationProvider);
    }

    public IpLocationDto locate(String ip) {
        if (!this.openPlatformProperties.anyProviderConfigured()) {
            return new IpLocationDto("\u672a\u914d\u7f6e IP \u5b9a\u4f4d\u5bc6\u94a5\uff08\u9ad8\u5fb7/\u767e\u5ea6/\u817e\u8baf\u81f3\u5c11\u586b\u5176\u4e00\uff09");
        }
        if (ip == null || ip.isBlank()) {
            return new IpLocationDto("");
        }
        String normalizedIp = ip.trim();
        if (!LbsIpLocationService.isPublicRoutableIp((String)normalizedIp)) {
            return new IpLocationDto("\u5185\u7f51IP");
        }
        Optional region = this.resolveRegion(normalizedIp);
        return new IpLocationDto(region.orElse("\u2014"));
    }

    public String provinceForPersist(String ip) {
        if (!this.openPlatformProperties.anyProviderConfigured()) {
            return "";
        }
        if (ip == null || ip.isBlank()) {
            return "";
        }
        String trimmed = ip.trim();
        if (!LbsIpLocationService.isPublicRoutableIp((String)trimmed)) {
            return "";
        }
        this.locate(trimmed);
        return LbsIpLocationService.stripTrailingShengForArticle((String)this.ipLocationCacheService.findProvinceRow(trimmed).orElse(""));
    }

    private static String stripTrailingShengForArticle(String province) {
        if (province == null || province.isBlank()) {
            return "";
        }
        String t = province.trim();
        return t.endsWith("\u7701") ? t.substring(0, t.length() - 1) : t;
    }

    public IpLocationBatchDto locateBatch(List<String> ips) {
        long startedAt = System.currentTimeMillis();
        if (!this.openPlatformProperties.anyProviderConfigured()) {
            return new IpLocationBatchDto(Map.of());
        }
        if (ips == null || ips.isEmpty()) {
            return new IpLocationBatchDto(Map.of());
        }
        Set uniq = ips.stream().filter(x -> x != null && !x.isBlank()).map(String::trim).collect(Collectors.toCollection(LinkedHashSet::new));
        int max = this.openPlatformProperties.maxBatchIps();
        ArrayList list = new ArrayList(uniq);
        if (list.size() > max) {
            list = new ArrayList(list.subList(0, max));
            log.warn("IP \u5f52\u5c5e\u5730\u6279\u91cf\u67e5\u8be2\u4ec5\u5904\u7406\u524d {} \u4e2a IP", (Object)max);
        }
        int total = list.size();
        int staleDays = this.openPlatformProperties.regionStaleDays();
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        Map fromDb = this.ipLocationCacheService.findValidRegionsByIps(list, staleDays);
        out.putAll(fromDb);
        int cacheHits = fromDb.size();
        int internalIpCount = 0;
        ArrayList<String> pending = new ArrayList<String>();
        for (String ip : list) {
            if (out.containsKey(ip)) continue;
            if (!LbsIpLocationService.isPublicRoutableIp((String)ip)) {
                out.put(ip, "\u5185\u7f51IP");
                ++internalIpCount;
                continue;
            }
            pending.add(ip);
        }
        BatchResolveResult batchResult = BatchResolveResult.empty();
        if (!pending.isEmpty() && !(batchResult = this.locateBatchMissesWithRetries(pending, out)).detailsByIp().isEmpty()) {
            this.ipLocationCacheService.upsertBatch(batchResult.detailsByIp());
        }
        int resolvedFromProviders = batchResult.detailsByIp().size();
        int unresolved = Math.max(0, pending.size() - resolvedFromProviders);
        log.info("IP \u6279\u91cf\u5b9a\u4f4d\u5b8c\u6210\uff1a\u603b {}\uff0c\u7f13\u5b58\u547d\u4e2d {}\uff0c\u5185\u7f51 {}\uff0c\u5916\u90e8\u6210\u529f {}\uff0c\u5916\u90e8\u5931\u8d25 {}\uff0c\u8f6e\u6b21 {}\uff0cProvider\u6210\u529f\u5206\u5e03 {}\uff0c\u8017\u65f6 {}ms", new Object[]{total, cacheHits, internalIpCount, resolvedFromProviders, unresolved, batchResult.attemptsRun(), batchResult.providerSuccessCount(), System.currentTimeMillis() - startedAt});
        return new IpLocationBatchDto(out);
    }

    private Optional<String> resolveRegion(String ip) {
        if (!LbsIpLocationService.isPublicRoutableIp((String)ip)) {
            return Optional.of("\u5185\u7f51IP");
        }
        int staleDays = this.openPlatformProperties.regionStaleDays();
        Optional fromDb = this.ipLocationCacheService.findValidRegion(ip, staleDays);
        if (fromDb.isPresent()) {
            return fromDb;
        }
        return this.fetchFromProvidersAndPersist(ip, false, null);
    }

    public Optional<String> forceFetchFromProvidersAndPersist(String ip, LbsProvider onlyProvider) {
        if (ip == null || ip.isBlank()) {
            return Optional.empty();
        }
        return this.fetchFromProvidersAndPersist(ip.trim(), true, onlyProvider);
    }

    public List<LbsProviderOptionDto> listLbsProviderOptions() {
        ArrayList<LbsProviderOptionDto> out = new ArrayList<LbsProviderOptionDto>(this.providerChain.size());
        for (IpLocationProvider p : this.providerChain) {
            LbsProvider lp = p.lbsProvider();
            out.add(new LbsProviderOptionDto(lp.name(), LbsIpLocationService.labelFor((LbsProvider)lp), p.isConfigured()));
        }
        return out;
    }

    public boolean isProviderConfigured(LbsProvider provider) {
        if (provider == null) {
            return false;
        }
        return this.providerChain.stream().filter(p -> p.lbsProvider() == provider).findFirst().map(IpLocationProvider::isConfigured).orElse(false);
    }

    private static String labelFor(LbsProvider lp) {
        return switch (1.$SwitchMap$com$bsball$model$enums$LbsProvider[lp.ordinal()]) {
            default -> throw new MatchException(null, null);
            case 1 -> "\u9ad8\u5fb7";
            case 2 -> "\u817e\u8baf";
            case 3 -> "\u767e\u5ea6";
            case 4 -> "\u63a5\u53e3\u76d2\u5b50";
        };
    }

    public static boolean isPublicRoutableIp(String ip) {
        try {
            int first;
            int second;
            InetAddress addr = InetAddress.getByName(ip);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
                return false;
            }
            byte[] b = addr.getAddress();
            if (b.length == 4 && (b[0] & 0xFF) == 100 && (second = b[1] & 0xFF) >= 64 && second <= 127) {
                return false;
            }
            return !(addr instanceof Inet6Address) || b.length != 16 || ((first = b[0] & 0xFF) & 0xFE) != 252;
        }
        catch (Exception e) {
            return false;
        }
    }

    private Optional<String> fetchFromProvidersAndPersist(String ip, boolean adminTriggeredRefresh, LbsProvider onlyProvider) {
        List toTry = onlyProvider == null ? this.providerChain.stream().filter(IpLocationProvider::isConfigured).toList() : this.providerChain.stream().filter(p -> p.lbsProvider() == onlyProvider).filter(IpLocationProvider::isConfigured).toList();
        if (toTry.isEmpty()) {
            return Optional.empty();
        }
        for (IpLocationProvider p2 : toTry) {
            Optional r = p2.locate(ip);
            if (r.isEmpty()) continue;
            IpLocationDetail detail = (IpLocationDetail)r.get();
            String formatted = detail.formatted();
            try {
                this.ipLocationCacheService.upsert(ip, detail, adminTriggeredRefresh);
            }
            catch (Exception e) {
                log.warn("\u5199\u5165 ip_location_cache \u5931\u8d25: {}", (Object)e.getMessage());
            }
            return Optional.of(formatted);
        }
        return Optional.empty();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private BatchResolveResult locateBatchMissesWithRetries(List<String> initialPending, Map<String, String> out) {
        List active = this.providerChain.stream().filter(IpLocationProvider::isConfigured).toList();
        if (active.isEmpty()) {
            return BatchResolveResult.empty();
        }
        int maxAttempts = this.openPlatformProperties.maxAttempts();
        List fullWeightedProviders = this.buildWeightedProviders(active);
        if (fullWeightedProviders.isEmpty()) {
            return BatchResolveResult.empty();
        }
        List<String> pending = new ArrayList<String>(initialPending);
        LinkedHashMap detailsByIp = new LinkedHashMap();
        LinkedHashMap providerSuccessCount = new LinkedHashMap();
        int attemptsRun = 0;
        for (int attempt = 1; attempt <= maxAttempts && !pending.isEmpty(); ++attempt) {
            attemptsRun = attempt;
            AttemptResult r = this.runOneAttempt(pending, fullWeightedProviders, active);
            Map<String, String> map = out;
            synchronized (map) {
                out.putAll(r.resolvedRegionByIp());
            }
            detailsByIp.putAll(r.resolvedDetailByIp());
            LbsIpLocationService.mergeCounter(providerSuccessCount, (Map)r.providerSuccessCount());
            log.info("IP \u6279\u91cf\u5b9a\u4f4d\u7b2c {}/{} \u8f6e\uff1a\u8f93\u5165 {}\uff0c\u6210\u529f {}\uff0c\u5f85\u91cd\u8bd5 {}", new Object[]{attempt, maxAttempts, pending.size(), r.resolvedRegionByIp().size(), r.nextPending().size()});
            pending = r.nextPending();
        }
        if (!pending.isEmpty()) {
            log.warn("IP \u6279\u91cf\u5b9a\u4f4d\u4ecd\u6709 {} \u4e2a IP \u5728 {} \u8f6e\u540e\u672a\u89e3\u6790\u6210\u529f", (Object)pending.size(), (Object)maxAttempts);
        }
        return new BatchResolveResult(detailsByIp, providerSuccessCount, attemptsRun);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private AttemptResult runOneAttempt(List<String> pending, List<IpLocationProvider> weightedProviders, List<IpLocationProvider> activeProviders) {
        HashMap<IpLocationProvider, ExecutorService> executors = new HashMap<IpLocationProvider, ExecutorService>();
        LinkedHashMap successMap = new LinkedHashMap();
        ConcurrentHashMap resolvedRegionByIp = new ConcurrentHashMap();
        ConcurrentHashMap resolvedDetailByIp = new ConcurrentHashMap();
        ConcurrentHashMap providerSuccessCount = new ConcurrentHashMap();
        ArrayList futures = new ArrayList(pending.size());
        try {
            for (IpLocationProvider p : activeProviders) {
                String name = LbsIpLocationService.providerName((IpLocationProvider)p);
                executors.put(p, Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "ip-loc-" + name);
                    t.setDaemon(true);
                    return t;
                }));
            }
            for (int i = 0; i < pending.size(); ++i) {
                String ip = pending.get(i);
                IpLocationProvider provider = weightedProviders.get(i % weightedProviders.size());
                ExecutorService executor = (ExecutorService)executors.get(provider);
                futures.add(CompletableFuture.runAsync(() -> {
                    Optional detailOpt = provider.locate(ip);
                    boolean ok = detailOpt.map(detail -> {
                        resolvedRegionByIp.put(ip, detail.formatted());
                        resolvedDetailByIp.put(ip, detail);
                        providerSuccessCount.merge(LbsIpLocationService.providerName((IpLocationProvider)provider), 1, Integer::sum);
                        return true;
                    }).orElse(false);
                    Map map = successMap;
                    synchronized (map) {
                        successMap.put(ip, ok);
                    }
                }, executor));
            }
            CompletableFuture.allOf((CompletableFuture[])futures.toArray(CompletableFuture[]::new)).join();
        }
        finally {
            for (ExecutorService executor : executors.values()) {
                executor.shutdown();
                try {
                    if (executor.awaitTermination(5L, TimeUnit.SECONDS)) continue;
                    executor.shutdownNow();
                }
                catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        ArrayList<String> nextPending = new ArrayList<String>();
        for (String ip : pending) {
            if (Boolean.TRUE.equals(successMap.get(ip))) continue;
            nextPending.add(ip);
        }
        return new AttemptResult(nextPending, resolvedRegionByIp, resolvedDetailByIp, providerSuccessCount);
    }

    private List<IpLocationProvider> buildWeightedProviders(List<IpLocationProvider> active) {
        ArrayList<IpLocationProvider> weighted = new ArrayList<IpLocationProvider>();
        for (IpLocationProvider provider : active) {
            int weight = Math.max(1, this.providerQps(provider));
            for (int i = 0; i < weight; ++i) {
                weighted.add(provider);
            }
        }
        return weighted;
    }

    private int providerQps(IpLocationProvider provider) {
        if (provider instanceof AmapIpLocationProvider) {
            OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.openPlatformProperties.getLbs().getProviders().get("amap");
            return Math.max(1, p != null ? p.getIpLocationApi().getMaxQps() : 1);
        }
        if (provider instanceof TencentIpLocationProvider) {
            OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.openPlatformProperties.getLbs().getProviders().get("tencent");
            return Math.max(1, p != null ? p.getIpLocationApi().getMaxQps() : 1);
        }
        if (provider instanceof BaiduIpLocationProvider) {
            OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.openPlatformProperties.getLbs().getProviders().get("baidu");
            return Math.max(1, p != null ? p.getIpLocationApi().getMaxQps() : 1);
        }
        if (provider instanceof InterfaceBoxIpLocationProvider) {
            OpenPlatformProperties.Provider p = (OpenPlatformProperties.Provider)this.openPlatformProperties.getLbs().getProviders().get("interface-box");
            return Math.max(1, p != null ? p.getIpLocationApi().getMaxQps() : 1);
        }
        return 1;
    }

    private static String providerName(IpLocationProvider provider) {
        if (provider instanceof AmapIpLocationProvider) {
            return "AMAP";
        }
        if (provider instanceof TencentIpLocationProvider) {
            return "TENCENT";
        }
        if (provider instanceof BaiduIpLocationProvider) {
            return "BAIDU";
        }
        if (provider instanceof InterfaceBoxIpLocationProvider) {
            return "INTERFACE_BOX";
        }
        return provider.getClass().getSimpleName();
    }

    private static void mergeCounter(Map<String, Integer> to, Map<String, Integer> from) {
        for (Map.Entry<String, Integer> e : from.entrySet()) {
            to.merge(e.getKey(), e.getValue(), Integer::sum);
        }
    }
}

