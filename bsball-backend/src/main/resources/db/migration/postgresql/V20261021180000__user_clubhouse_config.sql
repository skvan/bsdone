-- 个人驾驶舱配置：按用户 + 租户保存固定布局下的表现图/队内排名指标选择。
-- 第一版 config_json 只存储白名单内的指标与图表类型；后续模块化布局继续复用该字段。
CREATE TABLE IF NOT EXISTS bs_user_clubhouse_config (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    config_json TEXT NOT NULL,
    config_version INTEGER NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_user_clubhouse_config_user_tenant
    ON bs_user_clubhouse_config (user_id, tenant_id)
    WHERE deleted_at IS NULL;
