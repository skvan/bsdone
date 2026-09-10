-- 门户检测到用户打开开发者工具后的上报（IP 以服务端为准；UA/设备信息由前端提交）

CREATE TABLE IF NOT EXISTS portal_devtools_report (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    visitor_id      VARCHAR(64) NOT NULL,
    ip              VARCHAR(128),
    path            VARCHAR(512),
    user_agent      VARCHAR(512),
    client_meta     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_portal_devtools_report_tenant_created
    ON portal_devtools_report (tenant_id, created_at DESC);

COMMENT ON TABLE portal_devtools_report IS '门户开发者工具打开上报';
COMMENT ON COLUMN portal_devtools_report.client_meta IS 'JSON：屏幕、语言、时区等客户端补充信息';
