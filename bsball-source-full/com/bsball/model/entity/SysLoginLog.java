/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysLoginLog
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_login_log")
@Comment(value="\u767b\u5f55\u65e5\u5fd7")
public class SysLoginLog {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Comment(value="\u4e3b\u952eID")
    private Long id;
    @Comment(value="\u79df\u6237")
    private Long tenantId;
    @Comment(value="\u7528\u6237ID")
    private Long userId;
    @Comment(value="\u7528\u6237\u540d")
    private String username;
    @Comment(value="\u767b\u5f55\u65f6\u95f4")
    private LocalDateTime loginTime;
    @Comment(value="IP")
    private String ip;
    @Column(length=500)
    @Comment(value="\u8bbe\u5907\u4fe1\u606f")
    private String deviceInfo;
    @Comment(value="\u4f4d\u7f6e")
    private String location;
    @Comment(value="\u72b6\u6001")
    private String status = "success";
    @Comment(value="\u5931\u8d25\u539f\u56e0")
    private String failReason;
    @Comment(value="Token\u524d\u7f00")
    private String tokenPrefix;
    @Comment(value="\u521b\u5efa\u65f6\u95f4")
    private LocalDateTime createdAt;

    @Generated
    public SysLoginLog() {
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
    public String getUsername() {
        return this.username;
    }

    @Generated
    public LocalDateTime getLoginTime() {
        return this.loginTime;
    }

    @Generated
    public String getIp() {
        return this.ip;
    }

    @Generated
    public String getDeviceInfo() {
        return this.deviceInfo;
    }

    @Generated
    public String getLocation() {
        return this.location;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public String getFailReason() {
        return this.failReason;
    }

    @Generated
    public String getTokenPrefix() {
        return this.tokenPrefix;
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
    public void setUsername(String username) {
        this.username = username;
    }

    @Generated
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    @Generated
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Generated
    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    @Generated
    public void setLocation(String location) {
        this.location = location;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    @Generated
    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysLoginLog)) {
            return false;
        }
        SysLoginLog other = (SysLoginLog)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$id = this.getId();
        Long other$id = other.getId();
        if (this$id == null ? other$id != null : !((Object)this$id).equals(other$id)) {
            return false;
        }
        Long this$tenantId = this.getTenantId();
        Long other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !((Object)this$tenantId).equals(other$tenantId)) {
            return false;
        }
        Long this$userId = this.getUserId();
        Long other$userId = other.getUserId();
        if (this$userId == null ? other$userId != null : !((Object)this$userId).equals(other$userId)) {
            return false;
        }
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        LocalDateTime this$loginTime = this.getLoginTime();
        LocalDateTime other$loginTime = other.getLoginTime();
        if (this$loginTime == null ? other$loginTime != null : !((Object)this$loginTime).equals(other$loginTime)) {
            return false;
        }
        String this$ip = this.getIp();
        String other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !this$ip.equals(other$ip)) {
            return false;
        }
        String this$deviceInfo = this.getDeviceInfo();
        String other$deviceInfo = other.getDeviceInfo();
        if (this$deviceInfo == null ? other$deviceInfo != null : !this$deviceInfo.equals(other$deviceInfo)) {
            return false;
        }
        String this$location = this.getLocation();
        String other$location = other.getLocation();
        if (this$location == null ? other$location != null : !this$location.equals(other$location)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$failReason = this.getFailReason();
        String other$failReason = other.getFailReason();
        if (this$failReason == null ? other$failReason != null : !this$failReason.equals(other$failReason)) {
            return false;
        }
        String this$tokenPrefix = this.getTokenPrefix();
        String other$tokenPrefix = other.getTokenPrefix();
        if (this$tokenPrefix == null ? other$tokenPrefix != null : !this$tokenPrefix.equals(other$tokenPrefix)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysLoginLog;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $id = this.getId();
        result = result * 59 + ($id == null ? 43 : ((Object)$id).hashCode());
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $userId = this.getUserId();
        result = result * 59 + ($userId == null ? 43 : ((Object)$userId).hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        LocalDateTime $loginTime = this.getLoginTime();
        result = result * 59 + ($loginTime == null ? 43 : ((Object)$loginTime).hashCode());
        String $ip = this.getIp();
        result = result * 59 + ($ip == null ? 43 : $ip.hashCode());
        String $deviceInfo = this.getDeviceInfo();
        result = result * 59 + ($deviceInfo == null ? 43 : $deviceInfo.hashCode());
        String $location = this.getLocation();
        result = result * 59 + ($location == null ? 43 : $location.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $failReason = this.getFailReason();
        result = result * 59 + ($failReason == null ? 43 : $failReason.hashCode());
        String $tokenPrefix = this.getTokenPrefix();
        result = result * 59 + ($tokenPrefix == null ? 43 : $tokenPrefix.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SysLoginLog(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", loginTime=" + String.valueOf(this.getLoginTime()) + ", ip=" + this.getIp() + ", deviceInfo=" + this.getDeviceInfo() + ", location=" + this.getLocation() + ", status=" + this.getStatus() + ", failReason=" + this.getFailReason() + ", tokenPrefix=" + this.getTokenPrefix() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }
}

