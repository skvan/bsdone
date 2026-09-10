/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.entity.BaseEntity
 *  jakarta.persistence.Column
 *  jakarta.persistence.EntityListeners
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.MappedSuperclass
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  lombok.Generated
 *  org.hibernate.annotations.Comment
 *  org.springframework.data.annotation.CreatedBy
 *  org.springframework.data.annotation.LastModifiedBy
 *  org.springframework.data.jpa.domain.support.AuditingEntityListener
 */
package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Generated;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(value={AuditingEntityListener.class})
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Comment(value="\u4e3b\u952eID")
    private Long id;
    @CreatedBy
    @Comment(value="\u521b\u5efa\u4eba")
    private Long createdBy;
    @Column(updatable=false)
    @Comment(value="\u521b\u5efa\u65f6\u95f4")
    private LocalDateTime createdAt;
    @LastModifiedBy
    @Comment(value="\u66f4\u65b0\u4eba")
    private Long updatedBy;
    @Comment(value="\u66f4\u65b0\u65f6\u95f4")
    private LocalDateTime updatedAt;
    @Comment(value="\u5220\u9664\u4eba")
    private Long deletedBy;
    @Comment(value="\u5220\u9664\u65f6\u95f4")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Generated
    public Long getId() {
        return this.id;
    }

    @Generated
    public Long getCreatedBy() {
        return this.createdBy;
    }

    @Generated
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Generated
    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    @Generated
    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    @Generated
    public Long getDeletedBy() {
        return this.deletedBy;
    }

    @Generated
    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }

    @Generated
    public void setId(Long id) {
        this.id = id;
    }

    @Generated
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    @Generated
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Generated
    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Generated
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Generated
    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }

    @Generated
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}

