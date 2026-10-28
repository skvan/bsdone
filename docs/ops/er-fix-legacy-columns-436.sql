\set ON_ERROR_STOP on

-- 一次性生产修复脚本（Issue #436，2026-10-26 已执行）：
--   备份：/root/er-repair-436-20261025/stat-before-legacy-sync-20261026.dump
--         库内 er_repair_436_legacy_20261026.stat（53 行）
--   结果：36 行更新；53 行旧列不变量、24 个球队口径、12 场公开 API 全部通过。
--   注意：重跑会因 0 行更新触发断言中止，不要在生产重复执行。
--
-- 补修 12 场历史比赛投手统计的“旧列”：
--   pitchr / unearnedr / pendingr 是比赛详情 JPA 实体实际读写的列；
--   pitch_r / unearned_r / pending_r 是 2026-10-25 修复脚本写入的列，
--   已经按逐分判定校验过，但详情页不读取（聚合 Mapper 仍读部分新列）。
-- 本脚本把已验证的新列值同步到旧列，并在事务内断言。
--
-- 用法：
--   dry-run: psql -v do_commit=false -f er-fix-legacy-columns-436.sql
--   apply:   psql -v do_commit=true  -f er-fix-legacy-columns-436.sql
--
-- 回滚：备份表 er_repair_436_legacy_20261026.stat（53 行）
--   UPDATE bs_game_player_stat s
--      SET pitchr = b.pitchr, unearnedr = b.unearnedr, pendingr = b.pendingr
--     FROM er_repair_436_legacy_20261026.stat b
--    WHERE s.id = b.id;

BEGIN;

DO $$
DECLARE
  v_n integer;
  v_rows integer;
BEGIN
  -- ---------- 写前条件 ----------
  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL;
  IF v_n <> 53 THEN
    RAISE EXCEPTION 'expected 53 pitcher rows, got %', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND pitch_r IS NULL;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'new pitch_r has % null rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND pitch_r <> er + COALESCE(unearned_r, 0) + COALESCE(pending_r, 0);
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'new-column invariant failed on % rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND er > pitch_r;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'new-column ER > R on % rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM (SELECT DISTINCT game_id, team_id
            FROM bs_game_player_stat
           WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
             AND is_pitcher = 1
             AND deleted_at IS NULL) t;
  IF v_n <> 24 THEN
    RAISE EXCEPTION 'expected 24 team-game pairs, got %', v_n;
  END IF;

  -- ---------- 同步旧列 ----------
  UPDATE bs_game_player_stat
     SET pitchr = pitch_r,
         unearnedr = unearned_r,
         pendingr = pending_r
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND (pitchr IS DISTINCT FROM pitch_r
          OR unearnedr IS DISTINCT FROM unearned_r
          OR pendingr IS DISTINCT FROM pending_r);
  GET DIAGNOSTICS v_rows = ROW_COUNT;
  IF v_rows <> 36 THEN
    RAISE EXCEPTION 'expected 36 rows updated, got %', v_rows;
  END IF;

  -- ---------- 写后断言 ----------
  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL;
  IF v_n <> 53 THEN
    RAISE EXCEPTION 'row count changed to %', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND (pitchr IS DISTINCT FROM pitch_r
          OR unearnedr IS DISTINCT FROM unearned_r
          OR pendingr IS DISTINCT FROM pending_r);
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'legacy columns still differ on % rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND (pitchr IS NULL
          OR pitchr <> er + COALESCE(unearnedr, 0) + COALESCE(pendingr, 0));
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'legacy invariant failed on % rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM bs_game_player_stat
   WHERE game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
     AND is_pitcher = 1
     AND deleted_at IS NULL
     AND er > pitchr;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'legacy ER > R on % rows', v_n;
  END IF;

  SELECT count(*) INTO v_n
    FROM (
      SELECT s.game_id, s.team_id, SUM(s.pitchr) AS r
        FROM bs_game_player_stat s
       WHERE s.game_id IN (236,237,239,242,243,244,245,247,250,251,253,254)
         AND s.is_pitcher = 1
         AND s.deleted_at IS NULL
       GROUP BY s.game_id, s.team_id
    ) x
    JOIN bs_game g ON g.id = x.game_id
   WHERE x.r <> CASE WHEN x.team_id = g.home_team_id THEN g.away_score ELSE g.home_score END;
  IF v_n <> 0 THEN
    RAISE EXCEPTION 'team pitching runs do not match opponent score on % team-games', v_n;
  END IF;

  -- 245 场精确值
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
  IF NOT EXISTS (
    SELECT 1 FROM bs_game_player_stat
     WHERE game_id = 245 AND player_id = 1338 AND is_pitcher = 1
       AND deleted_at IS NULL
       AND pitchr = 4 AND er = 3 AND unearnedr = 1 AND pendingr = 0
  ) THEN
    RAISE EXCEPTION '245/1338 unexpected values';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM bs_game_player_stat
     WHERE game_id = 245 AND player_id = 1135 AND is_pitcher = 1
       AND deleted_at IS NULL
       AND pitchr = 1 AND er = 1 AND unearnedr = 0 AND pendingr = 0
  ) THEN
    RAISE EXCEPTION '245/1135 unexpected values';
  END IF;

  RAISE NOTICE 'legacy sync passed: % rows updated', v_rows;
END $$;

\if :do_commit
COMMIT;
\else
ROLLBACK;
\endif
