-- ER 历史数据只读重算核对（Issue #149/#204）
--
-- 作用：用 bs_earned_run_decision 的逐分判定，计算每个投手应得的
--       R / ER / UER / PENDING，并与 bs_game_player_stat 的库内值对比。
-- 安全：只读事务（BEGIN TRANSACTION READ ONLY + ROLLBACK），不含任何写语句。
--
-- 用法（连接参数由环境注入，勿写入仓库）：
--   PGPASSWORD="$PG_PASSWORD" psql -h "$PROD_HOST" -U "$PG_USER" -d "$PG_DB" \
--     -v ON_ERROR_STOP=1 -f docs/ops/er-audit-149.sql > er-audit.out
--
-- 输出列：
--   calc_*   = 由 bs_earned_run_decision 重算的值（没有判定时为 0）
--   stored_* = bs_game_player_stat 当前值
--   verdict  = OK / RECALCULABLE_DIFF / ER_GT_R / PENDING_REVIEW /
--              NO_DECISIONS / MISSING_STAT_ROW
--
-- 注意：没有 bs_earned_run_play 的 legacy 场次不会出现在明细里，见文件末尾查询。

BEGIN TRANSACTION READ ONLY;

WITH decisions AS (
    SELECT
        d.game_id,
        d.responsible_pitcher_id AS pitcher_id,
        COUNT(*) AS calc_runs,
        COUNT(*) FILTER (WHERE d.earned_status = 'EARNED')   AS calc_er,
        COUNT(*) FILTER (WHERE d.earned_status = 'UNEARNED') AS calc_uer,
        COUNT(*) FILTER (WHERE d.earned_status = 'PENDING')  AS calc_pending
    FROM bs_earned_run_decision d
    GROUP BY d.game_id, d.responsible_pitcher_id
),
engine_games AS (
    SELECT DISTINCT p.game_id
    FROM bs_earned_run_play p
),
stats AS (
    SELECT
        s.game_id,
        s.player_id AS pitcher_id,
        s.pitch_r AS stored_runs,
        s.er AS stored_er,
        s.unearned_r AS stored_uer,
        s.pending_r AS stored_pending
    FROM bs_game_player_stat s
    WHERE s.is_pitcher = 1
      AND s.deleted_at IS NULL
),
pitchers AS (
    SELECT game_id, pitcher_id FROM decisions
    UNION
    SELECT game_id, pitcher_id FROM stats
),
detail AS (
    SELECT
        g.id AS game_id,
        g.gameday,
        g.home_team_id,
        g.away_team_id,
        p.pitcher_id,
        COALESCE(d.calc_runs, 0) AS calc_runs,
        COALESCE(d.calc_er, 0) AS calc_er,
        COALESCE(d.calc_uer, 0) AS calc_uer,
        COALESCE(d.calc_pending, 0) AS calc_pending,
        s.stored_runs,
        s.stored_er,
        s.stored_uer,
        s.stored_pending,
        CASE
            WHEN s.pitcher_id IS NULL THEN 'MISSING_STAT_ROW'
            WHEN s.stored_er IS NOT NULL AND s.stored_runs IS NOT NULL
                 AND s.stored_er > s.stored_runs THEN 'ER_GT_R'
            WHEN d.pitcher_id IS NULL THEN 'NO_DECISIONS'
            WHEN s.stored_runs IS DISTINCT FROM COALESCE(d.calc_runs, 0)
                 OR s.stored_er IS DISTINCT FROM COALESCE(d.calc_er, 0)
                 OR s.stored_uer IS DISTINCT FROM COALESCE(d.calc_uer, 0)
                 OR s.stored_pending IS DISTINCT FROM COALESCE(d.calc_pending, 0)
                 THEN 'RECALCULABLE_DIFF'
            WHEN COALESCE(d.calc_pending, 0) > 0 THEN 'PENDING_REVIEW'
            ELSE 'OK'
        END AS verdict
    FROM engine_games eg
    JOIN bs_game g ON g.id = eg.game_id AND g.deleted_at IS NULL
    JOIN pitchers p ON p.game_id = eg.game_id
    LEFT JOIN decisions d
           ON d.game_id = p.game_id
          AND d.pitcher_id = p.pitcher_id
    LEFT JOIN stats s
           ON s.game_id = p.game_id
          AND s.pitcher_id = p.pitcher_id
)
SELECT *
FROM detail
ORDER BY game_id DESC, pitcher_id;

-- 汇总：按场统计 verdict
WITH decisions AS (
    SELECT d.game_id, d.responsible_pitcher_id AS pitcher_id,
           COUNT(*) AS runs,
           COUNT(*) FILTER (WHERE d.earned_status = 'EARNED')   AS er,
           COUNT(*) FILTER (WHERE d.earned_status = 'UNEARNED') AS uer,
           COUNT(*) FILTER (WHERE d.earned_status = 'PENDING')  AS pending
    FROM bs_earned_run_decision d
    GROUP BY d.game_id, d.responsible_pitcher_id
),
engine_games AS (
    SELECT DISTINCT p.game_id FROM bs_earned_run_play p
),
stats AS (
    SELECT s.game_id, s.player_id AS pitcher_id,
           s.pitch_r AS stored_runs, s.er AS stored_er,
           s.unearned_r AS stored_uer, s.pending_r AS stored_pending
    FROM bs_game_player_stat s
    WHERE s.is_pitcher = 1
      AND s.deleted_at IS NULL
),
pitchers AS (
    SELECT game_id, pitcher_id FROM decisions
    UNION
    SELECT game_id, pitcher_id FROM stats
),
detail AS (
    SELECT
        g.id AS game_id,
        p.pitcher_id,
        CASE
            WHEN s.pitcher_id IS NULL THEN 'MISSING_STAT_ROW'
            WHEN s.stored_er IS NOT NULL AND s.stored_runs IS NOT NULL
                 AND s.stored_er > s.stored_runs THEN 'ER_GT_R'
            WHEN d.pitcher_id IS NULL THEN 'NO_DECISIONS'
            WHEN s.stored_runs IS DISTINCT FROM COALESCE(d.runs, 0)
                 OR s.stored_er IS DISTINCT FROM COALESCE(d.er, 0)
                 OR s.stored_uer IS DISTINCT FROM COALESCE(d.uer, 0)
                 OR s.stored_pending IS DISTINCT FROM COALESCE(d.pending, 0)
                 THEN 'RECALCULABLE_DIFF'
            WHEN COALESCE(d.pending, 0) > 0 THEN 'PENDING_REVIEW'
            ELSE 'OK'
        END AS verdict
    FROM engine_games eg
    JOIN bs_game g ON g.id = eg.game_id AND g.deleted_at IS NULL
    JOIN pitchers p ON p.game_id = eg.game_id
    LEFT JOIN decisions d
           ON d.game_id = p.game_id
          AND d.pitcher_id = p.pitcher_id
    LEFT JOIN stats s
           ON s.game_id = p.game_id
          AND s.pitcher_id = p.pitcher_id
)
SELECT game_id,
       COUNT(*) AS pitchers,
       COUNT(*) FILTER (WHERE verdict = 'OK') AS ok,
       COUNT(*) FILTER (WHERE verdict <> 'OK') AS bad,
       STRING_AGG(DISTINCT verdict, ',' ORDER BY verdict) AS verdicts
FROM detail
GROUP BY game_id
ORDER BY game_id DESC;

-- 无任何 ER 引擎事件的场次（legacy；无法用判定重算，只能人工核对）
SELECT g.id AS game_id, g.gameday, g.home_team_id, g.away_team_id
FROM bs_game g
WHERE g.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM bs_earned_run_play p
      WHERE p.game_id = g.id
  )
ORDER BY g.id DESC;

ROLLBACK;
