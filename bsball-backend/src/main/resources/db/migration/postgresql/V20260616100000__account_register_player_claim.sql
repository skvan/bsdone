-- 账号注册、短信验证、球员身份认领、球队负责人

-- sys_user 扩展
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS phone_verified SMALLINT DEFAULT 0;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMP;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS privacy_accepted_at TIMESTAMP;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS register_source VARCHAR(32);
COMMENT ON COLUMN sys_user.phone_verified IS '手机号是否已验证：0否 1是';
COMMENT ON COLUMN sys_user.terms_accepted_at IS '接受服务协议时间';
COMMENT ON COLUMN sys_user.privacy_accepted_at IS '接受隐私政策时间';
COMMENT ON COLUMN sys_user.register_source IS '注册来源：portal/admin/invite';

CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_user_phone_active
    ON sys_user (phone)
    WHERE deleted_at IS NULL AND phone IS NOT NULL AND phone <> '';

-- 球员与用户绑定
ALTER TABLE bs_player ADD COLUMN IF NOT EXISTS user_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_bs_player_user_id ON bs_player (user_id) WHERE user_id IS NOT NULL;
COMMENT ON COLUMN bs_player.user_id IS '已认证认领的用户ID';

-- 短信验证码
CREATE TABLE IF NOT EXISTS sys_sms_code (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT,
    phone        VARCHAR(20) NOT NULL,
    code         VARCHAR(10) NOT NULL,
    scene        VARCHAR(32) NOT NULL,
    client_ip    VARCHAR(128),
    expires_at   TIMESTAMP NOT NULL,
    used_at      TIMESTAMP,
    created_by   BIGINT,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   BIGINT,
    updated_at   TIMESTAMP,
    deleted_by   BIGINT,
    deleted_at   TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_sms_code_phone_scene ON sys_sms_code (phone, scene, created_at DESC);
COMMENT ON TABLE sys_sms_code IS '短信验证码记录';
COMMENT ON COLUMN sys_sms_code.scene IS 'register/login/bind_phone/reset_password';

-- 球队负责人
CREATE TABLE IF NOT EXISTS bs_team_manager (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL,
    team_id      BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'active',
    created_by   BIGINT,
    created_at   TIMESTAMP,
    updated_by   BIGINT,
    updated_at   TIMESTAMP,
    deleted_by   BIGINT,
    deleted_at   TIMESTAMP,
    CONSTRAINT ux_bs_team_manager_team_user UNIQUE (team_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_bs_team_manager_user ON bs_team_manager (user_id) WHERE deleted_at IS NULL;
COMMENT ON TABLE bs_team_manager IS '球队负责人（审核球员认领）';
COMMENT ON COLUMN bs_team_manager.status IS 'active/inactive';

-- 球员身份认领申请
CREATE TABLE IF NOT EXISTS bs_player_claim (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    player_id       BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'pending',
    reviewer_type   VARCHAR(32),
    reviewer_id     BIGINT,
    reviewed_at     TIMESTAMP,
    reject_reason   VARCHAR(500),
    remark          VARCHAR(500),
    invite_id       BIGINT,
    created_by      BIGINT,
    created_at      TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP,
    deleted_by      BIGINT,
    deleted_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bs_player_claim_status ON bs_player_claim (tenant_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_bs_player_claim_user ON bs_player_claim (user_id) WHERE deleted_at IS NULL;
COMMENT ON TABLE bs_player_claim IS '球员身份认领申请';
COMMENT ON COLUMN bs_player_claim.status IS 'pending/approved/rejected/cancelled';
COMMENT ON COLUMN bs_player_claim.reviewer_type IS 'team_manager/platform_admin';

-- 认领邀请链接
CREATE TABLE IF NOT EXISTS bs_player_claim_invite (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    team_id         BIGINT NOT NULL,
    player_id       BIGINT,
    token           VARCHAR(64) NOT NULL,
    created_by      BIGINT NOT NULL,
    expires_at      TIMESTAMP NOT NULL,
    max_uses        INT NOT NULL DEFAULT 1,
    used_count      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'active',
    remark          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,
    deleted_at      TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_player_claim_invite_token ON bs_player_claim_invite (token);
COMMENT ON TABLE bs_player_claim_invite IS '球队负责人发出的球员认领邀请';
COMMENT ON COLUMN bs_player_claim_invite.status IS 'active/expired/revoked';

-- 门户会员角色（自助注册用户默认角色）
INSERT INTO sys_role (name, code, status, tenant_id, created_at, updated_at)
SELECT '门户会员', 'member', 1, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code = 'member' AND tenant_id IS NULL AND deleted_at IS NULL);

-- 平台级法律文档（隐私政策、服务协议）
INSERT INTO sys_article (tenant_id, title, summary, content, type, publish_target, content_type, status, created_at, updated_at)
SELECT NULL, '隐私政策', '棒垒球管理系统用户隐私保护政策',
       E'# 隐私政策\n\n更新日期：2026年6月16日\n\n## 一、信息收集\n\n我们可能在您注册账号、使用门户服务、进行球员身份认领时收集以下信息：\n\n- 手机号码（用于账号注册、登录验证）\n- 用户名、昵称等账号信息\n- 设备信息、访问日志（用于安全与统计分析）\n\n## 二、信息使用\n\n收集的信息将用于：\n\n1. 提供账号注册、登录及身份认证服务\n2. 球员档案认领与审核流程\n3. 保障系统安全、防止恶意刷短信等行为\n4. 改进产品体验\n\n## 三、信息保护\n\n我们采取合理的技术与管理措施保护您的个人信息，不会向无关第三方出售您的个人信息。\n\n## 四、您的权利\n\n您有权查询、更正或注销账号；如需帮助请联系平台管理员。\n\n## 五、联系我们\n\n如有隐私相关问题，请通过门户反馈渠道联系我们。',
       'privacy_policy', 'portal', 'markdown', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_article WHERE type = 'privacy_policy' AND tenant_id IS NULL AND deleted_at IS NULL);

INSERT INTO sys_article (tenant_id, title, summary, content, type, publish_target, content_type, status, created_at, updated_at)
SELECT NULL, '用户服务协议', '棒垒球管理系统用户服务协议',
       E'# 用户服务协议\n\n更新日期：2026年6月16日\n\n## 一、服务说明\n\n本系统为棒垒球赛事与球队管理提供信息化服务，包括门户浏览、账号注册、球员身份认领等功能。\n\n## 二、账号注册与使用\n\n1. 您应使用真实有效的手机号完成注册\n2. 用户名、密码由您自行保管，因保管不善导致的损失由您承担\n3. 禁止利用本系统进行任何违法违规活动\n\n## 三、球员身份认领\n\n1. 认领须提交真实信息，由球队负责人或平台管理员审核\n2. 虚假认领将被拒绝，严重者账号将被禁用\n3. 通过邀请链接认领同样受本协议约束\n\n## 四、短信服务\n\n注册/登录验证码短信仅用于身份验证，请勿向他人泄露验证码。\n\n## 五、协议变更\n\n我们可能适时更新本协议，更新后继续使用即视为同意。\n\n## 六、免责声明\n\n因不可抗力或第三方原因导致的服务中断，我们在法律允许范围内免责。',
       'terms_of_service', 'portal', 'markdown', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_article WHERE type = 'terms_of_service' AND tenant_id IS NULL AND deleted_at IS NULL);
