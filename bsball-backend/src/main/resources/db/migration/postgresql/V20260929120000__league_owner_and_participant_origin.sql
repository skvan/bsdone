-- 联盟主办方归属关系 + 参赛主体扩展位（跨区预留） + 门户自助配置项

CREATE TABLE IF NOT EXISTS bs_league_owner (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT NOT NULL REFERENCES sys_tenant(id),
    league_id    BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'active',
    grant_source VARCHAR(32) DEFAULT 'ADMIN_ASSIGN',
    created_by   BIGINT, created_at TIMESTAMP,
    updated_by   BIGINT, updated_at TIMESTAMP,
    deleted_by   BIGINT, deleted_at TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_league_owner_league_user ON bs_league_owner (league_id, user_id);
CREATE INDEX IF NOT EXISTS ix_bs_league_owner_tenant_user ON bs_league_owner (tenant_id, user_id);
CREATE INDEX IF NOT EXISTS ix_bs_league_owner_league_status ON bs_league_owner (league_id, status);
COMMENT ON TABLE  bs_league_owner IS '联盟主办方归属关系';
COMMENT ON COLUMN bs_league_owner.grant_source IS 'SELF_CREATE | ADMIN_ASSIGN';
COMMENT ON COLUMN bs_league_owner.status IS 'active | inactive';

ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS home_team_origin VARCHAR(32);
ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS away_team_origin VARCHAR(32);
COMMENT ON COLUMN bs_game.home_team_origin IS 'LOCAL | GUEST（客座参赛单位，预留）';
COMMENT ON COLUMN bs_game.away_team_origin IS 'LOCAL | GUEST（客座参赛单位，预留）';

-- sys_config 实际仅 3 列：tenant_id / config_key / config_value（见实体 SysConfig），故不写审计列
INSERT INTO sys_config (tenant_id, config_key, config_value)
SELECT 1, 'portalLeagueCreateRequireApproval', 'true'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'portalLeagueCreateRequireApproval');

INSERT INTO sys_config (tenant_id, config_key, config_value)
SELECT 1, 'portalTeamCreateRequireApproval', 'false'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'portalTeamCreateRequireApproval');

INSERT INTO sys_config (tenant_id, config_key, config_value)
SELECT 1, 'portalPlayerSelfCreateEnabled', 'true'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE tenant_id = 1 AND config_key = 'portalPlayerSelfCreateEnabled');
