-- 比赛球员统计、球场、球员、教练、人员变动、门户打点：租户隔离

ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
UPDATE bs_game_player_stat s
SET tenant_id = g.tenant_id
FROM bs_game g
WHERE s.game_id = g.id;

ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

ALTER TABLE bs_player ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
UPDATE bs_player p
SET tenant_id = t.tenant_id
FROM bs_team t
WHERE p.team_id = t.id;

ALTER TABLE bs_coach ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
UPDATE bs_coach c
SET tenant_id = t.tenant_id
FROM bs_team t
WHERE c.team_id = t.id;

ALTER TABLE bs_personnel_change ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
DO $$
BEGIN
  -- 兼容旧结构：bs_personnel_change 存在 team_id
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'bs_personnel_change'
      AND column_name = 'team_id'
  ) THEN
    UPDATE bs_personnel_change pc
    SET tenant_id = t.tenant_id
    FROM bs_team t
    WHERE pc.team_id = t.id;
  -- 兼容新结构：通过关联对象回填球队租户
  ELSIF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'bs_personnel_change'
      AND column_name = 'related_object_type'
  ) AND EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = current_schema()
      AND table_name = 'bs_personnel_change'
      AND column_name = 'related_object_id'
  ) THEN
    UPDATE bs_personnel_change pc
    SET tenant_id = t.tenant_id
    FROM bs_team t
    WHERE pc.related_object_type = 'team'
      AND pc.related_object_id = t.id;
  END IF;
END $$;

ALTER TABLE portal_visit_hit ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_bs_game_player_stat_tenant_id ON bs_game_player_stat(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bs_stadium_tenant_id ON bs_stadium(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bs_player_tenant_id ON bs_player(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bs_coach_tenant_id ON bs_coach(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bs_personnel_change_tenant_id ON bs_personnel_change(tenant_id);
CREATE INDEX IF NOT EXISTS idx_portal_visit_hit_tenant_date ON portal_visit_hit(tenant_id, hit_date);
