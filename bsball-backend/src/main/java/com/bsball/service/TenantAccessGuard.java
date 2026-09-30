/*
 * 账号权限重构（批次 3b，Task 3.12）：租户退租封禁的可用性守卫（spec §6.8）。
 *
 * 职责：
 *  - requireActive(tenantId)：统一「存在 + 未软删 + 启用」判定；软删/停用/不存在一律视为不可用，
 *    不可用即抛 403「租户已停止运营」；
 *  - Caffeine 缓存（TTL 可配，默认 60s）：正常读走 cache.get(key, loader)，命中不查库；
 *    退租/续租动作显式 evict，保证封禁与恢复即时生效（下次判定重查库）；
 *  - 三类请求级校验的纯函数决策（decideCodeHeader / decideStrictTenant），供 ApiPermissionFilter 调用与单测复用：
 *      码通路不可用 → 404「租户不存在」（终态，不落默认回退）；
 *      ID 头 / JWT 回退通路非超管且不可用 → 403「租户已停止运营」（超管豁免，保证平台方归档查看）。
 */
package com.bsball.service;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysTenant;
import com.bsball.repository.SysTenantRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessGuard {

    /** 请求级租户可用性决策：作为响应码与文案的单一来源。 */
    public enum Decision {
        ALLOW(0, null),
        TENANT_NOT_FOUND(404, "租户不存在"),
        TENANT_STOPPED(403, "租户已停止运营");

        private final int code;
        private final String msg;

        Decision(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        public int code() {
            return this.code;
        }

        public String msg() {
            return this.msg;
        }

        public boolean rejected() {
            return this.code != 0;
        }
    }

    /** 缺省缓存 TTL（秒）；Spring 环境由 app.tenant.access-cache-ttl-sec 覆盖。 */
    private static final int DEFAULT_TTL_SEC = 60;

    private final SysTenantRepository sysTenantRepository;

    @Value(value="${app.tenant.access-cache-ttl-sec:60}")
    private int accessCacheTtlSec = DEFAULT_TTL_SEC;

    private Cache<Long, Boolean> tenantActiveCache;

    @PostConstruct
    void initTenantAccessCache() {
        int ttl = Math.max(5, this.accessCacheTtlSec);
        this.tenantActiveCache = Caffeine.newBuilder()
                .expireAfterWrite((long)ttl, TimeUnit.SECONDS)
                .maximumSize(20000L)
                .build();
    }

    /** 租户是否可用：存在 + 未软删 + 启用。null 视为不可用。 */
    public boolean isActive(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        Cache<Long, Boolean> cache = this.tenantActiveCache;
        if (cache == null) {
            return this.loadActive(tenantId);
        }
        return Boolean.TRUE.equals(cache.get(tenantId, this::loadActive));
    }

    private boolean loadActive(Long tenantId) {
        return this.sysTenantRepository.findById(tenantId)
                .filter(t -> t.getDeletedAt() == null && t.isActive())
                .isPresent();
    }

    /** 校验租户可用，不可用（停用/软删/不存在）抛 403「租户已停止运营」。 */
    public void requireActive(Long tenantId) {
        if (!this.isActive(tenantId)) {
            throw new BusinessException(403, Decision.TENANT_STOPPED.msg());
        }
    }

    /** 停用/启用（退租/续租）后失效缓存，保证下次判定即时反映库中状态。 */
    public void evict(Long tenantId) {
        if (tenantId != null && this.tenantActiveCache != null) {
            this.tenantActiveCache.invalidate(tenantId);
        }
    }

    public void evictAll() {
        if (this.tenantActiveCache != null) {
            this.tenantActiveCache.invalidateAll();
        }
    }

    /**
     * 码通路决策：无码 → ALLOW（不干预回退链）；
     * 带码但不可用（解析不到/软删/停用）→ 404「租户不存在」（终态，调用方须直接拒绝、不再回退）。
     */
    public static Decision decideCodeHeader(String code, Optional<SysTenant> resolved) {
        if (code == null || code.isBlank()) {
            return Decision.ALLOW;
        }
        boolean usable = resolved != null && resolved.isPresent()
                && resolved.get().getDeletedAt() == null && resolved.get().isActive();
        return usable ? Decision.ALLOW : Decision.TENANT_NOT_FOUND;
    }

    /** ID 头 / JWT 回退通路决策：超管豁免；非超管且不可用 → 403「租户已停止运营」。 */
    public static Decision decideStrictTenant(boolean superAdmin, boolean tenantActive) {
        return superAdmin || tenantActive ? Decision.ALLOW : Decision.TENANT_STOPPED;
    }

    @Generated
    public TenantAccessGuard(SysTenantRepository sysTenantRepository) {
        this.sysTenantRepository = sysTenantRepository;
    }
}
