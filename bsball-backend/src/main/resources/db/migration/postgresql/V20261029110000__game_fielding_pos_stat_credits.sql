-- 逐位置守备扩展指标（P3）：PO/A/E 与 outs 同表逐位置累计
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS po INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS a INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS e INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS dp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS pb INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS catcher_cs INTEGER NOT NULL DEFAULT 0;
ALTER TABLE bs_game_fielding_pos_stat ADD COLUMN IF NOT EXISTS catcher_sb INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN bs_game_fielding_pos_stat.po IS '接杀数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.a IS '助杀数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.e IS '失误数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.dp IS '双杀守备数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.pb IS '捕逸数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.catcher_cs IS '盗垒阻杀数（该位置累计）';
COMMENT ON COLUMN bs_game_fielding_pos_stat.catcher_sb IS '被盗垒数（该位置累计）';
