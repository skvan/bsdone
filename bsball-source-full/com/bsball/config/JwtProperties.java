/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.JwtProperties
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 */
package com.bsball.config;

import java.time.Duration;
import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.jwt")
public class JwtProperties {
    private String secret = "";
    private Duration expiration = Duration.ofDays(7L);
    private String issuer = "bs-ball";
    private String audience = "";

    @Generated
    public JwtProperties() {
    }

    @Generated
    public String getSecret() {
        return this.secret;
    }

    @Generated
    public Duration getExpiration() {
        return this.expiration;
    }

    @Generated
    public String getIssuer() {
        return this.issuer;
    }

    @Generated
    public String getAudience() {
        return this.audience;
    }

    @Generated
    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Generated
    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    @Generated
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    @Generated
    public void setAudience(String audience) {
        this.audience = audience;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof JwtProperties)) {
            return false;
        }
        JwtProperties other = (JwtProperties)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$secret = this.getSecret();
        String other$secret = other.getSecret();
        if (this$secret == null ? other$secret != null : !this$secret.equals(other$secret)) {
            return false;
        }
        Duration this$expiration = this.getExpiration();
        Duration other$expiration = other.getExpiration();
        if (this$expiration == null ? other$expiration != null : !((Object)this$expiration).equals(other$expiration)) {
            return false;
        }
        String this$issuer = this.getIssuer();
        String other$issuer = other.getIssuer();
        if (this$issuer == null ? other$issuer != null : !this$issuer.equals(other$issuer)) {
            return false;
        }
        String this$audience = this.getAudience();
        String other$audience = other.getAudience();
        return !(this$audience == null ? other$audience != null : !this$audience.equals(other$audience));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof JwtProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $secret = this.getSecret();
        result = result * 59 + ($secret == null ? 43 : $secret.hashCode());
        Duration $expiration = this.getExpiration();
        result = result * 59 + ($expiration == null ? 43 : ((Object)$expiration).hashCode());
        String $issuer = this.getIssuer();
        result = result * 59 + ($issuer == null ? 43 : $issuer.hashCode());
        String $audience = this.getAudience();
        result = result * 59 + ($audience == null ? 43 : $audience.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "JwtProperties(secret=" + this.getSecret() + ", expiration=" + String.valueOf(this.getExpiration()) + ", issuer=" + this.getIssuer() + ", audience=" + this.getAudience() + ")";
    }
}

