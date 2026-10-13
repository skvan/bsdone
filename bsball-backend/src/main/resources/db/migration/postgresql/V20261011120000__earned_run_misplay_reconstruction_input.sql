ALTER TABLE bs_earned_run_decision
    ADD COLUMN IF NOT EXISTS would_score_without_misplay BOOLEAN;

COMMENT ON COLUMN bs_earned_run_decision.would_score_without_misplay IS
    '官方記錄員判斷：若沒有造成推進的捕逸等防守失誤，該跑者是否仍會得分；NULL 表示待覆核';
