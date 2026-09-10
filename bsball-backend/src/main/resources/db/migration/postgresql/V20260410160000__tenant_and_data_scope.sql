-- 租户（SaaS 数据边界）、用户-租户成员、数据范围（联盟/球队 + 展开策略）；联盟/球队表增加 tenant_id

CREATE TABLE IF NOT EXISTS sys_tenant (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    code         VARCHAR(64)  NOT NULL,
    status       INT DEFAULT 1,
    sort         INT DEFAULT 0,
    created_by   BIGINT,
    created_at   TIMESTAMP,
    updated_by   BIGINT,
    updated_at   TIMESTAMP,
    deleted_by   BIGINT,
    deleted_at   TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_tenant_code ON sys_tenant (code);
COMMENT ON TABLE sys_tenant IS '租户表';

INSERT INTO sys_tenant (id, name, code, status, sort, created_at, updated_at)
SELECT 1, '默认租户', 'default', 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE id = 1);

SELECT setval(
    pg_get_serial_sequence('sys_tenant', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM sys_tenant), 1)
);

CREATE TABLE IF NOT EXISTS sys_user_tenant (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    tenant_id    BIGINT NOT NULL REFERENCES sys_tenant (id),
    created_by   BIGINT,
    created_at   TIMESTAMP,
    updated_by   BIGINT,
    updated_at   TIMESTAMP,
    deleted_by   BIGINT,
    deleted_at   TIMESTAMP,
    CONSTRAINT ux_sys_user_tenant_user_tenant UNIQUE (user_id, tenant_id)
);
COMMENT ON TABLE sys_user_tenant IS '用户可访问的租户';

CREATE TABLE IF NOT EXISTS sys_data_scope (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    tenant_id    BIGINT NOT NULL REFERENCES sys_tenant (id),
    scope_type   VARCHAR(32) NOT NULL,
    ref_id       BIGINT      NOT NULL,
    expansion    VARCHAR(32) NOT NULL DEFAULT 'SELF',
    created_by   BIGINT,
    created_at   TIMESTAMP,
    updated_by   BIGINT,
    updated_at   TIMESTAMP,
    deleted_by   BIGINT,
    deleted_at   TIMESTAMP,
    CONSTRAINT ux_sys_data_scope_user_tenant_ref UNIQUE (user_id, tenant_id, scope_type, ref_id)
);
COMMENT ON TABLE sys_data_scope IS '数据范围';
COMMENT ON COLUMN sys_data_scope.scope_type IS 'LEAGUE | TEAM';
COMMENT ON COLUMN sys_data_scope.expansion IS 'SELF | INCLUDE_DESCENDANTS（联盟含下属球队）';

ALTER TABLE bs_league ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE bs_league SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE bs_league ALTER COLUMN tenant_id SET NOT NULL;

-- 不使用 DO $$ … EXCEPTION：Flyway 经 Druid 连接执行时 Wall 解析器不支持该匿名块，会报错
ALTER TABLE bs_league ADD CONSTRAINT fk_bs_league_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id);

ALTER TABLE bs_team ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
UPDATE bs_team SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE bs_team ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE bs_team ADD CONSTRAINT fk_bs_team_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id);

INSERT INTO sys_user_tenant (user_id, tenant_id, created_at, updated_at)
SELECT u.id, 1, NOW(), NOW()
FROM sys_user u
WHERE u.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user_tenant ut WHERE ut.user_id = u.id AND ut.tenant_id = 1);
