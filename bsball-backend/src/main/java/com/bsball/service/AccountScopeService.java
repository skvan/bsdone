/*
 * 账号权限重构（批次 1）：统一范围中枢（AccountScopeService）。
 *
 * 职责：把「关系归属 / 遗留 sys_data_scope / 超管与租户管理员」统一收敛为
 * EffectiveScope（T1.3 读写双语义值对象），供范围查询与守卫消费。
 *  - 关系驱动：ScopeRelationProvider（T1.4）各实现按账号类型注入联盟 / 球队 ID，新增账号类型只在此扩展；
 *  - 遗留兼容：sys_data_scope 若存在行则按行展开（含 INCLUDE_DESCENDANTS 批量展开）；
 *  - 语义铁律：门户角色（member / team_manager / league_organizer）无归属即空域，绝不回退不受限；
 *    legacy strict 仅影响「遗留自定义角色账号」；超管 / 租户管理员租户内不受限。
 *
 * 本类为统一范围中枢，新代码一律使用本类；遗留 DataScopeService 将随迁移逐步退场。
 *
 * 设计细化（本任务引入）：
 *  Caffeine 缓存的是与请求无关的核心范围 ScopeCore（不含 guestLikeRead 请求级标志），
 *  resolve 每次按当前请求现装 EffectiveScope。原因：guestLikeRead 是请求级标志
 *  （同一用户可能先在门户只读、再走管理读），若把整个 EffectiveScope 缓存，
 *  会把首个请求的标志泄漏到后续 scopeCacheTtlSec（默认 30s）内的请求。故核心范围跨请求复用，
 *  请求级标志每次现算。ScopeCore 见文件末尾 record。
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.core.GuestPublicApiHolder;
import com.bsball.model.dto.EffectiveScope;
import com.bsball.model.entity.SysDataScope;
import com.bsball.repository.SysDataScopeRepository;
import com.bsball.repository.TeamRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.scope.ScopeRelationProvider;
import com.bsball.service.scope.ScopeResolutionContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AccountScopeService {

    private static final Set<Long> EMPTY = Set.of();
    private static final ThreadLocal<Map<String, EffectiveScope>> REQ_CACHE = ThreadLocal.withInitial(HashMap::new);

    private final ApiPermissionService apiPermissionService;
    private final TenantProperties tenantProperties;
    private final List<ScopeRelationProvider> providers;
    private final SysDataScopeRepository sysDataScopeRepository;
    private final TeamRepository teamRepository;

    @Value(value = "${app.scope.cache-ttl-sec:30}")
    private int scopeCacheTtlSec;
    private Cache<String, ScopeCore> scopeCache;

    @PostConstruct
    void initScopeCache() {   // 包级可见（T1.8 单测需直接调用，沿用 ApiPermissionService.initGuestPermissionCache 风格）
        int ttl = Math.max(5, this.scopeCacheTtlSec);
        this.scopeCache = Caffeine.newBuilder().expireAfterWrite((long) ttl, TimeUnit.SECONDS).maximumSize(20000L).build();
    }

    public EffectiveScope resolveCurrent() {
        Long tenantId = CurrentUserHolder.getTenantId();
        long tid = tenantId == null ? 0L : tenantId.longValue();
        return resolve(CurrentUserHolder.get(), tid);
    }

    public EffectiveScope resolve(Long userId, long tenantId) {
        boolean guestLike = GuestPublicApiHolder.isGuestLikeRead();
        if (userId == null) {
            return EffectiveScope.restricted(guestLike, EMPTY, EMPTY);
        }
        String cacheKey = userId + "#" + tenantId;
        Map<String, EffectiveScope> req = REQ_CACHE.get();
        EffectiveScope cached = req.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        // 设计细化：Caffeine 缓存“与请求无关”的核心范围（ScopeCore，不含 guestLikeRead 请求级标志），
        // 每次请求装配 EffectiveScope 时再注入当前请求的 guestLike —— 避免首个请求的标志被 30s 缓存跨请求复用。
        ScopeCore core = this.scopeCache.get(cacheKey, k -> resolveCore(userId, tenantId));
        EffectiveScope out = core.unrestricted() ? EffectiveScope.unrestricted()
                : EffectiveScope.restricted(guestLike, core.leagueIds(), core.teamIds());
        req.put(cacheKey, out);
        return out;
    }

    private ScopeCore resolveCore(Long userId, long tenantId) {
        if (apiPermissionService.isSuperAdmin(userId)) return new ScopeCore(true, EMPTY, EMPTY);
        if (apiPermissionService.isTenantAdmin(userId)) return new ScopeCore(true, EMPTY, EMPTY);

        // 1) 关系驱动（新增账号类型只在此扩展：新增 Provider 即可）
        ScopeResolutionContext ctx = new ScopeResolutionContext(userId, tenantId);
        for (ScopeRelationProvider p : providers) {
            if (p.supports(ctx)) p.contribute(ctx);
        }

        // 2) 遗留兼容：sys_data_scope 行（有行则按行，直接返回）
        if (applyLegacyDataScopeIfPresent(ctx)) {
            return new ScopeCore(false, ctx.getLeagueIds(), ctx.getTeamIds());
        }

        // 3) 回退分支（关键）：门户角色即使无关系也返回空域（绝不回退 unrestricted）；遗留自定义角色账号保留旧语义
        boolean hasPortalRole = apiPermissionService.hasAnyRoleCode(userId, "member", "team_manager", "league_organizer");
        if (hasPortalRole) {
            return new ScopeCore(false, ctx.getLeagueIds(), ctx.getTeamIds());
        }
        return tenantProperties.isStrictDataScope() ? new ScopeCore(false, EMPTY, EMPTY)
                : new ScopeCore(true, EMPTY, EMPTY);
    }

    private boolean applyLegacyDataScopeIfPresent(ScopeResolutionContext ctx) {
        List<SysDataScope> rows = sysDataScopeRepository.findByUserIdAndTenantIdAndDeletedAtIsNull(ctx.getUserId(), Long.valueOf(ctx.getTenantId()));
        if (rows.isEmpty()) return false;
        for (SysDataScope row : rows) {
            if (SysDataScope.TYPE_TEAM.equals(row.getScopeType())) { ctx.addTeam(row.getRefId()); continue; }
            if (!SysDataScope.TYPE_LEAGUE.equals(row.getScopeType())) continue;
            ctx.addLeague(row.getRefId());
            if (SysDataScope.EXP_INCLUDE_DESCENDANTS.equals(row.getExpansion())) {
                ctx.getTeamIds().addAll(teamRepository.findIdsByLeagueIdAndTenantId(row.getRefId(), Long.valueOf(ctx.getTenantId())));
            }
        }
        return true;
    }

    public void evictUserScopeCache(Long userId) {
        if (userId == null || this.scopeCache == null) return;
        String prefix = userId + "#";
        this.scopeCache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
    }

    /** 范围缓存失效（后置到事务提交后）；无事务时立即失效。 */
    public void evictUserScopeCacheAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    AccountScopeService.this.evictUserScopeCache(userId);
                }
            });
        } else {
            this.evictUserScopeCache(userId);
        }
    }

    public void clearRequestScopeCache() { REQ_CACHE.remove(); }

    public void clearScopeCache() { if (this.scopeCache != null) this.scopeCache.invalidateAll(); }

    @Generated
    public AccountScopeService(ApiPermissionService apiPermissionService, TenantProperties tenantProperties,
            List<ScopeRelationProvider> providers, SysDataScopeRepository sysDataScopeRepository, TeamRepository teamRepository) {
        this.apiPermissionService = apiPermissionService;
        this.tenantProperties = tenantProperties;
        this.providers = providers;
        this.sysDataScopeRepository = sysDataScopeRepository;
        this.teamRepository = teamRepository;
    }

    /** 与请求无关的核心范围（不含 guestLikeRead 请求级标志），供 Caffeine 跨请求缓存。 */
    private record ScopeCore(boolean unrestricted, Set<Long> leagueIds, Set<Long> teamIds) {}
}
