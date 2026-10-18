-- Issue #347：补齐纸质记分表对应的打者/投手字段
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS io INTEGER DEFAULT 0;
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS pitch_doubles INTEGER DEFAULT 0;
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS pitch_triples INTEGER DEFAULT 0;
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS pitch_sh INTEGER DEFAULT 0;
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS pitch_sf INTEGER DEFAULT 0;
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS pitch_io INTEGER DEFAULT 0;

COMMENT ON COLUMN bs_game_player_stat.io IS '妨阻上垒（IO）';
COMMENT ON COLUMN bs_game_player_stat.pitch_doubles IS '投手被二垒打';
COMMENT ON COLUMN bs_game_player_stat.pitch_triples IS '投手被三垒打';
COMMENT ON COLUMN bs_game_player_stat.pitch_sh IS '投手被牺牲触击';
COMMENT ON COLUMN bs_game_player_stat.pitch_sf IS '投手被牺牲高飞';
COMMENT ON COLUMN bs_game_player_stat.pitch_io IS '投手被妨阻上垒（IO）';
