/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalFeedbackApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.model.dto.PortalFeedbackSubmitDto
 *  com.bsball.model.entity.PortalFeedback
 *  com.bsball.service.AuthCaptchaService
 *  com.bsball.service.CountAntiAbuseService
 *  com.bsball.service.PortalFeedbackService
 *  com.bsball.service.SysConfigService
 *  com.bsball.utils.HttpClientIpUtil
 *  jakarta.servlet.http.Cookie
 *  jakarta.servlet.http.HttpServletRequest
 *  jakarta.servlet.http.HttpServletResponse
 *  lombok.Generated
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.bsball.api;

import com.bsball.common.PageResult;
import com.bsball.common.Result;
import com.bsball.core.CurrentUserHolder;
import com.bsball.model.dto.PortalFeedbackSubmitDto;
import com.bsball.model.entity.PortalFeedback;
import com.bsball.service.AuthCaptchaService;
import com.bsball.service.CountAntiAbuseService;
import com.bsball.service.PortalFeedbackService;
import com.bsball.service.SysConfigService;
import com.bsball.utils.HttpClientIpUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * Exception performing whole class analysis ignored.
 */
@RestController
@RequestMapping(value={"/portal/feedback"})
public class PortalFeedbackApi {
    private static final String VISITOR_COOKIE = "portal_vid";
    private static final int COOKIE_MAX_AGE = 31536000;
    private final AuthCaptchaService authCaptchaService;
    private final PortalFeedbackService portalFeedbackService;
    private final SysConfigService sysConfigService;
    private final CountAntiAbuseService countAntiAbuseService;

    @GetMapping(value={"/captcha-image"})
    public Result<Map<String, Object>> captchaImage(HttpServletRequest request) {
        long tid = this.sysConfigService.effectiveTenantId(CurrentUserHolder.getTenantId());
        String ip = HttpClientIpUtil.getClientIp((HttpServletRequest)request);
        if (!this.countAntiAbuseService.acquirePortalFeedbackCaptcha(ip, tid)) {
            return Result.fail((int)429, (String)"\u9a8c\u8bc1\u7801\u5237\u65b0\u8fc7\u4e8e\u9891\u7e41\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5");
        }
        return Result.ok((Object)this.authCaptchaService.createPortalFeedbackCaptcha(tid));
    }

    @PostMapping(value={"/submit"})
    public Result<Map<String, Object>> submit(HttpServletRequest request, HttpServletResponse response, @RequestBody PortalFeedbackSubmitDto body) {
        String visitorId = PortalFeedbackApi.ensureVisitorCookie((HttpServletRequest)request, (HttpServletResponse)response);
        this.portalFeedbackService.submit(request, body, visitorId);
        return Result.ok((Object)Map.of((Object)"ok", (Object)true));
    }

    @GetMapping(value={"/my-list"})
    public Result<PageResult<Map<String, Object>>> myList(HttpServletRequest request, HttpServletResponse response, @RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize) {
        String visitorId = PortalFeedbackApi.ensureVisitorCookie((HttpServletRequest)request, (HttpServletResponse)response);
        PageResult rows = this.portalFeedbackService.myList(page, pageSize, visitorId);
        return Result.ok((Object)PageResult.of((List)rows.getList().stream().map(arg_0 -> this.toRow(arg_0)).toList(), (long)rows.getTotal()));
    }

    private Map<String, Object> toRow(PortalFeedback f) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", f.getId());
        out.put("feedbackType", f.getFeedbackType());
        out.put("title", f.getTitle());
        out.put("content", f.getContent());
        out.put("status", f.getStatus());
        out.put("pendingAppendCount", f.getPendingAppendCount());
        out.put("replyContent", f.getReplyContent());
        out.put("repliedAt", f.getRepliedAt());
        out.put("repliedBy", f.getRepliedBy());
        out.put("repliedByName", this.portalFeedbackService.userDisplayName(f.getRepliedBy()));
        out.put("createdAt", f.getCreatedAt());
        return out;
    }

    private static String ensureVisitorCookie(HttpServletRequest request, HttpServletResponse response) {
        String old = PortalFeedbackApi.readVisitorCookie((HttpServletRequest)request);
        if (old != null && !old.isBlank()) {
            return old;
        }
        String visitorId = UUID.randomUUID().toString().replace("-", "");
        Cookie c = new Cookie("portal_vid", visitorId);
        c.setPath("/");
        c.setMaxAge(31536000);
        c.setHttpOnly(true);
        c.setAttribute("SameSite", "Lax");
        response.addCookie(c);
        return visitorId;
    }

    private static String readVisitorCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (!"portal_vid".equals(c.getName())) continue;
            return c.getValue();
        }
        return null;
    }

    @Generated
    public PortalFeedbackApi(AuthCaptchaService authCaptchaService, PortalFeedbackService portalFeedbackService, SysConfigService sysConfigService, CountAntiAbuseService countAntiAbuseService) {
        this.authCaptchaService = authCaptchaService;
        this.portalFeedbackService = portalFeedbackService;
        this.sysConfigService = sysConfigService;
        this.countAntiAbuseService = countAntiAbuseService;
    }
}

