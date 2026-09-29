ALTER TABLE bs_game_player_stat
    ADD COLUMN IF NOT EXISTS unearned_r integer NOT NULL DEFAULT 0;

ALTER TABLE bs_game_player_stat
    ADD COLUMN IF NOT EXISTS pending_r integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN bs_game_player_stat.unearned_r IS '非自責分';
COMMENT ON COLUMN bs_game_player_stat.pending_r IS '待判定失分';
