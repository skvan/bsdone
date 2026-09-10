-- 比赛扩展：观众、裁判、环境（均可空，人工填报）
-- 空库时 bs_game 由 Hibernate 创建，仅当表已存在时补列。
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'bs_game'
    ) THEN
        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS spectator_count INTEGER;
        COMMENT ON COLUMN bs_game.spectator_count IS '观众人数（可空）';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS umpire_hp VARCHAR(128);
        COMMENT ON COLUMN bs_game.umpire_hp IS '主审';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS umpire_1b VARCHAR(128);
        COMMENT ON COLUMN bs_game.umpire_1b IS '一垒审';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS umpire_2b VARCHAR(128);
        COMMENT ON COLUMN bs_game.umpire_2b IS '二垒审';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS umpire_3b VARCHAR(128);
        COMMENT ON COLUMN bs_game.umpire_3b IS '三垒审';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS weather_summary VARCHAR(200);
        COMMENT ON COLUMN bs_game.weather_summary IS '天气简况（如晴/阴）';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS weather_temp_c DOUBLE PRECISION;
        COMMENT ON COLUMN bs_game.weather_temp_c IS '气温（摄氏度，可空）';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS weather_wind VARCHAR(128);
        COMMENT ON COLUMN bs_game.weather_wind IS '风速/风向描述（可空）';

        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS weather_rain_prob_pct INTEGER;
        COMMENT ON COLUMN bs_game.weather_rain_prob_pct IS '赛前预报降雨概率 0-100（可空，非实况降水量）';
    END IF;
END
$flyway$;
