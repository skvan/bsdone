-- 比赛记录员（多人逗号/顿号分隔）
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'bs_game'
    ) THEN
        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS recorders VARCHAR(500);
        COMMENT ON COLUMN bs_game.recorders IS '记录员，多人可用逗号或顿号分隔';
    END IF;
END
$flyway$;
