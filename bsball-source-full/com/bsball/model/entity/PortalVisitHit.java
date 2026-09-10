/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PortalVisitHit
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Index
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.Table
 *  jakarta.persistence.Transient
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 */
package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="portal_visit_hit", indexes={@Index(name="idx_portal_visit_hit_date", columnList="hit_date"), @Index(name="idx_portal_visit_hit_date_vid", columnList="hit_date,visitor_id"), @Index(name="idx_portal_visit_hit_tenant_date", columnList="tenant_id,hit_date")})
@Comment(value="\u95e8\u6237\u8bbf\u95ee\u6253\u70b9")
public class PortalVisitHit {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Comment(value="\u4e3b\u952eID")
    private Long id;
    @Column(name="tenant_id", nullable=false)
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Column(name="hit_date", nullable=false)
    @Comment(value="\u7edf\u8ba1\u65e5\uff08\u6309\u670d\u52a1\u5668\u65f6\u533a\uff09")
    private LocalDate hitDate;
    @Column(name="visitor_id", nullable=false, length=64)
    @Comment(value="\u8bbf\u5ba2\u533f\u540d ID\uff08Cookie\uff09")
    private String visitorId;
    @Column(length=512)
    @Comment(value="\u524d\u7aef\u8def\u7531\u8def\u5f84\uff08\u4e0d\u542b query/hash\uff1b\u8d85\u957f\u622a\u65ad\uff09")
    private String path;
    @Column(name="user_agent", length=512)
    @Comment(value="\u5ba2\u6237\u7aef User-Agent")
    private String userAgent;
    @Column(length=128)
    @Comment(value="\u5ba2\u6237\u7aef IP\uff08\u8131\u654f/\u622a\u65ad\uff09\uff1b\u7701\u7ea7\u5f52\u5c5e\u89c1 ip_location_cache \u540c ip")
    private String ip;
    @Transient
    private String ipRegion;
    @Column(name="created_at", nullable=false)
    @Comment(value="\u8bb0\u5f55\u65f6\u95f4")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.hitDate == null) {
            this.hitDate = now.toLocalDate();
        }
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
    public LocalDate getHitDate() {
        return this.hitDate;
    }

    @Generated
    public String getVisitorId() {
        return this.visitorId;
    }

    @Generated
    public String getPath() {
        return this.path;
    }

    @Generated
    public String getUserAgent() {
        return this.userAgent;
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
    public void setHitDate(LocalDate hitDate) {
        this.hitDate = hitDate;
    }

    @Generated
    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    @Generated
    public void setPath(String path) {
        this.path = path;
    }

    @Generated
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
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
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

