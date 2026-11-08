-- 守备表按先发打线排序：记录先发守备位置（先发野手/先发投手；DH/替补为空）
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS starting_position VARCHAR(8);
COMMENT ON COLUMN bs_game_player_stat.starting_position IS '本场先发守备位置（先发野手/先发投手；DH/替补为空）';
