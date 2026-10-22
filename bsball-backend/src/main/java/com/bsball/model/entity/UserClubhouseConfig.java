package com.bsball.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Comment;

/**
 * 个人驾驶舱的用户配置。
 * <p>当前为模块化布局 v3-modular，保存模块顺序/开关、表现图与四个队内排名指标；
 * 后续开放模块缩放/自由画布时，继续扩展 config_json，不新建表。</p>
 */
@Entity
@Table(name = "bs_user_clubhouse_config")
@Comment(value = "个人驾驶舱用户配置")
public class UserClubhouseConfig extends BaseEntity {
    @Comment(value = "用户ID")
    @Column(nullable = false)
    private Long userId;

    @Comment(value = "租户ID")
    @Column(nullable = false)
    private Long tenantId;

    @Comment(value = "配置 JSON（服务端校验后写入）")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String configJson;

    @Comment(value = "配置版本")
    @Column(nullable = false)
    private Integer configVersion = 1;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public Integer getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(Integer configVersion) {
        this.configVersion = configVersion;
    }
}
