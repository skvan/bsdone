/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.dto.TenantBrief
 *  com.bsball.model.entity.BaseEntity
 *  com.bsball.model.entity.SysUser
 *  com.fasterxml.jackson.annotation.JsonProperty
 *  com.fasterxml.jackson.annotation.JsonProperty$Access
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Table
 *  jakarta.persistence.Transient
 *  jakarta.validation.constraints.Size
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import com.bsball.model.dto.TenantBrief;
import com.bsball.model.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="sys_user")
@Comment(value="\u7cfb\u7edf\u7528\u6237")
public class SysUser
extends BaseEntity {
    @Comment(value="\u7528\u6237\u540d")
    private String username;
    @Comment(value="\u6635\u79f0")
    private String nickname;
    @Comment(value="\u771f\u5b9e\u59d3\u540d")
    private String realName;
    @Comment(value="\u5934\u50cf")
    private String avatar;
    @Comment(value="\u90ae\u7bb1")
    private String email;
    @Comment(value="\u90ae\u7bb1\u5df2\u9a8c\u8bc1")
    private Integer emailVerified = 0;
    @Comment(value="\u624b\u673a")
    private String phone;
    @Comment(value="\u624b\u673a\u53f7\u5df2\u9a8c\u8bc1")
    private Integer phoneVerified = 0;
    @Comment(value="\u63a5\u53d7\u670d\u52a1\u534f\u8bae\u65f6\u95f4")
    private LocalDateTime termsAcceptedAt;
    @Comment(value="\u63a5\u53d7\u9690\u79c1\u653f\u7b56\u65f6\u95f4")
    private LocalDateTime privacyAcceptedAt;
    @Comment(value="\u6ce8\u518c\u6765\u6e90")
    private String registerSource;
    @JsonProperty(access=JsonProperty.Access.WRITE_ONLY)
    @Comment(value="\u5bc6\u7801\uff08BCrypt \u5bc6\u6587\uff09")
    private String password;
    @Comment(value="\u6027\u522b")
    private Integer gender = 0;
    @Comment(value="\u751f\u65e5")
    private String birthDate;
    @Comment(value="\u72b6\u6001")
    private Integer status = 1;
    @Size(max=500)
    @Comment(value="\u8bf4\u660e/\u5907\u6ce8")
    private @Size(max=500) String description;
    @Transient
    private List<Long> roleIds;
    @JsonProperty
    @Transient
    private List<TenantBrief> tenants;
    @JsonProperty(access=JsonProperty.Access.READ_WRITE)
    @Transient
    private List<Long> tenantIds;

    @Generated
    public SysUser() {
    }

    @Generated
    public String getUsername() {
        return this.username;
    }

    @Generated
    public String getNickname() {
        return this.nickname;
    }

    @Generated
    public String getRealName() {
        return this.realName;
    }

    @Generated
    public String getAvatar() {
        return this.avatar;
    }

    @Generated
    public String getEmail() {
        return this.email;
    }

    @Generated
    public Integer getEmailVerified() {
        return this.emailVerified;
    }

    @Generated
    public String getPhone() {
        return this.phone;
    }

    @Generated
    public Integer getPhoneVerified() {
        return this.phoneVerified;
    }

    @Generated
    public LocalDateTime getTermsAcceptedAt() {
        return this.termsAcceptedAt;
    }

    @Generated
    public LocalDateTime getPrivacyAcceptedAt() {
        return this.privacyAcceptedAt;
    }

    @Generated
    public String getRegisterSource() {
        return this.registerSource;
    }

    @Generated
    public String getPassword() {
        return this.password;
    }

    @Generated
    public Integer getGender() {
        return this.gender;
    }

    @Generated
    public String getBirthDate() {
        return this.birthDate;
    }

    @Generated
    public Integer getStatus() {
        return this.status;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public List<Long> getRoleIds() {
        return this.roleIds;
    }

    @Generated
    public List<TenantBrief> getTenants() {
        return this.tenants;
    }

    @Generated
    public List<Long> getTenantIds() {
        return this.tenantIds;
    }

    @Generated
    public void setUsername(String username) {
        this.username = username;
    }

    @Generated
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Generated
    public void setRealName(String realName) {
        this.realName = realName;
    }

    @Generated
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Generated
    public void setEmail(String email) {
        this.email = email;
    }

    @Generated
    public void setEmailVerified(Integer emailVerified) {
        this.emailVerified = emailVerified;
    }

    @Generated
    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Generated
    public void setPhoneVerified(Integer phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    @Generated
    public void setTermsAcceptedAt(LocalDateTime termsAcceptedAt) {
        this.termsAcceptedAt = termsAcceptedAt;
    }

    @Generated
    public void setPrivacyAcceptedAt(LocalDateTime privacyAcceptedAt) {
        this.privacyAcceptedAt = privacyAcceptedAt;
    }

    @Generated
    public void setRegisterSource(String registerSource) {
        this.registerSource = registerSource;
    }

    @Generated
    public void setPassword(String password) {
        this.password = password;
    }

    @Generated
    public void setGender(Integer gender) {
        this.gender = gender;
    }

    @Generated
    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    @Generated
    public void setStatus(Integer status) {
        this.status = status;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }

    @Generated
    public void setTenants(List<TenantBrief> tenants) {
        this.tenants = tenants;
    }

    @Generated
    public void setTenantIds(List<Long> tenantIds) {
        this.tenantIds = tenantIds;
    }

    @Generated
    public String toString() {
        return "SysUser(username=" + this.getUsername() + ", nickname=" + this.getNickname() + ", realName=" + this.getRealName() + ", avatar=" + this.getAvatar() + ", email=" + this.getEmail() + ", emailVerified=" + this.getEmailVerified() + ", phone=" + this.getPhone() + ", phoneVerified=" + this.getPhoneVerified() + ", termsAcceptedAt=" + String.valueOf(this.getTermsAcceptedAt()) + ", privacyAcceptedAt=" + String.valueOf(this.getPrivacyAcceptedAt()) + ", registerSource=" + this.getRegisterSource() + ", password=" + this.getPassword() + ", gender=" + this.getGender() + ", birthDate=" + this.getBirthDate() + ", status=" + this.getStatus() + ", description=" + this.getDescription() + ", roleIds=" + String.valueOf(this.getRoleIds()) + ", tenants=" + String.valueOf(this.getTenants()) + ", tenantIds=" + String.valueOf(this.getTenantIds()) + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SysUser)) {
            return false;
        }
        SysUser other = (SysUser)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Integer this$emailVerified = this.getEmailVerified();
        Integer other$emailVerified = other.getEmailVerified();
        if (this$emailVerified == null ? other$emailVerified != null : !((Object)this$emailVerified).equals(other$emailVerified)) {
            return false;
        }
        Integer this$phoneVerified = this.getPhoneVerified();
        Integer other$phoneVerified = other.getPhoneVerified();
        if (this$phoneVerified == null ? other$phoneVerified != null : !((Object)this$phoneVerified).equals(other$phoneVerified)) {
            return false;
        }
        Integer this$gender = this.getGender();
        Integer other$gender = other.getGender();
        if (this$gender == null ? other$gender != null : !((Object)this$gender).equals(other$gender)) {
            return false;
        }
        Integer this$status = this.getStatus();
        Integer other$status = other.getStatus();
        if (this$status == null ? other$status != null : !((Object)this$status).equals(other$status)) {
            return false;
        }
        String this$username = this.getUsername();
        String other$username = other.getUsername();
        if (this$username == null ? other$username != null : !this$username.equals(other$username)) {
            return false;
        }
        String this$nickname = this.getNickname();
        String other$nickname = other.getNickname();
        if (this$nickname == null ? other$nickname != null : !this$nickname.equals(other$nickname)) {
            return false;
        }
        String this$realName = this.getRealName();
        String other$realName = other.getRealName();
        if (this$realName == null ? other$realName != null : !this$realName.equals(other$realName)) {
            return false;
        }
        String this$avatar = this.getAvatar();
        String other$avatar = other.getAvatar();
        if (this$avatar == null ? other$avatar != null : !this$avatar.equals(other$avatar)) {
            return false;
        }
        String this$email = this.getEmail();
        String other$email = other.getEmail();
        if (this$email == null ? other$email != null : !this$email.equals(other$email)) {
            return false;
        }
        String this$phone = this.getPhone();
        String other$phone = other.getPhone();
        if (this$phone == null ? other$phone != null : !this$phone.equals(other$phone)) {
            return false;
        }
        LocalDateTime this$termsAcceptedAt = this.getTermsAcceptedAt();
        LocalDateTime other$termsAcceptedAt = other.getTermsAcceptedAt();
        if (this$termsAcceptedAt == null ? other$termsAcceptedAt != null : !((Object)this$termsAcceptedAt).equals(other$termsAcceptedAt)) {
            return false;
        }
        LocalDateTime this$privacyAcceptedAt = this.getPrivacyAcceptedAt();
        LocalDateTime other$privacyAcceptedAt = other.getPrivacyAcceptedAt();
        if (this$privacyAcceptedAt == null ? other$privacyAcceptedAt != null : !((Object)this$privacyAcceptedAt).equals(other$privacyAcceptedAt)) {
            return false;
        }
        String this$registerSource = this.getRegisterSource();
        String other$registerSource = other.getRegisterSource();
        if (this$registerSource == null ? other$registerSource != null : !this$registerSource.equals(other$registerSource)) {
            return false;
        }
        String this$password = this.getPassword();
        String other$password = other.getPassword();
        if (this$password == null ? other$password != null : !this$password.equals(other$password)) {
            return false;
        }
        String this$birthDate = this.getBirthDate();
        String other$birthDate = other.getBirthDate();
        if (this$birthDate == null ? other$birthDate != null : !this$birthDate.equals(other$birthDate)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        List this$roleIds = this.getRoleIds();
        List other$roleIds = other.getRoleIds();
        if (this$roleIds == null ? other$roleIds != null : !((Object)this$roleIds).equals(other$roleIds)) {
            return false;
        }
        List this$tenants = this.getTenants();
        List other$tenants = other.getTenants();
        if (this$tenants == null ? other$tenants != null : !((Object)this$tenants).equals(other$tenants)) {
            return false;
        }
        List this$tenantIds = this.getTenantIds();
        List other$tenantIds = other.getTenantIds();
        return !(this$tenantIds == null ? other$tenantIds != null : !((Object)this$tenantIds).equals(other$tenantIds));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof SysUser;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Integer $emailVerified = this.getEmailVerified();
        result = result * 59 + ($emailVerified == null ? 43 : ((Object)$emailVerified).hashCode());
        Integer $phoneVerified = this.getPhoneVerified();
        result = result * 59 + ($phoneVerified == null ? 43 : ((Object)$phoneVerified).hashCode());
        Integer $gender = this.getGender();
        result = result * 59 + ($gender == null ? 43 : ((Object)$gender).hashCode());
        Integer $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : ((Object)$status).hashCode());
        String $username = this.getUsername();
        result = result * 59 + ($username == null ? 43 : $username.hashCode());
        String $nickname = this.getNickname();
        result = result * 59 + ($nickname == null ? 43 : $nickname.hashCode());
        String $realName = this.getRealName();
        result = result * 59 + ($realName == null ? 43 : $realName.hashCode());
        String $avatar = this.getAvatar();
        result = result * 59 + ($avatar == null ? 43 : $avatar.hashCode());
        String $email = this.getEmail();
        result = result * 59 + ($email == null ? 43 : $email.hashCode());
        String $phone = this.getPhone();
        result = result * 59 + ($phone == null ? 43 : $phone.hashCode());
        LocalDateTime $termsAcceptedAt = this.getTermsAcceptedAt();
        result = result * 59 + ($termsAcceptedAt == null ? 43 : ((Object)$termsAcceptedAt).hashCode());
        LocalDateTime $privacyAcceptedAt = this.getPrivacyAcceptedAt();
        result = result * 59 + ($privacyAcceptedAt == null ? 43 : ((Object)$privacyAcceptedAt).hashCode());
        String $registerSource = this.getRegisterSource();
        result = result * 59 + ($registerSource == null ? 43 : $registerSource.hashCode());
        String $password = this.getPassword();
        result = result * 59 + ($password == null ? 43 : $password.hashCode());
        String $birthDate = this.getBirthDate();
        result = result * 59 + ($birthDate == null ? 43 : $birthDate.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        List $roleIds = this.getRoleIds();
        result = result * 59 + ($roleIds == null ? 43 : ((Object)$roleIds).hashCode());
        List $tenants = this.getTenants();
        result = result * 59 + ($tenants == null ? 43 : ((Object)$tenants).hashCode());
        List $tenantIds = this.getTenantIds();
        result = result * 59 + ($tenantIds == null ? 43 : ((Object)$tenantIds).hashCode());
        return result;
    }
}

