-- =====================================================================
-- 投手出局數欄位（規範 §7：ERA 應以出局數計算，避免 Double 精度問題）
-- =====================================================================

ALTER TABLE bs_game_player_stat
    ADD COLUMN IF NOT EXISTS outs_recorded integer NOT NULL DEFAULT 0;

COMMENT ON COLUMN bs_game_player_stat.outs_recorded IS
    '投手取得的出局數（權威值；ip 欄位為顯示用衍生值）';

-- 從現有 ip（棒球記法 Double）回填出局數
-- 公式等價於 StatsFormatUtil.baseballIpToOuts：floor(ip)*3 + round((ip-floor(ip))*10)
UPDATE bs_game_player_stat
SET outs_recorded = (
    FLOOR(ip + 1e-10) * 3
    + LEAST(2, GREATEST(0, ROUND((ip + 1e-10 - FLOOR(ip + 1e-10)) * 10)))
)::int
WHERE ip IS NOT NULL AND ip > 0 AND outs_recorded = 0;
