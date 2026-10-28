-- =====================================================================
-- 投手出局數欄位（規範 §7：ERA 應以出局數計算，避免 Double 精度問題）
-- =====================================================================

-- 專案在 Hibernate 建表後才跑 Flyway（run-after-hibernate），此列可能已由
-- 實體自動建立為 nullable；ADD COLUMN IF NOT EXISTS 不會補上 NOT NULL/DEFAULT，
-- 因此後續需顯式回填、設默認值並收緊非空，不能只依賴 ADD COLUMN 的聲明。
ALTER TABLE bs_game_player_stat
    ADD COLUMN IF NOT EXISTS outs_recorded integer;

UPDATE bs_game_player_stat
SET outs_recorded = 0
WHERE outs_recorded IS NULL;

-- 從現有 ip（棒球記法 Double）回填出局數
-- 公式等價於 StatsFormatUtil.baseballIpToOuts：floor(ip)*3 + round((ip-floor(ip))*10)
UPDATE bs_game_player_stat
SET outs_recorded = (
    FLOOR(ip + 1e-10) * 3
    + LEAST(2, GREATEST(0, ROUND((ip + 1e-10 - FLOOR(ip + 1e-10)) * 10)))
)::int
WHERE ip IS NOT NULL AND ip > 0 AND outs_recorded = 0;

ALTER TABLE bs_game_player_stat
    ALTER COLUMN outs_recorded SET DEFAULT 0;

ALTER TABLE bs_game_player_stat
    ALTER COLUMN outs_recorded SET NOT NULL;

COMMENT ON COLUMN bs_game_player_stat.outs_recorded IS
    '投手取得的出局數（權威值；ip 欄位為顯示用衍生值）';
