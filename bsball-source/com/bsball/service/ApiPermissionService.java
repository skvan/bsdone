/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysApi
 *  com.bsball.model.entity.SysMenuApi
 *  com.bsball.model.entity.SysRole
 *  com.bsball.model.entity.SysRoleMenu
 *  com.bsball.repository.SysApiRepository
 *  com.bsball.repository.SysMenuApiRepository
 *  com.bsball.repository.SysRoleApiRepository
 *  com.bsball.repository.SysRoleMenuRepository
 *  com.bsball.repository.SysRoleRepository
 *  com.bsball.repository.SysUserRoleRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.ApiPermissionService$GuestPermissionSnapshot
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysMenuApi;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleMenu;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysMenuApiRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.service.ApiPermissionService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class ApiPermissionService {
    private static final String GUEST_ROLE_CODE = "guest";
    private static final String GUEST_PERM_CACHE_KEY = "guest";
    private static final ThreadLocal<Map<Long, List<Long>>> USER_ROLE_IDS_REQ_CACHE = ThreadLocal.withInitial(HashMap::new);
    private final SysRoleRepository sysRoleRepository;
    private final SysRoleApiRepository sysRoleApiRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final SysMenuApiRepository sysMenuApiRepository;
    private final SysApiRepository sysApiRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    @Value(value="${server.servlet.context-path:}")
    private String contextPath;
    @Value(value="${app.api-permission.guest-cache-ttl-sec:60}")
    private int guestCacheTtlSec;
    @Value(value="${app.api-permission.user-role-cache-ttl-sec:30}")
    private int userRoleCacheTtlSec;
    private Cache<String, GuestPermissionSnapshot> guestPermissionCache;
    private Cache<Long, List<Long>> userRoleCache;
    public static final String TENANT_ADMIN_ROLE_CODE = "tenant_admin";

    @PostConstruct
    void initGuestPermissionCache() {
        int ttl = Math.max(5, this.guestCacheTtlSec);
        this.guestPermissionCache = Caffeine.newBuilder().expireAfterWrite((long)ttl, TimeUnit.SECONDS).maximumSize(2L).build();
        int roleTtl = Math.max(5, this.userRoleCacheTtlSec);
        this.userRoleCache = Caffeine.newBuilder().expireAfterWrite((long)roleTtl, TimeUnit.SECONDS).maximumSize(20000L).build();
    }

    private GuestPermissionSnapshot loadGuestPermissionSnapshot() {
        List allApis = this.sysApiRepository.findAll();
        SysRole guest = this.sysRoleRepository.findByTenantIdIsNullAndCode("guest").orElse(null);
        HashSet guestApiIds = guest == null ? Set.of() : new HashSet(this.getApiIdsByRoleId(guest.getId()));
        return new GuestPermissionSnapshot(List.copyOf((Collection)allApis), guestApiIds);
    }

    public boolean isGuestAllowed(String requestPath, String method) {
        String pathWithoutContext;
        if (requestPath == null) {
            requestPath = "";
        }
        String pathForMatch = ApiPermissionService.normalizeApiPath((String)((pathWithoutContext = this.stripContextPath(requestPath)) == null || pathWithoutContext.isEmpty() ? "/" : pathWithoutContext));
        GuestPermissionSnapshot snap = (GuestPermissionSnapshot)this.guestPermissionCache.get((Object)"guest", k -> this.loadGuestPermissionSnapshot());
        List candidates = snap.allApis().stream().filter(a -> a.getMethod() == null || method.equalsIgnoreCase(a.getMethod())).filter(a -> ApiPermissionService.pathMatches((String)ApiPermissionService.normalizeApiPath((String)a.getPath()), (String)pathForMatch)).collect(Collectors.toList());
        Optional matched = ApiPermissionService.pickBestMatchingApi(candidates);
        if (matched.isEmpty()) {
            return false;
        }
        Long apiId = ((SysApi)matched.get()).getId();
        return snap.guestApiIds().contains(apiId);
    }

    public List<Long> getApiIdsByRoleId(Long roleId) {
        return this.sysRoleApiRepository.findByRoleId(roleId).stream().map(ra -> ra.getApiId()).distinct().toList();
    }

    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List roleIds = this.getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        if (roleIds.contains(1L)) {
            return true;
        }
        List roles = this.sysRoleRepository.findAllById((Iterable)roleIds);
        return roles.stream().anyMatch(r -> "admin".equals(r.getCode()) && r.getTenantId() == null);
    }

    public boolean isTenantAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List roleIds = this.getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        List roles = this.sysRoleRepository.findAllById((Iterable)roleIds);
        return roles.stream().anyMatch(r -> "tenant_admin".equals(r.getCode()));
    }

    public boolean canUserAccessApi(Long userId, String requestPath, String method) {
        if (userId == null) {
            return false;
        }
        String pathWithoutContext = this.stripContextPath(requestPath);
        String pathForMatch = ApiPermissionService.normalizeApiPath((String)(pathWithoutContext == null || pathWithoutContext.isEmpty() ? "/" : pathWithoutContext));
        List allApis = this.sysApiRepository.findAll();
        List candidates = allApis.stream().filter(a -> a.getMethod() == null || method.equalsIgnoreCase(a.getMethod())).filter(a -> ApiPermissionService.pathMatches((String)ApiPermissionService.normalizeApiPath((String)a.getPath()), (String)pathForMatch)).collect(Collectors.toList());
        Optional matched = ApiPermissionService.pickBestMatchingApi(candidates);
        if (matched.isEmpty()) {
            return true;
        }
        Long apiId = ((SysApi)matched.get()).getId();
        Set userApiIds = this.getApiIdsByUserId(userId);
        return userApiIds.contains(apiId);
    }

    private Set<Long> getApiIdsByUserId(Long userId) {
        List roleIds = this.getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        Set out = this.sysRoleApiRepository.findByRoleIdIn(roleIds).stream().map(ra -> ra.getApiId()).collect(HashSet::new, Set::add, Set::addAll);
        List rawMenuIds = this.sysRoleMenuRepository.findByRoleIdIn(roleIds).stream().map(SysRoleMenu::getMenuId).filter(Objects::nonNull).distinct().toList();
        if (!rawMenuIds.isEmpty()) {
            for (SysMenuApi ma : this.sysMenuApiRepository.findByMenuIdIn(new ArrayList(rawMenuIds))) {
                if (ma.getApiId() == null) continue;
                out.add(ma.getApiId());
            }
        }
        return out;
    }

    private String stripContextPath(String path) {
        Object prefix;
        if (this.contextPath == null || this.contextPath.isEmpty() || "/".equals(this.contextPath)) {
            return path;
        }
        Object object = prefix = this.contextPath.endsWith("/") ? this.contextPath : this.contextPath + "/";
        if (path.startsWith((String)prefix)) {
            String rest = path.substring(((String)prefix).length());
            return rest.isEmpty() ? "/" : (rest.startsWith("/") ? rest : "/" + rest);
        }
        if (path.equals(this.contextPath)) {
            return "/";
        }
        return path;
    }

    private static String normalizeApiPath(String path) {
        Object p;
        if (path == null || path.isEmpty()) {
            return "/";
        }
        Object object = p = path.startsWith("/") ? path : "/" + path;
        if (((String)p).startsWith("/api/")) {
            return ((String)p).substring(4);
        }
        if (((String)p).startsWith("/server/")) {
            return ((String)p).substring(8);
        }
        return p;
    }

    private static boolean pathMatches(String templatePath, String actualPath) {
        String[] actualSegments;
        if (templatePath == null || actualPath == null) {
            return false;
        }
        String[] templateSegments = templatePath.split("/");
        if (templateSegments.length != (actualSegments = actualPath.split("/")).length) {
            return false;
        }
        for (int i = 0; i < templateSegments.length; ++i) {
            String t = templateSegments[i];
            String a = actualSegments[i];
            if (t.startsWith(":") || t.equals(a)) continue;
            return false;
        }
        return true;
    }

    private static Optional<SysApi> pickBestMatchingApi(List<SysApi> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        return candidates.stream().min(Comparator.comparingInt(a -> ApiPermissionService.pathTemplateWildcardCount((String)ApiPermissionService.normalizeApiPath((String)a.getPath()))).thenComparing(a -> -ApiPermissionService.normalizeApiPath((String)a.getPath()).length()));
    }

    private static int pathTemplateWildcardCount(String templatePath) {
        if (templatePath == null || templatePath.isEmpty()) {
            return 0;
        }
        int c = 0;
        for (String s : templatePath.split("/")) {
            if (!s.startsWith(":")) continue;
            ++c;
        }
        return c;
    }

    public static void clearRequestCache() {
        USER_ROLE_IDS_REQ_CACHE.remove();
    }

    private List<Long> getRoleIdsByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        Map cache = (Map)USER_ROLE_IDS_REQ_CACHE.get();
        List cached = (List)cache.get(userId);
        if (cached != null) {
            return cached;
        }
        List roleIds = (List)this.userRoleCache.get((Object)userId, uid -> this.sysUserRoleRepository.findByUserId(uid).stream().map(ur -> ur.getRoleId()).distinct().toList());
        cache.put(userId, roleIds);
        return roleIds;
    }

    public void evictUserRoleCache(Long userId) {
        if (userId == null || this.userRoleCache == null) {
            return;
        }
        this.userRoleCache.invalidate((Object)userId);
    }

    public void clearUserRoleCache() {
        if (this.userRoleCache == null) {
            return;
        }
        this.userRoleCache.invalidateAll();
    }

    @Generated
    public ApiPermissionService(SysRoleRepository sysRoleRepository, SysRoleApiRepository sysRoleApiRepository, SysRoleMenuRepository sysRoleMenuRepository, SysMenuApiRepository sysMenuApiRepository, SysApiRepository sysApiRepository, SysUserRoleRepository sysUserRoleRepository) {
        this.sysRoleRepository = sysRoleRepository;
        this.sysRoleApiRepository = sysRoleApiRepository;
        this.sysRoleMenuRepository = sysRoleMenuRepository;
        this.sysMenuApiRepository = sysMenuApiRepository;
        this.sysApiRepository = sysApiRepository;
        this.sysUserRoleRepository = sysUserRoleRepository;
    }
}

