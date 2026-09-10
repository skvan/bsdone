-- 全站 IP 访问策略（可扩展 scope：GLOBAL；预留 role_id、api_pattern 供后续按角色/API 细化）
CREATE TABLE IF NOT EXISTS sys_ip_access_rule (
    id              BIGSERIAL PRIMARY KEY,
    rule_kind       VARCHAR(8)  NOT NULL,
    scope_type      VARCHAR(32) NOT NULL DEFAULT 'GLOBAL',
    role_id         BIGINT NULL,
    api_pattern     VARCHAR(512) NULL,
    cidr_or_ip      VARCHAR(128) NOT NULL,
    priority        INT         NOT NULL DEFAULT 0,
    enabled         SMALLINT    NOT NULL DEFAULT 1,
    remark          VARCHAR(512) NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT NULL,
    updated_by      BIGINT NULL,
    CONSTRAINT chk_sys_ip_access_rule_kind CHECK (rule_kind IN ('ALLOW', 'DENY')),
    CONSTRAINT chk_sys_ip_access_rule_scope CHECK (scope_type IN ('GLOBAL', 'ROLE', 'API_PREFIX'))
);

CREATE INDEX IF NOT EXISTS idx_sys_ip_access_rule_lookup
    ON sys_ip_access_rule (scope_type, enabled, rule_kind, priority DESC);

COMMENT ON TABLE sys_ip_access_rule IS 'IP 访问规则：黑名单/白名单模式见 sys_config.ipAccessPolicyMode';
COMMENT ON COLUMN sys_ip_access_rule.scope_type IS 'GLOBAL=全站；ROLE/API_PREFIX 预留';
COMMENT ON COLUMN sys_ip_access_rule.cidr_or_ip IS 'IPv4 单地址或 CIDR，如 203.0.113.10 或 10.0.0.0/8';

INSERT INTO sys_config (tenant_id, config_key, config_value)
SELECT 1, 'ipAccessPolicyMode', 'off'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'ipAccessPolicyMode');

INSERT INTO sys_config (tenant_id, config_key, config_value)
SELECT 1, 'ipAccessPolicyBypassPaths',
       '/health,/auth/login,/auth/captcha,/auth/captcha/image,/auth/captcha/options,/auth/captcha/verify-click,/auth/captcha/verify-drag'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'ipAccessPolicyBypassPaths');
