-- 资源文件、系统配置、登录/操作日志按租户隔离；sys_config 主键改为 (tenant_id, config_key)

-- 1) 资源文件
ALTER TABLE sys_resource ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES sys_tenant (id);
UPDATE sys_resource SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE sys_resource ALTER COLUMN tenant_id SET NOT NULL;

-- 2) 登录日志
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES sys_tenant (id);
UPDATE sys_login_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE sys_login_log ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sys_login_log_tenant_id ON sys_login_log (tenant_id);

-- 3) 操作日志
ALTER TABLE sys_operation_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES sys_tenant (id);
UPDATE sys_operation_log SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE sys_operation_log ALTER COLUMN tenant_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_sys_operation_log_tenant_id ON sys_operation_log (tenant_id);

-- 4) 系统配置：增加 tenant_id 并改复合主键
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT REFERENCES sys_tenant (id);
UPDATE sys_config SET tenant_id = 1 WHERE tenant_id IS NULL;
ALTER TABLE sys_config ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE sys_config DROP CONSTRAINT IF EXISTS sys_config_pkey;
ALTER TABLE sys_config ADD PRIMARY KEY (tenant_id, config_key);

COMMENT ON COLUMN sys_resource.tenant_id IS '所属租户';
COMMENT ON COLUMN sys_login_log.tenant_id IS '登录时租户上下文';
COMMENT ON COLUMN sys_operation_log.tenant_id IS '操作所属租户';
COMMENT ON COLUMN sys_config.tenant_id IS '配置所属租户';
