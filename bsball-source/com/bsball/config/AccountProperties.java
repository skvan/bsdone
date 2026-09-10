/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.AccountProperties
 *  lombok.Generated
 *  org.springframework.boot.context.properties.ConfigurationProperties
 *  org.springframework.stereotype.Component
 */
package com.bsball.config;

import lombok.Generated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="app.account")
public class AccountProperties {
    private boolean registerEnabled = true;
    private String defaultRoleCode = "member";
    private int inviteDefaultExpireHours = 72;
    private int inviteDefaultMaxUses = 1;

    @Generated
    public AccountProperties() {
    }

    @Generated
    public boolean isRegisterEnabled() {
        return this.registerEnabled;
    }

    @Generated
    public String getDefaultRoleCode() {
        return this.defaultRoleCode;
    }

    @Generated
    public int getInviteDefaultExpireHours() {
        return this.inviteDefaultExpireHours;
    }

    @Generated
    public int getInviteDefaultMaxUses() {
        return this.inviteDefaultMaxUses;
    }

    @Generated
    public void setRegisterEnabled(boolean registerEnabled) {
        this.registerEnabled = registerEnabled;
    }

    @Generated
    public void setDefaultRoleCode(String defaultRoleCode) {
        this.defaultRoleCode = defaultRoleCode;
    }

    @Generated
    public void setInviteDefaultExpireHours(int inviteDefaultExpireHours) {
        this.inviteDefaultExpireHours = inviteDefaultExpireHours;
    }

    @Generated
    public void setInviteDefaultMaxUses(int inviteDefaultMaxUses) {
        this.inviteDefaultMaxUses = inviteDefaultMaxUses;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof AccountProperties)) {
            return false;
        }
        AccountProperties other = (AccountProperties)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.isRegisterEnabled() != other.isRegisterEnabled()) {
            return false;
        }
        if (this.getInviteDefaultExpireHours() != other.getInviteDefaultExpireHours()) {
            return false;
        }
        if (this.getInviteDefaultMaxUses() != other.getInviteDefaultMaxUses()) {
            return false;
        }
        String this$defaultRoleCode = this.getDefaultRoleCode();
        String other$defaultRoleCode = other.getDefaultRoleCode();
        return !(this$defaultRoleCode == null ? other$defaultRoleCode != null : !this$defaultRoleCode.equals(other$defaultRoleCode));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof AccountProperties;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + (this.isRegisterEnabled() ? 79 : 97);
        result = result * 59 + this.getInviteDefaultExpireHours();
        result = result * 59 + this.getInviteDefaultMaxUses();
        String $defaultRoleCode = this.getDefaultRoleCode();
        result = result * 59 + ($defaultRoleCode == null ? 43 : $defaultRoleCode.hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "AccountProperties(registerEnabled=" + this.isRegisterEnabled() + ", defaultRoleCode=" + this.getDefaultRoleCode() + ", inviteDefaultExpireHours=" + this.getInviteDefaultExpireHours() + ", inviteDefaultMaxUses=" + this.getInviteDefaultMaxUses() + ")";
    }
}

