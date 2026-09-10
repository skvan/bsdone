/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.AccountProperties
 *  com.bsball.config.TenantProperties
 *  com.bsball.exception.BusinessException
 *  com.bsball.exception.UnauthorizedException
 *  com.bsball.model.entity.SysArticle
 *  com.bsball.model.entity.SysRole
 *  com.bsball.model.entity.SysTenant
 *  com.bsball.model.entity.SysUser
 *  com.bsball.model.entity.SysUserRole
 *  com.bsball.model.entity.SysUserTenant
 *  com.bsball.repository.SysArticleRepository
 *  com.bsball.repository.SysRoleRepository
 *  com.bsball.repository.SysTenantRepository
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.repository.SysUserRoleRepository
 *  com.bsball.repository.SysUserTenantRepository
 *  com.bsball.service.AccountRegisterService
 *  com.bsball.service.AuthService
 *  com.bsball.service.SmsService
 *  com.bsball.utils.PasswordEncoder
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  lombok.Generated
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.config.AccountProperties;
import com.bsball.config.TenantProperties;
import com.bsball.exception.BusinessException;
import com.bsball.exception.UnauthorizedException;
import com.bsball.model.entity.SysArticle;
import com.bsball.model.entity.SysRole;
import com.bsball.model.entity.SysTenant;
import com.bsball.model.entity.SysUser;
import com.bsball.model.entity.SysUserRole;
import com.bsball.model.entity.SysUserTenant;
import com.bsball.repository.SysArticleRepository;
import com.bsball.repository.SysRoleRepository;
import com.bsball.repository.SysTenantRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.repository.SysUserRoleRepository;
import com.bsball.repository.SysUserTenantRepository;
import com.bsball.service.AuthService;
import com.bsball.service.SmsService;
import com.bsball.utils.PasswordEncoder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Generated;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class AccountRegisterService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9]{4,16}$");
    private final AccountProperties accountProperties;
    private final TenantProperties tenantProperties;
    private final SmsService smsService;
    private final AuthService authService;
    private final SysUserRepository sysUserRepository;
    private final SysUserRoleRepository sysUserRoleRepository;
    private final SysUserTenantRepository sysUserTenantRepository;
    private final SysRoleRepository sysRoleRepository;
    private final SysTenantRepository sysTenantRepository;
    private final SysArticleRepository sysArticleRepository;
    private static final Pattern ALL_DIGITS_PATTERN = Pattern.compile("^[0-9]+$");

    public Map<String, Object> legalDocs() {
        HashMap<String, Object> out = new HashMap<String, Object>();
        out.put("privacyPolicy", this.findLegalDoc("privacy_policy"));
        out.put("termsOfService", this.findLegalDoc("terms_of_service"));
        return out;
    }

    private Map<String, Object> findLegalDoc(String type) {
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> cb.and(new Predicate[]{cb.isNull((Expression)root.get("tenantId")), cb.equal((Expression)root.get("type"), (Object)type), cb.equal((Expression)root.get("status"), (Object)1), cb.isNull((Expression)root.get("deletedAt"))});
        List list = this.sysArticleRepository.findAll((Specification)spec, Pageable.ofSize((int)1)).getContent();
        if (list.isEmpty()) {
            return null;
        }
        SysArticle a = (SysArticle)list.get(0);
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("summary", a.getSummary());
        m.put("type", a.getType());
        return m;
    }

    @Transactional
    public Map<String, Object> register(String phone, String smsCode, String username, String password, boolean acceptTerms, boolean acceptPrivacy, Long tenantId, String clientIp) {
        if (!this.accountProperties.isRegisterEnabled()) {
            throw new BusinessException(403, "\u6682\u672a\u5f00\u653e\u81ea\u52a9\u6ce8\u518c");
        }
        if (!acceptTerms || !acceptPrivacy) {
            throw new BusinessException(400, "\u8bf7\u5148\u9605\u8bfb\u5e76\u540c\u610f\u670d\u52a1\u534f\u8bae\u4e0e\u9690\u79c1\u653f\u7b56");
        }
        AccountRegisterService.validateUsername((String)username);
        AccountRegisterService.validatePassword((String)password);
        this.smsService.verifyCode(phone, "register", smsCode);
        String normalizedPhone = phone.trim();
        if (this.sysUserRepository.existsByPhoneAndDeletedAtIsNull(normalizedPhone)) {
            throw new BusinessException(400, "\u8be5\u624b\u673a\u53f7\u5df2\u6ce8\u518c");
        }
        if (this.sysUserRepository.findByUsernameAndDeletedAtIsNull(username.trim()).isPresent()) {
            throw new BusinessException(400, "\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528");
        }
        long tid = this.resolveTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setNickname(username.trim());
        user.setPhone(normalizedPhone);
        user.setPhoneVerified(Integer.valueOf(1));
        user.setPassword(PasswordEncoder.encode((CharSequence)password));
        user.setStatus(Integer.valueOf(1));
        user.setTermsAcceptedAt(now);
        user.setPrivacyAcceptedAt(now);
        user.setRegisterSource("portal");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        this.sysUserRepository.save((Object)user);
        SysUserTenant ut = new SysUserTenant();
        ut.setUserId(user.getId());
        ut.setTenantId(Long.valueOf(tid));
        ut.setCreatedAt(now);
        ut.setUpdatedAt(now);
        this.sysUserTenantRepository.save((Object)ut);
        this.assignDefaultRole(user.getId(), tid, now);
        return this.authService.login(user.getUsername(), password, Long.valueOf(tid));
    }

    @Transactional
    public Map<String, Object> loginByPhone(String phone, String smsCode, Long tenantId) {
        this.smsService.verifyCode(phone, "login", smsCode);
        SysUser user = (SysUser)this.sysUserRepository.findByPhoneAndDeletedAtIsNull(phone.trim()).orElseThrow(() -> new UnauthorizedException("\u8be5\u624b\u673a\u53f7\u672a\u6ce8\u518c"));
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "\u8d26\u53f7\u5df2\u7981\u7528");
        }
        long tid = this.resolveTenantId(tenantId);
        return this.authService.loginWithoutPassword(user, Long.valueOf(tid));
    }

    private void assignDefaultRole(Long userId, long tenantId, LocalDateTime now) {
        String roleCode = this.accountProperties.getDefaultRoleCode();
        SysRole role = this.sysRoleRepository.findByTenantIdIsNullAndCode(roleCode).orElse(null);
        if (role == null) {
            role = this.sysRoleRepository.findByTenantIdAndCode(Long.valueOf(tenantId), roleCode).orElse(null);
        }
        if (role == null) {
            return;
        }
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        ur.setCreatedAt(now);
        ur.setUpdatedAt(now);
        this.sysUserRoleRepository.save((Object)ur);
    }

    private long resolveTenantId(Long tenantId) {
        if (tenantId != null && tenantId > 0L) {
            SysTenant t = this.sysTenantRepository.findById((Object)tenantId).orElse(null);
            if (t == null || t.getDeletedAt() != null || t.getStatus() != null && t.getStatus() == 0) {
                throw new BusinessException(400, "\u79df\u6237\u65e0\u6548\u6216\u5df2\u505c\u7528");
            }
            return tenantId;
        }
        return this.tenantProperties.getDefaultId();
    }

    private static void validateUsername(String username) {
        String u;
        String string = u = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(u).matches()) {
            throw new BusinessException(400, "\u7528\u6237\u540d\u53ea\u80fd\u5305\u542b\u5c0f\u5199\u5b57\u6bcd\u548c\u6570\u5b57\uff0c\u957f\u5ea6 4\uff5e16 \u4f4d");
        }
        if (ALL_DIGITS_PATTERN.matcher(u).matches()) {
            throw new BusinessException(400, "\u7528\u6237\u540d\u4e0d\u80fd\u4e3a\u7eaf\u6570\u5b57\uff0c\u9700\u5305\u542b\u5c0f\u5199\u5b57\u6bcd");
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BusinessException(400, "\u5bc6\u7801\u957f\u5ea6\u4e0d\u80fd\u5c11\u4e8e 6 \u4f4d");
        }
    }

    @Generated
    public AccountRegisterService(AccountProperties accountProperties, TenantProperties tenantProperties, SmsService smsService, AuthService authService, SysUserRepository sysUserRepository, SysUserRoleRepository sysUserRoleRepository, SysUserTenantRepository sysUserTenantRepository, SysRoleRepository sysRoleRepository, SysTenantRepository sysTenantRepository, SysArticleRepository sysArticleRepository) {
        this.accountProperties = accountProperties;
        this.tenantProperties = tenantProperties;
        this.smsService = smsService;
        this.authService = authService;
        this.sysUserRepository = sysUserRepository;
        this.sysUserRoleRepository = sysUserRoleRepository;
        this.sysUserTenantRepository = sysUserTenantRepository;
        this.sysRoleRepository = sysRoleRepository;
        this.sysTenantRepository = sysTenantRepository;
        this.sysArticleRepository = sysArticleRepository;
    }
}

