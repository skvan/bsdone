-- 投手：被场内全垒打单独计数；pitch_hr 仅表示「墙外/一般全垒打」被全垒打数
ALTER TABLE bs_game_player_stat
    ADD COLUMN IF NOT EXISTS pitch_inside_park_hr INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN bs_game_player_stat.pitch_inside_park_hr IS '被场内全垒打（不计入 pitch_hr）';

-- 历史数据：场内全垒打曾错误同时计入打者 hr 与 inside_park_hr，此处从 hr 中扣除重复部分
UPDATE bs_game_player_stat
SET hr = hr - COALESCE(inside_park_hr, 0)
WHERE COALESCE(inside_park_hr, 0) > 0
  AND COALESCE(hr, 0) >= COALESCE(inside_park_hr, 0);
