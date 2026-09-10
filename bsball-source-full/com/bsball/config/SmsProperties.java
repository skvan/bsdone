/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.SmsProperties
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.stereotype.Component
 */
package com.bsball.config;

import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="app.sms")
public class SmsProperties {
    private boolean enabled = false;
    private String accessKeyId = "";
    private String accessKeySecret = "";
    private String signName = "";
    private String templateCodeRegister = "";
    private String templateCodeLogin = "";
    private String templateCodeResetPassword = "";
    private int codeLength = 6;
    private int codeExpireSec = 300;
    private int sendIntervalSec = 60;
    private int phoneDailyLimit = 10;
    private int ipHourlyLimit = 20;
    private int ipDailyLimit = 50;

    @Generated
    public SmsProperties() {
    }

    @Generated
    public boolean isEnabled() {
        return this.enabled;
    }

    @Generated
    public String getAccessKeyId() {
        return this.accessKeyId;
    }

    @Generated
    public String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    @Generated
    public String getSignName() {
        return this.signName;
    }

    @Generated
    public String getTemplateCodeRegister() {
        return this.templateCodeRegister;
    }

    @Generated
    public String getTemplateCodeLogin() {
        return this.templateCodeLogin;
    }

    @Generated
    public String getTemplateCodeResetPassword() {
        return this.templateCodeResetPassword;
    }

    @Generated
    public int getCodeLength() {
        return this.codeLength;
    }

    @Generated
    public int getCodeExpireSec() {
        return this.codeExpireSec;
    }

    @Generated
    public int getSendIntervalSec() {
        return this.sendIntervalSec;
    }

    @Generated
    public int getPhoneDailyLimit() {
        return this.phoneDailyLimit;
    }

    @Generated
    public int getIpHourlyLimit() {
        return this.ipHourlyLimit;
    }

    @Generated
    public int getIpDailyLimit() {
        return this.ipDailyLimit;
    }

    @Generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    @Generated
    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    @Generated
    public void setSignName(String signName) {
        this.signName = signName;
    }

    @Generated
    public void setTemplateCodeRegister(String templateCodeRegister) {
        this.templateCodeRegister = templateCodeRegister;
    }

    @Generated
    public void setTemplateCodeLogin(String templateCodeLogin) {
        this.templateCodeLogin = templateCodeLogin;
    }

    @Generated
    public void setTemplateCodeResetPassword(String templateCodeResetPassword) {
        this.templateCodeResetPassword = templateCodeResetPassword;
    }

    @Generated
    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    @Generated
    public void setCodeExpireSec(int codeExpireSec) {
        this.codeExpireSec = codeExpireSec;
    }

    @Generated
    public void setSendIntervalSec(int sendIntervalSec) {
        this.sendIntervalSec = sendIntervalSec;
    }

    @Generated
    public void setPhoneDailyLimit(int phoneDailyLimit) {
        this.phoneDailyLimit = phoneDailyLimit;
    }

    @Generated
    public void setIpHourlyLimit(int ipHourlyLimit) {
        this.ipHourlyLimit = ipHourlyLimit;
    }

    @Generated
    public void setIpDailyLimit(int ipDailyLimit) {
        this.ipDailyLimit = ipDailyLimit;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SmsProperties)) {
            return false;
        }
        SmsProperties other = (SmsProperties)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.isEnabled() != other.isEnabled()) {
            return false;
        }
        if (this.getCodeLength() != other.getCodeLength()) {
            return false;
        }
        if (this.getCodeExpireSec() != other.getCodeExpireSec()) {
            return false;
        }
        if (this.getSendIntervalSec() != other.getSendIntervalSec()) {
            return false;
        }
        if (this.getPhoneDailyLimit() != other.getPhoneDailyLimit()) {
            return false;
        }
        if (this.getIpHourlyLimit() != other.getIpHourlyLimit()) {
            return false;
        }
        if (this.getIpDailyLimit() != other.getIpDailyLimit()) {
            return false;
        }
        String this$accessKeyId = this.getAccessKeyId();
        String other$accessKeyId = other.getAccessKeyId();
        if (this$accessKeyId == null ? other$accessKeyId != null : !this$accessKeyId.equals(other$accessKeyId)) {
            return false;
        }
        String this$accessKeySecret = this.getAccessKeySecret();
        String other$accessKeySecret = other.getAccessKeySecret();
        if (this$accessKeySecret == null ? other$accessKeySecret != null : !this$accessKeySecret.equals(other$accessKeySecret)) {
            return false;
        }
        String this$signName = this.getSignName();
        String other$signName = other.getSignName();
        if (this$signName == null ? other$signName != null : !this$signName.equals(other$signName)) {
            return false;
        }
        String this$templateCodeRegister = this.getTemplateCodeRegister();
        String other$templateCodeRegister = other.getTemplateCodeRegister();
        if (this$templateCodeRegister == null ? other$templateCodeRegister != null : !this$templateCodeRegister.equals(other$templateCodeRegister)) {
            return false;
        }
        String this$templateCodeLogin = this.getTemplateCodeLogin();
        String other$templateCodeLogin = other.getTemplateCodeLogin();
        if (this$templateCodeLogin == null ? other$templateCodeLogin != null : !this$templateCodeLogin.equals(other$templateCodeLogin)) {
            return false;
        }
        String this$templateCodeResetPassword = this.getTemplateCodeResetPassword();
        String other$templateCodeResetPassword = other.getTemplateCodeResetPassword();
        return !(this$templateCodeResetPassword == null ? other$templateCodeResetPassword != null : !this$templateCodeResetPassword.equals(other$templateCodeResetPassword));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SmsProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isEnabled() ? 79 : 97);
        result = result * 59 + this.getCodeLength();
        result = result * 59 + this.getCodeExpireSec();
        result = result * 59 + this.getSendIntervalSec();
        result = result * 59 + this.getPhoneDailyLimit();
        result = result * 59 + this.getIpHourlyLimit();
        result = result * 59 + this.getIpDailyLimit();
        String $accessKeyId = this.getAccessKeyId();
        result = result * 59 + ($accessKeyId == null ? 43 : $accessKeyId.hashCode());
        String $accessKeySecret = this.getAccessKeySecret();
        result = result * 59 + ($accessKeySecret == null ? 43 : $accessKeySecret.hashCode());
        String $signName = this.getSignName();
        result = result * 59 + ($signName == null ? 43 : $signName.hashCode());
        String $templateCodeRegister = this.getTemplateCodeRegister();
        result = result * 59 + ($templateCodeRegister == null ? 43 : $templateCodeRegister.hashCode());
        String $templateCodeLogin = this.getTemplateCodeLogin();
        result = result * 59 + ($templateCodeLogin == null ? 43 : $templateCodeLogin.hashCode());
        String $templateCodeResetPassword = this.getTemplateCodeResetPassword();
        result = result * 59 + ($templateCodeResetPassword == null ? 43 : $templateCodeResetPassword.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SmsProperties(enabled=" + this.isEnabled() + ", accessKeyId=" + this.getAccessKeyId() + ", accessKeySecret=" + this.getAccessKeySecret() + ", signName=" + this.getSignName() + ", templateCodeRegister=" + this.getTemplateCodeRegister() + ", templateCodeLogin=" + this.getTemplateCodeLogin() + ", templateCodeResetPassword=" + this.getTemplateCodeResetPassword() + ", codeLength=" + this.getCodeLength() + ", codeExpireSec=" + this.getCodeExpireSec() + ", sendIntervalSec=" + this.getSendIntervalSec() + ", phoneDailyLimit=" + this.getPhoneDailyLimit() + ", ipHourlyLimit=" + this.getIpHourlyLimit() + ", ipDailyLimit=" + this.getIpDailyLimit() + ")";
    }
}

