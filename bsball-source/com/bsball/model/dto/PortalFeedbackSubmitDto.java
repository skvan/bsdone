/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.PortalFeedbackSubmitDto
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  lombok.Generated
 */
package com.bsball.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Generated;

public class PortalFeedbackSubmitDto {
    @JsonProperty(value="feedbackType")
    private String feedbackType;
    @JsonProperty(value="feedbackId")
    private Long feedbackId;
    private String title;
    private String content;
    @JsonProperty(value="contactType")
    private String contactType;
    @JsonProperty(value="contactValue")
    private String contactValue;
    @JsonProperty(value="captchaId")
    private String captchaId;
    @JsonProperty(value="captchaCode")
    private String captchaCode;
    @JsonProperty(value="captchaVerifyToken")
    private String captchaVerifyToken;
    @JsonProperty(value="clientVersion")
    private String clientVersion;
    @JsonProperty(value="pagePath")
    private String pagePath;

    @Generated
    public PortalFeedbackSubmitDto() {
    }

    @Generated
    public String getFeedbackType() {
        return this.feedbackType;
    }

    @Generated
    public Long getFeedbackId() {
        return this.feedbackId;
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
    public String getCaptchaId() {
        return this.captchaId;
    }

    @Generated
    public String getCaptchaCode() {
        return this.captchaCode;
    }

    @Generated
    public String getCaptchaVerifyToken() {
        return this.captchaVerifyToken;
    }

    @Generated
    public String getClientVersion() {
        return this.clientVersion;
    }

    @Generated
    public String getPagePath() {
        return this.pagePath;
    }

    @Generated
    public void setFeedbackType(String feedbackType) {
        this.feedbackType = feedbackType;
    }

    @Generated
    public void setFeedbackId(Long feedbackId) {
        this.feedbackId = feedbackId;
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
    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    @Generated
    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }

    @Generated
    public void setCaptchaVerifyToken(String captchaVerifyToken) {
        this.captchaVerifyToken = captchaVerifyToken;
    }

    @Generated
    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    @Generated
    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof PortalFeedbackSubmitDto)) {
            return false;
        }
        PortalFeedbackSubmitDto other = (PortalFeedbackSubmitDto)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$feedbackId = this.getFeedbackId();
        Long other$feedbackId = other.getFeedbackId();
        if (this$feedbackId == null ? other$feedbackId != null : !((Object)this$feedbackId).equals(other$feedbackId)) {
            return false;
        }
        String this$feedbackType = this.getFeedbackType();
        String other$feedbackType = other.getFeedbackType();
        if (this$feedbackType == null ? other$feedbackType != null : !this$feedbackType.equals(other$feedbackType)) {
            return false;
        }
        String this$title = this.getTitle();
        String other$title = other.getTitle();
        if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
            return false;
        }
        String this$content = this.getContent();
        String other$content = other.getContent();
        if (this$content == null ? other$content != null : !this$content.equals(other$content)) {
            return false;
        }
        String this$contactType = this.getContactType();
        String other$contactType = other.getContactType();
        if (this$contactType == null ? other$contactType != null : !this$contactType.equals(other$contactType)) {
            return false;
        }
        String this$contactValue = this.getContactValue();
        String other$contactValue = other.getContactValue();
        if (this$contactValue == null ? other$contactValue != null : !this$contactValue.equals(other$contactValue)) {
            return false;
        }
        String this$captchaId = this.getCaptchaId();
        String other$captchaId = other.getCaptchaId();
        if (this$captchaId == null ? other$captchaId != null : !this$captchaId.equals(other$captchaId)) {
            return false;
        }
        String this$captchaCode = this.getCaptchaCode();
        String other$captchaCode = other.getCaptchaCode();
        if (this$captchaCode == null ? other$captchaCode != null : !this$captchaCode.equals(other$captchaCode)) {
            return false;
        }
        String this$captchaVerifyToken = this.getCaptchaVerifyToken();
        String other$captchaVerifyToken = other.getCaptchaVerifyToken();
        if (this$captchaVerifyToken == null ? other$captchaVerifyToken != null : !this$captchaVerifyToken.equals(other$captchaVerifyToken)) {
            return false;
        }
        String this$clientVersion = this.getClientVersion();
        String other$clientVersion = other.getClientVersion();
        if (this$clientVersion == null ? other$clientVersion != null : !this$clientVersion.equals(other$clientVersion)) {
            return false;
        }
        String this$pagePath = this.getPagePath();
        String other$pagePath = other.getPagePath();
        return !(this$pagePath == null ? other$pagePath != null : !this$pagePath.equals(other$pagePath));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof PortalFeedbackSubmitDto;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $feedbackId = this.getFeedbackId();
        result = result * 59 + ($feedbackId == null ? 43 : ((Object)$feedbackId).hashCode());
        String $feedbackType = this.getFeedbackType();
        result = result * 59 + ($feedbackType == null ? 43 : $feedbackType.hashCode());
        String $title = this.getTitle();
        result = result * 59 + ($title == null ? 43 : $title.hashCode());
        String $content = this.getContent();
        result = result * 59 + ($content == null ? 43 : $content.hashCode());
        String $contactType = this.getContactType();
        result = result * 59 + ($contactType == null ? 43 : $contactType.hashCode());
        String $contactValue = this.getContactValue();
        result = result * 59 + ($contactValue == null ? 43 : $contactValue.hashCode());
        String $captchaId = this.getCaptchaId();
        result = result * 59 + ($captchaId == null ? 43 : $captchaId.hashCode());
        String $captchaCode = this.getCaptchaCode();
        result = result * 59 + ($captchaCode == null ? 43 : $captchaCode.hashCode());
        String $captchaVerifyToken = this.getCaptchaVerifyToken();
        result = result * 59 + ($captchaVerifyToken == null ? 43 : $captchaVerifyToken.hashCode());
        String $clientVersion = this.getClientVersion();
        result = result * 59 + ($clientVersion == null ? 43 : $clientVersion.hashCode());
        String $pagePath = this.getPagePath();
        result = result * 59 + ($pagePath == null ? 43 : $pagePath.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "PortalFeedbackSubmitDto(feedbackType=" + this.getFeedbackType() + ", feedbackId=" + this.getFeedbackId() + ", title=" + this.getTitle() + ", content=" + this.getContent() + ", contactType=" + this.getContactType() + ", contactValue=" + this.getContactValue() + ", captchaId=" + this.getCaptchaId() + ", captchaCode=" + this.getCaptchaCode() + ", captchaVerifyToken=" + this.getCaptchaVerifyToken() + ", clientVersion=" + this.getClientVersion() + ", pagePath=" + this.getPagePath() + ")";
    }
}

