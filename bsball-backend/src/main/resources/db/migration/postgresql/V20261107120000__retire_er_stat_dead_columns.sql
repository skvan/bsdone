-- 投手失分分解列的"同义重复列"止损标注：只写注释，不动数据、不动可空性、不删列。
-- PG 库 bs_game_player_stat 里存在两组语义相同的列：
--   应用在用（JPA 实体 + mapper 的 PostgreSQL 分支）：pitchr / er / unearnedr / pendingr
--   本文件标注的同义旧列：pitch_r / earned_r / unearned_r / pending_r
--     历史实体隐式命名/早期 migration 留下的同义列（unearned_r/pending_r 为 NOT NULL DEFAULT 0）；
--     当前应用读写无下划线列，这组旧列不再作为口径，历史非 0 值只是人工回填。
-- 之所以不删列：mapper 的非 PostgreSQL 分支（<otherwise>）仍引用这组名字，删除要与分支一起规划。
-- 实体侧已在 GamePlayerStat 用 @Column(name=...) 显式钉死在用列名，并有单测守住，
-- 防止 Hibernate 命名策略变动把读写悄悄切到本组退役列（表现是"引擎算对了但表与前台不动"）。
-- 仅在列真实存在时写注释：空库由 Hibernate 按实体钉死的新列名建表，
-- 旧列可能不存在；直接 COMMENT 会让 Flyway 在空库初始化时失败。
DO $$
DECLARE
    col record;
BEGIN
    FOR col IN
        SELECT * FROM (VALUES
            ('pitch_r', '【退役·勿用】投手失分的同义旧列；应用读写的是 pitchr，本列无维护'),
            ('earned_r', '【退役·勿用】投手自责分的同义旧列；应用读写的是 er，本列无维护'),
            ('unearned_r', '【退役·勿用】非自责分同义旧列（NOT NULL DEFAULT 0）；应用读写的是 unearnedr'),
            ('pending_r', '【退役·勿用】待判定失分同义旧列（NOT NULL DEFAULT 0）；应用读写的是 pendingr')
        ) AS v(name, comment_text)
    LOOP
        IF EXISTS (
            SELECT 1
            FROM pg_attribute a
            WHERE a.attrelid = to_regclass('bs_game_player_stat')
              AND a.attname = col.name
              AND a.attnum > 0
              AND NOT a.attisdropped
        ) THEN
            EXECUTE format(
                'COMMENT ON COLUMN %s.%I IS %L',
                to_regclass('bs_game_player_stat'), col.name, col.comment_text
            );
        END IF;
    END LOOP;
END $$;
