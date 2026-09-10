/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.dto.IpAccessPolicyVo
 *  com.bsball.model.entity.SysConfig
 *  com.bsball.model.entity.SysIpAccessRule
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.repository.SysConfigRepository
 *  com.bsball.repository.SysIpAccessRuleRepository
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.service.IpAccessPolicyService
 *  com.bsball.service.IpAccessPolicyService$PolicyMode
 *  com.bsball.service.IpAccessPolicyService$PolicySnapshot
 *  com.bsball.utils.Ipv4CidrMatch
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.scheduling.annotation.Scheduled
 *  org.springframework.stereotype.Service
 */
package com.bsball.service;

import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.IpAccessPolicyVo;
import com.bsball.model.entity.SysConfig;
import com.bsball.model.entity.SysIpAccessRule;
import com.bsball.model.entity.SysTenant;
import com.bsball.repository.SysConfigRepository;
import com.bsball.repository.SysIpAccessRuleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.service.IpAccessPolicyService;
import com.bsball.utils.Ipv4CidrMatch;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class IpAccessPolicyService {
    @Value(value="${app.security.ip-access.enabled:true}")
    private boolean ipAccessFilterEnabled;
    @Value(value="${app.security.ip-access.trust-x-forwarded-for:true}")
    private boolean trustXForwardedFor;
    private static final List<String> IMPLICIT_BYPASS_PREFIXES = List.of((Object)"/sys/ip-access-policy", (Object)"/sys/ip-access-rule");
    private final SysConfigRepository sysConfigRepository;
    private final SysIpAccessRuleRepository sysIpAccessRuleRepository;
    private final SysTenantRepository sysTenantRepository;
    private final TenantProperties tenantProperties;
    private volatile Map<Long, PolicySnapshot> snapshotsByTenant = Map.of();

    @PostConstruct
    void init() {
        this.refresh();
    }

    @Scheduled(fixedDelayString="${app.security.ip-access.refresh-ms:60000}")
    public void refresh() {
        List tenants = this.sysTenantRepository.findAll().stream().filter(t -> t.getDeletedAt() == null).toList();
        HashMap<Long, PolicySnapshot> next = new HashMap<Long, PolicySnapshot>();
        for (SysTenant t2 : tenants) {
            next.put(t2.getId(), this.buildSnapshotForTenant(t2.getId().longValue()));
        }
        this.snapshotsByTenant = Map.copyOf(next);
    }

    private PolicySnapshot buildSnapshotForTenant(long tid) {
        String modeStr = this.sysConfigRepository.findByTenantIdAndConfigKey(Long.valueOf(tid), "ipAccessPolicyMode").map(SysConfig::getConfigValue).orElse("off").trim().toLowerCase();
        PolicyMode mode = PolicyMode.from((String)modeStr);
        String bypassRaw = this.sysConfigRepository.findByTenantIdAndConfigKey(Long.valueOf(tid), "ipAccessPolicyBypassPaths").map(SysConfig::getConfigValue).orElse("");
        List bypass = IpAccessPolicyService.mergeBypassPrefixes((String)bypassRaw);
        ArrayList<String> deny = new ArrayList<String>();
        ArrayList<String> allow = new ArrayList<String>();
        for (SysIpAccessRule r : this.sysIpAccessRuleRepository.findByTenantIdAndScopeTypeAndEnabledOrderByPriorityDesc(Long.valueOf(tid), "GLOBAL", Short.valueOf((short)1))) {
            if ("DENY".equalsIgnoreCase(r.getRuleKind())) {
                deny.add(r.getCidrOrIp().trim());
                continue;
            }
            if (!"ALLOW".equalsIgnoreCase(r.getRuleKind())) continue;
            allow.add(r.getCidrOrIp().trim());
        }
        return new PolicySnapshot(mode, bypass, deny, allow);
    }

    public IpAccessPolicyVo getPolicyVo() {
        return this.getPolicyVoForTenant(this.effectiveTenantIdForPolicyApi());
    }

    public IpAccessPolicyVo getPolicyVoForTenant(long tenantId) {
        IpAccessPolicyVo vo = new IpAccessPolicyVo();
        vo.setMode(this.sysConfigRepository.findByTenantIdAndConfigKey(Long.valueOf(tenantId), "ipAccessPolicyMode").map(SysConfig::getConfigValue).orElse("off"));
        vo.setBypassPaths(this.sysConfigRepository.findByTenantIdAndConfigKey(Long.valueOf(tenantId), "ipAccessPolicyBypassPaths").map(SysConfig::getConfigValue).orElse(""));
        vo.setTrustXForwardedFor(this.trustXForwardedFor);
        vo.setIpAccessFilterEnabled(this.ipAccessFilterEnabled);
        return vo;
    }

    private long effectiveTenantIdForPolicyApi() {
        Long t = CurrentUserHolder.getTenantId();
        return t != null ? t.longValue() : this.tenantProperties.getDefaultId();
    }

    public boolean isIpBlocked(Long tenantId, String normalizedPath, String clientIp) {
        boolean isIpv4;
        long tid = tenantId != null ? tenantId.longValue() : this.tenantProperties.getDefaultId();
        PolicySnapshot s = this.snapshotsByTenant.getOrDefault(tid, PolicySnapshot.empty());
        if (s.mode == PolicyMode.OFF) {
            return false;
        }
        if (IpAccessPolicyService.matchesBypass((List)s.bypassPrefixes, (String)normalizedPath)) {
            return false;
        }
        boolean bl = isIpv4 = clientIp != null && clientIp.indexOf(58) < 0;
        if (s.mode == PolicyMode.BLACKLIST) {
            if (!isIpv4) {
                return false;
            }
            for (String cidr : s.denyCidrs) {
                if (!Ipv4CidrMatch.matches((String)cidr, (String)clientIp)) continue;
                return true;
            }
            return false;
        }
        if (s.mode == PolicyMode.WHITELIST) {
            if (!isIpv4) {
                return true;
            }
            for (String cidr : s.allowCidrs) {
                if (!Ipv4CidrMatch.matches((String)cidr, (String)clientIp)) continue;
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean matchesBypass(List<String> prefixes, String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (!path.equals(prefix) && !path.startsWith(prefix + "/")) continue;
            return true;
        }
        return false;
    }

    private static List<String> mergeBypassPrefixes(String commaSeparatedFromDb) {
        List fromDb = IpAccessPolicyService.parseCommaPrefixes((String)commaSeparatedFromDb);
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        ArrayList<String> out = new ArrayList<String>();
        for (String p : IMPLICIT_BYPASS_PREFIXES) {
            if (!seen.add(p)) continue;
            out.add(p);
        }
        for (String p : fromDb) {
            if (!seen.add(p)) continue;
            out.add(p);
        }
        return List.copyOf(out);
    }

    private static List<String> parseCommaPrefixes(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Generated
    public IpAccessPolicyService(SysConfigRepository sysConfigRepository, SysIpAccessRuleRepository sysIpAccessRuleRepository, SysTenantRepository sysTenantRepository, TenantProperties tenantProperties) {
        this.sysConfigRepository = sysConfigRepository;
        this.sysIpAccessRuleRepository = sysIpAccessRuleRepository;
        this.sysTenantRepository = sysTenantRepository;
        this.tenantProperties = tenantProperties;
    }
}

