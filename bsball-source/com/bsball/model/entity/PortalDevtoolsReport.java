/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.PortalDevtoolsReport
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
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;

@Entity
@Table(name="portal_devtools_report", indexes={@Index(name="idx_portal_devtools_report_tenant_created", columnList="tenant_id,created_at")})
@Comment(value="\u95e8\u6237\u5f00\u53d1\u8005\u5de5\u5177\u6253\u5f00\u4e0a\u62a5")
public class PortalDevtoolsReport {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="tenant_id", nullable=false)
    @Comment(value="\u79df\u6237ID")
    private Long tenantId;
    @Column(name="visitor_id", nullable=false, length=64)
    @Comment(value="\u8bbf\u5ba2\u533f\u540d ID\uff08Cookie\uff09")
    private String visitorId;
    @Column(length=128)
    @Comment(value="\u5ba2\u6237\u7aef IP\uff08\u670d\u52a1\u7aef\u89e3\u6790\uff09")
    private String ip;
    @Transient
    private String ipRegion;
    @Column(length=512)
    @Comment(value="\u5e94\u7528\u5185\u8def\u5f84\uff08\u4e0e PV \u4e00\u81f4\uff1a\u5df2\u53bb\u6389 query/hash\uff09")
    private String path;
    @Column(name="route_name", length=256)
    @Comment(value="Vue Router \u8def\u7531 name")
    private String routeName;
    @Column(name="user_agent", length=512)
    @Comment(value="User-Agent")
    private String userAgent;
    @Column(name="client_meta", columnDefinition="TEXT")
    @Comment(value="JSON\uff1a\u5c4f\u5e55\u3001\u8bed\u8a00\u3001\u65f6\u533a\u7b49")
    private String clientMeta;
    @Column(name="created_at", nullable=false)
    @Comment(value="\u8bb0\u5f55\u65f6\u95f4")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
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
    public String getVisitorId() {
        return this.visitorId;
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
    public String getPath() {
        return this.path;
    }

    @Generated
    public String getRouteName() {
        return this.routeName;
    }

    @Generated
    public String getUserAgent() {
        return this.userAgent;
    }

    @Generated
    public String getClientMeta() {
        return this.clientMeta;
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
    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
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
    public void setPath(String path) {
        this.path = path;
    }

    @Generated
    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    @Generated
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Generated
    public void setClientMeta(String clientMeta) {
        this.clientMeta = clientMeta;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

