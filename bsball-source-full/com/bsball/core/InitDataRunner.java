/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.InitSeedProperties
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.InitDataRunner
 *  com.bsball.core.InitDataRunner$1PortalRole
 *  com.bsball.core.InitDataRunner$ApiDef
 *  com.bsball.core.InitDataRunner$ButtonSeed
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.Stadium
 *  com.bsball.model.entity.SysApi
 *  com.bsball.model.entity.SysConfig
 *  com.bsball.model.entity.SysDictData
 *  com.bsball.model.entity.SysDictType
 *  com.bsball.model.entity.SysMenu
 *  com.bsball.model.entity.SysMenuApi
 *  com.bsball.model.entity.SysRole
 *  com.bsball.model.entity.SysRoleApi
 *  com.bsball.model.entity.SysRoleMenu
 *  com.bsball.model.entity.SysUser
 *  com.bsball.model.entity.SysUserRole
 *  com.bsball.model.enums.RoofType
 *  com.bsball.model.enums.StadiumLevel
 *  com.bsball.model.enums.StadiumOperatingStatus
 *  com.bsball.model.enums.TurfType
 *  com.bsball.repository.StadiumRepository
 *  com.bsball.repository.SysApiRepository
 *  com.bsball.repository.SysConfigRepository
 *  com.bsball.repository.SysDictDataRepository
 *  com.bsball.repository.SysDictTypeRepository
 *  com.bsball.repository.SysMenuApiRepository
 *  com.bsball.repository.SysMenuRepository
 *  com.bsball.repository.SysRoleApiRepository
 *  com.bsball.repository.SysRoleMenuRepository
 *  com.bsball.repository.SysRoleRepository
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.repository.SysUserRoleRepository
 *  com.bsball.utils.PasswordEncoder
 *  lombok.Generated
 *  org.locationtech.jts.geom.Coordinate
 *  org.locationtech.jts.geom.GeometryFactory
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.PrecisionModel
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.core.annotation.Order
 *  org.springframework.stereotype.Component
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.core;

import com.bsball.config.InitSeedProperties;
import com.bsball.config.TenantProperties;
import com.bsball.core.InitDataRunner;
import com.bsball.model.entity.BaseEntity;
import com.bsball.model.entity.Stadium;
import com.bsball.model.entity.SysApi;
import com.bsball.model.entity.SysConfig;
import com.bsball.model.entity.SysDictData;
import com.bsball.model.entity.SysDictType;
import com.bsball.model.entity.SysMenu;
import com.bsball.model.entity.SysMenuApi;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysRoleApi;
import com.bsball.model.entity.SysRoleMenu;
import com.bsball.model.entity.SysUser;
import com.bsball.model.entity.SysUserRole;
import com.bsball.model.enums.RoofType;
import com.bsball.model.enums.StadiumLevel;
import com.bsball.model.enums.StadiumOperatingStatus;
import com.bsball.model.enums.TurfType;
import com.bsball.repository.StadiumRepository;
import com.bsball.repository.SysApiRepository;
import com.bsball.repository.SysConfigRepository;
import com.bsball.repository.SysDictDataRepository;
import com.bsball.repository.SysDictTypeRepository;
import com.bsball.repository.SysMenuApiRepository;
import com.bsball.repository.SysMenuRepository;
import com.bsball.repository.SysRoleApiRepository;
import com.bsball.repository.SysRoleMenuRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.utils.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Generated;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
@Order(value=2)
public class InitDataRunner
implements CommandLineRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(InitDataRunner.class);
    private static final GeometryFactory GEOM_4326 = new GeometryFactory(new PrecisionModel(), 4326);
    private final SysMenuRepository sysMenuRepository;
    private final SysApiRepository sysApiRepository;
    private final SysDictTypeRepository sysDictTypeRepository;
    private final SysDictDataRepository sysDictDataRepository;
    private final SysConfigRepository sysConfigRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysUserRepository sysUserRepository;
    private final SysRoleMenuRepository sysRoleMenuRepository;
    private final SysRoleApiRepository sysRoleApiRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final StadiumRepository stadiumRepository;
    private final TenantProperties tenantProperties;
    private final InitSeedProperties initSeedProperties;
    private final SysMenuApiRepository sysMenuApiRepository;

    @Transactional
    public void run(String ... args) {
        try {
            if (this.sysMenuRepository.count() == 0L) {
                log.info("\u521d\u59cb\u5316\u79cd\u5b50\u79df\u6237 ID\uff1a{}\uff08app.init.seed-tenant-id=0 \u65f6\u8868\u793a\u4f7f\u7528 app.tenant.default-id={}\uff09", (Object)this.resolveSeedTenantId(), (Object)this.tenantProperties.getDefaultId());
                long adminId = this.createAdminUserAndReturnId();
                this.seedMenus(adminId);
                this.seedApis(adminId);
                this.ensureMenuDirectoryTypesAndDefaultButtons();
                this.seedDict(adminId);
                this.seedConfig(adminId);
                this.seedAdminRoleAndBindUser(adminId);
                this.ensurePortalRolesIfNeeded(adminId);
                if (this.initSeedProperties.isSeedDefaultStadiums()) {
                    this.ensureDefaultStadiumsIfEmpty(adminId);
                }
                log.info("\u521d\u59cb\u5316\u6570\u636e\u5b8c\u6210\uff1a\u83dc\u5355\u3001API\u3001\u5b57\u5178\u3001\u914d\u7f6e\u3001\u89d2\u8272\u6743\u9650{}\uff0c\u9ed8\u8ba4\u7528\u6237 admin / admin@123", (Object)(this.initSeedProperties.isSeedDefaultStadiums() ? "\u3001\u9ed8\u8ba4\u7403\u573a" : ""));
            } else {
                this.ensureAdminUserIfNeeded();
                this.ensureHistoryRecordMenuIfNeeded();
                this.ensureHighlightMomentMenuIfNeeded();
                this.ensureArticleMenuIfNeeded();
                this.ensureStadiumMenuIfNeeded();
                this.ensurePublicViewCountConfigIfNeeded();
                this.ensurePortalDevtoolsGuardConfigIfNeeded();
                this.ensurePortalMonitorMenusAndApisIfNeeded();
                this.ensureIpAccessPolicyMenuAndApisIfNeeded();
                this.ensureIpLocationGeoMenuAndApisIfNeeded();
                this.ensureLoginAndOperationLogsUnderMonitorIfNeeded();
                this.ensureTenantManagementMenusIfNeeded();
                this.ensureTenantApisIfNeeded();
                this.ensureMediaIconGalleryMenusAndApisIfNeeded();
                this.ensureLineupTemplateMenuIfNeeded();
                this.ensurePlayerClaimMenuIfNeeded();
                this.ensureMenuDirectoryTypesAndDefaultButtons();
                this.ensureAdminRoleBindsAllMenusIfNeeded();
                this.ensureAdminRoleBindsAllApisIfNeeded();
                this.ensureTenantAdminRoleIfNeeded();
                this.ensureTenantAdminRoleBindsAllowedApisIfNeeded();
                long opIdForRoles = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
                this.ensurePortalRolesIfNeeded(opIdForRoles);
                long operatorId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
                if (this.initSeedProperties.isSeedDefaultStadiums()) {
                    this.ensureDefaultStadiumsIfEmpty(operatorId);
                }
            }
        }
        catch (Exception e) {
            log.warn("\u521d\u59cb\u5316\u6570\u636e\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    private long resolveSeedTenantId() {
        long x = this.initSeedProperties.getSeedTenantId();
        return x > 0L ? x : this.tenantProperties.getDefaultId();
    }

    private long createAdminUserAndReturnId() {
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setNickname("\u7ba1\u7406\u5458");
        admin.setPassword(PasswordEncoder.encode((CharSequence)"admin@123"));
        admin.setStatus(Integer.valueOf(1));
        admin = (SysUser)this.sysUserRepository.save((Object)admin);
        long adminId = admin.getId();
        admin.setCreatedBy(Long.valueOf(adminId));
        admin.setUpdatedBy(Long.valueOf(adminId));
        this.sysUserRepository.save((Object)admin);
        return adminId;
    }

    private void ensureAdminUserIfNeeded() {
        if (this.sysUserRepository.count() > 0L) {
            return;
        }
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole == null) {
            adminRole = new SysRole();
            adminRole.setName("\u8d85\u7ea7\u7ba1\u7406\u5458");
            adminRole.setCode("admin");
            adminRole.setStatus(Integer.valueOf(1));
            adminRole.setSort(Integer.valueOf(0));
            adminRole = (SysRole)this.sysRoleRepository.save((Object)adminRole);
            List menus = this.sysMenuRepository.findAll();
            for (SysMenu m : menus) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(adminRole.getId());
                rm.setMenuId(m.getId());
                this.sysRoleMenuRepository.save((Object)rm);
            }
            List apis = this.sysApiRepository.findAll();
            for (SysApi a : apis) {
                SysRoleApi ra = new SysRoleApi();
                ra.setRoleId(adminRole.getId());
                ra.setApiId(a.getId());
                this.sysRoleApiRepository.save((Object)ra);
            }
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setNickname("\u7ba1\u7406\u5458");
        admin.setPassword(PasswordEncoder.encode((CharSequence)"admin@123"));
        admin.setStatus(Integer.valueOf(1));
        admin = (SysUser)this.sysUserRepository.save((Object)admin);
        long adminId = admin.getId();
        admin.setCreatedBy(Long.valueOf(adminId));
        admin.setUpdatedBy(Long.valueOf(adminId));
        this.sysUserRepository.save((Object)admin);
        if (adminRole != null) {
            adminRole.setCreatedBy(Long.valueOf(adminId));
            adminRole.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleRepository.save((Object)adminRole);
        }
        SysUserRole ur = new SysUserRole();
        ur.setUserId(admin.getId());
        ur.setRoleId(adminRole.getId());
        this.sysUserRoleRepository.save((Object)ur);
        log.info("\u5df2\u521d\u59cb\u5316\u9ed8\u8ba4\u7528\u6237: admin / admin@123\uff08\u5bc6\u7801\u5df2\u52a0\u5bc6\uff09");
    }

    private void ensurePublicViewCountConfigIfNeeded() {
        long tid = this.resolveSeedTenantId();
        if (this.sysConfigRepository.findByTenantIdAndConfigKey(Long.valueOf(tid), "publicViewCount").isPresent()) {
            return;
        }
        SysConfig c = new SysConfig();
        c.setTenantId(Long.valueOf(tid));
        c.setConfigKey("publicViewCount");
        c.setConfigValue("true");
        this.sysConfigRepository.save((Object)c);
        log.info("\u5df2\u8865\u5168\u914d\u7f6e\u9879\uff1apublicViewCount=true\uff08\u524d\u53f0\u516c\u5f00\u5c55\u793a\u6d4f\u89c8\u6b21\u6570\uff09");
    }

    private void ensurePortalDevtoolsGuardConfigIfNeeded() {
        long tid = this.resolveSeedTenantId();
        this.ensureSysConfigKeyIfAbsent(tid, "portalDevtoolsGuard", "true");
        this.ensureSysConfigKeyIfAbsent(tid, "portalDevtoolsGuardOverlay", "true");
        this.ensureSysConfigKeyIfAbsent(tid, "portalDevtoolsGuardDebuggerTrap", "true");
        this.ensureSysConfigKeyIfAbsent(tid, "portalDevtoolsGuardCopyrightNotice", "true");
    }

    private void ensureSysConfigKeyIfAbsent(long tenantId, String key, String value) {
        if (this.sysConfigRepository.findByTenantIdAndConfigKey(Long.valueOf(tenantId), key).isPresent()) {
            return;
        }
        SysConfig c = new SysConfig();
        c.setTenantId(Long.valueOf(tenantId));
        c.setConfigKey(key);
        c.setConfigValue(value);
        this.sysConfigRepository.save((Object)c);
        log.info("\u5df2\u8865\u5168\u914d\u7f6e\u9879\uff1a{}={}", (Object)key, (Object)value);
    }

    private void ensurePortalMonitorMenusAndApisIfNeeded() {
        SysMenu saved;
        SysMenu m2;
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        List apis = List.of((Object)new ApiDef("/sys/portal/devtools-report/list", "GET", "\u67e5\u8be2\u95e8\u6237\u5f00\u53d1\u8005\u5de5\u5177\u6253\u5f00\u8bb0\u5f55", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/portal/visit-hit/list", "GET", "\u67e5\u8be2\u95e8\u6237\u8bbf\u95ee\u6253\u70b9\u660e\u7ec6", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/portal/ip-location", "GET", "IP \u5f52\u5c5e\u5730\uff08\u9ad8\u5fb7\uff09", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/portal/ip-location/batch", "POST", "\u6279\u91cf IP \u5f52\u5c5e\u5730\uff08\u9ad8\u5fb7\uff09", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"));
        ArrayList<SysApi> existing = new ArrayList<SysApi>(this.sysApiRepository.findAll());
        for (ApiDef a : apis) {
            boolean exists = existing.stream().anyMatch(e -> a.path.equals(e.getPath()) && a.method.equalsIgnoreCase(e.getMethod()));
            if (exists) continue;
            SysApi api = new SysApi();
            api.setPath(a.path);
            api.setMethod(a.method);
            api.setDescription(a.desc);
            api.setGroupName(a.group);
            api.setCreatedBy(Long.valueOf(opId));
            api.setUpdatedBy(Long.valueOf(opId));
            SysApi saved2 = (SysApi)this.sysApiRepository.save((Object)api);
            existing.add(saved2);
        }
        boolean hasDevtools = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/monitor/portal-devtools-report".equals(m.getPath()));
        boolean hasVisit = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/monitor/portal-visit-hit".equals(m.getPath()));
        boolean hasFeedback = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/monitor/portal-feedback".equals(m.getPath()));
        if (hasDevtools && hasVisit && hasFeedback) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu monitorRoot = all.stream().filter(m -> "/monitor".equals(m.getPath()) || "\u7cfb\u7edf\u76d1\u63a7".equals(m.getTitle())).findFirst().orElse(null);
        if (monitorRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (!hasDevtools) {
            m2 = this.menu(monitorRoot.getId(), "DevTools \u4e0a\u62a5", "DevTools \u4e0a\u62a5", "/admin/monitor/portal-devtools-report", "AdminMonitorPortalDevtoolsReport", "views/admin/monitor/PortalDevtoolsReportList.vue", "Warning", 4, createdBy);
            saved = (SysMenu)this.sysMenuRepository.save((Object)m2);
            if (adminRole != null) {
                this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
            }
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1aDevTools \u4e0a\u62a5");
        }
        if (!hasVisit) {
            m2 = this.menu(monitorRoot.getId(), "\u95e8\u6237\u8bbf\u95ee\u6253\u70b9", "\u95e8\u6237\u8bbf\u95ee\u6253\u70b9", "/admin/monitor/portal-visit-hit", "AdminMonitorPortalVisitHit", "views/admin/monitor/PortalVisitHitList.vue", "Histogram", 5, createdBy);
            saved = (SysMenu)this.sysMenuRepository.save((Object)m2);
            if (adminRole != null) {
                this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
            }
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u95e8\u6237\u8bbf\u95ee\u6253\u70b9");
        }
        if (!hasFeedback) {
            m2 = this.menu(monitorRoot.getId(), "\u610f\u89c1\u53cd\u9988", "\u610f\u89c1\u53cd\u9988", "/admin/monitor/portal-feedback", "AdminMonitorPortalFeedback", "views/admin/monitor/PortalFeedbackList.vue", "ChatLineRound", 6, createdBy);
            saved = (SysMenu)this.sysMenuRepository.save((Object)m2);
            if (adminRole != null) {
                this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
            }
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u610f\u89c1\u53cd\u9988");
        }
    }

    private void ensureIpAccessPolicyMenuAndApisIfNeeded() {
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        List apis = List.of((Object)new ApiDef("/sys/ip-access-policy", "GET", "\u67e5\u8be2\u5168\u7ad9 IP \u7b56\u7565", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/ip-access-policy", "PUT", "\u66f4\u65b0\u5168\u7ad9 IP \u7b56\u7565", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/ip-access-rule/list", "GET", "\u67e5\u8be2\u5168\u7ad9 IP \u89c4\u5219\u5217\u8868", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/ip-access-rule/create", "POST", "\u521b\u5efa\u5168\u7ad9 IP \u89c4\u5219", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/ip-access-rule/update/:id", "PUT", "\u66f4\u65b0\u5168\u7ad9 IP \u89c4\u5219", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), (Object)new ApiDef("/sys/ip-access-rule/delete/:id", "DELETE", "\u5220\u9664\u5168\u7ad9 IP \u89c4\u5219", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"));
        ArrayList<SysApi> existing = new ArrayList<SysApi>(this.sysApiRepository.findAll());
        for (ApiDef a : apis) {
            boolean exists = existing.stream().anyMatch(e -> a.path.equals(e.getPath()) && a.method.equalsIgnoreCase(e.getMethod()));
            if (exists) continue;
            SysApi api = new SysApi();
            api.setPath(a.path);
            api.setMethod(a.method);
            api.setDescription(a.desc);
            api.setGroupName(a.group);
            api.setCreatedBy(Long.valueOf(opId));
            api.setUpdatedBy(Long.valueOf(opId));
            SysApi saved = (SysApi)this.sysApiRepository.save((Object)api);
            existing.add(saved);
        }
        boolean hasMenu = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/monitor/ip-access-policy".equals(m.getPath()));
        if (hasMenu) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu systemRoot = all.stream().filter(m -> "/system".equals(m.getPath()) || "\u7cfb\u7edf\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (systemRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        SysMenu m2 = this.menu(systemRoot.getId(), "IP\u7b56\u7565\u7ba1\u7406", "IP\u7b56\u7565\u7ba1\u7406", "/admin/monitor/ip-access-policy", "AdminMonitorIpAccessPolicy", "views/admin/monitor/IpAccessPolicy.vue", "Lock", 4, createdBy);
        SysMenu saved = (SysMenu)this.sysMenuRepository.save((Object)m2);
        if (adminRole != null) {
            this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
        }
        log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1aIP\u7b56\u7565\u7ba1\u7406");
    }

    private void ensureIpLocationGeoMenuAndApisIfNeeded() {
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        List apis = List.of((Object)new ApiDef("/sys/ip-location-cache/page", "GET", "\u5206\u9875\u67e5\u8be2 IP \u5f52\u5c5e\u5730\u7f13\u5b58", "IP\u5730\u7406\u4fe1\u606f"), (Object)new ApiDef("/sys/ip-location-cache/lbs-providers", "GET", "\u67e5\u8be2\u53ef\u9009 IP \u5f52\u5c5e\u5730\u6570\u636e\u6e90", "IP\u5730\u7406\u4fe1\u606f"), (Object)new ApiDef("/sys/ip-location-cache/refresh", "POST", "\u5f3a\u5236\u91cd\u65b0\u89e3\u6790 IP \u5e76\u5199\u56de\u7f13\u5b58", "IP\u5730\u7406\u4fe1\u606f"));
        ArrayList<SysApi> existing = new ArrayList<SysApi>(this.sysApiRepository.findAll());
        for (ApiDef a : apis) {
            boolean exists = existing.stream().anyMatch(e -> a.path.equals(e.getPath()) && a.method.equalsIgnoreCase(e.getMethod()));
            if (exists) continue;
            SysApi api = new SysApi();
            api.setPath(a.path);
            api.setMethod(a.method);
            api.setDescription(a.desc);
            api.setGroupName(a.group);
            api.setCreatedBy(Long.valueOf(opId));
            api.setUpdatedBy(Long.valueOf(opId));
            SysApi saved = (SysApi)this.sysApiRepository.save((Object)api);
            existing.add(saved);
        }
        boolean hasMenu = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/ip-location-cache".equals(m.getPath()));
        if (!hasMenu) {
            List all = this.sysMenuRepository.findAll();
            SysMenu systemRoot = all.stream().filter(m -> "/system".equals(m.getPath()) || "\u7cfb\u7edf\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
            if (systemRoot == null) {
                return;
            }
            long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
            SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
            SysMenu m2 = this.menu(systemRoot.getId(), "IP\u5730\u7406\u4fe1\u606f", "IP\u5730\u7406\u4fe1\u606f", "/admin/ip-location-cache", "AdminIpLocationCache", "views/admin/system/IpLocationCache.vue", "Location", 5, createdBy);
            SysMenu saved = (SysMenu)this.sysMenuRepository.save((Object)m2);
            if (adminRole != null) {
                this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
            }
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1aIP\u5730\u7406\u4fe1\u606f");
        }
        this.reorderSystemMenusIpGeoAfterIpPolicy();
    }

    private void reorderSystemMenusIpGeoAfterIpPolicy() {
        ArrayList all = new ArrayList(this.sysMenuRepository.findAll());
        SysMenu systemRoot = all.stream().filter(m -> "/system".equals(m.getPath()) || "\u7cfb\u7edf\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        SysMenu ipPolicy = all.stream().filter(m -> "/admin/monitor/ip-access-policy".equals(m.getPath())).findFirst().orElse(null);
        SysMenu geo = all.stream().filter(m -> "/admin/ip-location-cache".equals(m.getPath())).findFirst().orElse(null);
        if (systemRoot == null || ipPolicy == null || geo == null) {
            return;
        }
        if (!systemRoot.getId().equals(ipPolicy.getParentId()) || !systemRoot.getId().equals(geo.getParentId())) {
            return;
        }
        List children = all.stream().filter(m -> systemRoot.getId().equals(m.getParentId())).sorted(Comparator.comparingInt(SysMenu::getSort).thenComparing(BaseEntity::getId)).collect(Collectors.toCollection(ArrayList::new));
        children.removeIf(m -> geo.getId().equals(m.getId()));
        int idx = -1;
        for (int i = 0; i < children.size(); ++i) {
            if (!ipPolicy.getId().equals(((SysMenu)children.get(i)).getId())) continue;
            idx = i;
            break;
        }
        if (idx >= 0) {
            children.add(idx + 1, geo);
        } else {
            children.add(geo);
        }
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        boolean changed = false;
        for (int i = 0; i < children.size(); ++i) {
            SysMenu m2 = (SysMenu)children.get(i);
            if (m2.getSort() != null && m2.getSort() == i) continue;
            m2.setSort(Integer.valueOf(i));
            m2.setUpdatedBy(Long.valueOf(opId));
            changed = true;
        }
        if (changed) {
            this.sysMenuRepository.saveAll((Iterable)children);
            log.info("\u5df2\u6821\u6b63\u83dc\u5355\u987a\u5e8f\uff1aIP\u5730\u7406\u4fe1\u606f \u7d27\u968f IP\u7b56\u7565\u7ba1\u7406");
        }
    }

    private void ensureLoginAndOperationLogsUnderMonitorIfNeeded() {
        List all = this.sysMenuRepository.findAll();
        SysMenu monitorRoot = all.stream().filter(m -> "/monitor".equals(m.getPath()) || "\u7cfb\u7edf\u76d1\u63a7".equals(m.getTitle())).findFirst().orElse(null);
        if (monitorRoot == null) {
            return;
        }
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        for (SysMenu m2 : all) {
            boolean changed;
            if (m2.getPath() == null) continue;
            if ("/admin/login-logs".equals(m2.getPath())) {
                changed = false;
                if (!monitorRoot.getId().equals(m2.getParentId())) {
                    m2.setParentId(monitorRoot.getId());
                    changed = true;
                }
                if (!"\u767b\u5f55\u5386\u53f2".equals(m2.getTitle()) || !"\u767b\u5f55\u5386\u53f2".equals(m2.getName())) {
                    m2.setName("\u767b\u5f55\u5386\u53f2");
                    m2.setTitle("\u767b\u5f55\u5386\u53f2");
                    changed = true;
                }
                if (m2.getSort() != 6) {
                    m2.setSort(Integer.valueOf(6));
                    changed = true;
                }
                if (m2.getIcon() == null || !"Key".equals(m2.getIcon())) {
                    m2.setIcon("Key");
                    changed = true;
                }
                if (changed) {
                    m2.setUpdatedBy(Long.valueOf(opId));
                    this.sysMenuRepository.save((Object)m2);
                    log.info("\u5df2\u8fc1\u79fb/\u66f4\u65b0\u83dc\u5355\uff1a\u767b\u5f55\u5386\u53f2 \u2192 \u7cfb\u7edf\u76d1\u63a7");
                }
                if (adminRole == null) continue;
                this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), m2.getId().longValue());
                continue;
            }
            if (!"/admin/operation-logs".equals(m2.getPath())) continue;
            changed = false;
            if (!monitorRoot.getId().equals(m2.getParentId())) {
                m2.setParentId(monitorRoot.getId());
                changed = true;
            }
            if (m2.getSort() != 7) {
                m2.setSort(Integer.valueOf(7));
                changed = true;
            }
            if (changed) {
                m2.setUpdatedBy(Long.valueOf(opId));
                this.sysMenuRepository.save((Object)m2);
                log.info("\u5df2\u8fc1\u79fb/\u66f4\u65b0\u83dc\u5355\uff1a\u64cd\u4f5c\u5386\u53f2 \u2192 \u7cfb\u7edf\u76d1\u63a7");
            }
            if (adminRole == null) continue;
            this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), m2.getId().longValue());
        }
    }

    private void ensureTenantAdminRoleIfNeeded() {
        SysRole role = this.sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin").orElse(null);
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        boolean created = false;
        if (role == null) {
            role = new SysRole();
            role.setName("\u79df\u6237\u7ba1\u7406\u5458");
            role.setCode("tenant_admin");
            role.setStatus(Integer.valueOf(1));
            role.setSort(Integer.valueOf(2));
            role.setCreatedBy(Long.valueOf(opId));
            role.setUpdatedBy(Long.valueOf(opId));
            role = (SysRole)this.sysRoleRepository.save((Object)role);
            created = true;
            log.info("\u5df2\u521b\u5efa\u89d2\u8272\uff1a\u79df\u6237\u7ba1\u7406\u5458 (tenant_admin)\uff0c\u5e76\u5199\u5165\u9ed8\u8ba4\u83dc\u5355/API \u7ed1\u5b9a");
        }
        if (!created) {
            return;
        }
        List menus = this.sysMenuRepository.findAll();
        Set allowedMenuIds = InitDataRunner.collectTenantAdminDefaultMenuIds((List)menus);
        Set boundMenus = this.sysRoleMenuRepository.findByRoleId(role.getId()).stream().map(SysRoleMenu::getMenuId).collect(Collectors.toCollection(HashSet::new));
        for (Long mid : allowedMenuIds) {
            if (boundMenus.contains(mid)) continue;
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getId());
            rm.setMenuId(mid);
            rm.setCreatedBy(Long.valueOf(opId));
            rm.setUpdatedBy(Long.valueOf(opId));
            this.sysRoleMenuRepository.save((Object)rm);
        }
        List allApis = this.sysApiRepository.findAll();
        Set boundApis = this.sysRoleApiRepository.findByRoleId(role.getId()).stream().map(SysRoleApi::getApiId).collect(Collectors.toCollection(HashSet::new));
        int added = 0;
        for (SysApi a : allApis) {
            if (!InitDataRunner.isApiAllowedForTenantAdmin((SysApi)a) || boundApis.contains(a.getId())) continue;
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(role.getId());
            ra.setApiId(a.getId());
            ra.setCreatedBy(Long.valueOf(opId));
            ra.setUpdatedBy(Long.valueOf(opId));
            this.sysRoleApiRepository.save((Object)ra);
            ++added;
        }
        if (added > 0) {
            log.info("\u5df2\u4e3a\u79df\u6237\u7ba1\u7406\u5458\u89d2\u8272\u8865\u7ed1 {} \u4e2a API", (Object)added);
        }
    }

    private static boolean isApiAllowedForTenantAdmin(SysApi a) {
        String p = a.getPath();
        if (p == null || p.isBlank()) {
            return false;
        }
        if (p.startsWith("/sys/monitor")) {
            return false;
        }
        if (p.startsWith("/sys/tenant")) {
            return false;
        }
        if (p.startsWith("/sys/menu")) {
            return "/sys/menu/list".equals(p) && "GET".equalsIgnoreCase(a.getMethod());
        }
        if (p.startsWith("/sys/api")) {
            return false;
        }
        if (p.contains("/sys/dict")) {
            if (!"GET".equalsIgnoreCase(a.getMethod())) {
                return false;
            }
            return "/sys/dict/list".equals(p) || "/sys/dict/type/list".equals(p) || "/sys/dict/data/list".equals(p);
        }
        if (p.startsWith("/sys/user-tenant")) {
            return false;
        }
        if (p.startsWith("/auth/")) {
            return true;
        }
        if (p.startsWith("/sys/user")) {
            return true;
        }
        if (p.startsWith("/sys/role")) {
            return true;
        }
        if (p.startsWith("/sys/resource")) {
            return true;
        }
        if (p.startsWith("/sys/media-icon") || p.startsWith("/sys/media-gallery")) {
            return true;
        }
        if ("/sys/config".equals(p)) {
            return true;
        }
        if (p.startsWith("/sys/login-log")) {
            return true;
        }
        if (p.startsWith("/sys/operation-log")) {
            return true;
        }
        if (p.startsWith("/sys/ip-access-policy") || p.startsWith("/sys/ip-access-rule")) {
            return true;
        }
        if (p.startsWith("/sys/ip-location-cache")) {
            return true;
        }
        if (p.startsWith("/sys/portal/visit-hit") || p.startsWith("/sys/portal/ip-location")) {
            return true;
        }
        if (p.startsWith("/sys/portal-visit")) {
            return true;
        }
        if (p.startsWith("/sys/article") || p.startsWith("/sys/notice")) {
            return true;
        }
        return p.startsWith("/league") || p.startsWith("/team") || p.startsWith("/coach") || p.startsWith("/event") || p.startsWith("/game") || p.startsWith("/stats/") || p.startsWith("/player") || p.startsWith("/personnel-change") || p.startsWith("/highlight-moment") || p.startsWith("/stadium") || p.startsWith("/region/china");
    }

    private static Set<Long> collectTenantAdminDefaultMenuIds(List<SysMenu> menus) {
        Map<Long, SysMenu> byId = menus.stream().collect(Collectors.toMap(BaseEntity::getId, m -> m, (x, y) -> x));
        Long businessRootId = menus.stream().filter(m -> "/business".equals(m.getPath())).map(BaseEntity::getId).findFirst().orElse(null);
        Set leafPaths = Set.of((Object[])new String[]{"/admin/dashboard", "/admin/users", "/admin/roles", "/admin/config", "/admin/resources", "/admin/media-icons", "/admin/media-gallery", "/admin/announcements", "/admin/monitor/portal-devtools-report", "/admin/monitor/portal-visit-hit", "/admin/monitor/portal-feedback", "/admin/monitor/ip-access-policy", "/admin/login-logs", "/admin/operation-logs"});
        HashSet<Long> direct = new HashSet<Long>();
        for (SysMenu m2 : menus) {
            String path = m2.getPath();
            if (path != null && leafPaths.contains(path)) {
                direct.add(m2.getId());
            }
            if (businessRootId == null || !InitDataRunner.menuIsSelfOrDescendantOf((Long)m2.getId(), (Long)businessRootId, byId)) continue;
            direct.add(m2.getId());
        }
        HashSet<Long> withAncestors = new HashSet<Long>(direct);
        for (Long mid : direct) {
            InitDataRunner.addMenuAncestors((Long)mid, byId, withAncestors);
        }
        return withAncestors;
    }

    private static boolean menuIsSelfOrDescendantOf(Long menuId, Long rootId, Map<Long, SysMenu> byId) {
        Long cur = menuId;
        HashSet<Long> guard = new HashSet<Long>();
        while (cur != null && cur > 0L && guard.add(cur)) {
            if (cur.equals(rootId)) {
                return true;
            }
            SysMenu x = byId.get(cur);
            if (x == null) break;
            cur = x.getParentId();
        }
        return false;
    }

    private static void addMenuAncestors(Long menuId, Map<Long, SysMenu> byId, Set<Long> out) {
        SysMenu m = byId.get(menuId);
        if (m == null) {
            return;
        }
        Long p = m.getParentId();
        while (p != null && p > 0L) {
            out.add(p);
            SysMenu parent = byId.get(p);
            if (parent == null) break;
            p = parent.getParentId();
        }
    }

    private void ensurePortalRolesIfNeeded(long opId) {
        List roles = List.of((Object)new PortalRole("member", "\u666e\u901a\u7528\u6237", "\u95e8\u6237\u81ea\u52a9\u6ce8\u518c\u9ed8\u8ba4\u89d2\u8272\uff1a\u53ef\u8ba4\u9886\u7403\u5458\u3001\u7533\u8bf7\u52a0\u5165\u8054\u76df\u7403\u961f", 50), (Object)new PortalRole("team_manager", "\u7403\u961f\u8d1f\u8d23\u4eba", "\u542b\u666e\u901a\u7528\u6237\u6743\u9650\uff0c\u53ef\u521b\u5efa\u7403\u961f\u3001\u6dfb\u52a0\u7403\u5458\u3001\u5ba1\u6838\u7403\u5458\u8ba4\u9886", 51), (Object)new PortalRole("league_organizer", "\u8d5b\u4e8b\u4e3b\u529e\u65b9", "\u542b\u7403\u961f\u8d1f\u8d23\u4eba\u6743\u9650\uff0c\u53ef\u521b\u5efa\u8054\u76df\u3001\u7ba1\u7406\u672c\u8054\u76df\u8d5b\u4e8b\uff08\u4ec5\u9650\u672c\u8054\u76df\uff09", 52));
        for (PortalRole r : roles) {
            if (this.sysRoleRepository.findByTenantIdIsNullAndCode(r.code()).isPresent()) continue;
            SysRole role = new SysRole();
            role.setName(r.name());
            role.setCode(r.code());
            role.setStatus(Integer.valueOf(1));
            role.setSort(Integer.valueOf(r.sort()));
            role.setDescription(r.desc());
            role.setCreatedBy(Long.valueOf(opId));
            role.setUpdatedBy(Long.valueOf(opId));
            this.sysRoleRepository.save((Object)role);
            log.info("\u5df2\u521b\u5efa\u5e73\u53f0\u89d2\u8272\uff1a{} ({})", (Object)r.name(), (Object)r.code());
        }
    }

    private void ensureAdminRoleBindsAllApisIfNeeded() {
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole == null) {
            return;
        }
        Set bound = this.sysRoleApiRepository.findByRoleId(adminRole.getId()).stream().map(SysRoleApi::getApiId).collect(Collectors.toCollection(HashSet::new));
        List all = this.sysApiRepository.findAll();
        long opUserId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        int added = 0;
        for (SysApi a : all) {
            if (bound.contains(a.getId())) continue;
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(adminRole.getId());
            ra.setApiId(a.getId());
            ra.setCreatedBy(Long.valueOf(opUserId));
            ra.setUpdatedBy(Long.valueOf(opUserId));
            this.sysRoleApiRepository.save((Object)ra);
            bound.add(a.getId());
            ++added;
        }
        if (added > 0) {
            log.info("\u5df2\u4e3a\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u8865\u7ed1 {} \u4e2a API\uff08\u542b\u65b0\u589e\u63a5\u53e3\uff09", (Object)added);
        }
    }

    private void ensureTenantAdminRoleBindsAllowedApisIfNeeded() {
        SysRole role = this.sysRoleRepository.findByTenantIdIsNullAndCode("tenant_admin").orElse(null);
        if (role == null) {
            return;
        }
        Set bound = this.sysRoleApiRepository.findByRoleId(role.getId()).stream().map(SysRoleApi::getApiId).collect(Collectors.toCollection(HashSet::new));
        List all = this.sysApiRepository.findAll();
        long opUserId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        int added = 0;
        for (SysApi a : all) {
            if (!InitDataRunner.isApiAllowedForTenantAdmin((SysApi)a) || bound.contains(a.getId())) continue;
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(role.getId());
            ra.setApiId(a.getId());
            ra.setCreatedBy(Long.valueOf(opUserId));
            ra.setUpdatedBy(Long.valueOf(opUserId));
            this.sysRoleApiRepository.save((Object)ra);
            bound.add(a.getId());
            ++added;
        }
        if (added > 0) {
            log.info("\u5df2\u4e3a\u79df\u6237\u7ba1\u7406\u5458\u89d2\u8272\u8865\u7ed1 {} \u4e2a API\uff08\u767d\u540d\u5355\u5185\u65b0\u589e\uff09", (Object)added);
        }
    }

    private void ensureStadiumMenuIfNeeded() {
        boolean alreadyBound;
        boolean exists = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/stadiums".equals(m.getPath()));
        if (exists) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu businessRoot = all.stream().filter(m -> "/business".equals(m.getPath()) || "\u4e1a\u52a1\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (businessRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        SysMenu stadiumMenu = this.menu(businessRoot.getId(), "\u7403\u573a\u7ba1\u7406", "\u7403\u573a\u7ba1\u7406", "/admin/stadiums", "AdminStadiums", "views/admin/business/Stadiums.vue", "Place", 7, createdBy);
        SysMenu savedMenu = (SysMenu)this.sysMenuRepository.save((Object)stadiumMenu);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole != null && !(alreadyBound = this.sysRoleMenuRepository.findByRoleId(adminRole.getId()).stream().anyMatch(rm -> savedMenu.getId().equals(rm.getMenuId())))) {
            SysRoleMenu rm2 = new SysRoleMenu();
            rm2.setRoleId(adminRole.getId());
            rm2.setMenuId(savedMenu.getId());
            this.sysRoleMenuRepository.save((Object)rm2);
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u7403\u573a\u7ba1\u7406");
        }
    }

    private void ensureDefaultStadiumsIfEmpty(long operatorId) {
        if (this.stadiumRepository.countByDeletedAtIsNull() > 0L) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        long tid = this.resolveSeedTenantId();
        for (Stadium s : InitDataRunner.defaultStadiumSeeds()) {
            s.setTenantId(Long.valueOf(tid));
            s.setCreatedBy(Long.valueOf(operatorId));
            s.setUpdatedBy(Long.valueOf(operatorId));
            s.setCreatedAt(now);
            s.setUpdatedAt(now);
            this.stadiumRepository.save((Object)s);
        }
        log.info("\u5df2\u521d\u59cb\u5316\u9ed8\u8ba4\u7403\u573a\u6570\u636e\uff084 \u6761\uff09");
    }

    private static Point gcjPoint(double lng, double lat) {
        return GEOM_4326.createPoint(new Coordinate(lng, lat));
    }

    private static List<Stadium> defaultStadiumSeeds() {
        ArrayList<Stadium> list = new ArrayList<Stadium>();
        list.add(InitDataRunner.stadium((String)"\u4e2d\u5c71\u718a\u732b\u7eaa\u5ff5\u7403\u573a", (String)"\u718a\u732b\u7eaa\u5ff5\u7403\u573a", (String)"Panda Memorial Field", (String)"\u5e7f\u4e1c\u7701", (String)"\u4e2d\u5c71\u5e02", (String)"\u5c0f\u6984\u9547", (String)"442000118", (String)"528415", (String)"\u5c0f\u6984\u9547\u4e1c\u5347\u7247\u540c\u5fb7\u8857\uff09", (Point)InitDataRunner.gcjPoint((double)113.29682, (double)22.61505), (StadiumLevel)StadiumLevel.A, (int)3000, (int)1000, (int)100, (int)122, (int)100, (TurfType)TurfType.NATURAL));
        list.add(InitDataRunner.stadium((String)"\u4e2d\u5c71\u4e1c\u5347\u4e2d\u5b66\u7403\u573a", (String)"\u4e1c\u5347\u4e2d\u5b66\u7403\u573a", (String)"Dongsheng HS Field", (String)"\u5e7f\u4e1c\u7701", (String)"\u4e2d\u5c71\u5e02", (String)"\u5c0f\u6984\u9547", (String)"442000118", (String)"528414", (String)"\u4e1c\u5347\u5927\u905328\u53f7\u4e2d\u5c71\u5e02\u4e1c\u5347\u9ad8\u7ea7\u4e2d\u5b66", (Point)InitDataRunner.gcjPoint((double)113.29095, (double)22.61038), (StadiumLevel)StadiumLevel.B, (int)800, (int)400, (int)97, (int)120, (int)97, (TurfType)TurfType.NATURAL));
        list.add(InitDataRunner.stadium((String)"\u5e7f\u5dde\u5e7f\u4f53\u68d2\u7403\u573a", (String)"\u5e7f\u4f53\u68d2\u7403\u573a", (String)"GZSU Baseball Field", (String)"\u5e7f\u4e1c\u7701", (String)"\u5e7f\u5dde\u5e02", (String)"\u5929\u6cb3\u533a", (String)"440106", (String)"510500", (String)"\u5e7f\u5dde\u5927\u9053\u4e2d1268\u53f7\u5e7f\u5dde\u4f53\u80b2\u5b66\u9662\u4e3b\u6821\u533a\u6821\u5185\u68d2\u7403\u573a", (Point)InitDataRunner.gcjPoint((double)113.31805, (double)23.15218), (StadiumLevel)StadiumLevel.B, (int)1500, (int)800, (int)100, (int)122, (int)100, (TurfType)TurfType.NATURAL));
        list.add(InitDataRunner.stadium((String)"\u6df1\u5733\u4e2d\u5c71\u516c\u56ed\u68d2\u7403\u573a", (String)"\u4e2d\u5c71\u516c\u56ed\u68d2\u7403\u573a", (String)"Zhongshan Park SZ", (String)"\u5e7f\u4e1c\u7701", (String)"\u6df1\u5733\u5e02", (String)"\u5357\u5c71\u533a", (String)"440305", (String)"518052", (String)"\u5357\u5c71\u5927\u90533109\u53f7\u6df1\u5733\u4e2d\u5c71\u516c\u56ed\u5185", (Point)InitDataRunner.gcjPoint((double)113.92655, (double)22.53612), (StadiumLevel)StadiumLevel.A, (int)2500, (int)1200, (int)100, (int)125, (int)100, (TurfType)TurfType.NATURAL));
        return list;
    }

    private static Stadium stadium(String name, String shortName, String shortNameEn, String province, String city, String district, String districtAdcode, String postalCode, String addressDetail, Point location, StadiumLevel level, int capacityTotal, int seatingCapacity, int leftM, int centerM, int rightM, TurfType turfType) {
        Stadium s = new Stadium();
        s.setName(name);
        s.setShortName(shortName);
        s.setShortNameEn(shortNameEn);
        s.setAddrProvince(province);
        s.setAddrCity(city);
        s.setAddrDistrict(district);
        s.setAddrDistrictAdcode(districtAdcode);
        s.setPostalCode(postalCode);
        s.setAddressDetail(addressDetail);
        s.setLocation(location);
        s.setLevel(level);
        s.setOperatingStatus(StadiumOperatingStatus.IN_USE);
        s.setCapacityTotal(Integer.valueOf(capacityTotal));
        s.setSeatingCapacity(Integer.valueOf(seatingCapacity));
        s.setFieldDistanceLeftM(Integer.valueOf(leftM));
        s.setFieldDistanceCenterM(Integer.valueOf(centerM));
        s.setFieldDistanceRightM(Integer.valueOf(rightM));
        s.setTurfType(turfType);
        s.setRoofType(RoofType.OPEN);
        return s;
    }

    private void ensureArticleMenuIfNeeded() {
        boolean exists = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/articles".equals(m.getPath()));
        if (exists) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu systemRoot = all.stream().filter(m -> "/system".equals(m.getPath()) || "\u7cfb\u7edf\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (systemRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        SysMenu articleMenu = this.menu(systemRoot.getId(), "\u6587\u7ae0\u7ba1\u7406", "\u6587\u7ae0\u7ba1\u7406", "/admin/articles", "AdminArticles", "views/admin/business/Content.vue", "Document", 5, createdBy);
        SysMenu saved = (SysMenu)this.sysMenuRepository.save((Object)articleMenu);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole != null) {
            this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
        }
        log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u6587\u7ae0\u7ba1\u7406");
    }

    private void ensureHistoryRecordMenuIfNeeded() {
        boolean alreadyBound;
        List all = this.sysMenuRepository.findAll();
        SysMenu existed = all.stream().filter(m -> "/admin/history-records".equals(m.getPath()) || "/admin/personnel-changes".equals(m.getPath()) || "AdminHistoryRecords".equals(m.getRouteName()) || "AdminPersonnelChanges".equals(m.getRouteName()) || "views/admin/business/HistoryRecordList.vue".equals(m.getComponent()) || "views/admin/business/PersonnelChangeList.vue".equals(m.getComponent())).findFirst().orElse(null);
        if (existed != null) {
            boolean changed = false;
            if (!"\u6cbf\u9769\u8bb0\u5f55".equals(existed.getName())) {
                existed.setName("\u6cbf\u9769\u8bb0\u5f55");
                changed = true;
            }
            if (!"\u6cbf\u9769\u8bb0\u5f55".equals(existed.getTitle())) {
                existed.setTitle("\u6cbf\u9769\u8bb0\u5f55");
                changed = true;
            }
            if (!"/admin/history-records".equals(existed.getPath())) {
                existed.setPath("/admin/history-records");
                changed = true;
            }
            if (!"AdminHistoryRecords".equals(existed.getRouteName())) {
                existed.setRouteName("AdminHistoryRecords");
                changed = true;
            }
            if (!"views/admin/business/HistoryRecordList.vue".equals(existed.getComponent())) {
                existed.setComponent("views/admin/business/HistoryRecordList.vue");
                changed = true;
            }
            if (changed) {
                this.sysMenuRepository.save((Object)existed);
            }
            return;
        }
        SysMenu businessRoot = all.stream().filter(m -> "/business".equals(m.getPath()) || "\u4e1a\u52a1\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (businessRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        SysMenu personnelMenu = this.menu(businessRoot.getId(), "\u6cbf\u9769\u8bb0\u5f55", "\u6cbf\u9769\u8bb0\u5f55", "/admin/history-records", "AdminHistoryRecords", "views/admin/business/HistoryRecordList.vue", "DocumentCopy", 4, createdBy);
        SysMenu savedMenu = (SysMenu)this.sysMenuRepository.save((Object)personnelMenu);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole != null && !(alreadyBound = this.sysRoleMenuRepository.findByRoleId(adminRole.getId()).stream().anyMatch(rm -> savedMenu.getId().equals(rm.getMenuId())))) {
            SysRoleMenu rm2 = new SysRoleMenu();
            rm2.setRoleId(adminRole.getId());
            rm2.setMenuId(savedMenu.getId());
            this.sysRoleMenuRepository.save((Object)rm2);
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u6cbf\u9769\u8bb0\u5f55");
        }
    }

    private void ensureHighlightMomentMenuIfNeeded() {
        boolean exists = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/highlight-moments".equals(m.getPath()));
        if (exists) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu businessRoot = all.stream().filter(m -> "/business".equals(m.getPath()) || "\u4e1a\u52a1\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (businessRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        int maxSort = all.stream().filter(m -> businessRoot.getId().equals(m.getParentId())).mapToInt(m -> m.getSort() != null ? m.getSort() : 0).max().orElse(0);
        SysMenu highlightMenu = this.menu(businessRoot.getId(), "\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b", "/admin/highlight-moments", "AdminHighlightMoments", "views/admin/business/HighlightMomentList.vue", "VideoPlay", maxSort + 1, createdBy);
        SysMenu savedMenu = (SysMenu)this.sysMenuRepository.save((Object)highlightMenu);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole != null) {
            this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), savedMenu.getId().longValue());
        }
    }

    private void ensureLineupTemplateMenuIfNeeded() {
        boolean exists = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/lineup-templates".equals(m.getPath()));
        if (exists) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu businessRoot = all.stream().filter(m -> "/business".equals(m.getPath()) || "\u4e1a\u52a1\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (businessRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        int maxSort = all.stream().filter(m -> businessRoot.getId().equals(m.getParentId())).mapToInt(m -> m.getSort() != null ? m.getSort() : 0).max().orElse(0);
        SysMenu lineupMenu = this.menu(businessRoot.getId(), "\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f", "/admin/lineup-templates", "AdminLineupTemplates", "views/admin/business/LineupTemplates.vue", "Grid", maxSort + 1, createdBy);
        SysMenu savedMenu = (SysMenu)this.sysMenuRepository.save((Object)lineupMenu);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole != null) {
            this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), savedMenu.getId().longValue());
        }
        log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u9635\u5bb9\u6a21\u677f");
    }

    private void ensurePlayerClaimMenuIfNeeded() {
        boolean exists = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/player-claims".equals(m.getPath()));
        if (exists) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu businessRoot = all.stream().filter(m -> "/business".equals(m.getPath()) || "\u4e1a\u52a1\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (businessRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        int maxSort = all.stream().filter(m -> businessRoot.getId().equals(m.getParentId())).mapToInt(m -> m.getSort() != null ? m.getSort() : 0).max().orElse(0);
        SysMenu claimMenu = this.menu(businessRoot.getId(), "\u7403\u5458\u8ba4\u9886\u5ba1\u6838", "\u7403\u5458\u8ba4\u9886\u5ba1\u6838", "/admin/player-claims", "AdminPlayerClaimReview", "views/admin/account/PlayerClaimReview.vue", "Stamp", maxSort + 1, createdBy);
        SysMenu savedMenu = (SysMenu)this.sysMenuRepository.save((Object)claimMenu);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole != null) {
            this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), savedMenu.getId().longValue());
        }
        log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u7403\u5458\u8ba4\u9886\u5ba1\u6838");
    }

    private void ensureTenantManagementMenusIfNeeded() {
        boolean hasTenants = this.sysMenuRepository.findAll().stream().anyMatch(m -> "/admin/tenants".equals(m.getPath()));
        if (hasTenants) {
            return;
        }
        List all = this.sysMenuRepository.findAll();
        SysMenu systemRoot = all.stream().filter(m -> "/system".equals(m.getPath()) || "\u7cfb\u7edf\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (systemRoot == null) {
            return;
        }
        long createdBy = all.stream().map(BaseEntity::getCreatedBy).filter(id -> id != null && id > 0L).findFirst().orElse(1L);
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        SysMenu m2 = this.menu(systemRoot.getId(), "\u79df\u6237\u7ba1\u7406", "\u79df\u6237\u7ba1\u7406", "/admin/tenants", "AdminTenants", "views/admin/system/Tenants.vue", "OfficeBuilding", 1, createdBy);
        m2 = (SysMenu)this.sysMenuRepository.save((Object)m2);
        if (adminRole != null) {
            this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), m2.getId().longValue());
        }
        log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u79df\u6237\u7ba1\u7406");
    }

    private void bindMenuToAdminIfNeeded(long roleId, long menuId) {
        boolean already = this.sysRoleMenuRepository.findByRoleId(Long.valueOf(roleId)).stream().anyMatch(rm -> Objects.equals(menuId, rm.getMenuId()));
        if (already) {
            return;
        }
        SysRoleMenu rm2 = new SysRoleMenu();
        rm2.setRoleId(Long.valueOf(roleId));
        rm2.setMenuId(Long.valueOf(menuId));
        this.sysRoleMenuRepository.save((Object)rm2);
    }

    private void ensureMediaIconGalleryMenusAndApisIfNeeded() {
        SysMenu saved;
        SysMenu m2;
        int maxSort;
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        List apis = List.of((Object[])new ApiDef[]{new ApiDef("/sys/media-icon/list", "GET", "\u67e5\u8be2 SVG \u56fe\u6807\u5217\u8868", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/:id", "GET", "\u67e5\u8be2 SVG \u56fe\u6807\u8be6\u60c5", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/create", "POST", "\u521b\u5efa SVG \u56fe\u6807", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/update/:id", "PUT", "\u66f4\u65b0 SVG \u56fe\u6807", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/delete/:id", "DELETE", "\u5220\u9664 SVG \u56fe\u6807", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/upload", "POST", "\u4e0a\u4f20 SVG \u6587\u4ef6", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-gallery/list", "GET", "\u67e5\u8be2\u56fe\u5e93\u5217\u8868", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/:id", "GET", "\u67e5\u8be2\u56fe\u5e93\u9879\u8be6\u60c5", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/create", "POST", "\u521b\u5efa\u56fe\u5e93\u9879", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/update/:id", "PUT", "\u66f4\u65b0\u56fe\u5e93\u9879", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/delete/:id", "DELETE", "\u5220\u9664\u56fe\u5e93\u9879", "\u56fe\u5e93\u7ba1\u7406")});
        ArrayList<SysApi> existingApis = new ArrayList<SysApi>(this.sysApiRepository.findAll());
        for (ApiDef a : apis) {
            boolean exists = existingApis.stream().anyMatch(e -> a.path.equals(e.getPath()) && a.method.equalsIgnoreCase(e.getMethod()));
            if (exists) continue;
            SysApi api = new SysApi();
            api.setPath(a.path);
            api.setMethod(a.method);
            api.setDescription(a.desc);
            api.setGroupName(a.group);
            api.setCreatedBy(Long.valueOf(opId));
            api.setUpdatedBy(Long.valueOf(opId));
            existingApis.add((SysApi)this.sysApiRepository.save((Object)api));
        }
        List allMenus = this.sysMenuRepository.findAll();
        boolean hasIcons = allMenus.stream().anyMatch(m -> "/admin/media-icons".equals(m.getPath()));
        boolean hasGallery = allMenus.stream().anyMatch(m -> "/admin/media-gallery".equals(m.getPath()));
        if (hasIcons && hasGallery) {
            return;
        }
        SysMenu systemRoot = allMenus.stream().filter(m -> "/system".equals(m.getPath()) || "\u7cfb\u7edf\u7ba1\u7406".equals(m.getTitle())).findFirst().orElse(null);
        if (systemRoot == null) {
            return;
        }
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (!hasIcons) {
            maxSort = this.sysMenuRepository.findAll().stream().filter(m -> systemRoot.getId().equals(m.getParentId())).mapToInt(m -> m.getSort() != null ? m.getSort() : 0).max().orElse(0);
            m2 = this.menu(systemRoot.getId(), "\u56fe\u6807\u7ba1\u7406", "\u56fe\u6807\u7ba1\u7406", "/admin/media-icons", "AdminMediaIcons", "views/admin/system/MediaIcons.vue", "Grid", maxSort + 1, opId);
            saved = (SysMenu)this.sysMenuRepository.save((Object)m2);
            if (adminRole != null) {
                this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
            }
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u56fe\u6807\u7ba1\u7406");
        }
        if (!hasGallery) {
            maxSort = this.sysMenuRepository.findAll().stream().filter(m -> systemRoot.getId().equals(m.getParentId())).mapToInt(m -> m.getSort() != null ? m.getSort() : 0).max().orElse(0);
            m2 = this.menu(systemRoot.getId(), "\u56fe\u5e93\u7ba1\u7406", "\u56fe\u5e93\u7ba1\u7406", "/admin/media-gallery", "AdminMediaGallery", "views/admin/system/MediaGallery.vue", "Picture", maxSort + 1, opId);
            saved = (SysMenu)this.sysMenuRepository.save((Object)m2);
            if (adminRole != null) {
                this.bindMenuToAdminIfNeeded(adminRole.getId().longValue(), saved.getId().longValue());
            }
            log.info("\u5df2\u8865\u63d2\u83dc\u5355\uff1a\u56fe\u5e93\u7ba1\u7406");
        }
    }

    private void ensureTenantApisIfNeeded() {
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        List apis = List.of((Object[])new ApiDef[]{new ApiDef("/sys/tenant/list", "GET", "\u67e5\u8be2\u79df\u6237\u5217\u8868", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/tenant/scope-options", "GET", "\u79df\u6237\u6570\u636e\u8303\u56f4\u4e0b\u62c9", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/tenant/my-tenants", "GET", "\u5f53\u524d\u7528\u6237\u79df\u6237\u5217\u8868", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/tenant/:id", "GET", "\u67e5\u8be2\u79df\u6237\u8be6\u60c5", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/tenant/create", "POST", "\u521b\u5efa\u79df\u6237", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/tenant/update/:id", "PUT", "\u66f4\u65b0\u79df\u6237", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/tenant/delete/:id", "DELETE", "\u5220\u9664\u79df\u6237", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/user-tenant/list", "GET", "\u67e5\u8be2\u7528\u6237\u79df\u6237\u6210\u5458", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/user-tenant/replace", "PUT", "\u66ff\u6362\u7528\u6237\u79df\u6237\u6210\u5458", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/user-data-scope", "GET", "\u67e5\u8be2\u7528\u6237\u6570\u636e\u8303\u56f4", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/sys/user-data-scope", "PUT", "\u4fdd\u5b58\u7528\u6237\u6570\u636e\u8303\u56f4", "\u79df\u6237\u7ba1\u7406"), new ApiDef("/auth/switch-tenant", "POST", "\u5207\u6362\u79df\u6237", "\u8ba4\u8bc1")});
        ArrayList<SysApi> existing = new ArrayList<SysApi>(this.sysApiRepository.findAll());
        for (ApiDef a : apis) {
            boolean exists = existing.stream().anyMatch(e -> a.path.equals(e.getPath()) && a.method.equalsIgnoreCase(e.getMethod()));
            if (exists) continue;
            SysApi api = new SysApi();
            api.setPath(a.path);
            api.setMethod(a.method);
            api.setDescription(a.desc);
            api.setGroupName(a.group);
            api.setCreatedBy(Long.valueOf(opId));
            api.setUpdatedBy(Long.valueOf(opId));
            SysApi saved = (SysApi)this.sysApiRepository.save((Object)api);
            existing.add(saved);
        }
    }

    private boolean shouldSeedApi(ApiDef a) {
        String p = a.path();
        if (p == null) {
            return true;
        }
        if (p.startsWith("/sys/article") || p.startsWith("/sys/notice")) {
            return this.initSeedProperties.isSeedContentMenusAndApis();
        }
        if (p.startsWith("/sys/login-log") || p.startsWith("/sys/operation-log")) {
            return this.initSeedProperties.isSeedAuditMenusAndApis();
        }
        if (p.startsWith("/league") || p.startsWith("/team") || p.startsWith("/coach") || p.startsWith("/event") || p.startsWith("/game") || p.startsWith("/stats/") || p.startsWith("/player") || p.startsWith("/personnel-change") || p.startsWith("/highlight-moment")) {
            return this.initSeedProperties.isSeedBusinessMenusAndApis();
        }
        return true;
    }

    private void seedMenus(long adminId) {
        SysMenu m1 = (SysMenu)this.sysMenuRepository.save((Object)this.menu(Long.valueOf(0L), "\u5de5\u4f5c\u53f0", "\u5de5\u4f5c\u53f0", "/admin/dashboard", "AdminDashboard", "views/admin/Dashboard.vue", "Odometer", 0, adminId));
        SysMenu m2 = (SysMenu)this.sysMenuRepository.save((Object)this.menu(Long.valueOf(0L), "\u7cfb\u7edf\u7ba1\u7406", "\u7cfb\u7edf\u7ba1\u7406", "/system", null, null, "Setting", 1, adminId));
        SysMenu m4 = (SysMenu)this.sysMenuRepository.save((Object)this.menu(Long.valueOf(0L), "\u7cfb\u7edf\u76d1\u63a7", "\u7cfb\u7edf\u76d1\u63a7", "/monitor", null, null, "Monitor", 2, adminId));
        SysMenu m3 = (SysMenu)this.sysMenuRepository.save((Object)this.menu(Long.valueOf(0L), "\u4e1a\u52a1\u7ba1\u7406", "\u4e1a\u52a1\u7ba1\u7406", "/business", null, null, "Trophy", 3, adminId));
        int ms = 0;
        ArrayList<SysMenu> monitorChildren = new ArrayList<SysMenu>();
        monitorChildren.add(this.menu(m4.getId(), "\u6570\u636e\u76d1\u63a7", "\u6570\u636e\u76d1\u63a7", "/admin/monitor/data", "AdminMonitorData", "views/admin/monitor/DataMonitor.vue", "DataLine", ms++, adminId));
        monitorChildren.add(this.menu(m4.getId(), "\u670d\u52a1\u76d1\u63a7", "\u670d\u52a1\u76d1\u63a7", "/admin/monitor/server", "AdminMonitorServer", "views/admin/monitor/ServerMonitor.vue", "Cpu", ms++, adminId));
        monitorChildren.add(this.menu(m4.getId(), "\u7f13\u5b58\u76d1\u63a7", "\u7f13\u5b58\u76d1\u63a7", "/admin/monitor/cache", "AdminMonitorCache", "views/admin/monitor/CacheMonitor.vue", "Coin", ms++, adminId));
        monitorChildren.add(this.menu(m4.getId(), "\u7f13\u5b58\u5217\u8868", "\u7f13\u5b58\u5217\u8868", "/admin/monitor/cache-list", "AdminMonitorCacheList", "views/admin/monitor/CacheList.vue", "List", ms++, adminId));
        monitorChildren.add(this.menu(m4.getId(), "DevTools \u4e0a\u62a5", "DevTools \u4e0a\u62a5", "/admin/monitor/portal-devtools-report", "AdminMonitorPortalDevtoolsReport", "views/admin/monitor/PortalDevtoolsReportList.vue", "Warning", ms++, adminId));
        monitorChildren.add(this.menu(m4.getId(), "\u95e8\u6237\u8bbf\u95ee\u6253\u70b9", "\u95e8\u6237\u8bbf\u95ee\u6253\u70b9", "/admin/monitor/portal-visit-hit", "AdminMonitorPortalVisitHit", "views/admin/monitor/PortalVisitHitList.vue", "Histogram", ms++, adminId));
        monitorChildren.add(this.menu(m4.getId(), "\u610f\u89c1\u53cd\u9988", "\u610f\u89c1\u53cd\u9988", "/admin/monitor/portal-feedback", "AdminMonitorPortalFeedback", "views/admin/monitor/PortalFeedbackList.vue", "ChatLineRound", ms++, adminId));
        if (this.initSeedProperties.isSeedAuditMenusAndApis()) {
            monitorChildren.add(this.menu(m4.getId(), "\u767b\u5f55\u5386\u53f2", "\u767b\u5f55\u5386\u53f2", "/admin/login-logs", "AdminLoginLogs", "views/admin/system/LoginLogs.vue", "Key", ms++, adminId));
            monitorChildren.add(this.menu(m4.getId(), "\u64cd\u4f5c\u5386\u53f2", "\u64cd\u4f5c\u5386\u53f2", "/admin/operation-logs", "AdminOperationLogs", "views/admin/system/OperationLogs.vue", "DocumentCopy", ms++, adminId));
        }
        this.sysMenuRepository.saveAll(monitorChildren);
        int ss = 0;
        ArrayList<SysMenu> systemChildren = new ArrayList<SysMenu>();
        systemChildren.add(this.menu(m2.getId(), "\u7528\u6237\u7ba1\u7406", "\u7528\u6237\u7ba1\u7406", "/admin/users", "AdminUsers", "views/admin/system/Users.vue", "User", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "\u89d2\u8272\u7ba1\u7406", "\u89d2\u8272\u7ba1\u7406", "/admin/roles", "AdminRoles", "views/admin/system/Roles.vue", "UserFilled", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "\u83dc\u5355\u7ba1\u7406", "\u83dc\u5355\u7ba1\u7406", "/admin/menus", "AdminMenus", "views/admin/system/Menus.vue", "Menu", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "API\u7ba1\u7406", "API\u7ba1\u7406", "/admin/apis", "AdminApis", "views/admin/system/Apis.vue", "Connection", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "IP\u7b56\u7565\u7ba1\u7406", "IP\u7b56\u7565\u7ba1\u7406", "/admin/monitor/ip-access-policy", "AdminMonitorIpAccessPolicy", "views/admin/monitor/IpAccessPolicy.vue", "Lock", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "IP\u5730\u7406\u4fe1\u606f", "IP\u5730\u7406\u4fe1\u606f", "/admin/ip-location-cache", "AdminIpLocationCache", "views/admin/system/IpLocationCache.vue", "Location", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "\u79df\u6237\u7ba1\u7406", "\u79df\u6237\u7ba1\u7406", "/admin/tenants", "AdminTenants", "views/admin/system/Tenants.vue", "OfficeBuilding", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "\u914d\u7f6e\u7ba1\u7406", "\u914d\u7f6e\u7ba1\u7406", "/admin/config", "AdminConfig", "views/admin/system/AppConfig.vue", "Tools", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "\u5b57\u5178\u7ba1\u7406", "\u5b57\u5178\u7ba1\u7406", "/admin/dict", "AdminDict", "views/admin/system/Dict.vue", "Collection", ss++, adminId));
        if (this.initSeedProperties.isSeedContentMenusAndApis()) {
            systemChildren.add(this.menu(m2.getId(), "\u6587\u7ae0\u7ba1\u7406", "\u6587\u7ae0\u7ba1\u7406", "/admin/articles", "AdminArticles", "views/admin/business/Content.vue", "Document", ss++, adminId));
            systemChildren.add(this.menu(m2.getId(), "\u901a\u77e5\u7ba1\u7406", "\u901a\u77e5\u7ba1\u7406", "/admin/announcements", "AdminAnnouncements", "views/admin/system/Announcements.vue", "Bell", ss++, adminId));
        }
        systemChildren.add(this.menu(m2.getId(), "\u8d44\u6e90\u6587\u4ef6", "\u8d44\u6e90\u6587\u4ef6", "/admin/resources", "AdminResources", "views/admin/system/Resources.vue", "Folder", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "\u56fe\u6807\u7ba1\u7406", "\u56fe\u6807\u7ba1\u7406", "/admin/media-icons", "AdminMediaIcons", "views/admin/system/MediaIcons.vue", "Grid", ss++, adminId));
        systemChildren.add(this.menu(m2.getId(), "\u56fe\u5e93\u7ba1\u7406", "\u56fe\u5e93\u7ba1\u7406", "/admin/media-gallery", "AdminMediaGallery", "views/admin/system/MediaGallery.vue", "Picture", ss++, adminId));
        this.sysMenuRepository.saveAll(systemChildren);
        int bs = 0;
        ArrayList<SysMenu> businessChildren = new ArrayList<SysMenu>();
        if (this.initSeedProperties.isSeedBusinessMenusAndApis()) {
            businessChildren.add(this.menu(m3.getId(), "\u8054\u76df\u7ba1\u7406", "\u8054\u76df\u7ba1\u7406", "/admin/leagues", "AdminLeagues", "views/admin/business/Leagues.vue", "Medal", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u7403\u961f\u7ba1\u7406", "\u7403\u961f\u7ba1\u7406", "/admin/teams", "AdminTeams", "views/admin/business/Teams.vue", "School", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f", "/admin/lineup-templates", "AdminLineupTemplates", "views/admin/business/LineupTemplates.vue", "Grid", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u6559\u7ec3\u7ba1\u7406", "\u6559\u7ec3\u7ba1\u7406", "/admin/coaches", "AdminCoaches", "views/admin/business/Coaches.vue", "User", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u7403\u5458\u7ba1\u7406", "\u7403\u5458\u7ba1\u7406", "/admin/players", "AdminPlayerList", "views/admin/business/Players.vue", "User", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u7403\u5458\u8ba4\u9886\u5ba1\u6838", "\u7403\u5458\u8ba4\u9886\u5ba1\u6838", "/admin/player-claims", "AdminPlayerClaimReview", "views/admin/account/PlayerClaimReview.vue", "Stamp", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u6cbf\u9769\u8bb0\u5f55", "\u6cbf\u9769\u8bb0\u5f55", "/admin/history-records", "AdminHistoryRecords", "views/admin/business/HistoryRecordList.vue", "DocumentCopy", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b", "/admin/highlight-moments", "AdminHighlightMoments", "views/admin/business/HighlightMomentList.vue", "VideoPlay", bs++, adminId));
            businessChildren.add(this.menu(m3.getId(), "\u8d5b\u4e8b\u7ba1\u7406", "\u8d5b\u4e8b\u7ba1\u7406", "/admin/events", "AdminEvents", "views/admin/business/Events.vue", "Calendar", bs++, adminId));
        }
        if (this.initSeedProperties.isSeedContentMenusAndApis()) {
            businessChildren.add(this.menu(m3.getId(), "\u5185\u5bb9\u7ba1\u7406", "\u5185\u5bb9\u7ba1\u7406", "/admin/content", "AdminContent", "views/admin/business/Content.vue", "Document", bs++, adminId));
        }
        businessChildren.add(this.menu(m3.getId(), "\u7403\u573a\u7ba1\u7406", "\u7403\u573a\u7ba1\u7406", "/admin/stadiums", "AdminStadiums", "views/admin/business/Stadiums.vue", "Place", bs++, adminId));
        this.sysMenuRepository.saveAll(businessChildren);
    }

    private SysMenu menu(Long parentId, String name, String title, String path, String routeName, String component, String icon, int sort, long adminId) {
        SysMenu m = new SysMenu();
        m.setParentId(parentId);
        m.setName(name);
        m.setTitle(title);
        m.setPath(path);
        m.setRouteName(routeName);
        m.setComponent(component);
        m.setIcon(icon);
        m.setSort(Integer.valueOf(sort));
        m.setCreatedBy(Long.valueOf(adminId));
        m.setUpdatedBy(Long.valueOf(adminId));
        boolean directory = !(path == null || !"/system".equals(path) && !"/business".equals(path) && !"/monitor".equals(path) || component != null && !component.isBlank());
        m.setMenuType(Integer.valueOf(directory ? 1 : 2));
        return m;
    }

    private void ensureMenuDirectoryTypesAndDefaultButtons() {
        List all = this.sysMenuRepository.findAll();
        boolean changed = false;
        for (SysMenu m2 : all) {
            String p = m2.getPath();
            if (p != null && ("/system".equals(p) || "/business".equals(p) || "/monitor".equals(p)) && (m2.getComponent() == null || m2.getComponent().isBlank())) {
                if (Objects.equals(m2.getMenuType(), 1)) continue;
                m2.setMenuType(Integer.valueOf(1));
                changed = true;
                continue;
            }
            if (Objects.equals(m2.getMenuType(), 3) || m2.getMenuType() != null && m2.getPermission() != null && !m2.getPermission().isBlank()) continue;
            m2.setMenuType(Integer.valueOf(2));
            changed = true;
        }
        if (changed) {
            this.sysMenuRepository.saveAll((Iterable)all);
            log.info("\u5df2\u6821\u6b63\u83dc\u5355\u7c7b\u578b\uff08\u76ee\u5f55/\u83dc\u5355\uff09");
        }
        Map<String, SysMenu> byPath = this.sysMenuRepository.findAll().stream().filter(x -> x.getPath() != null && !x.getPath().isBlank()).collect(Collectors.toMap(SysMenu::getPath, x -> x, (a, b) -> a));
        Map apiLookup = this.buildApiPathMethodLookup();
        long opId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        List seeds = List.of((Object[])new ButtonSeed[]{new ButtonSeed("/admin/events", "\u53d1\u5e03\u8d5b\u4e8b", "business:event:create", 1, new String[]{"POST|/event/create"}), new ButtonSeed("/admin/events", "\u7f16\u8f91\u8d5b\u4e8b", "business:event:edit", 2, new String[]{"PUT|/event/update/:id"}), new ButtonSeed("/admin/events", "\u5220\u9664\u8d5b\u4e8b", "business:event:delete", 3, new String[]{"DELETE|/event/delete/:id"}), new ButtonSeed("/admin/events", "\u8d5b\u7a0b\u4e0e\u7ed3\u679c", "business:event:games", 4, new String[]{"GET|/game/list"}), new ButtonSeed("/admin/events", "\u5bf9\u6218\u8be6\u60c5", "business:event:bracket", 5, new String[]{"GET|/event/:id"}), new ButtonSeed("/admin/events", "\u5bfc\u5165\u6bd4\u8d5b\u6570\u636e", "business:game:import", 6, new String[]{"POST|/event/:eventId/import-game-result"}), new ButtonSeed("/admin/events", "\u65b0\u589e\u6bd4\u8d5b", "business:game:create", 7, new String[]{"POST|/game/create"}), new ButtonSeed("/admin/events", "\u7f16\u8f91\u6bd4\u8d5b", "business:game:edit", 8, new String[]{"PUT|/game/update/:id"}), new ButtonSeed("/admin/events", "\u5220\u9664\u6bd4\u8d5b", "business:game:delete", 9, new String[]{"DELETE|/game/delete/:id"}), new ButtonSeed("/admin/events", "\u5bfc\u51fa\u6bd4\u8d5b\u6570\u636e", "business:game:export", 10, new String[0]), new ButtonSeed("/admin/events", "\u4fdd\u5b58\u6bd4\u8d5b\u7ed3\u679c", "business:game:saveResult", 11, new String[]{"POST|/game/:id/save-result"}), new ButtonSeed("/admin/events", "\u4e00\u952e\u62fc\u56fe", "business:game:collage", 12, new String[0]), new ButtonSeed("/admin/teams", "\u65b0\u589e\u7403\u961f", "business:team:create", 1, new String[]{"POST|/team/create"}), new ButtonSeed("/admin/teams", "\u7f16\u8f91\u7403\u961f", "business:team:edit", 2, new String[]{"PUT|/team/update/:id"}), new ButtonSeed("/admin/teams", "\u5220\u9664\u7403\u961f", "business:team:delete", 3, new String[]{"DELETE|/team/delete/:id"}), new ButtonSeed("/admin/teams", "\u7ba1\u7406\u7403\u5458", "business:team:managePlayers", 4, new String[]{"GET|/player/list"}), new ButtonSeed("/admin/lineup-templates", "\u9635\u5bb9\u6a21\u677f\u5206\u9875\u5217\u8868", "business:lineup-template:manage", 1, new String[]{"GET|/lineup-template/list"}), new ButtonSeed("/admin/lineup-templates", "\u9635\u5bb9\u6a21\u677f\u5217\u8868", "business:lineup-template:manage", 2, new String[]{"GET|/team/:teamId/lineup-template/list"}), new ButtonSeed("/admin/lineup-templates", "\u9635\u5bb9\u6a21\u677f\u8be6\u60c5", "business:lineup-template:manage", 3, new String[]{"GET|/team/:teamId/lineup-template/:id"}), new ButtonSeed("/admin/lineup-templates", "\u65b0\u589e\u9635\u5bb9\u6a21\u677f", "business:lineup-template:manage", 4, new String[]{"POST|/team/:teamId/lineup-template/create"}), new ButtonSeed("/admin/lineup-templates", "\u4ece\u6bd4\u8d5b\u590d\u5236\u6a21\u677f", "business:lineup-template:manage", 5, new String[]{"POST|/team/:teamId/lineup-template/copy-from-game"}), new ButtonSeed("/admin/lineup-templates", "\u7f16\u8f91\u9635\u5bb9\u6a21\u677f", "business:lineup-template:manage", 6, new String[]{"PUT|/team/:teamId/lineup-template/update/:id"}), new ButtonSeed("/admin/lineup-templates", "\u5220\u9664\u9635\u5bb9\u6a21\u677f", "business:lineup-template:manage", 7, new String[]{"DELETE|/team/:teamId/lineup-template/delete/:id"}), new ButtonSeed("/admin/lineup-templates", "\u7403\u961f\u7403\u5458\u9009\u9879", "business:lineup-template:manage", 8, new String[]{"GET|/player/team-options"}), new ButtonSeed("/admin/users", "\u65b0\u589e\u7528\u6237", "sys:user:create", 1, new String[]{"POST|/sys/user/create"}), new ButtonSeed("/admin/users", "\u7f16\u8f91\u7528\u6237", "sys:user:edit", 2, new String[]{"PUT|/sys/user/update/:id"}), new ButtonSeed("/admin/users", "\u5220\u9664\u7528\u6237", "sys:user:delete", 3, new String[]{"DELETE|/sys/user/delete/:id"}), new ButtonSeed("/admin/roles", "\u65b0\u589e\u89d2\u8272", "sys:role:create", 1, new String[]{"POST|/sys/role/create"}), new ButtonSeed("/admin/roles", "\u7f16\u8f91\u89d2\u8272", "sys:role:edit", 2, new String[]{"PUT|/sys/role/update/:id"}), new ButtonSeed("/admin/roles", "\u8bbe\u7f6e\u6743\u9650", "sys:role:permission", 3, new String[]{"PUT|/sys/role/update/:id"}), new ButtonSeed("/admin/menus", "\u65b0\u589e\u83dc\u5355", "sys:menu:create", 1, new String[]{"POST|/sys/menu/create"}), new ButtonSeed("/admin/menus", "\u7f16\u8f91\u83dc\u5355", "sys:menu:edit", 2, new String[]{"PUT|/sys/menu/update/:id"}), new ButtonSeed("/admin/menus", "\u5220\u9664\u83dc\u5355", "sys:menu:delete", 3, new String[]{"DELETE|/sys/menu/delete/:id"})});
        int inserted = 0;
        for (ButtonSeed s : seeds) {
            boolean exists;
            SysMenu parent = byPath.get(s.parentPath);
            if (parent == null || (exists = this.sysMenuRepository.findAll().stream().anyMatch(m -> Objects.equals(m.getParentId(), parent.getId()) && s.permission.equals(m.getPermission())))) continue;
            SysMenu b2 = new SysMenu();
            b2.setParentId(parent.getId());
            b2.setName(s.title);
            b2.setTitle(s.title);
            b2.setMenuType(Integer.valueOf(3));
            b2.setPermission(s.permission);
            b2.setSort(Integer.valueOf(s.sort));
            b2.setHideMenu(Integer.valueOf(0));
            b2.setCreatedBy(Long.valueOf(opId));
            b2.setUpdatedBy(Long.valueOf(opId));
            b2 = (SysMenu)this.sysMenuRepository.save((Object)b2);
            ++inserted;
            if (s.apiBindings == null || s.apiBindings.length <= 0) continue;
            for (String bind : s.apiBindings) {
                Long apiId = this.resolveApiId(bind, apiLookup);
                if (apiId == null) continue;
                SysMenuApi link = new SysMenuApi();
                link.setMenuId(b2.getId());
                link.setApiId(apiId);
                link.setCreatedBy(Long.valueOf(opId));
                link.setUpdatedBy(Long.valueOf(opId));
                this.sysMenuApiRepository.save((Object)link);
            }
        }
        if (inserted > 0) {
            log.info("\u5df2\u8865\u63d2\u9ed8\u8ba4\u83dc\u5355\u6309\u94ae {} \u6761\uff08\u53ef\u4e0e\u89d2\u8272 permission \u5bf9\u7167\uff09", (Object)inserted);
        }
    }

    private Map<String, Long> buildApiPathMethodLookup() {
        HashMap<String, Long> map = new HashMap<String, Long>();
        for (SysApi a : this.sysApiRepository.findAll()) {
            if (a.getPath() == null || a.getMethod() == null) continue;
            map.put(a.getMethod().trim().toUpperCase() + "|" + a.getPath().trim(), a.getId());
        }
        return map;
    }

    private Long resolveApiId(String methodPath, Map<String, Long> directLookup) {
        String template;
        int bar = methodPath.indexOf(124);
        if (bar < 0) {
            return null;
        }
        String method = methodPath.substring(0, bar).trim().toUpperCase();
        Long id = directLookup.get(method + "|" + (template = methodPath.substring(bar + 1).trim()));
        if (id != null) {
            return id;
        }
        String[] tSeg = template.split("/");
        for (SysApi a : this.sysApiRepository.findAll()) {
            if (a.getMethod() == null || a.getPath() == null || !a.getMethod().trim().equalsIgnoreCase(method) || !InitDataRunner.pathTemplateMatches((String)a.getPath(), (String)template)) continue;
            return a.getId();
        }
        return null;
    }

    private static boolean pathTemplateMatches(String registered, String seedTemplate) {
        String[] b;
        String[] a = registered.split("/");
        if (a.length != (b = seedTemplate.split("/")).length) {
            return false;
        }
        for (int i = 0; i < a.length; ++i) {
            String x = a[i];
            String y = b[i];
            if (y.startsWith(":") || Objects.equals(x, y)) continue;
            return false;
        }
        return true;
    }

    private void ensureAdminRoleBindsAllMenusIfNeeded() {
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole == null) {
            return;
        }
        Set bound = this.sysRoleMenuRepository.findByRoleId(adminRole.getId()).stream().map(SysRoleMenu::getMenuId).collect(Collectors.toCollection(HashSet::new));
        long opUserId = this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").map(BaseEntity::getId).orElse(1L);
        int added = 0;
        for (SysMenu m : this.sysMenuRepository.findAll()) {
            if (bound.contains(m.getId())) continue;
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(adminRole.getId());
            rm.setMenuId(m.getId());
            rm.setCreatedBy(Long.valueOf(opUserId));
            rm.setUpdatedBy(Long.valueOf(opUserId));
            this.sysRoleMenuRepository.save((Object)rm);
            bound.add(m.getId());
            ++added;
        }
        if (added > 0) {
            log.info("\u5df2\u4e3a\u8d85\u7ea7\u7ba1\u7406\u5458\u8865\u7ed1 {} \u4e2a\u83dc\u5355\u8282\u70b9\uff08\u542b\u65b0\u589e\u6309\u94ae\uff09", (Object)added);
        }
    }

    private void seedApis(long adminId) {
        List apis = List.of((Object[])new ApiDef[]{new ApiDef("/auth/login", "POST", "\u767b\u5f55", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/options", "GET", "\u67e5\u8be2\u767b\u5f55\u9a8c\u8bc1\u7801\u914d\u7f6e", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/image", "GET", "\u751f\u6210\u767b\u5f55\u56fe\u5f62\u9a8c\u8bc1\u7801", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/verify-click", "POST", "\u6821\u9a8c\u70b9\u9009\u9a8c\u8bc1\u7801", "\u8ba4\u8bc1"), new ApiDef("/auth/captcha/verify-drag", "POST", "\u6821\u9a8c\u62d6\u62fd\u62fc\u56fe\u9a8c\u8bc1\u7801", "\u8ba4\u8bc1"), new ApiDef("/auth/me", "GET", "\u67e5\u8be2\u5f53\u524d\u7528\u6237", "\u8ba4\u8bc1"), new ApiDef("/auth/change-password", "PATCH", "\u4fee\u6539\u5bc6\u7801", "\u8ba4\u8bc1"), new ApiDef("/sys/user/list", "GET", "\u67e5\u8be2\u7528\u6237\u5217\u8868", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/:id", "GET", "\u67e5\u8be2\u7528\u6237\u8be6\u60c5", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/create", "POST", "\u521b\u5efa\u7528\u6237", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/update/:id", "PUT", "\u66f4\u65b0\u7528\u6237", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/user/delete/:id", "DELETE", "\u5220\u9664\u7528\u6237", "\u7528\u6237\u7ba1\u7406"), new ApiDef("/sys/role/list", "GET", "\u67e5\u8be2\u89d2\u8272\u5217\u8868", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/assign-options", "GET", "\u7528\u6237\u7ba1\u7406\u5206\u914d\u89d2\u8272\u9009\u9879", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/:id", "GET", "\u67e5\u8be2\u89d2\u8272\u8be6\u60c5", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/create", "POST", "\u521b\u5efa\u89d2\u8272", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/update/:id", "PUT", "\u66f4\u65b0\u89d2\u8272", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/role/delete/:id", "DELETE", "\u5220\u9664\u89d2\u8272", "\u89d2\u8272\u7ba1\u7406"), new ApiDef("/sys/menu/list", "GET", "\u67e5\u8be2\u83dc\u5355\u5217\u8868", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/menu/create", "POST", "\u521b\u5efa\u83dc\u5355", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/menu/update/:id", "PUT", "\u66f4\u65b0\u83dc\u5355", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/menu/delete/:id", "DELETE", "\u5220\u9664\u83dc\u5355", "\u83dc\u5355\u7ba1\u7406"), new ApiDef("/sys/api/list", "GET", "\u67e5\u8be2API\u5217\u8868", "API\u7ba1\u7406"), new ApiDef("/sys/api/create", "POST", "\u521b\u5efaAPI", "API\u7ba1\u7406"), new ApiDef("/sys/api/update/:id", "PUT", "\u66f4\u65b0API", "API\u7ba1\u7406"), new ApiDef("/sys/api/delete/:id", "DELETE", "\u5220\u9664API", "API\u7ba1\u7406"), new ApiDef("/sys/dict/type/list", "GET", "\u67e5\u8be2\u5b57\u5178\u7c7b\u578b\u5217\u8868", "\u5b57\u5178\u7c7b\u578b\u7ba1\u7406"), new ApiDef("/sys/dict/type/create", "POST", "\u521b\u5efa\u5b57\u5178\u7c7b\u578b", "\u5b57\u5178\u7c7b\u578b\u7ba1\u7406"), new ApiDef("/sys/dict/type/update/:id", "PUT", "\u66f4\u65b0\u5b57\u5178\u7c7b\u578b", "\u5b57\u5178\u7c7b\u578b\u7ba1\u7406"), new ApiDef("/sys/dict/type/delete/:id", "DELETE", "\u5220\u9664\u5b57\u5178\u7c7b\u578b", "\u5b57\u5178\u7c7b\u578b\u7ba1\u7406"), new ApiDef("/sys/dict/data/list", "GET", "\u67e5\u8be2\u5b57\u5178\u6570\u636e\u5217\u8868", "\u5b57\u5178\u6570\u636e\u7ba1\u7406"), new ApiDef("/sys/dict/data/create", "POST", "\u521b\u5efa\u5b57\u5178\u6570\u636e", "\u5b57\u5178\u6570\u636e\u7ba1\u7406"), new ApiDef("/sys/dict/data/update/:id", "PUT", "\u66f4\u65b0\u5b57\u5178\u6570\u636e", "\u5b57\u5178\u6570\u636e\u7ba1\u7406"), new ApiDef("/sys/dict/data/delete/:id", "DELETE", "\u5220\u9664\u5b57\u5178\u6570\u636e", "\u5b57\u5178\u6570\u636e\u7ba1\u7406"), new ApiDef("/sys/dict/list", "GET", "\u67e5\u8be2\u5b57\u5178\u5217\u8868", "\u5b57\u5178\u7ba1\u7406"), new ApiDef("/sys/dict/create", "POST", "\u521b\u5efa\u5b57\u5178", "\u5b57\u5178\u7ba1\u7406"), new ApiDef("/sys/dict/update/:id", "PUT", "\u66f4\u65b0\u5b57\u5178", "\u5b57\u5178\u7ba1\u7406"), new ApiDef("/sys/dict/delete/:id", "DELETE", "\u5220\u9664\u5b57\u5178", "\u5b57\u5178\u7ba1\u7406"), new ApiDef("/sys/article/list", "GET", "\u67e5\u8be2\u6587\u7ae0\u5217\u8868", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/article/platform/list", "GET", "\u67e5\u8be2\u5e73\u53f0\u7ea7\u6587\u7ae0\u5217\u8868\uff08\u95e8\u6237\uff09", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/article/:id", "GET", "\u67e5\u8be2\u6587\u7ae0\u8be6\u60c5", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/article/create", "POST", "\u521b\u5efa\u6587\u7ae0", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/article/update/:id", "PUT", "\u66f4\u65b0\u6587\u7ae0", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/article/delete/:id", "DELETE", "\u5220\u9664\u6587\u7ae0", "\u6587\u7ae0\u7ba1\u7406"), new ApiDef("/sys/notice/list", "GET", "\u67e5\u8be2\u7cfb\u7edf\u901a\u77e5\u5217\u8868", "\u901a\u77e5\u7ba1\u7406"), new ApiDef("/sys/notice/:id", "GET", "\u67e5\u8be2\u7cfb\u7edf\u901a\u77e5\u8be6\u60c5", "\u901a\u77e5\u7ba1\u7406"), new ApiDef("/sys/notice/create", "POST", "\u521b\u5efa\u7cfb\u7edf\u901a\u77e5", "\u901a\u77e5\u7ba1\u7406"), new ApiDef("/sys/notice/update/:id", "PUT", "\u66f4\u65b0\u7cfb\u7edf\u901a\u77e5", "\u901a\u77e5\u7ba1\u7406"), new ApiDef("/sys/notice/delete/:id", "DELETE", "\u5220\u9664\u7cfb\u7edf\u901a\u77e5", "\u901a\u77e5\u7ba1\u7406"), new ApiDef("/sys/resource/list", "GET", "\u67e5\u8be2\u8d44\u6e90\u5217\u8868", "\u8d44\u6e90\u7ba1\u7406"), new ApiDef("/sys/resource/create", "POST", "\u521b\u5efa\u8d44\u6e90", "\u8d44\u6e90\u7ba1\u7406"), new ApiDef("/sys/resource/upload", "POST", "\u4e0a\u4f20\u8d44\u6e90", "\u8d44\u6e90\u7ba1\u7406"), new ApiDef("/sys/resource/delete/:id", "DELETE", "\u5220\u9664\u8d44\u6e90", "\u8d44\u6e90\u7ba1\u7406"), new ApiDef("/sys/media-icon/list", "GET", "\u67e5\u8be2 SVG \u56fe\u6807\u5217\u8868", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/:id", "GET", "\u67e5\u8be2 SVG \u56fe\u6807\u8be6\u60c5", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/create", "POST", "\u521b\u5efa SVG \u56fe\u6807", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/update/:id", "PUT", "\u66f4\u65b0 SVG \u56fe\u6807", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/delete/:id", "DELETE", "\u5220\u9664 SVG \u56fe\u6807", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-icon/upload", "POST", "\u4e0a\u4f20 SVG \u6587\u4ef6", "\u56fe\u6807\u7ba1\u7406"), new ApiDef("/sys/media-gallery/list", "GET", "\u67e5\u8be2\u56fe\u5e93\u5217\u8868", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/:id", "GET", "\u67e5\u8be2\u56fe\u5e93\u9879\u8be6\u60c5", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/create", "POST", "\u521b\u5efa\u56fe\u5e93\u9879", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/update/:id", "PUT", "\u66f4\u65b0\u56fe\u5e93\u9879", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/media-gallery/delete/:id", "DELETE", "\u5220\u9664\u56fe\u5e93\u9879", "\u56fe\u5e93\u7ba1\u7406"), new ApiDef("/sys/login-log/list", "GET", "\u67e5\u8be2\u767b\u5f55\u8bb0\u5f55\u5217\u8868", "\u767b\u5f55\u5386\u53f2"), new ApiDef("/sys/operation-log/list", "GET", "\u67e5\u8be2\u64cd\u4f5c\u5386\u53f2\u5217\u8868", "\u64cd\u4f5c\u5386\u53f2"), new ApiDef("/sys/config", "GET", "\u67e5\u8be2\u7cfb\u7edf\u914d\u7f6e", "\u914d\u7f6e\u7ba1\u7406"), new ApiDef("/sys/config", "PUT", "\u66f4\u65b0\u7cfb\u7edf\u914d\u7f6e", "\u914d\u7f6e\u7ba1\u7406"), new ApiDef("/sys/monitor/datasource/url", "GET", "\u67e5\u8be2\u6570\u636e\u76d1\u63a7\u5730\u5740", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/server", "GET", "\u67e5\u8be2\u670d\u52a1\u76d1\u63a7", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache", "GET", "\u67e5\u8be2\u7f13\u5b58\u76d1\u63a7", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/keys", "GET", "\u67e5\u8be2\u7f13\u5b58\u5217\u8868", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/value", "GET", "\u67e5\u8be2\u7f13\u5b58\u503c", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/key", "DELETE", "\u5220\u9664\u7f13\u5b58\u952e", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/monitor/cache/clear", "DELETE", "\u6e05\u7a7a\u7f13\u5b58", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-access-policy", "GET", "\u67e5\u8be2\u5168\u7ad9 IP \u7b56\u7565", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-access-policy", "PUT", "\u66f4\u65b0\u5168\u7ad9 IP \u7b56\u7565", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-access-rule/list", "GET", "\u67e5\u8be2\u5168\u7ad9 IP \u89c4\u5219\u5217\u8868", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-access-rule/create", "POST", "\u521b\u5efa\u5168\u7ad9 IP \u89c4\u5219", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-access-rule/update/:id", "PUT", "\u66f4\u65b0\u5168\u7ad9 IP \u89c4\u5219", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-access-rule/delete/:id", "DELETE", "\u5220\u9664\u5168\u7ad9 IP \u89c4\u5219", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/ip-location-cache/page", "GET", "\u5206\u9875\u67e5\u8be2 IP \u5f52\u5c5e\u5730\u7f13\u5b58", "IP\u5730\u7406\u4fe1\u606f"), new ApiDef("/sys/ip-location-cache/lbs-providers", "GET", "\u67e5\u8be2\u53ef\u9009 IP \u5f52\u5c5e\u5730\u6570\u636e\u6e90", "IP\u5730\u7406\u4fe1\u606f"), new ApiDef("/sys/ip-location-cache/refresh", "POST", "\u5f3a\u5236\u91cd\u65b0\u89e3\u6790 IP \u5e76\u5199\u56de\u7f13\u5b58", "IP\u5730\u7406\u4fe1\u606f"), new ApiDef("/sys/portal/devtools-report/list", "GET", "\u67e5\u8be2\u95e8\u6237\u5f00\u53d1\u8005\u5de5\u5177\u6253\u5f00\u8bb0\u5f55", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/visit-hit/list", "GET", "\u67e5\u8be2\u95e8\u6237\u8bbf\u95ee\u6253\u70b9\u660e\u7ec6", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/ip-location", "GET", "IP \u5f52\u5c5e\u5730\uff08\u9ad8\u5fb7\uff09", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/ip-location/batch", "POST", "\u6279\u91cf IP \u5f52\u5c5e\u5730\uff08\u9ad8\u5fb7\uff09", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/league/list", "GET", "\u67e5\u8be2\u8054\u76df\u5217\u8868", "\u8054\u76df\u7ba1\u7406"), new ApiDef("/league/:id", "GET", "\u67e5\u8be2\u8054\u76df\u8be6\u60c5", "\u8054\u76df\u7ba1\u7406"), new ApiDef("/league/create", "POST", "\u521b\u5efa\u8054\u76df", "\u8054\u76df\u7ba1\u7406"), new ApiDef("/league/update/:id", "PUT", "\u66f4\u65b0\u8054\u76df", "\u8054\u76df\u7ba1\u7406"), new ApiDef("/league/delete/:id", "DELETE", "\u5220\u9664\u8054\u76df", "\u8054\u76df\u7ba1\u7406"), new ApiDef("/team/list", "GET", "\u67e5\u8be2\u7403\u961f\u5217\u8868", "\u7403\u961f\u7ba1\u7406"), new ApiDef("/team/:id", "GET", "\u67e5\u8be2\u7403\u961f\u8be6\u60c5", "\u7403\u961f\u7ba1\u7406"), new ApiDef("/team/create", "POST", "\u521b\u5efa\u7403\u961f", "\u7403\u961f\u7ba1\u7406"), new ApiDef("/team/update/:id", "PUT", "\u66f4\u65b0\u7403\u961f", "\u7403\u961f\u7ba1\u7406"), new ApiDef("/team/delete/:id", "DELETE", "\u5220\u9664\u7403\u961f", "\u7403\u961f\u7ba1\u7406"), new ApiDef("/lineup-template/list", "GET", "\u5206\u9875\u67e5\u8be2\u9635\u5bb9\u6a21\u677f\uff08\u53ef\u6309\u7403\u961f\u7b5b\u9009\uff09", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/list", "GET", "\u67e5\u8be2\u7403\u961f\u9635\u5bb9\u6a21\u677f\u5217\u8868", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/:id", "GET", "\u67e5\u8be2\u7403\u961f\u9635\u5bb9\u6a21\u677f\u8be6\u60c5", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/create", "POST", "\u521b\u5efa\u7403\u961f\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/copy-from-game", "POST", "\u4ece\u6bd4\u8d5b\u590d\u5236\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/update/:id", "PUT", "\u66f4\u65b0\u7403\u961f\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/team/:teamId/lineup-template/delete/:id", "DELETE", "\u5220\u9664\u7403\u961f\u9635\u5bb9\u6a21\u677f", "\u9635\u5bb9\u6a21\u677f"), new ApiDef("/coach/list", "GET", "\u67e5\u8be2\u6559\u7ec3\u5217\u8868", "\u6559\u7ec3\u7ba1\u7406"), new ApiDef("/coach/:id", "GET", "\u67e5\u8be2\u6559\u7ec3\u8be6\u60c5", "\u6559\u7ec3\u7ba1\u7406"), new ApiDef("/coach/create", "POST", "\u521b\u5efa\u6559\u7ec3", "\u6559\u7ec3\u7ba1\u7406"), new ApiDef("/coach/update/:id", "PUT", "\u66f4\u65b0\u6559\u7ec3", "\u6559\u7ec3\u7ba1\u7406"), new ApiDef("/coach/delete/:id", "DELETE", "\u5220\u9664\u6559\u7ec3", "\u6559\u7ec3\u7ba1\u7406"), new ApiDef("/event/list", "GET", "\u67e5\u8be2\u8d5b\u4e8b\u5217\u8868", "\u8d5b\u4e8b\u7ba1\u7406"), new ApiDef("/event/:id", "GET", "\u67e5\u8be2\u8d5b\u4e8b\u8be6\u60c5", "\u8d5b\u4e8b\u7ba1\u7406"), new ApiDef("/event/create", "POST", "\u521b\u5efa\u8d5b\u4e8b", "\u8d5b\u4e8b\u7ba1\u7406"), new ApiDef("/event/update/:id", "PUT", "\u66f4\u65b0\u8d5b\u4e8b", "\u8d5b\u4e8b\u7ba1\u7406"), new ApiDef("/event/delete/:id", "DELETE", "\u5220\u9664\u8d5b\u4e8b", "\u8d5b\u4e8b\u7ba1\u7406"), new ApiDef("/event/:eventId/import-game-result", "POST", "\u5bfc\u5165\u6bd4\u8d5b\u7ed3\u679c\uff08\u5355\u4e8b\u52a1\uff09", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/list", "GET", "\u67e5\u8be2\u6bd4\u8d5b\u5217\u8868", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:id", "GET", "\u67e5\u8be2\u6bd4\u8d5b\u8be6\u60c5", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:gameId/stats", "GET", "\u67e5\u8be2\u6bd4\u8d5b\u7403\u5458\u6570\u636e", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/create", "POST", "\u521b\u5efa\u6bd4\u8d5b", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/update/:id", "PUT", "\u66f4\u65b0\u6bd4\u8d5b", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/delete/:id", "DELETE", "\u5220\u9664\u6bd4\u8d5b", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:id/save-live", "POST", "\u5b9e\u65f6\u4fdd\u5b58\u6bd4\u8d5b", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/:id/save-result", "POST", "\u4fdd\u5b58\u6bd4\u8d5b\u7ed3\u679c\uff08\u6bd4\u8d5b+\u7edf\u8ba1\u4e8b\u52a1\uff09", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/stats/create", "POST", "\u521b\u5efa\u6bd4\u8d5b\u6570\u636e", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/stats/update/:id", "PUT", "\u66f4\u65b0\u6bd4\u8d5b\u6570\u636e", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/game/stats/delete/:id", "DELETE", "\u5220\u9664\u6bd4\u8d5b\u6570\u636e", "\u6bd4\u8d5b\u7ba1\u7406"), new ApiDef("/stats/leaders/batting", "GET", "\u67e5\u8be2\u6253\u51fb\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/pitching", "GET", "\u67e5\u8be2\u6295\u7403\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/fielding", "GET", "\u67e5\u8be2\u9632\u5b88\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/team-batting", "GET", "\u67e5\u8be2\u7403\u961f\u8fdb\u653b\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/team-pitching", "GET", "\u67e5\u8be2\u7403\u961f\u6295\u7403\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/leaders/team-fielding", "GET", "\u67e5\u8be2\u7403\u961f\u9632\u5b88\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/standings", "GET", "\u67e5\u8be2\u7403\u961f\u79ef\u5206\u699c", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/stats/star/toplist", "GET", "\u67e5\u8be2\u660e\u661f\u7403\u5458\u5355\u9879\u699c\u805a\u5408", "\u6570\u636e\u6392\u884c\u7ba1\u7406"), new ApiDef("/player/list", "GET", "\u67e5\u8be2\u7403\u5458\u5217\u8868", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/team-options", "GET", "\u6309\u7403\u961f\u67e5\u8be2\u7403\u5458\u9009\u9879", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/check-full-name", "GET", "\u6821\u9a8c\u7403\u5458\u5168\u540d\u662f\u5426\u91cd\u590d", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id", "GET", "\u67e5\u8be2\u7403\u5458\u8be6\u60c5", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats", "GET", "\u67e5\u8be2\u7403\u5458\u6570\u636e\u7edf\u8ba1", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/by-season", "GET", "\u67e5\u8be2\u7403\u5458\u6309\u8d5b\u5b63\u7edf\u8ba1", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/game-log", "GET", "\u67e5\u8be2\u7403\u5458\u6bd4\u8d5b\u65e5\u5fd7", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/drill-down/batting", "GET", "\u7403\u5458\u6570\u636e\u7edf\u8ba1\u94bb\u53d6\uff08\u6253\u51fb\uff09", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/drill-down/pitching", "GET", "\u7403\u5458\u6570\u636e\u7edf\u8ba1\u94bb\u53d6\uff08\u6295\u7403\uff09", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/:id/stats/drill-down/fielding", "GET", "\u7403\u5458\u6570\u636e\u7edf\u8ba1\u94bb\u53d6\uff08\u9632\u5b88\uff09", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/create", "POST", "\u521b\u5efa\u7403\u5458", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/update/:id", "PUT", "\u66f4\u65b0\u7403\u5458", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/delete/:id", "DELETE", "\u5220\u9664\u7403\u5458", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/player/import", "POST", "\u6279\u91cf\u5bfc\u5165\u7403\u5458", "\u7403\u5458\u7ba1\u7406"), new ApiDef("/personnel-change/list", "GET", "\u67e5\u8be2\u6cbf\u9769\u8bb0\u5f55\u5217\u8868", "\u6cbf\u9769\u7ba1\u7406"), new ApiDef("/personnel-change/create", "POST", "\u521b\u5efa\u6cbf\u9769\u8bb0\u5f55", "\u6cbf\u9769\u7ba1\u7406"), new ApiDef("/highlight-moment/list", "GET", "\u67e5\u8be2\u9ad8\u5149\u65f6\u523b\u5217\u8868", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/highlight-moment/create", "POST", "\u521b\u5efa\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/highlight-moment/update/{id}", "PUT", "\u66f4\u65b0\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/highlight-moment/delete/{id}", "DELETE", "\u5220\u9664\u9ad8\u5149\u65f6\u523b", "\u9ad8\u5149\u65f6\u523b\u7ba1\u7406"), new ApiDef("/region/china/children", "GET", "\u4e2d\u56fd\u884c\u653f\u533a\u5212\u5b50\u7ea7\uff08\u7701\u5e02\u533a\u61d2\u52a0\u8f7d\uff09", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/list", "GET", "\u67e5\u8be2\u7403\u573a\u5217\u8868", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/geojson", "GET", "\u7403\u573a\u5206\u5e03 GeoJSON", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/nearby", "GET", "\u9644\u8fd1\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/:id", "GET", "\u67e5\u8be2\u7403\u573a\u8be6\u60c5", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/create", "POST", "\u521b\u5efa\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/update/:id", "PUT", "\u66f4\u65b0\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/stadium/delete/:id", "DELETE", "\u5220\u9664\u7403\u573a", "\u7403\u573a\u7ba1\u7406"), new ApiDef("/sys/portal-visit/summary", "GET", "\u95e8\u6237\u8bbf\u95ee\u7edf\u8ba1\uff08PV/UV\uff09", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/sys/portal-visit/by-province", "GET", "\u95e8\u6237\u8bbf\u95ee\u6309\u7701\u805a\u5408\uff08PV/UV\uff09", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/sys/portal-visit/by-province/cities", "GET", "\u95e8\u6237\u8bbf\u95ee\u6307\u5b9a\u7701\u6309\u5730\u5e02\u805a\u5408\uff08PV/UV\uff09", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/devtools/report", "POST", "\u95e8\u6237\u5f00\u53d1\u8005\u5de5\u5177\u6253\u5f00\u4e0a\u62a5", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/feedback/captcha-image", "GET", "\u95e8\u6237\u610f\u89c1\u53cd\u9988\u9a8c\u8bc1\u7801", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/feedback/submit", "POST", "\u63d0\u4ea4\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/portal/feedback/my-list", "GET", "\u67e5\u8be2\u6211\u7684\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u95e8\u6237\u7edf\u8ba1"), new ApiDef("/sys/portal/feedback/list", "GET", "\u5206\u9875\u67e5\u8be2\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406"), new ApiDef("/sys/portal/feedback/reply", "POST", "\u56de\u590d\u95e8\u6237\u610f\u89c1\u53cd\u9988", "\u7cfb\u7edf\u76d1\u63a7\u7ba1\u7406")});
        for (ApiDef a : apis) {
            if (!this.shouldSeedApi(a)) continue;
            SysApi api = new SysApi();
            api.setPath(a.path());
            api.setMethod(a.method());
            api.setDescription(a.desc());
            api.setGroupName(a.group());
            api.setCreatedBy(Long.valueOf(adminId));
            api.setUpdatedBy(Long.valueOf(adminId));
            this.sysApiRepository.save((Object)api);
        }
    }

    private void seedDict(long adminId) {
        SysDictData dd;
        if (this.sysDictTypeRepository.findAllByType("sys_user_sex").isEmpty()) {
            SysDictType dt = new SysDictType();
            dt.setName("\u7528\u6237\u6027\u522b");
            dt.setType("sys_user_sex");
            dt.setStatus(Integer.valueOf(1));
            dt.setRemark("0\u672a\u77e5 1\u7537 2\u5973");
            dt.setCreatedBy(Long.valueOf(adminId));
            dt.setUpdatedBy(Long.valueOf(adminId));
            dt = (SysDictType)this.sysDictTypeRepository.save((Object)dt);
            for (Object e : List.of((Object)new String[]{"\u672a\u77e5", "0"}, (Object)new String[]{"\u7537", "1"}, (Object)new String[]{"\u5973", "2"})) {
                dd = new SysDictData();
                dd.setDictTypeId(dt.getId());
                dd.setLabel(e[0]);
                dd.setValue(e[1]);
                dd.setSort(Integer.valueOf(0));
                dd.setStatus(Integer.valueOf(1));
                dd.setCreatedBy(Long.valueOf(adminId));
                dd.setUpdatedBy(Long.valueOf(adminId));
                this.sysDictDataRepository.save((Object)dd);
            }
        }
        if (this.sysDictTypeRepository.findAllByType("api_group").isEmpty()) {
            SysDictType apiGroup = new SysDictType();
            apiGroup.setName("API\u5206\u7ec4");
            apiGroup.setType("api_group");
            apiGroup.setStatus(Integer.valueOf(1));
            apiGroup.setRemark("API \u6743\u9650\u5206\u7ec4");
            apiGroup.setCreatedBy(Long.valueOf(adminId));
            apiGroup.setUpdatedBy(Long.valueOf(adminId));
            apiGroup = (SysDictType)this.sysDictTypeRepository.save((Object)apiGroup);
            int sort = 0;
            for (String g : List.of((Object[])new String[]{"\u8ba4\u8bc1", "\u7528\u6237", "\u89d2\u8272", "\u83dc\u5355", "API\u7ba1\u7406", "\u5b57\u5178\u7c7b\u578b", "\u5b57\u5178\u6570\u636e", "\u5b57\u5178", "\u6587\u7ae0\u7ba1\u7406", "\u901a\u77e5\u7ba1\u7406", "\u8d44\u6e90", "\u56fe\u6807\u7ba1\u7406", "\u56fe\u5e93\u7ba1\u7406", "\u767b\u5f55\u5386\u53f2", "\u64cd\u4f5c\u5386\u53f2", "\u914d\u7f6e", "\u7cfb\u7edf\u76d1\u63a7", "IP\u5730\u7406\u4fe1\u606f", "\u8054\u76df", "\u7403\u961f", "\u6559\u7ec3\u7ba1\u7406", "\u8d5b\u4e8b", "\u6bd4\u8d5b", "\u6570\u636e\u6392\u884c", "\u7403\u5458", "\u5185\u5bb9\u7ba1\u7406", "\u4eba\u4e8b\u53d8\u52a8\u7ba1\u7406", "\u7403\u573a\u7ba1\u7406", "\u95e8\u6237\u7edf\u8ba1"})) {
                SysDictData dd2 = new SysDictData();
                dd2.setDictTypeId(apiGroup.getId());
                dd2.setLabel(g);
                dd2.setValue(g);
                dd2.setSort(Integer.valueOf(sort++));
                dd2.setStatus(Integer.valueOf(1));
                dd2.setCreatedBy(Long.valueOf(adminId));
                dd2.setUpdatedBy(Long.valueOf(adminId));
                this.sysDictDataRepository.save((Object)dd2);
            }
        }
        if (this.sysDictTypeRepository.findAllByType("venue").isEmpty()) {
            SysDictType venue = new SysDictType();
            venue.setName("\u6bd4\u8d5b\u573a\u5730");
            venue.setType("venue");
            venue.setStatus(Integer.valueOf(1));
            venue.setRemark("\u7403\u573a/\u573a\u5730");
            venue.setCreatedBy(Long.valueOf(adminId));
            venue.setUpdatedBy(Long.valueOf(adminId));
            venue = (SysDictType)this.sysDictTypeRepository.save((Object)venue);
            for (Object e : List.of((Object)new String[]{"\u4e3b\u573a", "home"}, (Object)new String[]{"\u5ba2\u573a", "away"}, (Object)new String[]{"\u4e2d\u7acb", "neutral"})) {
                dd = new SysDictData();
                dd.setDictTypeId(venue.getId());
                dd.setLabel(e[0]);
                dd.setValue(e[1]);
                dd.setSort(Integer.valueOf(0));
                dd.setStatus(Integer.valueOf(1));
                dd.setCreatedBy(Long.valueOf(adminId));
                dd.setUpdatedBy(Long.valueOf(adminId));
                this.sysDictDataRepository.save((Object)dd);
            }
        }
        if (this.sysDictTypeRepository.findAllByType("player_status").isEmpty()) {
            SysDictType playerStatus = new SysDictType();
            playerStatus.setName("\u7403\u5458\u72b6\u6001");
            playerStatus.setType("player_status");
            playerStatus.setStatus(Integer.valueOf(1));
            playerStatus.setRemark("\u5728\u5f79/\u9000\u5f79\u7b49");
            playerStatus.setCreatedBy(Long.valueOf(adminId));
            playerStatus.setUpdatedBy(Long.valueOf(adminId));
            playerStatus = (SysDictType)this.sysDictTypeRepository.save((Object)playerStatus);
            for (Object e : List.of((Object)new String[]{"\u5728\u5f79", "active"}, (Object)new String[]{"\u9000\u5f79", "retired"}, (Object)new String[]{"\u4f24\u75c5", "injured"}, (Object)new String[]{"\u5176\u4ed6", "other"})) {
                dd = new SysDictData();
                dd.setDictTypeId(playerStatus.getId());
                dd.setLabel(e[0]);
                dd.setValue(e[1]);
                dd.setSort(Integer.valueOf(0));
                dd.setStatus(Integer.valueOf(1));
                dd.setCreatedBy(Long.valueOf(adminId));
                dd.setUpdatedBy(Long.valueOf(adminId));
                this.sysDictDataRepository.save((Object)dd);
            }
        }
        if (this.sysDictTypeRepository.findAllByType("notice_type").isEmpty()) {
            SysDictType noticeType = new SysDictType();
            noticeType.setName("\u5185\u5bb9\u7c7b\u578b");
            noticeType.setType("notice_type");
            noticeType.setStatus(Integer.valueOf(1));
            noticeType.setRemark("\u5185\u5bb9\u7ba1\u7406\uff1a\u95e8\u6237\u8d44\u8baf/\u516c\u544a\u5206\u7c7b");
            noticeType.setCreatedBy(Long.valueOf(adminId));
            noticeType.setUpdatedBy(Long.valueOf(adminId));
            noticeType = (SysDictType)this.sysDictTypeRepository.save((Object)noticeType);
            for (Object e : List.of((Object)new String[]{"\u8d44\u8baf", "news"}, (Object)new String[]{"\u516c\u544a", "announcement"}, (Object)new String[]{"\u901a\u77e5", "notice"}, (Object)new String[]{"\u516c\u793a", "publicity"})) {
                dd = new SysDictData();
                dd.setDictTypeId(noticeType.getId());
                dd.setLabel(e[0]);
                dd.setValue(e[1]);
                dd.setSort(Integer.valueOf(0));
                dd.setStatus(Integer.valueOf(1));
                dd.setCreatedBy(Long.valueOf(adminId));
                dd.setUpdatedBy(Long.valueOf(adminId));
                this.sysDictDataRepository.save((Object)dd);
            }
        }
        if (this.sysDictTypeRepository.findAllByType("content_format").isEmpty()) {
            SysDictType contentFormat = new SysDictType();
            contentFormat.setName("\u5185\u5bb9\u683c\u5f0f");
            contentFormat.setType("content_format");
            contentFormat.setStatus(Integer.valueOf(1));
            contentFormat.setRemark("\u5185\u5bb9\u7ba1\u7406\uff1aHTML / Markdown");
            contentFormat.setCreatedBy(Long.valueOf(adminId));
            contentFormat.setUpdatedBy(Long.valueOf(adminId));
            contentFormat = (SysDictType)this.sysDictTypeRepository.save((Object)contentFormat);
            for (Object e : List.of((Object)new String[]{"HTML", "html"}, (Object)new String[]{"Markdown", "markdown"})) {
                dd = new SysDictData();
                dd.setDictTypeId(contentFormat.getId());
                dd.setLabel(e[0]);
                dd.setValue(e[1]);
                dd.setSort(Integer.valueOf(0));
                dd.setStatus(Integer.valueOf(1));
                dd.setCreatedBy(Long.valueOf(adminId));
                dd.setUpdatedBy(Long.valueOf(adminId));
                this.sysDictDataRepository.save((Object)dd);
            }
        }
    }

    private void seedConfig(long adminId) {
        for (String[] e : List.of((Object)new String[]{"siteName", "BS Ball"}, (Object)new String[]{"siteTitle", "\u8d5b\u4e8b\u4e0e\u6570\u636e\u5c55\u793a"}, (Object)new String[]{"publicViewCount", "true"}, (Object)new String[]{"portalDevtoolsGuard", "true"}, (Object)new String[]{"portalDevtoolsGuardOverlay", "true"}, (Object)new String[]{"portalDevtoolsGuardDebuggerTrap", "true"}, (Object)new String[]{"portalDevtoolsGuardCopyrightNotice", "true"})) {
            SysConfig c = new SysConfig();
            c.setTenantId(Long.valueOf(this.resolveSeedTenantId()));
            c.setConfigKey(e[0]);
            c.setConfigValue(e[1]);
            this.sysConfigRepository.save((Object)c);
        }
    }

    private void seedAdminRoleAndBindUser(long adminId) {
        List menus = this.sysMenuRepository.findAll();
        List apis = this.sysApiRepository.findAll();
        List allMenuIds = menus.stream().map(BaseEntity::getId).collect(Collectors.toList());
        List allApiIds = apis.stream().map(BaseEntity::getId).collect(Collectors.toList());
        List guestApiIds = apis.stream().filter(arg_0 -> this.isGuestRolePublicGetApi(arg_0)).map(BaseEntity::getId).collect(Collectors.toList());
        SysRole adminRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("admin").orElse(null);
        if (adminRole == null) {
            adminRole = new SysRole();
            adminRole.setName("\u8d85\u7ea7\u7ba1\u7406\u5458");
            adminRole.setCode("admin");
            adminRole.setStatus(Integer.valueOf(1));
            adminRole.setSort(Integer.valueOf(0));
        }
        adminRole.setCreatedBy(Long.valueOf(adminId));
        adminRole.setUpdatedBy(Long.valueOf(adminId));
        adminRole = (SysRole)this.sysRoleRepository.save((Object)adminRole);
        long adminRoleId = adminRole.getId();
        this.sysRoleMenuRepository.deleteByRoleId(Long.valueOf(adminRoleId));
        for (Long menuId : allMenuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(Long.valueOf(adminRoleId));
            rm.setMenuId(menuId);
            rm.setCreatedBy(Long.valueOf(adminId));
            rm.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleMenuRepository.save((Object)rm);
        }
        this.sysRoleApiRepository.deleteByRoleId(Long.valueOf(adminRoleId));
        for (Long apiId : allApiIds) {
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(Long.valueOf(adminRoleId));
            ra.setApiId(apiId);
            ra.setCreatedBy(Long.valueOf(adminId));
            ra.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra);
        }
        ArrayList<String> guestMenuPaths = new ArrayList<String>();
        if (this.initSeedProperties.isSeedBusinessMenusAndApis()) {
            guestMenuPaths.add("/admin/leagues");
            guestMenuPaths.add("/admin/teams");
            guestMenuPaths.add("/admin/events");
            guestMenuPaths.add("/admin/players");
        }
        if (this.initSeedProperties.isSeedContentMenusAndApis()) {
            guestMenuPaths.add("/admin/content");
        }
        List guestMenuIds = menus.stream().filter(m -> m.getPath() != null && guestMenuPaths.contains(m.getPath())).map(BaseEntity::getId).distinct().toList();
        SysRole guestRole = this.sysRoleRepository.findByTenantIdIsNullAndCode("guest").orElse(null);
        if (guestRole == null) {
            guestRole = new SysRole();
            guestRole.setName("\u6e38\u5ba2");
            guestRole.setCode("guest");
            guestRole.setStatus(Integer.valueOf(1));
            guestRole.setSort(Integer.valueOf(99));
        }
        guestRole.setCreatedBy(Long.valueOf(adminId));
        guestRole.setUpdatedBy(Long.valueOf(adminId));
        guestRole = (SysRole)this.sysRoleRepository.save((Object)guestRole);
        this.sysRoleMenuRepository.deleteByRoleId(guestRole.getId());
        for (Long menuId : guestMenuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(guestRole.getId());
            rm.setMenuId(menuId);
            rm.setCreatedBy(Long.valueOf(adminId));
            rm.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleMenuRepository.save((Object)rm);
        }
        this.sysRoleApiRepository.deleteByRoleId(guestRole.getId());
        for (Long apiId : guestApiIds) {
            SysRoleApi ra = new SysRoleApi();
            ra.setRoleId(guestRole.getId());
            ra.setApiId(apiId);
            ra.setCreatedBy(Long.valueOf(adminId));
            ra.setUpdatedBy(Long.valueOf(adminId));
            this.sysRoleApiRepository.save((Object)ra);
        }
        if (this.sysUserRepository.count() == 0L) {
            SysUser admin = (SysUser)this.sysUserRepository.findById((Object)adminId).orElseThrow();
            SysUserRole ur = new SysUserRole();
            ur.setUserId(admin.getId());
            ur.setRoleId(Long.valueOf(adminRoleId));
            ur.setCreatedBy(Long.valueOf(adminId));
            ur.setUpdatedBy(Long.valueOf(adminId));
            this.sysUserRoleRepository.save((Object)ur);
        } else {
            this.sysUserRepository.findByUsernameAndDeletedAtIsNull("admin").ifPresent(u -> {
                this.sysUserRoleRepository.deleteByUserId(u.getId());
                SysUserRole ur = new SysUserRole();
                ur.setUserId(u.getId());
                ur.setRoleId(Long.valueOf(adminRoleId));
                ur.setCreatedBy(u.getId());
                ur.setUpdatedBy(u.getId());
                this.sysUserRoleRepository.save((Object)ur);
            });
        }
    }

    private String toJsonArray(List<Long> ids) {
        return "[" + ids.stream().map(String::valueOf).collect(Collectors.joining(",")) + "]";
    }

    private boolean isGuestRolePublicGetApi(SysApi a) {
        boolean contentGet;
        if (!"GET".equals(a.getMethod()) || a.getPath() == null) {
            return false;
        }
        String path = a.getPath();
        if (path.contains("/stadium")) {
            return true;
        }
        boolean businessGet = path.contains("/league") || path.contains("/team") || path.contains("/event") || path.contains("/game") || path.contains("/stats") || path.contains("/player") || path.contains("/coach");
        boolean bl = contentGet = path.contains("/sys/article") || path.contains("/sys/notice");
        if (this.initSeedProperties.isSeedBusinessMenusAndApis() && businessGet) {
            return true;
        }
        return this.initSeedProperties.isSeedContentMenusAndApis() && contentGet;
    }

    @Generated
    public InitDataRunner(SysMenuRepository sysMenuRepository, SysApiRepository sysApiRepository, SysDictTypeRepository sysDictTypeRepository, SysDictDataRepository sysDictDataRepository, SysConfigRepository sysConfigRepository, SysRoleRepository sysRoleRepository, SysUserRepository sysUserRepository, SysRoleMenuRepository sysRoleMenuRepository, SysRoleApiRepository sysRoleApiRepository, SysUserRoleRepository sysUserRoleRepository, StadiumRepository stadiumRepository, TenantProperties tenantProperties, InitSeedProperties initSeedProperties, SysMenuApiRepository sysMenuApiRepository) {
        this.sysMenuRepository = sysMenuRepository;
        this.sysApiRepository = sysApiRepository;
        this.sysDictTypeRepository = sysDictTypeRepository;
        this.sysDictDataRepository = sysDictDataRepository;
        this.sysConfigRepository = sysConfigRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysUserRepository = sysUserRepository;
        this.sysRoleMenuRepository = sysRoleMenuRepository;
        this.sysRoleApiRepository = sysRoleApiRepository;
        this.sysUserRoleRepository = sysUserRoleRepository;
        this.stadiumRepository = stadiumRepository;
        this.tenantProperties = tenantProperties;
        this.initSeedProperties = initSeedProperties;
        this.sysMenuApiRepository = sysMenuApiRepository;
    }
}

