-- 场次标签（如冠军赛），有值时前台显示奖杯标识
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'bs_game'
    ) THEN
        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS game_tag VARCHAR(64);
        COMMENT ON COLUMN bs_game.game_tag IS '场次标签，如冠军赛；有值时前台显示奖杯标识';
    END IF;
END
$flyway$;
