-- 逐位置守备扩展指标（P3）：PO/A/E 与 outs 同表逐位置累计
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS po INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS a INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS e INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN bs_game_fielding_pos_stat.po IS '接杀数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.a IS '助杀数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.e IS '失误数（该位置累计）';
