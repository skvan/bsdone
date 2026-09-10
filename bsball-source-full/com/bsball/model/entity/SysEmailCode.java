/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysEmailCode
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_email_code")
@Comment(value="\u90ae\u7bb1\u9a8c\u8bc1\u7801")
public class SysEmailCode
extends BaseEntity {
    public static final String SCENE_BIND_EMAIL = "bind_email";
    public static final String SCENE_RESET_PASSWORD = "reset_password";
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Comment(value="\u90ae\u7bb1")
    private String email;
    @Comment(value="\u9a8c\u8bc1\u7801")
    private String code;
    @Comment(value="\u573a\u666f")
    private String scene;
    @Comment(value="\u5ba2\u6237\u7aefIP")
    private String clientIp;
    @Comment(value="\u8fc7\u671f\u65f6\u95f4")
    private LocalDateTime expiresAt;
    @Comment(value="\u4f7f\u7528\u65f6\u95f4")
    private LocalDateTime usedAt;

    @Generated
    public SysEmailCode() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public String getEmail() {
        return this.email;
    }

    @Generated
    public String getCode() {
        return this.code;
    }

    @Generated
    public String getScene() {
        return this.scene;
    }

    @Generated
    public String getClientIp() {
        return this.clientIp;
    }

    @Generated
    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    @Generated
    public LocalDateTime getUsedAt() {
        return this.usedAt;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setEmail(String email) {
        this.email = email;
    }

    @Generated
    public void setCode(String code) {
        this.code = code;
    }

    @Generated
    public void setScene(String scene) {
        this.scene = scene;
    }

    @Generated
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    @Generated
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Generated
    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    @Generated
    public String toString() {
        return "SysEmailCode(tenantId=" + this.getTenantId() + ", email=" + this.getEmail() + ", code=" + this.getCode() + ", scene=" + this.getScene() + ", clientIp=" + this.getClientIp() + ", expiresAt=" + String.valueOf(this.getExpiresAt()) + ", usedAt=" + String.valueOf(this.getUsedAt()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysEmailCode)) {
            return false;
        }
        SysEmailCode other = (SysEmailCode)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$code = this.getCode();
        String other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
            return false;
        }
        String this$scene = this.getScene();
        String other$scene = other.getScene();
        if (this$scene == null ? other$scene != null : !this$scene.equals(other$scene)) {
            return false;
        }
        String this$clientIp = this.getClientIp();
        String other$clientIp = other.getClientIp();
        if (this$clientIp == null ? other$clientIp != null : !this$clientIp.equals(other$clientIp)) {
            return false;
        }
        LocalDateTime this$expiresAt = this.getExpiresAt();
        LocalDateTime other$expiresAt = other.getExpiresAt();
        if (this$expiresAt == null ? other$expiresAt != null : !((Object)this$expiresAt).equals(other$expiresAt)) {
            return false;
        }
        LocalDateTime this$usedAt = this.getUsedAt();
        LocalDateTime other$usedAt = other.getUsedAt();
        return !(this$usedAt == null ? other$usedAt != null : !((Object)this$usedAt).equals(other$usedAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysEmailCode;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $code = this.getCode();
        result = result * 59 + ($code == null ? 43 : $code.hashCode());
        String $scene = this.getScene();
        result = result * 59 + ($scene == null ? 43 : $scene.hashCode());
        String $clientIp = this.getClientIp();
        result = result * 59 + ($clientIp == null ? 43 : $clientIp.hashCode());
        LocalDateTime $expiresAt = this.getExpiresAt();
        result = result * 59 + ($expiresAt == null ? 43 : ((Object)$expiresAt).hashCode());
        LocalDateTime $usedAt = this.getUsedAt();
        result = result * 59 + ($usedAt == null ? 43 : ((Object)$usedAt).hashCode());
        return result;
    }
}

