/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.EmailProperties
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysEmailCode
 *  com.bsball.repository.SysEmailCodeRepository
 *  com.bsball.service.EmailService
 *  com.github.benmanes.caffeine.cache.Cache
 *  com.github.benmanes.caffeine.cache.Caffeine
 *  jakarta.annotation.PostConstruct
 *  jakarta.mail.Address
 *  jakarta.mail.Authenticator
 *  jakarta.mail.Message
 *  jakarta.mail.Message$RecipientType
 *  jakarta.mail.Session
 *  jakarta.mail.Transport
 *  jakarta.mail.internet.InternetAddress
 *  jakarta.mail.internet.MimeMessage
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.service;

import com.bsball.config.EmailProperties;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysEmailCode;
import com.bsball.repository.SysEmailCodeRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class EmailService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private final EmailProperties emailProperties;
    private final SysEmailCodeRepository emailCodeRepository;
    private final SecureRandom random = new SecureRandom();
    private Cache<String, Boolean> sendCooldown;

    @PostConstruct
    void init() {
        int sec = Math.max(30, this.emailProperties.getSendIntervalSec());
        this.sendCooldown = Caffeine.newBuilder().expireAfterWrite((long)sec, TimeUnit.SECONDS).maximumSize(100000L).build();
    }

    public void sendCode(String email, String scene, long tenantId, String clientIp) {
        String normalized = EmailService.normalizeEmail((String)email);
        EmailService.validateScene((String)scene);
        this.checkAntiAbuse(normalized, scene);
        String code = this.generateCode();
        int expireSec = Math.max(60, this.emailProperties.getCodeExpireSec());
        int expireMin = Math.max(1, expireSec / 60);
        LocalDateTime now = LocalDateTime.now();
        SysEmailCode record = new SysEmailCode();
        record.setTenantId(tenantId > 0L ? Long.valueOf(tenantId) : null);
        record.setEmail(normalized);
        record.setCode(code);
        record.setScene(scene);
        record.setClientIp(EmailService.safeIp((String)clientIp));
        record.setExpiresAt(now.plusSeconds(expireSec));
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        this.emailCodeRepository.save((Object)record);
        boolean sent = false;
        if (this.emailProperties.isEnabled() && !this.emailProperties.getHost().isBlank()) {
            try {
                this.deliver(normalized, code, expireMin);
                sent = true;
            }
            catch (Exception e) {
                log.warn("[EMAIL] \u53d1\u9001\u5931\u8d25 to={}: {}", (Object)normalized, (Object)e.getMessage());
            }
        }
        if (!sent) {
            log.info("[EMAIL-MOCK] email={} scene={} code={} (\u914d\u7f6e app.email.enabled=true \u4e14\u586b\u5199 SMTP \u540e\u771f\u5b9e\u53d1\u9001)", new Object[]{normalized, scene, code});
        }
        this.sendCooldown.put((Object)(normalized + ":" + scene), (Object)Boolean.TRUE);
    }

    @Transactional
    public void verifyCode(String email, String scene, String code) {
        String normalized = EmailService.normalizeEmail((String)email);
        EmailService.validateScene((String)scene);
        if (code == null || code.isBlank()) {
            throw new BusinessException(400, "\u9a8c\u8bc1\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        SysEmailCode record = (SysEmailCode)this.emailCodeRepository.findLatestValid(normalized, scene, LocalDateTime.now()).orElseThrow(() -> new BusinessException(400, "\u9a8c\u8bc1\u7801\u65e0\u6548\u6216\u5df2\u8fc7\u671f"));
        if (!record.getCode().equals(code.trim())) {
            throw new BusinessException(400, "\u9a8c\u8bc1\u7801\u9519\u8bef");
        }
        record.setUsedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        this.emailCodeRepository.save((Object)record);
    }

    private void deliver(String to, String code, int expireMin) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", this.emailProperties.getHost());
        props.put("mail.smtp.port", String.valueOf(this.emailProperties.getPort()));
        props.put("mail.smtp.auth", "true");
        if (this.emailProperties.isUseTls()) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        Session session = Session.getInstance((Properties)props, (Authenticator)new /* Unavailable Anonymous Inner Class!! */);
        String from = this.emailProperties.getFrom().isBlank() ? this.emailProperties.getUsername() : this.emailProperties.getFrom();
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom((Address)new InternetAddress(from, this.emailProperties.getFromName(), StandardCharsets.UTF_8.name()));
        msg.setRecipients(Message.RecipientType.TO, (Address[])InternetAddress.parse((String)to));
        msg.setSubject("\u9a8c\u8bc1\u7801", StandardCharsets.UTF_8.name());
        msg.setText("\u60a8\u7684\u9a8c\u8bc1\u7801\u662f " + code + "\uff0c" + expireMin + " \u5206\u949f\u5185\u6709\u6548\u3002\u5982\u975e\u672c\u4eba\u64cd\u4f5c\u8bf7\u5ffd\u7565\u3002", StandardCharsets.UTF_8.name());
        Transport.send((Message)msg);
    }

    private void checkAntiAbuse(String email, String scene) {
        if (this.sendCooldown.getIfPresent((Object)(email + ":" + scene)) != null) {
            throw new BusinessException(429, "\u53d1\u9001\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        }
        LocalDateTime dayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long dayCount = this.emailCodeRepository.countRecentByEmailAndScene(email, scene, dayStart);
        if (dayCount >= (long)this.emailProperties.getDailyLimit()) {
            throw new BusinessException(429, "\u8be5\u90ae\u7bb1\u4eca\u65e5\u9a8c\u8bc1\u7801\u6b21\u6570\u5df2\u8fbe\u4e0a\u9650");
        }
    }

    private String generateCode() {
        int len = Math.max(4, Math.min(8, this.emailProperties.getCodeLength()));
        int bound = (int)Math.pow(10.0, len);
        int n = this.random.nextInt(bound);
        return String.format("%0" + len + "d", n);
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            throw new BusinessException(400, "\u90ae\u7bb1\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String e = email.trim().toLowerCase();
        if (e.isEmpty()) {
            throw new BusinessException(400, "\u90ae\u7bb1\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!EMAIL.matcher(e).matches()) {
            throw new BusinessException(400, "\u90ae\u7bb1\u683c\u5f0f\u4e0d\u6b63\u786e");
        }
        return e;
    }

    private static void validateScene(String scene) {
        if (!"bind_email".equals(scene) && !"reset_password".equals(scene)) {
            throw new BusinessException(400, "\u65e0\u6548\u7684\u9a8c\u8bc1\u7801\u573a\u666f");
        }
    }

    private static String safeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.length() > 128 ? ip.substring(0, 128) : ip;
    }

    @Generated
    public EmailService(EmailProperties emailProperties, SysEmailCodeRepository emailCodeRepository) {
        this.emailProperties = emailProperties;
        this.emailCodeRepository = emailCodeRepository;
    }
}

