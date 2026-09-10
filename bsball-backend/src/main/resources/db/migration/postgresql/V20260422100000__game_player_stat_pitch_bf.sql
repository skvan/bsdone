-- 投手单场：轮击数 BF（Batters Faced），与 PA 可分别统计
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS pitch_bf INTEGER;
COMMENT ON COLUMN bs_game_player_stat.pitch_bf IS '轮击数（面对打者 BF）';
