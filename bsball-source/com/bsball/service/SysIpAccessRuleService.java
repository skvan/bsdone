/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.dto.IpAccessRulePayload
 *  com.bsball.model.entity.SysIpAccessRule
 *  com.bsball.repository.SysIpAccessRuleRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.IpAccessPolicyService
 *  com.bsball.service.SuperAdminTenantOverrideService
 *  com.bsball.service.SysIpAccessRuleService
 *  com.bsball.utils.Ipv4CidrMatch
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.dto.IpAccessRulePayload;
import com.bsball.model.entity.SysIpAccessRule;
import com.bsball.repository.SysIpAccessRuleRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.IpAccessPolicyService;
import com.bsball.service.SuperAdminTenantOverrideService;
import com.bsball.utils.Ipv4CidrMatch;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class SysIpAccessRuleService {
    private final SysIpAccessRuleRepository sysIpAccessRuleRepository;
    private final IpAccessPolicyService ipAccessPolicyService;
    private final SuperAdminTenantOverrideService superAdminTenantOverrideService;
    private final ApiPermissionService apiPermissionService;

    public List<SysIpAccessRule> list(Long requestedTenantId) {
        long tid = this.superAdminTenantOverrideService.resolveConfigTenantId(requestedTenantId);
        return this.sysIpAccessRuleRepository.findByTenantIdAndScopeTypeOrderByPriorityDesc(Long.valueOf(tid), "GLOBAL");
    }

    @Transactional
    public SysIpAccessRule create(Long requestedTenantId, IpAccessRulePayload p) {
        this.validatePayload(p, true);
        long tid = this.superAdminTenantOverrideService.resolveConfigTenantId(requestedTenantId);
        SysIpAccessRule e = new SysIpAccessRule();
        e.setTenantId(Long.valueOf(tid));
        SysIpAccessRuleService.applyPayload((SysIpAccessRule)e, (IpAccessRulePayload)p);
        Long uid = CurrentUserHolder.get();
        e.setCreatedBy(uid);
        e.setUpdatedBy(uid);
        SysIpAccessRule saved = (SysIpAccessRule)this.sysIpAccessRuleRepository.save((Object)e);
        this.ipAccessPolicyService.refresh();
        return saved;
    }

    @Transactional
    public void update(long id, IpAccessRulePayload p) {
        SysIpAccessRule e = (SysIpAccessRule)this.sysIpAccessRuleRepository.findById((Object)id).orElseThrow(() -> new IllegalArgumentException("\u89c4\u5219\u4e0d\u5b58\u5728"));
        this.assertCanModifyRule(e);
        if (!"GLOBAL".equalsIgnoreCase(e.getScopeType())) {
            throw new IllegalArgumentException("\u6682\u4ec5\u652f\u6301\u7f16\u8f91 GLOBAL \u89c4\u5219");
        }
        this.validatePayload(p, false);
        SysIpAccessRuleService.applyPayload((SysIpAccessRule)e, (IpAccessRulePayload)p);
        e.setUpdatedBy(CurrentUserHolder.get());
        this.sysIpAccessRuleRepository.save((Object)e);
        this.ipAccessPolicyService.refresh();
    }

    @Transactional
    public void delete(long id) {
        SysIpAccessRule e = (SysIpAccessRule)this.sysIpAccessRuleRepository.findById((Object)id).orElseThrow(() -> new IllegalArgumentException("\u89c4\u5219\u4e0d\u5b58\u5728"));
        this.assertCanModifyRule(e);
        if (!"GLOBAL".equalsIgnoreCase(e.getScopeType())) {
            throw new IllegalArgumentException("\u6682\u4ec5\u652f\u6301\u5220\u9664 GLOBAL \u89c4\u5219");
        }
        this.sysIpAccessRuleRepository.delete((Object)e);
        this.ipAccessPolicyService.refresh();
    }

    private void assertCanModifyRule(SysIpAccessRule e) {
        Long uid = CurrentUserHolder.get();
        if (uid != null && this.apiPermissionService.isSuperAdmin(uid)) {
            return;
        }
        long opTid = this.superAdminTenantOverrideService.resolveConfigTenantId(null);
        if (e.getTenantId() == null || !Long.valueOf(opTid).equals(e.getTenantId())) {
            throw new BusinessException(403, "\u65e0\u6743\u64cd\u4f5c\u5176\u4ed6\u79df\u6237\u7684 IP \u89c4\u5219");
        }
    }

    private void validatePayload(IpAccessRulePayload p, boolean creating) {
        String scope;
        String string = scope = p.getScopeType() != null ? p.getScopeType().trim() : "GLOBAL";
        if (!"GLOBAL".equalsIgnoreCase(scope)) {
            throw new IllegalArgumentException("\u672c\u671f\u4ec5\u652f\u6301 scope_type=GLOBAL");
        }
        String kind = p.getRuleKind();
        if (kind == null || !"ALLOW".equalsIgnoreCase(kind) && !"DENY".equalsIgnoreCase(kind)) {
            throw new IllegalArgumentException("ruleKind \u987b\u4e3a ALLOW \u6216 DENY");
        }
        if (p.getCidrOrIp() == null || !Ipv4CidrMatch.isValidRule((String)p.getCidrOrIp())) {
            throw new IllegalArgumentException("cidrOrIp \u987b\u4e3a\u5408\u6cd5 IPv4 \u6216 IPv4 CIDR");
        }
        if (p.getEnabled() != null && p.getEnabled() != 0 && p.getEnabled() != 1) {
            throw new IllegalArgumentException("enabled \u987b\u4e3a 0 \u6216 1");
        }
    }

    private static void applyPayload(SysIpAccessRule e, IpAccessRulePayload p) {
        e.setRuleKind(p.getRuleKind().trim().toUpperCase());
        e.setScopeType("GLOBAL");
        e.setRoleId(null);
        e.setApiPattern(null);
        e.setCidrOrIp(p.getCidrOrIp().trim());
        e.setPriority(Integer.valueOf(p.getPriority() != null ? p.getPriority() : 0));
        if (p.getEnabled() != null) {
            e.setEnabled(Short.valueOf((short)(p.getEnabled() != 0 ? 1 : 0)));
        } else if (e.getId() == null) {
            e.setEnabled(Short.valueOf((short)1));
        }
        e.setRemark(p.getRemark());
    }

    @Generated
    public SysIpAccessRuleService(SysIpAccessRuleRepository sysIpAccessRuleRepository, IpAccessPolicyService ipAccessPolicyService, SuperAdminTenantOverrideService superAdminTenantOverrideService, ApiPermissionService apiPermissionService) {
        this.sysIpAccessRuleRepository = sysIpAccessRuleRepository;
        this.ipAccessPolicyService = ipAccessPolicyService;
        this.superAdminTenantOverrideService = superAdminTenantOverrideService;
        this.apiPermissionService = apiPermissionService;
    }
}

