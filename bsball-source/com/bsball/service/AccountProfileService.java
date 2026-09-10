/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysUser
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.service.AccountProfileService
 *  com.bsball.service.EmailService
 *  com.bsball.service.SmsService
 *  com.bsball.utils.PasswordEncoder
 *  lombok.Generated
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysUser;
import com.bsball.repository.SysUserRepository;
import com.bsball.service.EmailService;
import com.bsball.service.SmsService;
import com.bsball.utils.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class AccountProfileService {
    private static final Pattern PHONE_CN = Pattern.compile("^1[3-9]\\d{9}$");
    private final SysUserRepository sysUserRepository;
    private final SmsService smsService;
    private final EmailService emailService;

    public Map<String, Object> getProfile(Long userId) {
        SysUser u = (SysUser)this.sysUserRepository.findById((Object)userId).orElseThrow(() -> new BusinessException(404, "\u7528\u6237\u4e0d\u5b58\u5728"));
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", u.getId());
        out.put("username", u.getUsername());
        out.put("nickname", u.getNickname());
        out.put("realName", u.getRealName());
        out.put("avatar", u.getAvatar());
        out.put("email", u.getEmail());
        out.put("emailVerified", u.getEmailVerified());
        out.put("phone", u.getPhone());
        out.put("phoneVerified", u.getPhoneVerified());
        out.put("gender", u.getGender());
        out.put("birthDate", u.getBirthDate());
        return out;
    }

    @Transactional
    public Map<String, Object> updateProfile(Long userId, Map<String, Object> body) {
        SysUser u = (SysUser)this.sysUserRepository.findById((Object)userId).orElseThrow(() -> new BusinessException(404, "\u7528\u6237\u4e0d\u5b58\u5728"));
        if (body.containsKey("nickname")) {
            String nn = AccountProfileService.str((Object)body.get("nickname"));
            if (nn == null || nn.isBlank()) {
                throw new BusinessException(400, "\u6635\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (nn.length() > 30) {
                throw new BusinessException(400, "\u6635\u79f0\u4e0d\u80fd\u8d85\u8fc7 30 \u5b57");
            }
            u.setNickname(nn);
        }
        if (body.containsKey("realName")) {
            String rn = AccountProfileService.str((Object)body.get("realName"));
            String string = rn = rn == null ? "" : rn;
            if (rn.length() > 20) {
                throw new BusinessException(400, "\u771f\u5b9e\u59d3\u540d\u4e0d\u80fd\u8d85\u8fc7 20 \u5b57");
            }
            u.setRealName(rn);
        }
        if (body.containsKey("birthDate")) {
            u.setBirthDate(AccountProfileService.str((Object)body.get("birthDate")));
        }
        if (body.containsKey("gender") && body.get("gender") != null) {
            int g;
            try {
                int n;
                Object object = body.get("gender");
                if (object instanceof Number) {
                    Number n2 = (Number)object;
                    n = n2.intValue();
                } else {
                    n = Integer.parseInt(body.get("gender").toString().trim());
                }
                g = n;
            }
            catch (NumberFormatException e) {
                g = 0;
            }
            if (g < 0 || g > 2) {
                g = 0;
            }
            u.setGender(Integer.valueOf(g));
        }
        u.setUpdatedAt(LocalDateTime.now());
        this.sysUserRepository.save((Object)u);
        return this.getProfile(userId);
    }

    @Transactional
    public Map<String, Object> bindEmail(Long userId, String email, String code) {
        String normalized = EmailService.normalizeEmail((String)email);
        if (this.sysUserRepository.existsByEmailAndDeletedAtIsNullAndIdNot(normalized, userId)) {
            throw new BusinessException(400, "\u8be5\u90ae\u7bb1\u5df2\u88ab\u5176\u4ed6\u8d26\u53f7\u7ed1\u5b9a");
        }
        this.emailService.verifyCode(normalized, "bind_email", code);
        SysUser u = (SysUser)this.sysUserRepository.findById((Object)userId).orElseThrow(() -> new BusinessException(404, "\u7528\u6237\u4e0d\u5b58\u5728"));
        u.setEmail(normalized);
        u.setEmailVerified(Integer.valueOf(1));
        u.setUpdatedAt(LocalDateTime.now());
        this.sysUserRepository.save((Object)u);
        return this.getProfile(userId);
    }

    @Transactional
    public Map<String, Object> changePhone(Long userId, String phone, String smsCode) {
        String normalized = AccountProfileService.normalizePhone((String)phone);
        if (this.sysUserRepository.existsByPhoneAndDeletedAtIsNullAndIdNot(normalized, userId)) {
            throw new BusinessException(400, "\u8be5\u624b\u673a\u53f7\u5df2\u88ab\u5176\u4ed6\u8d26\u53f7\u7ed1\u5b9a");
        }
        this.smsService.verifyCode(normalized, "bind_phone", smsCode);
        SysUser u = (SysUser)this.sysUserRepository.findById((Object)userId).orElseThrow(() -> new BusinessException(404, "\u7528\u6237\u4e0d\u5b58\u5728"));
        u.setPhone(normalized);
        u.setPhoneVerified(Integer.valueOf(1));
        u.setUpdatedAt(LocalDateTime.now());
        this.sysUserRepository.save((Object)u);
        return this.getProfile(userId);
    }

    public void forgotSendCode(String channel, String account, long tenantId, String clientIp) {
        if ("phone".equals(channel)) {
            String normalized = AccountProfileService.normalizePhone((String)account);
            if (this.sysUserRepository.findByPhoneAndDeletedAtIsNull(normalized).isEmpty()) {
                throw new BusinessException(404, "\u8be5\u624b\u673a\u53f7\u672a\u6ce8\u518c");
            }
            this.smsService.sendCode(normalized, "reset_password", tenantId, clientIp);
        } else if ("email".equals(channel)) {
            String normalized = EmailService.normalizeEmail((String)account);
            if (this.sysUserRepository.findByEmailAndDeletedAtIsNull(normalized).isEmpty()) {
                throw new BusinessException(404, "\u8be5\u90ae\u7bb1\u672a\u6ce8\u518c");
            }
            this.emailService.sendCode(normalized, "reset_password", tenantId, clientIp);
        } else {
            throw new BusinessException(400, "\u65e0\u6548\u7684\u627e\u56de\u65b9\u5f0f");
        }
    }

    @Transactional
    public void resetPassword(String channel, String account, String code, String newPassword) {
        SysUser user;
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(400, "\u5bc6\u7801\u957f\u5ea6\u4e0d\u80fd\u5c11\u4e8e 6 \u4f4d");
        }
        if ("phone".equals(channel)) {
            String normalized = AccountProfileService.normalizePhone((String)account);
            this.smsService.verifyCode(normalized, "reset_password", code);
            user = (SysUser)this.sysUserRepository.findByPhoneAndDeletedAtIsNull(normalized).orElseThrow(() -> new BusinessException(404, "\u8be5\u624b\u673a\u53f7\u672a\u6ce8\u518c"));
        } else if ("email".equals(channel)) {
            String normalized = EmailService.normalizeEmail((String)account);
            this.emailService.verifyCode(normalized, "reset_password", code);
            user = (SysUser)this.sysUserRepository.findByEmailAndDeletedAtIsNull(normalized).orElseThrow(() -> new BusinessException(404, "\u8be5\u90ae\u7bb1\u672a\u6ce8\u518c"));
        } else {
            throw new BusinessException(400, "\u65e0\u6548\u7684\u627e\u56de\u65b9\u5f0f");
        }
        user.setPassword(PasswordEncoder.encode((CharSequence)newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        this.sysUserRepository.save((Object)user);
    }

    private static String str(Object v) {
        return v == null ? null : v.toString().trim();
    }

    private static String normalizePhone(String phone) {
        if (phone == null) {
            throw new BusinessException(400, "\u624b\u673a\u53f7\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String p = phone.trim();
        if (!PHONE_CN.matcher(p).matches()) {
            throw new BusinessException(400, "\u624b\u673a\u53f7\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
        return p;
    }

    @Generated
    public AccountProfileService(SysUserRepository sysUserRepository, SmsService smsService, EmailService emailService) {
        this.sysUserRepository = sysUserRepository;
        this.smsService = smsService;
        this.emailService = emailService;
    }
}

