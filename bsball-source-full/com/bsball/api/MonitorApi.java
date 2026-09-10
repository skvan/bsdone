/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.MonitorApi
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.core.DruidEntryTokenCache
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.MonitorService
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.core.DruidEntryTokenCache;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.MonitorService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/sys/monitor"})
public class MonitorApi {
    private final MonitorService monitorService;
    private final ApiPermissionService apiPermissionService;
    private final Optional<DruidEntryTokenCache> druidEntryTokenCache;
    @Value(value="${server.servlet.context-path:/api}")
    private String contextPath;

    @GetMapping(value={"/datasource/url"})
    public Result<Map<String, String>> getDatasourceMonitorUrl() {
        String url = this.contextPath + "/druid/login.html";
        Long userId = CurrentUserHolder.get();
        if (userId != null && this.druidEntryTokenCache.isPresent() && this.apiPermissionService.isSuperAdmin(userId)) {
            String token = UUID.randomUUID().toString();
            ((DruidEntryTokenCache)this.druidEntryTokenCache.get()).put(token);
            url = this.contextPath + "/druid-entry?token=" + token;
        }
        return Result.ok((Object)Map.of((Object)"url", (Object)url, (Object)"tip", (Object)""));
    }

    @GetMapping(value={"/server"})
    public Result<Map<String, Object>> getServerInfo() {
        return Result.ok((Object)this.monitorService.getServerInfo());
    }

    @GetMapping(value={"/cache"})
    public Result<Map<String, Object>> getCacheInfo() {
        return Result.ok((Object)this.monitorService.getCacheInfo());
    }

    @GetMapping(value={"/cache/keys"})
    public Result<Map<String, Object>> getCacheKeys(@RequestParam String cacheName, @RequestParam(required=false) String pattern, @RequestParam(required=false) Integer limit) {
        return Result.ok((Object)this.monitorService.getCacheKeys(cacheName, pattern, limit));
    }

    @GetMapping(value={"/cache/value"})
    public Result<Map<String, Object>> getCacheValue(@RequestParam String cacheName, @RequestParam String key) {
        return Result.ok((Object)this.monitorService.getCacheValue(cacheName, key));
    }

    @DeleteMapping(value={"/cache/key"})
    public Result<Map<String, Object>> removeCacheKey(@RequestParam String cacheName, @RequestParam String key) {
        return Result.ok((Object)this.monitorService.removeCacheKey(cacheName, key));
    }

    @DeleteMapping(value={"/cache/clear"})
    public Result<Map<String, Object>> clearCache(@RequestParam String cacheName) {
        return Result.ok((Object)this.monitorService.clearCache(cacheName));
    }

    @Generated
    public MonitorApi(MonitorService monitorService, ApiPermissionService apiPermissionService, Optional<DruidEntryTokenCache> druidEntryTokenCache) {
        this.monitorService = monitorService;
        this.apiPermissionService = apiPermissionService;
        this.druidEntryTokenCache = druidEntryTokenCache;
    }
}

