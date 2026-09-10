/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.SmsProperties
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysSmsCode
 *  com.bsball.repository.SysSmsCodeRepository
 *  com.bsball.service.SmsService
 *  com.bsball.service.sms.AliyunSmsClient
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  jakarta.annotation.PostConstruct
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.data.domain.Pageable
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.config.SmsProperties;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysSmsCode;
import com.bsball.repository.SysSmsCodeRepository;
import com.bsball.service.sms.AliyunSmsClient;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class SmsService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final Pattern PHONE_CN = Pattern.compile("^1[3-9]\\d{9}$");
    private final SmsProperties smsProperties;
    private final SysSmsCodeRepository smsCodeRepository;
    private final AliyunSmsClient aliyunSmsClient;
    private final SecureRandom random = new SecureRandom();
    private Cache<String, Boolean> phoneSendCooldown;

    @PostConstruct
    void init() {
        int sec = Math.max(30, this.smsProperties.getSendIntervalSec());
        this.phoneSendCooldown = Caffeine.newBuilder().expireAfterWrite((long)sec, TimeUnit.SECONDS).maximumSize(100000L).build();
        if (this.smsProperties.isEnabled()) {
            log.info("[SMS] \u77ed\u4fe1\u670d\u52a1\u5df2\u542f\u7528 | AK={} | \u7b7e\u540d={} | \u6ce8\u518c\u6a21\u677f={} | \u767b\u5f55\u6a21\u677f={} | \u91cd\u7f6e\u5bc6\u7801\u6a21\u677f={}", new Object[]{SmsService.maskKey((String)this.smsProperties.getAccessKeyId()), this.smsProperties.getSignName(), this.smsProperties.getTemplateCodeRegister(), this.smsProperties.getTemplateCodeLogin(), SmsService.orDash((String)this.smsProperties.getTemplateCodeResetPassword())});
        } else {
            log.info("[SMS] \u77ed\u4fe1\u670d\u52a1\u672a\u542f\u7528\uff08\u4ec5 MOCK \u6a21\u5f0f\uff1b\u8bbe\u7f6e APP_SMS_ENABLED=true \u5f00\u542f\u771f\u5b9e\u53d1\u9001\uff09");
        }
    }

    public void sendCode(String phone, String scene, long tenantId, String clientIp) {
        String template;
        String normalized = SmsService.normalizePhone((String)phone);
        SmsService.validateScene((String)scene);
        this.checkAntiAbuse(normalized, scene, clientIp);
        String code = this.generateCode();
        int expireSec = Math.max(60, this.smsProperties.getCodeExpireSec());
        int expireMin = Math.max(1, expireSec / 60);
        LocalDateTime now = LocalDateTime.now();
        SysSmsCode record = new SysSmsCode();
        record.setTenantId(tenantId > 0L ? Long.valueOf(tenantId) : null);
        record.setPhone(normalized);
        record.setCode(code);
        record.setScene(scene);
        record.setClientIp(SmsService.safeIp((String)clientIp));
        record.setExpiresAt(now.plusSeconds(expireSec));
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        this.smsCodeRepository.save((Object)record);
        boolean sent = false;
        if (this.smsProperties.isEnabled() && (template = this.resolveTemplate(scene)) != null && !template.isBlank()) {
            sent = this.aliyunSmsClient.send(normalized, template, Map.of((Object)"code", (Object)code, (Object)"time", (Object)String.valueOf(expireMin)));
        }
        if (!sent) {
            log.info("[SMS-MOCK] phone={} scene={} code={} (\u914d\u7f6e app.sms.enabled=true \u4e14\u586b\u5199\u963f\u91cc\u4e91\u5bc6\u94a5\u540e\u771f\u5b9e\u53d1\u9001)", new Object[]{normalized, scene, code});
        }
        this.phoneSendCooldown.put((Object)(normalized + ":" + scene), (Object)Boolean.TRUE);
    }

    @Transactional
    public void verifyCode(String phone, String scene, String code) {
        String normalized = SmsService.normalizePhone((String)phone);
        SmsService.validateScene((String)scene);
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "\u9a8c\u8bc1\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        SysSmsCode record = (SysSmsCode)this.smsCodeRepository.findLatestValid(normalized, scene, LocalDateTime.now()).orElseThrow(() -> new BusinessException(400, "\u9a8c\u8bc1\u7801\u65e0\u6548\u6216\u5df2\u8fc7\u671f"));
        if (!record.getCode().equals(code.trim())) {
            throw new BusinessException(400, "\u9a8c\u8bc1\u7801\u9519\u8bef");
        }
        record.setUsedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        this.smsCodeRepository.save((Object)record);
    }

    private void checkAntiAbuse(String phone, String scene, String clientIp) {
        SysSmsCode last;
        LocalDateTime hourAgo;
        String cooldownKey = phone + ":" + scene;
        if (this.phoneSendCooldown.getIfPresent((Object)cooldownKey) != null) {
            throw new BusinessException(429, "\u53d1\u9001\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        }
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long phoneDayCount = this.smsCodeRepository.countRecentByPhoneAndScene(phone, scene, dayStart);
        if (phoneDayCount >= (long)this.smsProperties.getPhoneDailyLimit()) {
            throw new BusinessException(429, "\u8be5\u624b\u673a\u53f7\u4eca\u65e5\u9a8c\u8bc1\u7801\u6b21\u6570\u5df2\u8fbe\u4e0a\u9650");
        }
        String ip = SmsService.safeIp((String)clientIp);
        long ipHourCount = this.smsCodeRepository.countRecentByIp(ip, hourAgo = LocalDateTime.now().minusHours(1L));
        if (ipHourCount >= (long)this.smsProperties.getIpHourlyLimit()) {
            throw new BusinessException(429, "\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        }
        long ipDayCount = this.smsCodeRepository.countRecentByIp(ip, dayStart);
        if (ipDayCount >= (long)this.smsProperties.getIpDailyLimit()) {
            throw new BusinessException(429, "\u8bf7\u6c42\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u660e\u65e5\u518d\u8bd5");
        }
        List recent = this.smsCodeRepository.findRecentByPhoneAndScene(phone, scene, Pageable.ofSize((int)1));
        if (!recent.isEmpty() && (last = (SysSmsCode)recent.get(0)).getCreatedAt() != null && last.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(Math.max(30, this.smsProperties.getSendIntervalSec())))) {
            throw new BusinessException(429, "\u53d1\u9001\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        }
    }

    private String generateCode() {
        int len = Math.max(4, Math.min(8, this.smsProperties.getCodeLength()));
        int bound = (int)Math.pow(10.0, len);
        int n = this.random.nextInt(bound);
        return String.format("%0" + len + "d", n);
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

    private static void validateScene(String scene) {
        if (scene == null || scene.isBlank()) {
            throw new BusinessException(400, "\u573a\u666f\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!("register".equals(scene) || "login".equals(scene) || "bind_phone".equals(scene) || "reset_password".equals(scene))) {
            throw new BusinessException(400, "\u65e0\u6548\u7684\u9a8c\u8bc1\u7801\u573a\u666f");
        }
    }

    private static String safeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.length() > 128 ? ip.substring(0, 128) : ip;
    }

    private String resolveTemplate(String scene) {
        String resetTemplate;
        if ("login".equals(scene)) {
            return this.smsProperties.getTemplateCodeLogin();
        }
        if ("reset_password".equals(scene) && (resetTemplate = this.smsProperties.getTemplateCodeResetPassword()) != null && !resetTemplate.isBlank()) {
            return resetTemplate;
        }
        return this.smsProperties.getTemplateCodeRegister();
    }

    private static String maskKey(String key) {
        if (key == null || key.isBlank()) {
            return "(\u7a7a)";
        }
        if (key.length() <= 8) {
            return "***" + key.substring(key.length() - 2);
        }
        return key.substring(0, 4) + "***" + key.substring(key.length() - 4);
    }

    private static String orDash(String s) {
        if (s == null || s.isBlank()) {
            return "(\u672a\u914d\u7f6e)";
        }
        return s;
    }

    @Generated
    public SmsService(SmsProperties smsProperties, SysSmsCodeRepository smsCodeRepository, AliyunSmsClient aliyunSmsClient) {
        this.smsProperties = smsProperties;
        this.smsCodeRepository = smsCodeRepository;
        this.aliyunSmsClient = aliyunSmsClient;
    }
}

