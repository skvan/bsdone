-- 球队先发阵容模板（手动维护或从某场比赛拷贝）
CREATE TABLE IF NOT EXISTS bs_team_lineup_template (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    team_id         BIGINT NOT NULL,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(500),
    slots_json      TEXT NOT NULL,
    created_by      BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by      BIGINT,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_team_lineup_tpl_team ON bs_team_lineup_template (team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_team_lineup_tpl_tenant ON bs_team_lineup_template (tenant_id) WHERE deleted_at IS NULL;

COMMENT ON TABLE bs_team_lineup_template IS '球队先发阵容模板';
COMMENT ON COLUMN bs_team_lineup_template.slots_json IS 'JSON 数组：9 条 {playerId,battingOrder,position,number?,batHand?,fieldingGs}';
