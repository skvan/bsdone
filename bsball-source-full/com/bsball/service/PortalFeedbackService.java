/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.config.TenantProperties
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.exception.UnauthorizedException
 *  com.bsball.model.dto.PortalFeedbackSubmitDto
 *  com.bsball.model.entity.PortalFeedback
 *  com.bsball.repository.PortalFeedbackRepository
 *  com.bsball.repository.SysUserRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.AuthCaptchaService
 *  com.bsball.service.CountAntiAbuseService
 *  com.bsball.service.PortalFeedbackService
 *  com.bsball.service.SysConfigService
 *  com.bsball.utils.HttpClientIpUtil
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  jakarta.servlet.http.HttpServletRequest
 *  lombok.Generated
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.util.StringUtils
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.config.TenantProperties;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.exception.UnauthorizedException;
import com.bsball.model.dto.PortalFeedbackSubmitDto;
import com.bsball.model.entity.PortalFeedback;
import com.bsball.repository.PortalFeedbackRepository;
import com.bsball.repository.SysUserRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.AuthCaptchaService;
import com.bsball.service.CountAntiAbuseService;
import com.bsball.service.SysConfigService;
import com.bsball.utils.HttpClientIpUtil;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.Generated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class PortalFeedbackService {
    private static final int SINGLE_CONTENT_MAX_LEN = 500;
    private static final int MAX_PENDING_APPENDS = 3;
    private static final Set<String> FEEDBACK_TYPES = Set.of((Object)"suggestion", (Object)"bug", (Object)"content", (Object)"account", (Object)"other");
    private static final Set<String> CONTACT_TYPES = Set.of((Object)"phone", (Object)"email", (Object)"qq", (Object)"wechat", (Object)"other");
    private static final Set<String> STATUS_TYPES = Set.of((Object)"pending", (Object)"replied", (Object)"closed");
    private static final Pattern PHONE_CN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_SIMPLE = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern QQ_DIGITS = Pattern.compile("^\\d{5,12}$");
    private static final Set<String> COMMON_EMAIL_DOMAINS = Set.of((Object[])new String[]{"qq.com", "foxmail.com", "163.com", "126.com", "yeah.net", "gmail.com", "outlook.com", "hotmail.com", "live.com", "msn.com", "icloud.com", "me.com", "sina.com", "sina.cn", "sohu.com", "aliyun.com"});
    private static final DateTimeFormatter APPEND_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RESERVED_USER_APPEND_MARKER = "--- \u7528\u6237\u8865\u5145\uff08";
    private static final String RESERVED_OPS_REPLY_MARKER = "--- \u8fd0\u8425\u56de\u590d\uff08";
    private final PortalFeedbackRepository portalFeedbackRepository;
    private final AuthCaptchaService authCaptchaService;
    private final CountAntiAbuseService countAntiAbuseService;
    private final SysConfigService sysConfigService;
    private final ApiPermissionService apiPermissionService;
    private final TenantProperties tenantProperties;
    private final SysUserRepository sysUserRepository;
    @Value(value="${app.release-version:}")
    private String serverReleaseVersion;

    public void submit(HttpServletRequest request, PortalFeedbackSubmitDto dto, String visitorId) {
        if (dto == null) {
            throw new BusinessException(400, "\u8bf7\u6c42\u4f53\u4e0d\u80fd\u4e3a\u7a7a");
        }
        long tenantId = this.sysConfigService.effectiveTenantId(CurrentUserHolder.getTenantId());
        String ip = HttpClientIpUtil.getClientIp((HttpServletRequest)request);
        if (!this.countAntiAbuseService.acquirePortalFeedback(ip, tenantId)) {
            throw new BusinessException(429, "\u63d0\u4ea4\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        }
        String type = PortalFeedbackService.normalize((String)dto.getFeedbackType());
        if (!FEEDBACK_TYPES.contains(type)) {
            throw new BusinessException(400, "\u8bf7\u9009\u62e9\u53cd\u9988\u7c7b\u578b");
        }
        String content = PortalFeedbackService.trim((String)dto.getContent());
        if (content.isEmpty()) {
            throw new BusinessException(400, "\u8bf7\u586b\u5199\u8be6\u7ec6\u63cf\u8ff0");
        }
        if (content.length() > 500) {
            throw new BusinessException(400, "\u8be6\u7ec6\u63cf\u8ff0\u8fc7\u957f\uff0c\u8bf7\u63a7\u5236\u5728 500 \u5b57\u4ee5\u5185");
        }
        PortalFeedbackService.assertNoFeedbackStructureMarkers((String)content);
        String title = PortalFeedbackService.trim((String)dto.getTitle());
        if (title.length() > 200) {
            throw new BusinessException(400, "\u6807\u9898\u8fc7\u957f");
        }
        Long feedbackId = dto.getFeedbackId();
        boolean appendMode = feedbackId != null && feedbackId > 0L;
        Long uid = CurrentUserHolder.get();
        String contactType = PortalFeedbackService.normalize((String)dto.getContactType());
        String contactValue = PortalFeedbackService.trim((String)dto.getContactValue());
        if (appendMode) {
            if (StringUtils.hasText((String)contactValue)) {
                if (!CONTACT_TYPES.contains(contactType)) {
                    throw new BusinessException(400, "\u8bf7\u9009\u62e9\u8054\u7cfb\u65b9\u5f0f\u7c7b\u578b");
                }
                PortalFeedbackService.validateContact((String)contactType, (String)contactValue);
            } else {
                contactType = null;
                contactValue = null;
            }
        } else if (uid == null) {
            if (!CONTACT_TYPES.contains(contactType)) {
                throw new BusinessException(400, "\u8bf7\u9009\u62e9\u8054\u7cfb\u65b9\u5f0f\u7c7b\u578b");
            }
            if (!StringUtils.hasText((String)contactValue)) {
                throw new BusinessException(400, "\u8bf7\u586b\u5199\u8054\u7cfb\u65b9\u5f0f\uff0c\u4fbf\u4e8e\u6211\u4eec\u56de\u590d\u60a8");
            }
            PortalFeedbackService.validateContact((String)contactType, (String)contactValue);
        } else if (StringUtils.hasText((String)contactValue)) {
            if (!CONTACT_TYPES.contains(contactType)) {
                throw new BusinessException(400, "\u8bf7\u9009\u62e9\u8054\u7cfb\u65b9\u5f0f\u7c7b\u578b");
            }
            PortalFeedbackService.validateContact((String)contactType, (String)contactValue);
        } else {
            contactType = null;
            contactValue = null;
        }
        HashMap<String, String> captchaBody = new HashMap<String, String>();
        captchaBody.put("captchaId", dto.getCaptchaId());
        captchaBody.put("captchaVerifyToken", dto.getCaptchaVerifyToken());
        try {
            this.authCaptchaService.validateAndConsumePortalFeedbackCaptcha(tenantId, captchaBody);
        }
        catch (UnauthorizedException e) {
            throw new BusinessException(400, e.getMessage());
        }
        if (appendMode) {
            this.appendToExisting(feedbackId, uid, visitorId, tenantId, content, ip, request, dto, contactType, contactValue);
            return;
        }
        PortalFeedback row = new PortalFeedback();
        row.setTenantId(Long.valueOf(tenantId));
        row.setUserId(uid);
        row.setVisitorId(PortalFeedbackService.isBlank((String)visitorId) ? null : PortalFeedbackService.truncate((String)visitorId, (int)64));
        row.setFeedbackType(type);
        row.setTitle(title.isEmpty() ? null : title);
        row.setContent(content);
        row.setContactType(PortalFeedbackService.isBlank((String)contactType) ? null : contactType);
        row.setContactValue(PortalFeedbackService.isBlank((String)contactValue) ? null : contactValue);
        row.setUserAgent(PortalFeedbackService.truncate((String)(request != null ? request.getHeader("User-Agent") : null), (int)512));
        row.setClientVersion(PortalFeedbackService.truncate((String)PortalFeedbackService.trim((String)dto.getClientVersion()), (int)64));
        row.setServerVersion(this.resolveServerVersion());
        row.setClientIp(ip);
        row.setPagePath(PortalFeedbackService.truncate((String)PortalFeedbackService.trim((String)dto.getPagePath()), (int)512));
        row.setStatus("pending");
        row.setPendingAppendCount(Integer.valueOf(0));
        this.portalFeedbackRepository.save((Object)row);
    }

    public PageResult<PortalFeedback> myList(Integer page, Integer pageSize, String visitorId) {
        Long uid = CurrentUserHolder.get();
        long tenantId = this.sysConfigService.effectiveTenantId(CurrentUserHolder.getTenantId());
        PageRequest p = PageRequest.of((int)((page != null && page > 0 ? page : 1) - 1), (int)PaginationSupport.resolvePageSize((Integer)pageSize), (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"}));
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            if (uid != null) {
                return cb.and((Expression)cb.equal((Expression)root.get("tenantId"), (Object)tenantId), (Expression)cb.equal((Expression)root.get("userId"), (Object)uid));
            }
            String vid = PortalFeedbackService.trim((String)visitorId);
            if (vid.isEmpty()) {
                throw new BusinessException(401, "\u8bf7\u5148\u63d0\u4ea4\u53cd\u9988\u540e\u518d\u67e5\u770b\u8bb0\u5f55");
            }
            return cb.and((Expression)cb.equal((Expression)root.get("tenantId"), (Object)tenantId), (Expression)cb.equal((Expression)root.get("visitorId"), (Object)vid));
        };
        Page rs = this.portalFeedbackRepository.findAll((Specification)spec, (Pageable)p);
        return PageResult.of((List)rs.getContent(), (long)rs.getTotalElements());
    }

    public PageResult<PortalFeedback> adminList(Integer page, Integer pageSize, String keyword, String feedbackType, String status) {
        Long uid = CurrentUserHolder.get();
        long currentTid = CurrentUserHolder.getTenantId() != null ? CurrentUserHolder.getTenantId().longValue() : this.tenantProperties.getDefaultId();
        boolean superGlobal = uid != null && this.apiPermissionService.isSuperAdmin(uid) && currentTid == 0L;
        PageRequest p = PageRequest.of((int)((page != null && page > 0 ? page : 1) - 1), (int)PaginationSupport.resolvePageSize((Integer)pageSize), (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"createdAt"}));
        String kw = PortalFeedbackService.normalize((String)keyword);
        String ft = PortalFeedbackService.normalize((String)feedbackType);
        String st = PortalFeedbackService.normalize((String)status);
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList<Predicate> preds = new ArrayList<Predicate>();
            if (!superGlobal) {
                preds.add(cb.equal((Expression)root.get("tenantId"), (Object)currentTid));
            }
            if (!kw.isEmpty()) {
                String like = "%" + kw + "%";
                preds.add(cb.or(new Predicate[]{cb.like(cb.lower(cb.coalesce((Expression)root.get("title"), (Object)"")), like), cb.like(cb.lower(cb.coalesce((Expression)root.get("content"), (Object)"")), like), cb.like(cb.lower(cb.coalesce((Expression)root.get("contactValue"), (Object)"")), like), cb.like(cb.lower(cb.coalesce((Expression)root.get("clientIp"), (Object)"")), like)}));
            }
            if (FEEDBACK_TYPES.contains(ft)) {
                preds.add(cb.equal((Expression)root.get("feedbackType"), (Object)ft));
            }
            if (STATUS_TYPES.contains(st)) {
                preds.add(cb.equal((Expression)root.get("status"), (Object)st));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        Page rs = this.portalFeedbackRepository.findAll((Specification)spec, (Pageable)p);
        return PageResult.of((List)rs.getContent(), (long)rs.getTotalElements());
    }

    public void adminReply(Long id, String status, String replyContent) {
        if (id == null || id <= 0L) {
            throw new BusinessException(400, "\u53cd\u9988 ID \u65e0\u6548");
        }
        PortalFeedback row = (PortalFeedback)this.portalFeedbackRepository.findById((Object)id).orElseThrow(() -> new BusinessException(404, "\u53cd\u9988\u4e0d\u5b58\u5728"));
        if ("closed".equals(PortalFeedbackService.normalize((String)row.getStatus()))) {
            throw new BusinessException(400, "\u8be5\u53cd\u9988\u5df2\u5173\u95ed\uff0c\u4e0d\u80fd\u518d\u6b21\u5904\u7406");
        }
        String st = PortalFeedbackService.normalize((String)status);
        if (!STATUS_TYPES.contains(st)) {
            throw new BusinessException(400, "\u72b6\u6001\u503c\u65e0\u6548");
        }
        String reply = PortalFeedbackService.trim((String)replyContent);
        if (!reply.isEmpty()) {
            PortalFeedbackService.assertNoFeedbackStructureMarkers((String)reply);
        }
        if ("pending".equals(st) && !reply.isEmpty()) {
            throw new BusinessException(400, "\u5f85\u5904\u7406\u72b6\u6001\u4e0d\u5e94\u586b\u5199\u56de\u590d\u5185\u5bb9");
        }
        if (("replied".equals(st) || "closed".equals(st)) && reply.isEmpty()) {
            throw new BusinessException(400, "\u8bf7\u8f93\u5165\u56de\u590d\u5185\u5bb9");
        }
        row.setStatus(st);
        Long opId = CurrentUserHolder.get();
        if (!reply.isEmpty()) {
            String opName = this.userDisplayName(opId);
            if (opName == null || opName.isBlank()) {
                opName = "\u5904\u7406\u4eba";
            }
            row.setReplyContent(PortalFeedbackService.buildAppendedAdminReply((String)row.getReplyContent(), (String)reply, (String)opName));
            row.setRepliedBy(opId);
            row.setRepliedAt(LocalDateTime.now());
        }
        row.setPendingAppendCount(Integer.valueOf(0));
        this.portalFeedbackRepository.save((Object)row);
    }

    public String userDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        return this.sysUserRepository.findById((Object)userId).map(u -> {
            String n = PortalFeedbackService.trim((String)u.getNickname());
            return n.isEmpty() ? PortalFeedbackService.trim((String)u.getUsername()) : n;
        }).orElse(null);
    }

    private void appendToExisting(Long feedbackId, Long uid, String visitorId, long tenantId, String appendedContent, String ip, HttpServletRequest request, PortalFeedbackSubmitDto dto, String contactType, String contactValue) {
        int oldAppendCount;
        String vid;
        PortalFeedback row = (PortalFeedback)this.portalFeedbackRepository.findById((Object)feedbackId).orElseThrow(() -> new BusinessException(404, "\u53cd\u9988\u4e0d\u5b58\u5728"));
        if (!Objects.equals(row.getTenantId(), tenantId)) {
            throw new BusinessException(404, "\u53cd\u9988\u4e0d\u5b58\u5728");
        }
        if (uid != null ? !Objects.equals(row.getUserId(), uid) : (vid = PortalFeedbackService.trim((String)visitorId)).isEmpty() || !vid.equals(PortalFeedbackService.trim((String)row.getVisitorId()))) {
            throw new BusinessException(403, "\u65e0\u6743\u8865\u5145\u8be5\u53cd\u9988");
        }
        String oldStatus = PortalFeedbackService.normalize((String)row.getStatus());
        if ("closed".equals(oldStatus)) {
            throw new BusinessException(400, "\u8be5\u53cd\u9988\u5df2\u5173\u95ed\uff0c\u4e0d\u80fd\u7ee7\u7eed\u8865\u5145");
        }
        int n = oldAppendCount = row.getPendingAppendCount() == null ? 0 : row.getPendingAppendCount();
        if ("pending".equals(oldStatus) && oldAppendCount >= 3) {
            throw new BusinessException(400, "\u8be5\u53cd\u9988\u5728\u5f85\u5904\u7406\u9636\u6bb5\u6700\u591a\u8865\u5145 3 \u6b21\uff0c\u8bf7\u7b49\u5f85\u5904\u7406\u7ed3\u679c\u6216\u65b0\u5efa\u53cd\u9988");
        }
        row.setContent(PortalFeedbackService.buildAppendedContent((String)row.getContent(), (String)appendedContent));
        row.setPagePath(PortalFeedbackService.truncate((String)PortalFeedbackService.trim((String)dto.getPagePath()), (int)512));
        row.setClientVersion(PortalFeedbackService.truncate((String)PortalFeedbackService.trim((String)dto.getClientVersion()), (int)64));
        row.setServerVersion(this.resolveServerVersion());
        row.setClientIp(ip);
        row.setUserAgent(PortalFeedbackService.truncate((String)(request != null ? request.getHeader("User-Agent") : null), (int)512));
        if (!PortalFeedbackService.isBlank((String)contactValue)) {
            row.setContactType(contactType);
            row.setContactValue(contactValue);
        }
        row.setStatus("pending");
        row.setPendingAppendCount(Integer.valueOf("pending".equals(oldStatus) ? oldAppendCount + 1 : 1));
        this.portalFeedbackRepository.save((Object)row);
    }

    private static void assertNoFeedbackStructureMarkers(String text) {
        if (text.contains("--- \u7528\u6237\u8865\u5145\uff08") || text.contains("--- \u8fd0\u8425\u56de\u590d\uff08")) {
            throw new BusinessException(400, "\u5185\u5bb9\u5305\u542b\u7cfb\u7edf\u7528\u4e8e\u5206\u6bb5\u7684\u4fdd\u7559\u6807\u8bb0\uff0c\u8bf7\u5220\u9664\u76f8\u5173\u6587\u5b57\u540e\u91cd\u8bd5");
        }
    }

    private static String buildAppendedContent(String oldContent, String appended) {
        String base = PortalFeedbackService.trim((String)oldContent);
        String extra = PortalFeedbackService.trim((String)appended);
        String mark = "\n\n--- \u7528\u6237\u8865\u5145\uff08" + LocalDateTime.now().format(APPEND_TIME_FMT) + "\uff09---\n";
        if (base.isEmpty()) {
            return extra;
        }
        return base + mark + extra;
    }

    private static String buildAppendedAdminReply(String oldReply, String newReply, String operatorName) {
        String base = PortalFeedbackService.trim((String)oldReply);
        String extra = PortalFeedbackService.trim((String)newReply);
        String on = PortalFeedbackService.trim((String)operatorName);
        if (on.isEmpty()) {
            on = "\u5904\u7406\u4eba";
        }
        String mark = "\n\n--- \u8fd0\u8425\u56de\u590d\uff08" + LocalDateTime.now().format(APPEND_TIME_FMT) + " \u00b7 " + on + "\uff09---\n";
        if (base.isEmpty()) {
            return extra;
        }
        return base + mark + extra;
    }

    private static void validateContact(String contactType, String contactValue) {
        switch (contactType) {
            case "phone": {
                if (PHONE_CN.matcher(contactValue).matches()) break;
                throw new BusinessException(400, "\u8bf7\u8f93\u5165\u6709\u6548\u7684\u624b\u673a\u53f7\u7801");
            }
            case "email": {
                if (EMAIL_SIMPLE.matcher(contactValue).matches() && PortalFeedbackService.isCommonEmail((String)contactValue)) break;
                throw new BusinessException(400, "\u8bf7\u8f93\u5165\u5e38\u89c1\u90ae\u7bb1\u670d\u52a1\u5546\u5730\u5740");
            }
            case "qq": {
                if (QQ_DIGITS.matcher(contactValue).matches()) break;
                throw new BusinessException(400, "\u8bf7\u8f93\u5165\u6709\u6548\u7684 QQ \u53f7");
            }
            case "wechat": {
                if (contactValue.length() >= 2 && contactValue.length() <= 32) break;
                throw new BusinessException(400, "\u5fae\u4fe1\u53f7\u957f\u5ea6\u5e94\u5728 2\uff5e32 \u4e2a\u5b57\u7b26");
            }
            case "other": {
                if (contactValue.length() <= 120) break;
                throw new BusinessException(400, "\u8054\u7cfb\u65b9\u5f0f\u5185\u5bb9\u8fc7\u957f");
            }
            default: {
                throw new BusinessException(400, "\u8054\u7cfb\u65b9\u5f0f\u7c7b\u578b\u65e0\u6548");
            }
        }
    }

    private String resolveServerVersion() {
        String v = PortalFeedbackService.trim((String)this.serverReleaseVersion);
        if (v.isEmpty() || v.contains("@")) {
            return "unknown";
        }
        return PortalFeedbackService.truncate((String)v, (int)64);
    }

    private static String normalize(String s) {
        return PortalFeedbackService.trim((String)s).toLowerCase(Locale.ROOT);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static boolean isCommonEmail(String email) {
        String v = PortalFeedbackService.trim((String)email).toLowerCase(Locale.ROOT);
        int at = v.lastIndexOf(64);
        if (at <= 0 || at >= v.length() - 1) {
            return false;
        }
        String domain = v.substring(at + 1);
        return COMMON_EMAIL_DOMAINS.contains(domain);
    }

    @Generated
    public PortalFeedbackService(PortalFeedbackRepository portalFeedbackRepository, AuthCaptchaService authCaptchaService, CountAntiAbuseService countAntiAbuseService, SysConfigService sysConfigService, ApiPermissionService apiPermissionService, TenantProperties tenantProperties, SysUserRepository sysUserRepository) {
        this.portalFeedbackRepository = portalFeedbackRepository;
        this.authCaptchaService = authCaptchaService;
        this.countAntiAbuseService = countAntiAbuseService;
        this.sysConfigService = sysConfigService;
        this.apiPermissionService = apiPermissionService;
        this.tenantProperties = tenantProperties;
        this.sysUserRepository = sysUserRepository;
    }
}

