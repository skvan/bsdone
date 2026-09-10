/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.EmailProperties
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.stereotype.Component
 */
package com.bsball.config;

import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="app.email")
public class EmailProperties {
    private boolean enabled = false;
    private String host = "";
    private int port = 465;
    private String username = "";
    private String password = "";
    private String from = "";
    private String fromName = "\u7403\u76df";
    private boolean useTls = true;
    private int codeLength = 6;
    private int codeExpireSec = 600;
    private int sendIntervalSec = 60;
    private int dailyLimit = 15;

    @Generated
    public EmailProperties() {
    }

    @Generated
    public boolean isEnabled() {
        return this.enabled;
    }

    @Generated
    public String getHost() {
        return this.host;
    }

    @Generated
    public int getPort() {
        return this.port;
    }

    @Generated
    public String getUsername() {
        return this.username;
    }

    @Generated
    public String getPassword() {
        return this.password;
    }

    @Generated
    public String getFrom() {
        return this.from;
    }

    @Generated
    public String getFromName() {
        return this.fromName;
    }

    @Generated
    public boolean isUseTls() {
        return this.useTls;
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
    public int getDailyLimit() {
        return this.dailyLimit;
    }

    @Generated
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Generated
    public void setHost(String host) {
        this.host = host;
    }

    @Generated
    public void setPort(int port) {
        this.port = port;
    }

    @Generated
    public void setUsername(String username) {
        this.username = username;
    }

    @Generated
    public void setPassword(String password) {
        this.password = password;
    }

    @Generated
    public void setFrom(String from) {
        this.from = from;
    }

    @Generated
    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    @Generated
    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
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
    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof EmailProperties)) {
            return false;
        }
        EmailProperties other = (EmailProperties)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.isEnabled() != other.isEnabled()) {
            return false;
        }
        if (this.getPort() != other.getPort()) {
            return false;
        }
        if (this.isUseTls() != other.isUseTls()) {
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
        if (this.getDailyLimit() != other.getDailyLimit()) {
            return false;
        }
        String this$host = this.getHost();
        String other$host = other.getHost();
        if (this$host == null ? other$host != null : !this$host.equals(other$host)) {
            return false;
        }
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        String this$password = this.getPassword();
        String other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) {
            return false;
        }
        String this$from = this.getFrom();
        String other$from = other.getFrom();
        if (this$from == null ? other$from != null : !this$from.equals(other$from)) {
            return false;
        }
        String this$fromName = this.getFromName();
        String other$fromName = other.getFromName();
        return !(this$fromName == null ? other$fromName != null : !this$fromName.equals(other$fromName));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof EmailProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isEnabled() ? 79 : 97);
        result = result * 59 + this.getPort();
        result = result * 59 + (this.isUseTls() ? 79 : 97);
        result = result * 59 + this.getCodeLength();
        result = result * 59 + this.getCodeExpireSec();
        result = result * 59 + this.getSendIntervalSec();
        result = result * 59 + this.getDailyLimit();
        String $host = this.getHost();
        result = result * 59 + ($host == null ? 43 : $host.hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        String $password = this.getPassword();
        result = result * 59 + ($password == null ? 43 : $password.hashCode());
        String $from = this.getFrom();
        result = result * 59 + ($from == null ? 43 : $from.hashCode());
        String $fromName = this.getFromName();
        result = result * 59 + ($fromName == null ? 43 : $fromName.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "EmailProperties(enabled=" + this.isEnabled() + ", host=" + this.getHost() + ", port=" + this.getPort() + ", username=" + this.getUsername() + ", password=" + this.getPassword() + ", from=" + this.getFrom() + ", fromName=" + this.getFromName() + ", useTls=" + this.isUseTls() + ", codeLength=" + this.getCodeLength() + ", codeExpireSec=" + this.getCodeExpireSec() + ", sendIntervalSec=" + this.getSendIntervalSec() + ", dailyLimit=" + this.getDailyLimit() + ")";
    }
}

