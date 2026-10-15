-- 联赛级特殊规则开关（Issue #275）：extra_batter（EP 制）/ force_half（强制换局）
-- 语义：行存在=以记录为准（支持显式关闭）；无记录=默认值（extra_batter=false、force_half=true）
-- 幂等：CREATE TABLE IF NOT EXISTS + 回填 WHERE NOT EXISTS；空库时表亦可由实体 ddl-auto 创建（以实体列为准）
-- 回填守卫：软删行不计证据（deleted_at IS NULL）；快照仅按数组判型取长度（jsonb_typeof='array'），缺键/非数组/非 '{' 开头静默跳过

CREATE TABLE IF NOT EXISTS bs_league_special_rule (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    league_id   BIGINT NOT NULL,
    rule_key    VARCHAR(64) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    created_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT,
    updated_at  TIMESTAMP,
    deleted_by  BIGINT,
    deleted_at  TIMESTAMP,
    CONSTRAINT uk_bs_league_special_rule UNIQUE (league_id, rule_key)
);
COMMENT ON TABLE  bs_league_special_rule IS '联赛级特殊规则开关：行存在=以记录为准，无记录=默认值';
COMMENT ON COLUMN bs_league_special_rule.tenant_id IS '租户ID';
COMMENT ON COLUMN bs_league_special_rule.league_id IS '联赛ID（bs_league.id，逻辑关联）';
COMMENT ON COLUMN bs_league_special_rule.rule_key IS '规则键：extra_batter / force_half';
COMMENT ON COLUMN bs_league_special_rule.enabled IS '是否启用';

-- 存量白名单（Issue #275 裁决 3）：含超默认阵型（棒球>9 / 垒球>10 打击）的联赛自动开启 extra_batter
INSERT INTO bs_league_special_rule (tenant_id, league_id, rule_key, enabled, created_at)
SELECT DISTINCT e.tenant_id, e.league_id, 'extra_batter', TRUE, CURRENT_TIMESTAMP
FROM (
    SELECT g.event_id
    FROM bs_game_player_stat s
    JOIN bs_game g ON g.id = s.game_id
    WHERE s.deleted_at IS NULL
      AND g.deleted_at IS NULL
      AND ((g.game_mode = 'BASEBALL' AND s.batting_order > 9)
        OR (g.game_mode = 'SOFTBALL' AND s.batting_order > 10))
    UNION
    SELECT g.event_id
    FROM bs_game g
    WHERE g.deleted_at IS NULL
      AND g.live_snapshot_json LIKE '{%'
      AND ((g.game_mode = 'BASEBALL'
            AND ((jsonb_typeof((g.live_snapshot_json::jsonb) -> 'homeLineup') = 'array'
                  AND jsonb_array_length((g.live_snapshot_json::jsonb) -> 'homeLineup') > 9)
              OR (jsonb_typeof((g.live_snapshot_json::jsonb) -> 'awayLineup') = 'array'
                  AND jsonb_array_length((g.live_snapshot_json::jsonb) -> 'awayLineup') > 9)))
        OR (g.game_mode = 'SOFTBALL'
            AND ((jsonb_typeof((g.live_snapshot_json::jsonb) -> 'homeLineup') = 'array'
                  AND jsonb_array_length((g.live_snapshot_json::jsonb) -> 'homeLineup') > 10)
              OR (jsonb_typeof((g.live_snapshot_json::jsonb) -> 'awayLineup') = 'array'
                  AND jsonb_array_length((g.live_snapshot_json::jsonb) -> 'awayLineup') > 10))))
) x
JOIN bs_event e ON e.id = x.event_id AND e.league_id IS NOT NULL AND e.deleted_at IS NULL
WHERE NOT EXISTS (
    SELECT 1 FROM bs_league_special_rule r
    WHERE r.league_id = e.league_id AND r.rule_key = 'extra_batter'
);
