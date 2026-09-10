/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.SysOperationLog
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 *  jakarta.persistence.Transient
 *  jakarta.validation.constraints.Size
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
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_operation_log")
@Comment(value="\u64cd\u4f5c\u65e5\u5fd7")
public class SysOperationLog {
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
    @Comment(value="\u6a21\u5757")
    private String module;
    @Comment(value="\u64cd\u4f5c")
    private String action;
    @Comment(value="\u76ee\u6807\u7c7b\u578b")
    private String targetType;
    @Comment(value="\u76ee\u6807ID")
    private Long targetId;
    @Column(length=2000)
    @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u63cf\u8ff0")
    private @Size(max=2000, message="\u63cf\u8ff0\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String description;
    @Comment(value="IP")
    private String ip;
    @Transient
    private String ipRegion;
    @Column(length=2000)
    @Size(max=2000, message="\u6269\u5c55\u4fe1\u606f\u4e0d\u80fd\u8d85\u8fc72000\u5b57")
    @Comment(value="\u6269\u5c55\u4fe1\u606f")
    private @Size(max=2000, message="\u6269\u5c55\u4fe1\u606f\u4e0d\u80fd\u8d85\u8fc72000\u5b57") String extra;
    @Comment(value="\u521b\u5efa\u65f6\u95f4")
    private LocalDateTime createdAt;

    @Generated
    public SysOperationLog() {
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
    public String getModule() {
        return this.module;
    }

    @Generated
    public String getAction() {
        return this.action;
    }

    @Generated
    public String getTargetType() {
        return this.targetType;
    }

    @Generated
    public Long getTargetId() {
        return this.targetId;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public String getIp() {
        return this.ip;
    }

    @Generated
    public String getIpRegion() {
        return this.ipRegion;
    }

    @Generated
    public String getExtra() {
        return this.extra;
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
    public void setModule(String module) {
        this.module = module;
    }

    @Generated
    public void setAction(String action) {
        this.action = action;
    }

    @Generated
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    @Generated
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setIp(String ip) {
        this.ip = ip;
    }

    @Generated
    public void setIpRegion(String ipRegion) {
        this.ipRegion = ipRegion;
    }

    @Generated
    public void setExtra(String extra) {
        this.extra = extra;
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
        if (!(o instanceof SysOperationLog)) {
            return false;
        }
        SysOperationLog other = (SysOperationLog)o;
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
        Long this$targetId = this.getTargetId();
        Long other$targetId = other.getTargetId();
        if (this$targetId == null ? other$targetId != null : !((Object)this$targetId).equals(other$targetId)) {
            return false;
        }
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        String this$module = this.getModule();
        String other$module = other.getModule();
        if (this$module == null ? other$module != null : !this$module.equals(other$module)) {
            return false;
        }
        String this$action = this.getAction();
        String other$action = other.getAction();
        if (this$action == null ? other$action != null : !this$action.equals(other$action)) {
            return false;
        }
        String this$targetType = this.getTargetType();
        String other$targetType = other.getTargetType();
        if (this$targetType == null ? other$targetType != null : !this$targetType.equals(other$targetType)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$ip = this.getIp();
        String other$ip = other.getIp();
        if (this$ip == null ? other$ip != null : !this$ip.equals(other$ip)) {
            return false;
        }
        String this$ipRegion = this.getIpRegion();
        String other$ipRegion = other.getIpRegion();
        if (this$ipRegion == null ? other$ipRegion != null : !this$ipRegion.equals(other$ipRegion)) {
            return false;
        }
        String this$extra = this.getExtra();
        String other$extra = other.getExtra();
        if (this$extra == null ? other$extra != null : !this$extra.equals(other$extra)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysOperationLog;
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
        Long $targetId = this.getTargetId();
        result = result * 59 + ($targetId == null ? 43 : ((Object)$targetId).hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        String $module = this.getModule();
        result = result * 59 + ($module == null ? 43 : $module.hashCode());
        String $action = this.getAction();
        result = result * 59 + ($action == null ? 43 : $action.hashCode());
        String $targetType = this.getTargetType();
        result = result * 59 + ($targetType == null ? 43 : $targetType.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $ip = this.getIp();
        result = result * 59 + ($ip == null ? 43 : $ip.hashCode());
        String $ipRegion = this.getIpRegion();
        result = result * 59 + ($ipRegion == null ? 43 : $ipRegion.hashCode());
        String $extra = this.getExtra();
        result = result * 59 + ($extra == null ? 43 : $extra.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    @Generated
    public String toString() {
        return "SysOperationLog(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", userId=" + this.getUserId() + ", username=" + this.getUsername() + ", module=" + this.getModule() + ", action=" + this.getAction() + ", targetType=" + this.getTargetType() + ", targetId=" + this.getTargetId() + ", description=" + this.getDescription() + ", ip=" + this.getIp() + ", ipRegion=" + this.getIpRegion() + ", extra=" + this.getExtra() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }
}

