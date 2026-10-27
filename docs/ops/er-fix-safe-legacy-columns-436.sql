\set ON_ERROR_STOP on

-- 一次性生产修复脚本（Issue #436，2026-10-26 已执行）：
--   结果：101 个球队口径 / 200 个投手行，147 行更新；200 行断言、101 个球队合计、
--         12 场既有修复与 245 精确值全部通过。
--   备份：er_repair_436_safe_20261026.stat（200 行）
--         /root/er-repair-436-20261025/stat-before-safe-legacy-sync-20261026.dump
--   注意：重跑会因 0 行更新触发断言中止，不要在生产重复执行。
--
-- 一次性安全补修（Issue #436，2026-10-26）：
--   适用范围：已完赛、且球队所有投手的 ER + UER + P 正好等于对手得分的球队口径。
--   这批球队的每一分都有归属，因此可以把每个投手的 R 绝对覆盖成 ER+UER+P。
--   共 101 个球队口径（70 场）、200 个投手行；其中 147 行需要更新（12 场 53 行已对平）。
--   同时同步旧列与新列，避免详情页和聚合 Mapper 再次漂移。
--
-- 用法：
--   dry-run: psql -v do_commit=false -f er-fix-safe-legacy-columns-436.sql
--   apply:   psql -v do_commit=true  -f er-fix-safe-legacy-columns-436.sql
--
-- 备份：er_repair_436_safe_20261026.stat（200 行）
--   /root/er-repair-436-20261025/stat-before-safe-legacy-sync-20261026.dump
-- 回滚：
--   UPDATE bs_game_player_stat s
--      SET pitchr=b.pitchr, unearnedr=b.unearnedr, pendingr=b.pendingr,
--          pitch_r=b.pitch_r, unearned_r=b.unearned_r, pending_r=b.pending_r
--     FROM er_repair_436_safe_20261026.stat b
--    WHERE s.id=b.id;

BEGIN;

CREATE TEMP TABLE tmp_safe_targets ON COMMIT DROP AS
WITH teamg AS (
    SELECT s.game_id, s.team_id
      FROM bs_game_player_stat s
      JOIN bs_game g ON g.id = s.game_id
     WHERE s.is_pitcher = 1
       AND s.deleted_at IS NULL
     GROUP BY s.game_id, s.team_id, g.home_team_id, g.away_score, g.home_score, g.game_end_time
    HAVING count(er) = count(*)
       AND sum(COALESCE(er, 0)) + sum(COALESCE(unearnedr, 0)) + sum(COALESCE(pendingr, 0))
           = CASE WHEN s.team_id = g.home_team_id THEN g.away_score ELSE g.home_score END
       AND g.game_end_time IS NOT NULL
)
SELECT s.id,
       s.game_id,
       s.team_id,
       s.player_id,
       s.er,
       COALESCE(s.unearnedr, 0) AS target_uer,
       COALESCE(s.pendingr, 0) AS target_p,
       COALESCE(s.er, 0) + COALESCE(s.unearnedr, 0) + COALESCE(s.pendingr, 0) AS target_r
  FROM bs_game_player_stat s
  JOIN teamg t ON t.game_id = s.game_id AND t.team_id = s.team_id
 WHERE s.is_pitcher = 1
   AND s.deleted_at IS NULL;

DO $$
DECLARE
  v_n integer;
  v_rows integer;
BEGIN
  SELECT count(*) INTO v_n FROM tmp_safe_targets;
  IF v_n <> 200 THEN
    RAISE EXCEPTION 'expected 200 targets, got %', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM (SELECT DISTINCT game_id, team_id FROM tmp_safe_targets) x;
  IF v_n <> 101 THEN
    RAISE EXCEPTION 'expected 101 team-games, got %', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM tmp_safe_targets t
    LEFT JOIN er_repair_436_safe_20261026.stat b ON b.id = t.id
   WHERE b.id IS NULL;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'backup missing % targets', v_n;
  END IF;

  UPDATE bs_game_player_stat s
     SET pitchr = t.target_r,
         unearnedr = t.target_uer,
         pendingr = t.target_p,
         pitch_r = t.target_r,
         unearned_r = t.target_uer,
         pending_r = t.target_p
    FROM tmp_safe_targets t
   WHERE s.id = t.id
     AND (s.pitchr IS DISTINCT FROM t.target_r
          OR s.unearnedr IS DISTINCT FROM t.target_uer
          OR s.pendingr IS DISTINCT FROM t.target_p
          OR s.pitch_r IS DISTINCT FROM t.target_r
          OR s.unearned_r IS DISTINCT FROM t.target_uer
          OR s.pending_r IS DISTINCT FROM t.target_p);
  GET DIAGNOSTICS v_rows = ROW_COUNT;
  IF v_rows <> 147 THEN
    RAISE EXCEPTION 'expected 147 rows updated, got %', v_rows;
  END IF;

  SELECT count(*) INTO v_n
    FROM tmp_safe_targets t
    JOIN bs_game_player_stat s ON s.id = t.id
   WHERE s.pitchr IS DISTINCT FROM t.target_r
      OR s.unearnedr IS DISTINCT FROM t.target_uer
      OR s.pendingr IS DISTINCT FROM t.target_p
      OR s.pitch_r IS DISTINCT FROM t.target_r
      OR s.unearned_r IS DISTINCT FROM t.target_uer
      OR s.pending_r IS DISTINCT FROM t.target_p;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'target fields differ after update on % rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM tmp_safe_targets t
    JOIN bs_game_player_stat s ON s.id = t.id
   WHERE s.pitchr <> s.er + COALESCE(s.unearnedr, 0) + COALESCE(s.pendingr, 0)
      OR s.pitch_r <> s.er + COALESCE(s.unearned_r, 0) + COALESCE(s.pending_r, 0)
      OR s.er > s.pitchr
      OR s.er > s.pitch_r;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'invariant failed on % rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM (
      SELECT t.game_id, t.team_id,
             sum(s.pitchr) AS r_legacy,
             sum(s.pitch_r) AS r_new
        FROM tmp_safe_targets t
        JOIN bs_game_player_stat s ON s.id = t.id
       GROUP BY t.game_id, t.team_id
    ) x
    JOIN bs_game g ON g.id = x.game_id
   WHERE x.r_legacy <> CASE WHEN x.team_id = g.home_team_id THEN g.away_score ELSE g.home_score END
      OR x.r_new <> CASE WHEN x.team_id = g.home_team_id THEN g.away_score ELSE g.home_score END;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'team totals mismatch on % team-games', v_n;
  END IF;

  -- 12 场已补修数据不得漂移
  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND (pitchr IS DISTINCT FROM pitch_r
          OR pitchr <> er + COALESCE(unearnedr, 0) + COALESCE(pendingr, 0)
          OR er > pitchr);
  IF v_n <> 0 THEN
    RAISE EXCEPTION '12-game repaired rows drifted on % rows', v_n;
  END IF;

  -- 245 精确值
  IF NOT EXISTS (
    SELECT 1 FROM bs_game_player_stat
     WHERE game_id = 245 AND player_id = 1324 AND is_pitcher = 1
       AND deleted_at IS NULL
       AND pitchr = 9 AND er = 9 AND unearnedr = 0 AND pendingr = 0
  ) THEN
    RAISE EXCEPTION '245/1324 unexpected values';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM bs_game_player_stat
     WHERE game_id = 245 AND player_id = 1358 AND is_pitcher = 1
       AND deleted_at IS NULL
       AND pitchr = 5 AND er = 5 AND unearnedr = 0 AND pendingr = 0
  ) THEN
    RAISE EXCEPTION '245/1358 unexpected values';
  END IF;

  RAISE NOTICE 'safe legacy sync passed: % targets, % rows updated', 200, v_rows;
END $$;

\if :do_commit
COMMIT;
\else
ROLLBACK;
\endif
