-- 联盟创建申请（门户自助，默认需平台审核）

CREATE TABLE IF NOT EXISTS bs_league_create_request (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT NOT NULL REFERENCES sys_tenant(id),
    applicant_user_id BIGINT NOT NULL,
    name              VARCHAR(200) NOT NULL,
    name_en           VARCHAR(200),
    description       VARCHAR(2000),
    status            VARCHAR(16) NOT NULL DEFAULT 'pending',
    league_id         BIGINT,
    reviewed_by       BIGINT,
    reviewed_at       TIMESTAMP,
    reject_reason     VARCHAR(500),
    created_by BIGINT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT, deleted_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bs_league_create_request_tenant_status ON bs_league_create_request (tenant_id, status);
COMMENT ON TABLE  bs_league_create_request IS '联盟创建申请（门户自助，默认需平台审核）';
COMMENT ON COLUMN bs_league_create_request.tenant_id IS '租户ID';
COMMENT ON COLUMN bs_league_create_request.applicant_user_id IS '申请人用户ID';
COMMENT ON COLUMN bs_league_create_request.name IS '联盟名称';
COMMENT ON COLUMN bs_league_create_request.name_en IS '联盟英文名';
COMMENT ON COLUMN bs_league_create_request.description IS '联盟简介';
COMMENT ON COLUMN bs_league_create_request.status IS 'pending | approved | rejected';
COMMENT ON COLUMN bs_league_create_request.league_id IS '审核通过后生成的联盟ID';
COMMENT ON COLUMN bs_league_create_request.reviewed_by IS '审核人用户ID';
COMMENT ON COLUMN bs_league_create_request.reviewed_at IS '审核时间';
COMMENT ON COLUMN bs_league_create_request.reject_reason IS '驳回原因';
