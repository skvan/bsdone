/*
 * 账号权限重构（批次 3b，Task 3.15b / spec §8.6）：门户角色租户级目录覆盖明细。
 *
 * 语义：某租户对某平台门户角色的「生效菜单/按钮集合」（全量替换，可为空集）；
 *       与全局 sys_role_menu 绑定彼此独立，覆盖保存绝不触碰全局绑定行。
 */
package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="bs_tenant_role_menu")
public class TenantRoleMenu {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="config_id", nullable=false)
    private Long configId;
    @Column(name="menu_id", nullable=false)
    private Long menuId;

    public Long getId() {
        return this.id;
    }

    public Long getConfigId() {
        return this.configId;
    }

    public Long getMenuId() {
        return this.menuId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
