-- 历史首个守位回填（#446 关联，2026-10-29）
-- 仅填充 first_position 为空的行，不覆盖实时录入已写入的值；可重复执行。
-- 一次性回填：Flyway 在单事务内执行并更新大量历史行，建议低峰发布。
--
-- 数据来源：bs_game_substitution_event（2026-10-27 起记录换人/换位/换投/DH取消）。
-- 规则：取最早一条能确定守备位的事件：
--   SUBSTITUTION 换上 → in_position；换上投手/DH取消 → in_position 或 P；
--   攻方半局换上的代打/代跑不取 in_position（继承的是被换下者守位，不是防守事实）；
--   SUBSTITUTION 换下 → out_position；
--   POSITION_CHANGE / PITCHING_CHANGE → out_position；
--   由 DH/PH/EP 进场守备（out_position 不是守位）→ 该事件 in_position。
-- 同场有事件但事件未给出守位、且有守备证据的行 → 当前守位。
-- 无事件的旧场次保持 NULL，由前端回退 position 展示；不臆造首个守位。
-- 投手并行行复制同场同球员的球员行（二刀流不落成 P）；纯投手无球员行时补 P。

WITH event_position AS (
    SELECT s.id AS stat_id,
           e.sequence,
           e.change_seq,
           e.id AS event_id,
           UPPER(BTRIM(
               CASE
                   WHEN e.in_player_id = s.player_id AND e.change_type = 'SUBSTITUTION'
                        AND NOT (
                            (e.top_bottom = 'top' AND e.team_id = g.away_team_id)
                            OR (e.top_bottom = 'bottom' AND e.team_id = g.home_team_id)
                        )
                       THEN e.in_position
                   WHEN e.in_player_id = s.player_id
                        AND e.change_type IN ('PITCHING_CHANGE', 'DH_CANCEL')
                       THEN COALESCE(NULLIF(BTRIM(e.in_position), ''), 'P')
                   WHEN e.out_player_id = s.player_id
                        AND e.in_player_id = s.player_id
                        AND UPPER(BTRIM(COALESCE(e.out_position, ''))) IN ('DH', 'PH', 'EP')
                       THEN e.in_position
                   WHEN e.out_player_id = s.player_id
                       THEN e.out_position
               END)) AS pos
    FROM bs_game_player_stat s
    JOIN bs_game g ON g.id = s.game_id
    JOIN bs_game_substitution_event e
      ON e.game_id = s.game_id
     AND e.team_id = s.team_id
     AND e.deleted_at IS NULL
     AND (e.in_player_id = s.player_id OR e.out_player_id = s.player_id)
    WHERE s.deleted_at IS NULL
      AND s.first_position IS NULL
      AND COALESCE(s.is_pitcher, 0) = 0
      AND s.position IS NOT NULL
      AND UPPER(BTRIM(s.position)) NOT IN ('DH', 'PH', 'EP')
      AND (
        COALESCE(s.tc, 0) > 0 OR COALESCE(s.po, 0) > 0 OR COALESCE(s.a, 0) > 0
        OR COALESCE(s.e, 0) > 0 OR COALESCE(s.dp, 0) > 0 OR COALESCE(s.pb, 0) > 0
        OR COALESCE(s.catcher_cs, 0) > 0 OR COALESCE(s.catcher_sb, 0) > 0
        OR COALESCE(s.def_inn, 0) > 0 OR COALESCE(s.fielding_gs, 0) > 0
      )
),
first_fielding_event AS (
    SELECT DISTINCT ON (stat_id) stat_id, pos
    FROM event_position
    WHERE pos IN ('P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF')
    ORDER BY stat_id, sequence, change_seq, event_id
)
UPDATE bs_game_player_stat s
SET first_position = f.pos
FROM first_fielding_event f
WHERE s.id = f.stat_id
  AND s.first_position IS NULL;

-- 同场有事件但事件未给出守位：首个守位即当前守位（有守备证据才写）
UPDATE bs_game_player_stat s
SET first_position = UPPER(BTRIM(s.position))
WHERE s.deleted_at IS NULL
  AND s.first_position IS NULL
  AND COALESCE(s.is_pitcher, 0) = 0
  AND s.position IS NOT NULL
  AND UPPER(BTRIM(s.position)) IN ('P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF')
  AND (
    COALESCE(s.tc, 0) > 0 OR COALESCE(s.po, 0) > 0 OR COALESCE(s.a, 0) > 0
    OR COALESCE(s.e, 0) > 0 OR COALESCE(s.dp, 0) > 0 OR COALESCE(s.pb, 0) > 0
    OR COALESCE(s.catcher_cs, 0) > 0 OR COALESCE(s.catcher_sb, 0) > 0
    OR COALESCE(s.def_inn, 0) > 0 OR COALESCE(s.fielding_gs, 0) > 0
  )
  AND EXISTS (
    SELECT 1 FROM bs_game_substitution_event e
    WHERE e.game_id = s.game_id
      AND e.team_id = s.team_id
      AND e.deleted_at IS NULL
  );

-- 投手并行行：复制同场同球队同球员的球员行首个守位（二刀流不落成 P）
UPDATE bs_game_player_stat p
SET first_position = UPPER(BTRIM(b.first_position))
FROM bs_game_player_stat b
WHERE p.deleted_at IS NULL
  AND p.first_position IS NULL
  AND COALESCE(p.is_pitcher, 0) = 1
  AND COALESCE(b.is_pitcher, 0) = 0
  AND b.deleted_at IS NULL
  AND b.game_id = p.game_id
  AND b.team_id = p.team_id
  AND b.player_id = p.player_id
  AND b.first_position IS NOT NULL
  AND UPPER(BTRIM(b.first_position)) IN ('P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF');

-- 纯投手行（无球员行）：首个守位为 P（仅限有投球/守备证据的行）
UPDATE bs_game_player_stat p
SET first_position = 'P'
WHERE p.deleted_at IS NULL
  AND p.first_position IS NULL
  AND COALESCE(p.is_pitcher, 0) = 1
  AND (
    COALESCE(p.tc, 0) > 0 OR COALESCE(p.po, 0) > 0 OR COALESCE(p.a, 0) > 0
    OR COALESCE(p.e, 0) > 0 OR COALESCE(p.dp, 0) > 0 OR COALESCE(p.pb, 0) > 0
    OR COALESCE(p.catcher_cs, 0) > 0 OR COALESCE(p.catcher_sb, 0) > 0
    OR COALESCE(p.def_inn, 0) > 0 OR COALESCE(p.fielding_gs, 0) > 0
    OR COALESCE(p.ip, 0) > 0 OR COALESCE(p.np, 0) > 0 OR COALESCE(p.pitch_bf, 0) > 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM bs_game_player_stat b
    WHERE b.deleted_at IS NULL
      AND b.game_id = p.game_id
      AND b.team_id = p.team_id
      AND b.player_id = p.player_id
      AND COALESCE(b.is_pitcher, 0) = 0
  );
