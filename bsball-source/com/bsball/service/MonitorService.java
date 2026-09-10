/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.service.MonitorService
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.stats.CacheStats
 *  org.springframework.cache.Cache
 *  org.springframework.cache.CacheManager
 *  org.springframework.cache.caffeine.CaffeineCache
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class MonitorService {
    private static final ObjectMapper CACHE_SIZE_MAPPER = new ObjectMapper();
    private static final int CACHE_MEMORY_SAMPLE_MAX = 200;
    private final Optional<CacheManager> cacheManager;
    private static final Map<String, String> CACHE_REMARKS = Map.of((Object)"config", (Object)"\u7cfb\u7edf\u914d\u7f6e", (Object)"sys_dict", (Object)"\u6570\u636e\u5b57\u5178");

    public MonitorService(Optional<CacheManager> cacheManager) {
        this.cacheManager = cacheManager != null ? cacheManager : Optional.empty();
    }

    public Map<String, Object> getServerInfo() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        this.fillCpuInfo(map);
        this.fillMemoryInfo(map);
        this.fillServerInfo(map);
        this.fillJvmInfo(map);
        this.fillDiskInfo(map);
        return map;
    }

    private void fillCpuInfo(Map<String, Object> map) {
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        int processors = os.getAvailableProcessors();
        double systemCpuLoad = -1.0;
        if (os instanceof OperatingSystemMXBean) {
            OperatingSystemMXBean sunOs = (OperatingSystemMXBean)os;
            try {
                systemCpuLoad = sunOs.getSystemCpuLoad();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        double load = systemCpuLoad >= 0.0 ? systemCpuLoad : 0.0;
        double idleRate = Math.max(0.0, 1.0 - load);
        LinkedHashMap<String, Integer> cpu = new LinkedHashMap<String, Integer>();
        cpu.put("coreCount", processors);
        cpu.put("userUsagePercent", systemCpuLoad >= 0.0 ? Integer.valueOf((int)Math.round(load * 100.0)) : null);
        cpu.put("systemUsagePercent", 0);
        cpu.put("idleRatePercent", systemCpuLoad >= 0.0 ? Integer.valueOf((int)Math.round(idleRate * 100.0)) : null);
        map.put("cpu", cpu);
    }

    private void fillMemoryInfo(Map<String, Object> map) {
        long totalPhysical = -1L;
        long freePhysical = -1L;
        java.lang.management.OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMXBean instanceof OperatingSystemMXBean) {
            OperatingSystemMXBean sunOs = (OperatingSystemMXBean)operatingSystemMXBean;
            try {
                totalPhysical = sunOs.getTotalPhysicalMemorySize();
                freePhysical = sunOs.getFreePhysicalMemorySize();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        long totalMemory = rt.totalMemory();
        long freeMemory = rt.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double jvmUsageRate = maxMemory > 0L ? (double)usedMemory / (double)maxMemory : 0.0;
        LinkedHashMap<String, Number> jvm = new LinkedHashMap<String, Number>();
        jvm.put("maxMemory", maxMemory);
        jvm.put("totalMemory", totalMemory);
        jvm.put("freeMemory", freeMemory);
        jvm.put("usedMemory", usedMemory);
        jvm.put("maxMemoryMB", maxMemory / 0x100000L);
        jvm.put("totalMemoryMB", totalMemory / 0x100000L);
        jvm.put("usedMemoryMB", usedMemory / 0x100000L);
        jvm.put("freeMemoryMB", freeMemory / 0x100000L);
        jvm.put("usageRatePercent", (double)Math.round(jvmUsageRate * 10000.0) / 100.0);
        map.put("jvm", jvm);
        if (totalPhysical >= 0L && freePhysical >= 0L) {
            long usedPhysical = totalPhysical - freePhysical;
            double physicalUsageRate = totalPhysical > 0L ? (double)usedPhysical / (double)totalPhysical : 0.0;
            LinkedHashMap<String, Number> mem = new LinkedHashMap<String, Number>();
            mem.put("totalMB", totalPhysical / 0x100000L);
            mem.put("usedMB", usedPhysical / 0x100000L);
            mem.put("freeMB", freePhysical / 0x100000L);
            mem.put("usageRatePercent", (double)Math.round(physicalUsageRate * 10000.0) / 100.0);
            map.put("physicalMemory", mem);
        }
    }

    private void fillServerInfo(Map<String, Object> map) {
        String serverName = "";
        try {
            InetAddress addr = InetAddress.getLocalHost();
            serverName = addr.getHostName();
        }
        catch (UnknownHostException addr) {
            // empty catch block
        }
        List ips = MonitorService.collectNonLoopbackIpv4Addresses();
        String serverIp = ips.isEmpty() ? "" : String.join((CharSequence)",", ips);
        java.lang.management.OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        LinkedHashMap<String, String> server = new LinkedHashMap<String, String>();
        server.put("name", serverName);
        server.put("ip", serverIp);
        server.put("osName", os.getName());
        server.put("arch", os.getArch());
        server.put("osVersion", os.getVersion());
        map.put("server", server);
    }

    private static List<String> collectNonLoopbackIpv4Addresses() {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                if (!nic.isUp() || nic.isLoopback()) continue;
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    String host;
                    InetAddress a = addrs.nextElement();
                    if (a.isLoopbackAddress() || a.isLinkLocalAddress() || !(a instanceof Inet4Address) || (host = a.getHostAddress()) == null || host.isBlank()) continue;
                    set.add(host);
                }
            }
        }
        catch (Exception nics) {
            // empty catch block
        }
        if (set.isEmpty()) {
            try {
                InetAddress local = InetAddress.getLocalHost();
                if (local != null && !local.isLoopbackAddress()) {
                    set.add(local.getHostAddress());
                }
            }
            catch (UnknownHostException local) {
                // empty catch block
            }
        }
        ArrayList<String> list = new ArrayList<String>(set);
        Collections.sort(list);
        return list;
    }

    private void fillJvmInfo(Map<String, Object> map) {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        long uptime = rb.getUptime();
        long seconds = uptime / 1000L;
        long days = seconds / 86400L;
        long hours = seconds % 86400L / 3600L;
        long minutes = seconds % 3600L / 60L;
        String uptimeStr = String.format("%d\u5929%d\u5c0f\u65f6%d\u5206\u949f", days, hours, minutes);
        LinkedHashMap<String, Object> jvmInfo = new LinkedHashMap<String, Object>();
        jvmInfo.put("name", rb.getVmName());
        jvmInfo.put("version", System.getProperty("java.version", ""));
        jvmInfo.put("startTime", rb.getStartTime());
        jvmInfo.put("uptime", uptime);
        jvmInfo.put("uptimeSeconds", seconds);
        jvmInfo.put("uptimeFormatted", uptimeStr);
        jvmInfo.put("javaHome", System.getProperty("java.home", ""));
        jvmInfo.put("projectDir", System.getProperty("user.dir", ""));
        try {
            jvmInfo.put("inputArguments", rb.getInputArguments());
        }
        catch (Throwable t) {
            jvmInfo.put("inputArguments", List.of());
        }
        map.put("runtime", jvmInfo);
    }

    private void fillDiskInfo(Map<String, Object> map) {
        File[] roots = File.listRoots();
        if (roots == null || roots.length == 0) {
            map.put("disks", List.of());
            return;
        }
        boolean isWindows = MonitorService.isWindowsOs();
        ArrayList disks = new ArrayList();
        for (File root : roots) {
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            if (total <= 0L) continue;
            long used = total - free;
            double usedPercent = (double)used * 100.0 / (double)total;
            String fileSystem = "";
            String driveType = "";
            try {
                Path path = root.toPath();
                FileStore store = Files.getFileStore(path);
                fileSystem = store.type();
                driveType = MonitorService.resolveDriveType((FileStore)store, (boolean)isWindows);
            }
            catch (Throwable path) {
                // empty catch block
            }
            LinkedHashMap<String, Object> d = new LinkedHashMap<String, Object>();
            d.put("path", root.getPath());
            d.put("fileSystem", fileSystem);
            d.put("driveType", driveType);
            d.put("totalGB", (double)Math.round((double)total / 1.073741824E9 * 100.0) / 100.0);
            d.put("freeGB", (double)Math.round((double)free / 1.073741824E9 * 100.0) / 100.0);
            d.put("usedGB", (double)Math.round((double)used / 1.073741824E9 * 100.0) / 100.0);
            d.put("usedPercent", (double)Math.round(usedPercent * 100.0) / 100.0);
            disks.add(d);
        }
        map.put("disks", disks);
    }

    private static boolean isWindowsOs() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().startsWith("windows");
    }

    private static String resolveDriveType(FileStore store, boolean isWindows) {
        try {
            String s;
            Object volumeType = store.getAttribute("volume:volumeType");
            if (volumeType != null && !(s = volumeType.toString().trim()).isEmpty()) {
                return s;
            }
        }
        catch (Throwable volumeType) {
            // empty catch block
        }
        try {
            Boolean isCdrom = (Boolean)store.getAttribute("volume:isCdrom");
            if (Boolean.TRUE.equals(isCdrom)) {
                return isWindows ? "\u5149\u9a71" : "CD-ROM";
            }
        }
        catch (Throwable isCdrom) {
            // empty catch block
        }
        try {
            Boolean isRemovable = (Boolean)store.getAttribute("volume:isRemovable");
            if (Boolean.TRUE.equals(isRemovable)) {
                return isWindows ? "\u53ef\u79fb\u52a8\u78c1\u76d8" : "Removable";
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return isWindows ? "\u672c\u5730\u78c1\u76d8" : "";
    }

    public Map<String, Object> getCacheInfo() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        if (this.cacheManager.isEmpty()) {
            result.put("type", "none");
            result.put("cacheNames", List.of());
            result.put("caches", List.of());
            return result;
        }
        CacheManager cm = (CacheManager)this.cacheManager.get();
        String type = cm.getClass().getSimpleName().toLowerCase().contains("redis") ? "redis" : "local";
        result.put("type", type);
        Collection names = cm.getCacheNames();
        result.put("cacheNames", names == null ? List.of() : new ArrayList(names));
        result.put("memoryHint", "approxMemoryMB \u4e3a\u6309\u91c7\u6837\u6761\u76ee\u4f30\u7b97\u7684\u5806\u5360\u7528\u91cf\u7ea7\uff0c\u975e\u7cbe\u786e\u503c\uff1bRedis \u6a21\u5f0f\u65e0\u6b64\u9879\u4e0e\u547d\u4e2d\u7edf\u8ba1\u3002");
        ArrayList caches = new ArrayList();
        if (names != null) {
            for (String name : names) {
                Cache cache = cm.getCache(name);
                if (cache == null) continue;
                LinkedHashMap<String, Object> stat = new LinkedHashMap<String, Object>();
                stat.put("name", name);
                stat.put("remark", CACHE_REMARKS.getOrDefault(name, ""));
                if (cache instanceof CaffeineCache) {
                    CaffeineCache caffeineCache = (CaffeineCache)cache;
                    com.github.benmanes.caffeine.cache.Cache nativeCache = caffeineCache.getNativeCache();
                    long est = nativeCache.estimatedSize();
                    stat.put("estimatedSize", est);
                    CacheStats stats = nativeCache.stats();
                    stat.put("hitCount", stats.hitCount());
                    stat.put("missCount", stats.missCount());
                    stat.put("loadSuccessCount", stats.loadSuccessCount());
                    stat.put("loadFailureCount", stats.loadFailureCount());
                    long approxBytes = MonitorService.estimateCaffeineCacheMemoryBytes((com.github.benmanes.caffeine.cache.Cache)nativeCache, (long)est);
                    stat.put("approxMemoryBytes", approxBytes);
                    stat.put("approxMemoryMB", approxBytes <= 0L ? 0.0 : (double)Math.round((double)approxBytes / 1048576.0 * 100.0) / 100.0);
                } else {
                    stat.put("statsNote", "\u975e\u672c\u5730 Caffeine \u65f6\u65e0\u547d\u4e2d\u7edf\u8ba1\uff1bRedis \u8bf7\u7528 Redis \u76d1\u63a7");
                }
                caches.add(stat);
            }
        }
        result.put("caches", caches);
        return result;
    }

    public Map<String, Object> getCacheKeys(String cacheName, String pattern, Integer limit) {
        int max;
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        int n = max = limit != null && limit > 0 ? Math.min(limit, 1000) : 100;
        if (this.cacheManager.isEmpty()) {
            result.put("keys", List.of());
            result.put("total", 0);
            return result;
        }
        Cache cache = ((CacheManager)this.cacheManager.get()).getCache(cacheName);
        if (cache == null) {
            result.put("keys", List.of());
            result.put("total", 0);
            result.put("message", "\u7f13\u5b58\u4e0d\u5b58\u5728");
            return result;
        }
        if (!(cache instanceof CaffeineCache)) {
            result.put("keys", List.of());
            result.put("total", -1);
            result.put("message", "\u4ec5\u652f\u6301\u672c\u5730 Caffeine \u7f13\u5b58\u952e\u5217\u8868");
            return result;
        }
        CaffeineCache caffeineCache = (CaffeineCache)cache;
        Set keys = caffeineCache.getNativeCache().asMap().keySet();
        List keyList = keys.stream().map(Objects::toString).filter(k -> pattern == null || pattern.isBlank() || k.contains(pattern)).limit(max).collect(Collectors.toList());
        long total = keys.size();
        if (pattern != null && !pattern.isBlank()) {
            total = keys.stream().map(Objects::toString).filter(k -> k.contains(pattern)).count();
        }
        result.put("keys", keyList);
        result.put("total", total);
        result.put("returned", keyList.size());
        return result;
    }

    public Map<String, Object> getCacheValue(String cacheName, String key) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("cacheName", cacheName);
        result.put("key", key);
        result.put("value", "");
        if (this.cacheManager.isEmpty()) {
            result.put("message", "\u672a\u542f\u7528\u7f13\u5b58");
            return result;
        }
        Cache cache = ((CacheManager)this.cacheManager.get()).getCache(cacheName);
        if (cache == null) {
            result.put("message", "\u7f13\u5b58\u4e0d\u5b58\u5728");
            return result;
        }
        if (!(cache instanceof CaffeineCache)) {
            result.put("message", "\u4ec5\u652f\u6301\u672c\u5730 Caffeine \u7f13\u5b58");
            return result;
        }
        CaffeineCache caffeineCache = (CaffeineCache)cache;
        Object val = caffeineCache.getNativeCache().getIfPresent((Object)key);
        if (val == null) {
            result.put("value", "");
            result.put("message", "\u952e\u4e0d\u5b58\u5728\u6216\u5df2\u8fc7\u671f");
            return result;
        }
        String valueStr = MonitorService.formatCacheValue((Object)val);
        result.put("value", valueStr);
        return result;
    }

    private static String formatCacheValue(Object val) {
        if (val == null) {
            return "";
        }
        if (val instanceof String) {
            String s = (String)val;
            return s;
        }
        if (val instanceof Collection || val instanceof Map) {
            try {
                return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(val);
            }
            catch (Exception e) {
                return val.toString();
            }
        }
        return val.toString();
    }

    public Map<String, Object> removeCacheKey(String cacheName, String key) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        if (this.cacheManager.isEmpty()) {
            result.put("ok", false);
            result.put("message", "\u672a\u542f\u7528\u7f13\u5b58");
            return result;
        }
        Cache cache = ((CacheManager)this.cacheManager.get()).getCache(cacheName);
        if (cache == null) {
            result.put("ok", false);
            result.put("message", "\u7f13\u5b58\u4e0d\u5b58\u5728");
            return result;
        }
        if (!(cache instanceof CaffeineCache)) {
            result.put("ok", false);
            result.put("message", "\u4ec5\u652f\u6301\u672c\u5730 Caffeine \u7f13\u5b58");
            return result;
        }
        CaffeineCache caffeineCache = (CaffeineCache)cache;
        caffeineCache.getNativeCache().invalidate((Object)key);
        result.put("ok", true);
        return result;
    }

    public Map<String, Object> clearCache(String cacheName) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        if (this.cacheManager.isEmpty()) {
            result.put("ok", false);
            result.put("message", "\u672a\u542f\u7528\u7f13\u5b58");
            return result;
        }
        Cache cache = ((CacheManager)this.cacheManager.get()).getCache(cacheName);
        if (cache == null) {
            result.put("ok", false);
            result.put("message", "\u7f13\u5b58\u4e0d\u5b58\u5728");
            return result;
        }
        if (!(cache instanceof CaffeineCache)) {
            result.put("ok", false);
            result.put("message", "\u4ec5\u652f\u6301\u672c\u5730 Caffeine \u7f13\u5b58");
            return result;
        }
        CaffeineCache caffeineCache = (CaffeineCache)cache;
        caffeineCache.getNativeCache().invalidateAll();
        result.put("ok", true);
        return result;
    }

    private static long estimateCaffeineCacheMemoryBytes(com.github.benmanes.caffeine.cache.Cache<?, ?> nativeCache, long estimatedSize) {
        if (estimatedSize <= 0L) {
            return 0L;
        }
        long sampleCap = Math.min(estimatedSize, 200L);
        long sum = 0L;
        int n = 0;
        for (Map.Entry e : nativeCache.asMap().entrySet()) {
            sum += MonitorService.roughKeyValueBytes(e.getKey(), e.getValue());
            if ((long)(++n) < sampleCap) continue;
            break;
        }
        if (n == 0) {
            return 0L;
        }
        long avg = sum / (long)n;
        return avg * estimatedSize;
    }

    private static long roughKeyValueBytes(Object k, Object v) {
        return MonitorService.roughObjectBytes((Object)k) + MonitorService.roughObjectBytes((Object)v);
    }

    private static long roughObjectBytes(Object o) {
        if (o == null) {
            return 0L;
        }
        if (o instanceof byte[]) {
            byte[] b = (byte[])o;
            return b.length;
        }
        if (o instanceof String) {
            String s = (String)o;
            return (long)s.length() * 2L;
        }
        if (o instanceof Number || o instanceof Boolean || o instanceof Enum) {
            return 32L;
        }
        try {
            return CACHE_SIZE_MAPPER.writeValueAsBytes(o).length;
        }
        catch (Exception e) {
            String t = String.valueOf(o);
            return (long)t.length() * 2L;
        }
    }
}

