-- IP 访问规则按租户隔离（与 sys_config 中 ipAccessPolicy* 一致）
ALTER TABLE sys_ip_access_rule
    ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

UPDATE sys_ip_access_rule SET tenant_id = 1 WHERE tenant_id IS NULL;

COMMENT ON COLUMN sys_ip_access_rule.tenant_id IS '租户：该租 GLOBAL 规则与对应 sys_config 策略共同作用';

CREATE INDEX IF NOT EXISTS idx_sys_ip_access_rule_tenant_scope
    ON sys_ip_access_rule (tenant_id, scope_type, enabled, priority DESC);
