-- 门户意见反馈（按租户隔离）
CREATE TABLE IF NOT EXISTS portal_feedback (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    user_id         BIGINT NULL,
    visitor_id      VARCHAR(64)  NULL,
    feedback_type   VARCHAR(32)  NOT NULL,
    title           VARCHAR(200) NULL,
    content         TEXT         NOT NULL,
    contact_type    VARCHAR(16)  NULL,
    contact_value   VARCHAR(256) NULL,
    user_agent      VARCHAR(512) NULL,
    client_version  VARCHAR(64)  NULL,
    server_version  VARCHAR(64)  NULL,
    client_ip       VARCHAR(128) NULL,
    page_path       VARCHAR(512) NULL,
    pending_append_count INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_portal_feedback_tenant_created
    ON portal_feedback (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_portal_feedback_tenant_visitor_created
    ON portal_feedback (tenant_id, visitor_id, created_at DESC);

COMMENT ON TABLE portal_feedback IS '门户意见反馈';
COMMENT ON COLUMN portal_feedback.tenant_id IS '租户ID';
COMMENT ON COLUMN portal_feedback.user_id IS '提交用户（后台 JWT 与门户租户一致时写入）';
COMMENT ON COLUMN portal_feedback.visitor_id IS '游客访客ID（portal_vid）';
COMMENT ON COLUMN portal_feedback.feedback_type IS '反馈类型代码';
COMMENT ON COLUMN portal_feedback.contact_type IS '联系方式类型：phone/email/qq/wechat/other';
COMMENT ON COLUMN portal_feedback.pending_append_count IS '当前待处理周期内，用户已补充次数';
