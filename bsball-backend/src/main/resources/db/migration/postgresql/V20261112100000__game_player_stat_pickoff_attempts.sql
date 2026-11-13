-- 牵制企图（POA）：投手在两次投球之间向垒上投球，同垒牵制的三种结果
-- （未果回垒 / 牵制出局 / 暴传失误推进）均计 1 次；捕手牵制不计。
-- 内部辅助统计，不在 WBSC 官方基础统计清单内。
ALTER TABLE bs_game_player_stat
    ADD COLUMN IF NOT EXISTS pickoff_attempts INTEGER DEFAULT 0;

COMMENT ON COLUMN bs_game_player_stat.pickoff_attempts IS
    '牵制企图（POA）：投手向垒上牵制投球次数，含未果/出局/暴传；非官方统计';
