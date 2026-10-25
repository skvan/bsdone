-- 赛事级特殊规则（Issue #275，2026-10-24 口径调整）
-- 两层语义：
--   1) 联赛级 special_rules_enabled = 收费能力；无记录默认关闭，已配置旧规则的联赛迁移时自动开启；
--   2) 赛事级 extra_batter / force_half = 实际规则；能力关闭后已配置规则的赛事保留，新建赛事不可写入。
-- 旧 bs_league_special_rule 保留：本迁移不改旧规则行，仅复制到赛事并补写联赛能力开关。

CREATE TABLE IF NOT EXISTS bs_event_special_rule (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    event_id    BIGINT NOT NULL,
    rule_key    VARCHAR(64) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    created_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT,
    updated_at  TIMESTAMP,
    deleted_by  BIGINT,
    deleted_at  TIMESTAMP,
    CONSTRAINT uk_bs_event_special_rule UNIQUE (event_id, rule_key)
);
COMMENT ON TABLE  bs_event_special_rule IS '赛事级特殊规则开关：行存在=以记录为准，无记录=默认值';
COMMENT ON COLUMN bs_event_special_rule.tenant_id IS '租户ID';
COMMENT ON COLUMN bs_event_special_rule.event_id IS '赛事ID（bs_event.id，逻辑关联）';
COMMENT ON COLUMN bs_event_special_rule.rule_key IS '规则键：extra_batter / force_half';
COMMENT ON COLUMN bs_event_special_rule.enabled IS '是否启用';

-- 存量迁移：旧联赛级实际规则复制到该联赛下每个赛事（幂等）
INSERT INTO bs_event_special_rule (tenant_id, event_id, rule_key, enabled, created_at)
SELECT e.tenant_id, e.id, r.rule_key, r.enabled, CURRENT_TIMESTAMP
FROM bs_league_special_rule r
JOIN bs_event e ON e.league_id = r.league_id AND e.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND r.rule_key IN ('extra_batter', 'force_half')
ON CONFLICT (event_id, rule_key) DO NOTHING;

-- 存量联赛：存在旧实际规则行的联赛自动开启收费能力（保持现状）
INSERT INTO bs_league_special_rule (tenant_id, league_id, rule_key, enabled, created_at)
SELECT DISTINCT r.tenant_id, r.league_id, 'special_rules_enabled', TRUE, CURRENT_TIMESTAMP
FROM bs_league_special_rule r
WHERE r.deleted_at IS NULL
  AND r.rule_key IN ('extra_batter', 'force_half')
ON CONFLICT (league_id, rule_key) DO NOTHING;
