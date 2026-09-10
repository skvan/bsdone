-- 投手单场：自由击球数（可与 BF 独立录入）；为空时仍按 BF−BB−IBB−HBP 推算
ALTER TABLE bs_game_player_stat ADD COLUMN IF NOT EXISTS pitch_ab INTEGER;
COMMENT ON COLUMN bs_game_player_stat.pitch_ab IS '投手自由击球数（与 BF 独立；NULL 时按 BF−BB−IBB−HBP 推算）';
