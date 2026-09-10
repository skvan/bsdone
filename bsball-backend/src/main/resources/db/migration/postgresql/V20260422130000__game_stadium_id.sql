-- 比赛关联球场：门户从场地跳转球场介绍页；venue 仍为展示用名称
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'bs_game'
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'bs_stadium'
    ) THEN
        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS stadium_id BIGINT REFERENCES bs_stadium (id) ON DELETE SET NULL;

        CREATE INDEX IF NOT EXISTS idx_bs_game_stadium_id ON bs_game (stadium_id) WHERE stadium_id IS NOT NULL AND deleted_at IS NULL;

        COMMENT ON COLUMN bs_game.stadium_id IS '关联球场 ID（门户链到球场介绍）；场地名称仍以 venue 为准';

        UPDATE bs_game g
        SET stadium_id = s.id
        FROM bs_stadium s
        WHERE g.stadium_id IS NULL
          AND g.deleted_at IS NULL
          AND g.venue IS NOT NULL
          AND TRIM(g.venue) = TRIM(s.name)
          AND g.tenant_id = s.tenant_id
          AND s.deleted_at IS NULL;
    END IF;
END
$flyway$;
