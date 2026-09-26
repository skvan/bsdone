-- 球员-球队经历：一位球员可注册多支球队，背号/守备位置按队存储，当前球队可多选
CREATE TABLE IF NOT EXISTS bs_player_team (
    id          BIGSERIAL PRIMARY KEY,
    player_id   BIGINT NOT NULL,
    team_id     BIGINT NOT NULL,
    tenant_id   BIGINT NOT NULL,
    number      VARCHAR(64),
    positions   TEXT,
    is_current  BOOLEAN NOT NULL DEFAULT FALSE,
    sort        INTEGER NOT NULL DEFAULT 0,
    created_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by  BIGINT,
    deleted_at  TIMESTAMP
);

COMMENT ON TABLE bs_player_team IS '球员球队经历';
COMMENT ON COLUMN bs_player_team.player_id IS '球员ID';
COMMENT ON COLUMN bs_player_team.team_id IS '球队ID';
COMMENT ON COLUMN bs_player_team.tenant_id IS '租户ID';
COMMENT ON COLUMN bs_player_team.number IS '该队背号';
COMMENT ON COLUMN bs_player_team.positions IS '该队守备位置（JSON 数组字符串）';
COMMENT ON COLUMN bs_player_team.is_current IS '是否当前球队';
COMMENT ON COLUMN bs_player_team.sort IS '排序';
COMMENT ON COLUMN bs_player_team.created_by IS '创建人';
COMMENT ON COLUMN bs_player_team.created_at IS '创建时间';
COMMENT ON COLUMN bs_player_team.updated_by IS '更新人';
COMMENT ON COLUMN bs_player_team.updated_at IS '更新时间';
COMMENT ON COLUMN bs_player_team.deleted_by IS '删除人';
COMMENT ON COLUMN bs_player_team.deleted_at IS '删除时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_player_team_player_team ON bs_player_team (player_id, team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_player_team_team_current ON bs_player_team (team_id) WHERE deleted_at IS NULL AND is_current;
CREATE INDEX IF NOT EXISTS idx_player_team_player ON bs_player_team (player_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_player_team_tenant ON bs_player_team (tenant_id) WHERE deleted_at IS NULL;

-- 回填：现有单队球员建档为一条“当前球队”经历（球员租户缺失时以球队租户兜底）
INSERT INTO bs_player_team (player_id, team_id, tenant_id, number, positions, is_current, sort)
SELECT p.id, p.team_id, COALESCE(p.tenant_id, t.tenant_id), p.number, p.positions, TRUE, COALESCE(p.sort, 0)
FROM bs_player p
JOIN bs_team t ON t.id = p.team_id
WHERE p.deleted_at IS NULL
  AND p.team_id IS NOT NULL
  AND p.team_id > 0
  AND NOT EXISTS (
      SELECT 1 FROM bs_player_team e
      WHERE e.player_id = p.id AND e.team_id = p.team_id AND e.deleted_at IS NULL
  );
