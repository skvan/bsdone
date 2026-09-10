/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.config.TenantProperties$HostMapping
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.service.TenantResolutionService
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  jakarta.annotation.PostConstruct
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 *  org.springframework.util.StringUtils
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.SysTenant;
import com.bsball.repository.SysTenantRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantResolutionService {
    private final SysTenantRepository sysTenantRepository;
    private final TenantProperties tenantProperties;
    @Value(value="${app.tenant.code-cache-ttl-sec:60}")
    private int tenantCodeCacheTtlSec;
    private Cache<String, Optional<SysTenant>> tenantByCodeCache;

    @PostConstruct
    void initTenantCodeCache() {
        int ttl = Math.max(5, this.tenantCodeCacheTtlSec);
        this.tenantByCodeCache = Caffeine.newBuilder().expireAfterWrite((long)ttl, TimeUnit.SECONDS).maximumSize(2000L).build();
    }

    public Optional<SysTenant> findTenantByCodeCached(String code) {
        if (!StringUtils.hasText((String)code)) {
            return Optional.empty();
        }
        String lookup = code.trim();
        String key = lookup.toLowerCase(Locale.ROOT);
        return (Optional)this.tenantByCodeCache.get((Object)key, k -> this.sysTenantRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(lookup));
    }

    public Long resolve(HttpServletRequest req) {
        if (req == null) {
            return null;
        }
        Long byNumericHeader = this.resolveByNumericTenantIdHeader(req);
        if (byNumericHeader != null) {
            return byNumericHeader;
        }
        Long byCode = this.resolveByCodeHeader(req);
        if (byCode != null) {
            return byCode;
        }
        Long byParam = this.resolveByQueryParam(req);
        if (byParam != null) {
            return byParam;
        }
        return this.resolveByHost(req.getServerName());
    }

    public Long resolveFromRefererHeader(HttpServletRequest req) {
        if (req == null) {
            return null;
        }
        String ref = req.getHeader("Referer");
        if (!StringUtils.hasText((String)ref)) {
            return null;
        }
        try {
            URI uri = URI.create(ref.trim());
            String path = uri.getPath();
            return this.resolveTenantIdFromPathFirstSegment(path);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    public Long resolveTenantIdFromPathFirstSegment(String path) {
        int i;
        String seg;
        if (!StringUtils.hasText((String)path)) {
            return null;
        }
        Object p = path.trim();
        if (!((String)p).startsWith("/")) {
            p = "/" + (String)p;
        }
        String string = seg = (i = ((String)p).indexOf(47, 1)) < 0 ? ((String)p).substring(1) : ((String)p).substring(1, i);
        if (!StringUtils.hasText((String)seg)) {
            return null;
        }
        return this.findIdByCode(seg.trim());
    }

    private Long resolveByNumericTenantIdHeader(HttpServletRequest req) {
        long id;
        String h = req.getHeader("X-Tenant-Id");
        if (!StringUtils.hasText((String)h)) {
            return null;
        }
        try {
            id = Long.parseLong(h.trim());
        }
        catch (NumberFormatException e) {
            return null;
        }
        return this.sysTenantRepository.findById((Object)id).filter(t -> t.getDeletedAt() == null && t.isActive()).map(BaseEntity::getId).orElse(null);
    }

    private Long resolveByCodeHeader(HttpServletRequest req) {
        String h = req.getHeader("X-Tenant-Code");
        if (!StringUtils.hasText((String)h)) {
            return null;
        }
        return this.findIdByCode(h.trim());
    }

    private Long resolveByQueryParam(HttpServletRequest req) {
        String q = req.getParameter("tenantCode");
        if (!StringUtils.hasText((String)q)) {
            return null;
        }
        return this.findIdByCode(q.trim());
    }

    private Long findIdByCode(String code) {
        return this.findTenantByCodeCached(code).filter(SysTenant::isActive).map(BaseEntity::getId).orElse(null);
    }

    private Long resolveByHost(String serverName) {
        TenantProperties.HostMapping hm = this.tenantProperties.getHostMapping();
        if (hm == null || !hm.isEnabled() || !StringUtils.hasText((String)hm.getBaseHost())) {
            return null;
        }
        String base = hm.getBaseHost().trim().toLowerCase();
        if (serverName == null || serverName.isBlank()) {
            return null;
        }
        String host = serverName.trim().toLowerCase();
        if (host.equals(base)) {
            return null;
        }
        String suffix = "." + base;
        if (!host.endsWith(suffix)) {
            return null;
        }
        String prefix = host.substring(0, host.length() - suffix.length());
        if (prefix.isEmpty() || prefix.contains(".")) {
            return null;
        }
        if ("www".equals(prefix)) {
            return null;
        }
        return this.findIdByCode(prefix);
    }

    @Generated
    public TenantResolutionService(SysTenantRepository sysTenantRepository, TenantProperties tenantProperties) {
        this.sysTenantRepository = sysTenantRepository;
        this.tenantProperties = tenantProperties;
    }
}

