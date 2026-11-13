-- 赛事赛制、积分制与比赛阶段元数据（Issue #665）
ALTER TABLE bs_event
    ADD COLUMN IF NOT EXISTS format_type varchar(32) NOT NULL DEFAULT 'SINGLE_ROUND_ROBIN',
    ADD COLUMN IF NOT EXISTS points_win integer NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS points_draw integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS points_loss integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS group_count integer NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS advance_per_group integer NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS third_place boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS bracket_size integer NOT NULL DEFAULT 8;

-- ddl-auto=update + flyway.run-after-hibernate 时，Hibernate 可能先以 nullable/无默认值建列，
-- 上面的 ADD COLUMN 会变成 no-op；这里补默认值与 NOT NULL，保证存量赛事可直接出积分榜。
UPDATE bs_event SET format_type = 'SINGLE_ROUND_ROBIN' WHERE format_type IS NULL;
UPDATE bs_event SET points_win = 2 WHERE points_win IS NULL;
UPDATE bs_event SET points_draw = 1 WHERE points_draw IS NULL;
UPDATE bs_event SET points_loss = 0 WHERE points_loss IS NULL;
UPDATE bs_event SET group_count = 2 WHERE group_count IS NULL;
UPDATE bs_event SET advance_per_group = 2 WHERE advance_per_group IS NULL;
UPDATE bs_event SET third_place = true WHERE third_place IS NULL;
UPDATE bs_event SET bracket_size = 8 WHERE bracket_size IS NULL;

ALTER TABLE bs_event
    ALTER COLUMN format_type SET DEFAULT 'SINGLE_ROUND_ROBIN',
    ALTER COLUMN points_win SET DEFAULT 2,
    ALTER COLUMN points_draw SET DEFAULT 1,
    ALTER COLUMN points_loss SET DEFAULT 0,
    ALTER COLUMN group_count SET DEFAULT 2,
    ALTER COLUMN advance_per_group SET DEFAULT 2,
    ALTER COLUMN third_place SET DEFAULT true,
    ALTER COLUMN bracket_size SET DEFAULT 8,
    ALTER COLUMN format_type SET NOT NULL,
    ALTER COLUMN points_win SET NOT NULL,
    ALTER COLUMN points_draw SET NOT NULL,
    ALTER COLUMN points_loss SET NOT NULL,
    ALTER COLUMN group_count SET NOT NULL,
    ALTER COLUMN advance_per_group SET NOT NULL,
    ALTER COLUMN third_place SET NOT NULL,
    ALTER COLUMN bracket_size SET NOT NULL;

ALTER TABLE bs_game
    ADD COLUMN IF NOT EXISTS stage_code varchar(32),
    ADD COLUMN IF NOT EXISTS group_code varchar(16),
    ADD COLUMN IF NOT EXISTS round_no integer,
    ADD COLUMN IF NOT EXISTS bracket_slot integer;

ALTER TABLE bs_event_team
    ADD COLUMN IF NOT EXISTS group_code varchar(16);

CREATE INDEX IF NOT EXISTS idx_bs_game_event_stage_group
    ON bs_game (event_id, stage_code, group_code);
CREATE INDEX IF NOT EXISTS idx_bs_event_team_event_group
    ON bs_event_team (event_id, group_code);
