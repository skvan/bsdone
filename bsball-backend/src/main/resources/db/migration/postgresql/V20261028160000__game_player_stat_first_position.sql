-- 盒子比分守备表：记录球员本场第一次上场防守时的守位（#446 关联）
-- 之后场上换位不更新该列；旧数据为空时前端回退使用 position。
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS first_position VARCHAR(8);
COMMENT ON COLUMN bs_game_player_stat.first_position IS '本场首次上场防守的守位（守备表归组用）';
