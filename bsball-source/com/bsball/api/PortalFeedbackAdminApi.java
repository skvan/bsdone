/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.api.PortalFeedbackAdminApi
 *  com.bsball.common.PageResult
 *  com.bsball.common.Result
 *  com.bsball.model.entity.PortalFeedback
 *  com.bsball.service.PortalFeedbackService
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
import com.bsball.model.entity.PortalFeedback;
import com.bsball.service.PortalFeedbackService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
@RequestMapping(value={"/sys/portal/feedback"})
public class PortalFeedbackAdminApi {
    private final PortalFeedbackService portalFeedbackService;

    @GetMapping(value={"/list"})
    public Result<PageResult<Map<String, Object>>> list(@RequestParam(required=false) Integer page, @RequestParam(required=false) Integer pageSize, @RequestParam(required=false) String keyword, @RequestParam(required=false) String feedbackType, @RequestParam(required=false) String status) {
        PageResult rows = this.portalFeedbackService.adminList(page, pageSize, keyword, feedbackType, status);
        return Result.ok((Object)PageResult.of((List)rows.getList().stream().map(arg_0 -> this.toRow(arg_0)).toList(), (long)rows.getTotal()));
    }

    @PostMapping(value={"/reply"})
    public Result<Map<String, Object>> reply(@RequestBody Map<String, Object> body) {
        Long id = PortalFeedbackAdminApi.parseLong((Object)body.get("id"));
        String status = body.get("status") != null ? String.valueOf(body.get("status")) : null;
        String replyContent = body.get("replyContent") != null ? String.valueOf(body.get("replyContent")) : null;
        this.portalFeedbackService.adminReply(id, status, replyContent);
        return Result.ok((Object)Map.of((Object)"ok", (Object)true));
    }

    private Map<String, Object> toRow(PortalFeedback f) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", f.getId());
        out.put("tenantId", f.getTenantId());
        out.put("feedbackType", f.getFeedbackType());
        out.put("title", f.getTitle());
        out.put("content", f.getContent());
        out.put("contactType", f.getContactType());
        out.put("contactValue", f.getContactValue());
        out.put("status", f.getStatus());
        out.put("pendingAppendCount", f.getPendingAppendCount());
        out.put("replyContent", f.getReplyContent());
        out.put("repliedAt", f.getRepliedAt());
        out.put("repliedBy", f.getRepliedBy());
        out.put("repliedByName", this.portalFeedbackService.userDisplayName(f.getRepliedBy()));
        out.put("clientVersion", f.getClientVersion());
        out.put("serverVersion", f.getServerVersion());
        out.put("clientIp", f.getClientIp());
        out.put("pagePath", f.getPagePath());
        out.put("userAgent", f.getUserAgent());
        out.put("createdAt", f.getCreatedAt());
        return out;
    }

    private static Long parseLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            Number n = (Number)v;
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        }
        catch (Exception e) {
            return null;
        }
    }

    @Generated
    public PortalFeedbackAdminApi(PortalFeedbackService portalFeedbackService) {
        this.portalFeedbackService = portalFeedbackService;
    }
}

