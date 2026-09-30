/*
 * 账号权限重构（批次 3b，Task 3.15b / spec §8.6）：门户角色租户级目录覆盖配置头。
 *
 * 语义：行存在 = 该租户对该平台级门户角色（team_manager / league_organizer）启用覆盖（即使明细为空集）；
 *       行删除 = 回落全局 role_menu 绑定。仅平台门户角色参与覆盖，其它角色照旧走全局绑定。
 */
package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name="bs_tenant_role_menu_config", uniqueConstraints={@UniqueConstraint(columnNames={"tenant_id", "role_id"})})
public class TenantRoleMenuConfig {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="tenant_id", nullable=false)
    private Long tenantId;
    @Column(name="role_id", nullable=false)
    private Long roleId;
    @Column(name="updated_by")
    private Long updatedBy;
    @Column(name="created_at", updatable=false)
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

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

    public Long getId() {
        return this.id;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getRoleId() {
        return this.roleId;
    }

    public Long getUpdatedBy() {
        return this.updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
