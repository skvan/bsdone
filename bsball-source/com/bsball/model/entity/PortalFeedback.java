/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PortalFeedback
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Index
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="portal_feedback", indexes={@Index(name="idx_portal_feedback_tenant_created", columnList="tenant_id,created_at"), @Index(name="idx_portal_feedback_tenant_visitor_created", columnList="tenant_id,visitor_id,created_at")})
@Comment(value="\u95e8\u6237\u610f\u89c1\u53cd\u9988")
public class PortalFeedback {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Comment(value="\u4e3b\u952eID")
    private Long id;
    @Column(name="tenant_id", nullable=false)
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Column(name="user_id")
    @Comment(value="\u63d0\u4ea4\u7528\u6237\uff08\u540e\u53f0\u767b\u5f55\u4e14\u95e8\u6237 URL \u79df\u6237\u4e0e JWT \u4e00\u81f4\u65f6\uff09")
    private Long userId;
    @Column(name="visitor_id", length=64)
    @Comment(value="\u6e38\u5ba2\u8bbf\u5ba2ID\uff08portal_vid\uff09")
    private String visitorId;
    @Column(name="feedback_type", nullable=false, length=32)
    @Comment(value="\u53cd\u9988\u7c7b\u578b\u4ee3\u7801")
    private String feedbackType;
    @Column(length=200)
    @Comment(value="\u6807\u9898\uff08\u53ef\u9009\uff09")
    private String title;
    @Column(nullable=false, columnDefinition="TEXT")
    @Comment(value="\u8be6\u7ec6\u63cf\u8ff0")
    private String content;
    @Column(name="contact_type", length=16)
    @Comment(value="\u8054\u7cfb\u65b9\u5f0f\u7c7b\u578b\uff1aphone/email/qq/wechat/other")
    private String contactType;
    @Column(name="contact_value", length=256)
    @Comment(value="\u8054\u7cfb\u65b9\u5f0f\u5185\u5bb9")
    private String contactValue;
    @Column(name="user_agent", length=512)
    @Comment(value="\u6d4f\u89c8\u5668 UA")
    private String userAgent;
    @Column(name="client_version", length=64)
    @Comment(value="\u524d\u7aef\u7248\u672c\u53f7")
    private String clientVersion;
    @Column(name="server_version", length=64)
    @Comment(value="\u540e\u7aef\u7248\u672c\u53f7")
    private String serverVersion;
    @Column(name="client_ip", length=128)
    @Comment(value="\u5ba2\u6237\u7aef IP")
    private String clientIp;
    @Column(name="page_path", length=512)
    @Comment(value="\u63d0\u4ea4\u65f6\u9875\u9762\u8def\u5f84\uff08\u524d\u7aef\u4e0a\u62a5\uff09")
    private String pagePath;
    @Column(length=16, nullable=false)
    @Comment(value="\u5904\u7406\u72b6\u6001\uff1apending/replied/closed")
    private String status;
    @Column(name="pending_append_count", nullable=false)
    @Comment(value="\u5f53\u524d\u5f85\u5904\u7406\u5468\u671f\u5185\uff0c\u7528\u6237\u5df2\u8865\u5145\u6b21\u6570")
    private Integer pendingAppendCount;
    @Column(name="replied_by")
    @Comment(value="\u56de\u590d\u4eba\u7528\u6237ID")
    private Long repliedBy;
    @Column(name="replied_at")
    @Comment(value="\u56de\u590d\u65f6\u95f4")
    private LocalDateTime repliedAt;
    @Column(name="reply_content", columnDefinition="TEXT")
    @Comment(value="\u8fd0\u8425\u56de\u590d")
    private String replyContent;
    @Column(name="created_at", nullable=false)
    @Comment(value="\u521b\u5efa\u65f6\u95f4")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = "pending";
        }
        if (this.pendingAppendCount == null) {
            this.pendingAppendCount = 0;
        }
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getUserId() {
        return this.userId;
    }

    @Generated
    public String getVisitorId() {
        return this.visitorId;
    }

    @Generated
    public String getFeedbackType() {
        return this.feedbackType;
    }

    @Generated
    public String getTitle() {
        return this.title;
    }

    @Generated
    public String getContent() {
        return this.content;
    }

    @Generated
    public String getContactType() {
        return this.contactType;
    }

    @Generated
    public String getContactValue() {
        return this.contactValue;
    }

    @Generated
    public String getUserAgent() {
        return this.userAgent;
    }

    @Generated
    public String getClientVersion() {
        return this.clientVersion;
    }

    @Generated
    public String getServerVersion() {
        return this.serverVersion;
    }

    @Generated
    public String getClientIp() {
        return this.clientIp;
    }

    @Generated
    public String getPagePath() {
        return this.pagePath;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public Integer getPendingAppendCount() {
        return this.pendingAppendCount;
    }

    @Generated
    public Long getRepliedBy() {
        return this.repliedBy;
    }

    @Generated
    public LocalDateTime getRepliedAt() {
        return this.repliedAt;
    }

    @Generated
    public String getReplyContent() {
        return this.replyContent;
    }

    @Generated
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Generated
    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    @Generated
    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    @Generated
    public void setTitle(String title) {
        this.title = title;
    }

    @Generated
    public void setContent(String content) {
        this.content = content;
    }

    @Generated
    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    @Generated
    public void setContactValue(String contactValue) {
        this.contactValue = contactValue;
    }

    @Generated
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Generated
    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    @Generated
    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    @Generated
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    @Generated
    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setPendingAppendCount(Integer pendingAppendCount) {
        this.pendingAppendCount = pendingAppendCount;
    }

    @Generated
    public void setRepliedBy(Long repliedBy) {
        this.repliedBy = repliedBy;
    }

    @Generated
    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }

    @Generated
    public void setReplyContent(String replyContent) {
        this.replyContent = replyContent;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

