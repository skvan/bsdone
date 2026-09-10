-- 三振分型：挥空三振 (K) / 目送三振 (ꓘ)，与 so 满足 so = so_swing + so_looking（实时录入时由前端保证）
-- 空库时 Flyway 先于 Hibernate：若表尚未存在则跳过，列由实体 ddl-auto 创建；旧库则在此补列。
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables t
        WHERE t.table_schema = current_schema()
          AND t.table_name = 'bs_game_player_stat'
    ) THEN
        ALTER TABLE bs_game_player_stat
          ADD COLUMN IF NOT EXISTS so_swing integer DEFAULT 0;
        ALTER TABLE bs_game_player_stat
          ADD COLUMN IF NOT EXISTS so_looking integer DEFAULT 0;

        COMMENT ON COLUMN bs_game_player_stat.so_swing IS '挥空三振';
        COMMENT ON COLUMN bs_game_player_stat.so_looking IS '目送三振';
    END IF;
END
$flyway$;
