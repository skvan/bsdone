-- 投手失分分解列的"同义重复列"止损标注：只写注释，不动数据、不动可空性、不删列。
-- PG 库 bs_game_player_stat 里存在两组语义相同的列：
--   应用在用（JPA 实体 + mapper 的 PostgreSQL 分支）：pitchr / er / unearnedr / pendingr
--   本文件标注的退役列：pitch_r / earned_r / unearned_r / pending_r
--     由早期 migration 按 snake_case 建出（unearned_r/pending_r 为 NOT NULL DEFAULT 0，
--     全表被默认值灌满），应用不读不写，历史非 0 值只是人工回填，不能作为任何口径。
-- 之所以不删列：mapper 的非 PostgreSQL 分支（<otherwise>）仍引用这组名字，删除要与分支一起规划。
-- 实体侧已在 GamePlayerStat 用 @Column(name=...) 显式钉死在用列名，并有单测守住，
-- 防止 Hibernate 命名策略变动把读写悄悄切到本组退役列（表现是"引擎算对了但表与前台不动"）。
COMMENT ON COLUMN bs_game_player_stat.pitch_r IS
    '【退役·勿用】投手失分的同义旧列；应用读写的是 pitchr，本列无维护';
COMMENT ON COLUMN bs_game_player_stat.earned_r IS
    '【退役·勿用】投手自责分的同义旧列；应用读写的是 er，本列无维护';
COMMENT ON COLUMN bs_game_player_stat.unearned_r IS
    '【退役·勿用】非自责分同义旧列（NOT NULL DEFAULT 0，全表被默认值灌满）；应用读写的是 unearnedr';
COMMENT ON COLUMN bs_game_player_stat.pending_r IS
    '【退役·勿用】待判定失分同义旧列（NOT NULL DEFAULT 0，全表被默认值灌满）；应用读写的是 pendingr';
