-- D5: 規範 §5.1 / §8.1 審計追溯欄位

-- bs_earned_run_decision：上壘時的投手（通常=responsible_pitcher_id，特殊情況不同）
ALTER TABLE bs_earned_run_decision
    ADD COLUMN IF NOT EXISTS reached_base_pitcher_id BIGINT;

-- 回填：既有判定行，上壘投手=責任投手
UPDATE bs_earned_run_decision
   SET reached_base_pitcher_id = responsible_pitcher_id
 WHERE reached_base_pitcher_id IS NULL AND responsible_pitcher_id IS NOT NULL;

-- bs_game_player_stat：投手進階統計（§8.1）
ALTER TABLE bs_game_player_stat
    ADD COLUMN IF NOT EXISTS inherited_runners_scored integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS passed_ball_runs        integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS error_related_runs      integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS official_scorer_note    varchar(500),
    ADD COLUMN IF NOT EXISTS scoring_revision_status varchar(20) DEFAULT 'PRELIMINARY';

COMMENT ON COLUMN bs_earned_run_decision.reached_base_pitcher_id IS '跑者上壘時的投手（§5.1，可能≠責任投手）';
COMMENT ON COLUMN bs_game_player_stat.inherited_runners_scored IS '繼承跑者中得分的數量（§8.1）';
COMMENT ON COLUMN bs_game_player_stat.passed_ball_runs IS '因捕逸失分數（§8.1）';
COMMENT ON COLUMN bs_game_player_stat.error_related_runs IS '因失誤相關失分數（§8.1）';
COMMENT ON COLUMN bs_game_player_stat.official_scorer_note IS '記錄員備註（§8.1）';
COMMENT ON COLUMN bs_game_player_stat.scoring_revision_status IS '計分修訂狀態：PRELIMINARY/UNDER_REVIEW/FINAL（§8.1）';
