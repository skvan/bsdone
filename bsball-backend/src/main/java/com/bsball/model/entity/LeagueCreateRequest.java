package com.bsball.model.entity;

import com.bsball.model.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="bs_league_create_request")
@Comment(value="联盟创建申请（门户自助，默认需平台审核）")
public class LeagueCreateRequest
extends BaseEntity {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    @Column(nullable = false)
    @Comment(value="租户ID")
    private Long tenantId;
    @Column(nullable = false)
    @Comment(value="申请人用户ID")
    private Long applicantUserId;
    @Column(nullable = false, length = 200)
    @Comment(value="联盟名称")
    private String name;
    @Column(length = 200)
    @Comment(value="联盟英文名")
    private String nameEn;
    @Column(length = 2000)
    @Comment(value="联盟简介")
    private String description;
    @Column(nullable = false, length = 16)
    @Comment(value="状态: pending | approved | rejected")
    private String status = STATUS_PENDING;
    @Column
    @Comment(value="审核通过后生成的联盟ID")
    private Long leagueId;
    @Column
    @Comment(value="审核人用户ID")
    private Long reviewedBy;
    @Column
    @Comment(value="审核时间")
    private LocalDateTime reviewedAt;
    @Column(length = 500)
    @Comment(value="驳回原因")
    private String rejectReason;

    @Generated
    public LeagueCreateRequest() {
    }

    @Generated
    public Long getTenantId() {
        return this.tenantId;
    }

    @Generated
    public Long getApplicantUserId() {
        return this.applicantUserId;
    }

    @Generated
    public String getName() {
        return this.name;
    }

    @Generated
    public String getNameEn() {
        return this.nameEn;
    }

    @Generated
    public String getDescription() {
        return this.description;
    }

    @Generated
    public String getStatus() {
        return this.status;
    }

    @Generated
    public Long getLeagueId() {
        return this.leagueId;
    }

    @Generated
    public Long getReviewedBy() {
        return this.reviewedBy;
    }

    @Generated
    public LocalDateTime getReviewedAt() {
        return this.reviewedAt;
    }

    @Generated
    public String getRejectReason() {
        return this.rejectReason;
    }

    @Generated
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    @Generated
    public void setApplicantUserId(Long applicantUserId) {
        this.applicantUserId = applicantUserId;
    }

    @Generated
    public void setName(String name) {
        this.name = name;
    }

    @Generated
    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    @Generated
    public void setDescription(String description) {
        this.description = description;
    }

    @Generated
    public void setStatus(String status) {
        this.status = status;
    }

    @Generated
    public void setLeagueId(Long leagueId) {
        this.leagueId = leagueId;
    }

    @Generated
    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    @Generated
    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    @Generated
    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    @Generated
    public String toString() {
        return "LeagueCreateRequest(tenantId=" + this.getTenantId() + ", applicantUserId=" + this.getApplicantUserId() + ", name=" + this.getName() + ", nameEn=" + this.getNameEn() + ", description=" + this.getDescription() + ", status=" + this.getStatus() + ", leagueId=" + this.getLeagueId() + ", reviewedBy=" + this.getReviewedBy() + ", reviewedAt=" + String.valueOf(this.getReviewedAt()) + ", rejectReason=" + this.getRejectReason() + ")";
    }

    @Generated
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof LeagueCreateRequest)) {
            return false;
        }
        LeagueCreateRequest other = (LeagueCreateRequest)o;
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
        Long this$applicantUserId = this.getApplicantUserId();
        Long other$applicantUserId = other.getApplicantUserId();
        if (this$applicantUserId == null ? other$applicantUserId != null : !((Object)this$applicantUserId).equals(other$applicantUserId)) {
            return false;
        }
        Long this$leagueId = this.getLeagueId();
        Long other$leagueId = other.getLeagueId();
        if (this$leagueId == null ? other$leagueId != null : !((Object)this$leagueId).equals(other$leagueId)) {
            return false;
        }
        Long this$reviewedBy = this.getReviewedBy();
        Long other$reviewedBy = other.getReviewedBy();
        if (this$reviewedBy == null ? other$reviewedBy != null : !((Object)this$reviewedBy).equals(other$reviewedBy)) {
            return false;
        }
        String this$name = this.getName();
        String other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
            return false;
        }
        String this$nameEn = this.getNameEn();
        String other$nameEn = other.getNameEn();
        if (this$nameEn == null ? other$nameEn != null : !this$nameEn.equals(other$nameEn)) {
            return false;
        }
        String this$description = this.getDescription();
        String other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        LocalDateTime this$reviewedAt = this.getReviewedAt();
        LocalDateTime other$reviewedAt = other.getReviewedAt();
        if (this$reviewedAt == null ? other$reviewedAt != null : !((Object)this$reviewedAt).equals(other$reviewedAt)) {
            return false;
        }
        String this$rejectReason = this.getRejectReason();
        String other$rejectReason = other.getRejectReason();
        return !(this$rejectReason == null ? other$rejectReason != null : !this$rejectReason.equals(other$rejectReason));
    }

    @Generated
    protected boolean canEqual(Object other) {
        return other instanceof LeagueCreateRequest;
    }

    @Generated
    public int hashCode() {
        int PRIME = 59;
        int result = super.hashCode();
        Long $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : ((Object)$tenantId).hashCode());
        Long $applicantUserId = this.getApplicantUserId();
        result = result * 59 + ($applicantUserId == null ? 43 : ((Object)$applicantUserId).hashCode());
        Long $leagueId = this.getLeagueId();
        result = result * 59 + ($leagueId == null ? 43 : ((Object)$leagueId).hashCode());
        Long $reviewedBy = this.getReviewedBy();
        result = result * 59 + ($reviewedBy == null ? 43 : ((Object)$reviewedBy).hashCode());
        String $name = this.getName();
        result = result * 59 + ($name == null ? 43 : $name.hashCode());
        String $nameEn = this.getNameEn();
        result = result * 59 + ($nameEn == null ? 43 : $nameEn.hashCode());
        String $description = this.getDescription();
        result = result * 59 + ($description == null ? 43 : $description.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        LocalDateTime $reviewedAt = this.getReviewedAt();
        result = result * 59 + ($reviewedAt == null ? 43 : ((Object)$reviewedAt).hashCode());
        String $rejectReason = this.getRejectReason();
        result = result * 59 + ($rejectReason == null ? 43 : $rejectReason.hashCode());
        return result;
    }
}
