-- ============================================================
-- 诊断脚本（Issue #447，比赛 237 投手 ER 归属不对）
-- 只读：不改任何数据。用来判断 237 属于哪种数据形态，再决定修法。
-- 用法：psql -v game_id=237 -f diagnose-447-game237.sql
--       （脚本内已把 gid 设为 237；要查别的比赛改 \set gid 那一行）
-- ============================================================

\set ON_ERROR_STOP off
\set gid 237

-- [1] 比赛基本信息：状态、租户、主客队、时间（bs_game 用 game_time/game_end_time，无 started_at）
SELECT id AS game_id, tenant_id, home_team_id, away_team_id, status, game_mode, game_time, game_end_time
FROM bs_game
WHERE id = :gid;

-- [2] 是否有 ER 引擎数据（hasEngineData 的判据 = play 表有行）
SELECT
  (SELECT count(*) FROM bs_earned_run_play     WHERE game_id = :gid) AS engine_play_rows,
  (SELECT count(*) FROM bs_earned_run_decision WHERE game_id = :gid) AS engine_decision_rows;

-- [3] 投手统计行现状（引擎唯一写入的列）：看 er 是否明显偏低/为 0
SELECT
  s.player_id,
  p.name            AS player_name,
  s.is_pitcher,
  s.pitch_r         AS r,
  s.er,
  s.unearned_r      AS uer,
  s.pending_r,
  s.outs_recorded,
  s.ip
FROM bs_game_player_stat s
LEFT JOIN bs_player p ON p.id = s.player_id
WHERE s.game_id = :gid AND s.is_pitcher = 1
ORDER BY s.player_id;

-- [4] 引擎侧从 decisions 聚合出的“每投手自责分”（真相，应与 [3] 的 er 对齐）
--     EARNED→er，UNEARNED→uer，PENDING→pending
SELECT
  d.responsible_pitcher_id                     AS pitcher_id,
  p.name                                       AS pitcher_name,
  count(*)                                     AS runs,
  count(*) FILTER (WHERE d.earned_status='EARNED')   AS earned,
  count(*) FILTER (WHERE d.earned_status='UNEARNED') AS unearned,
  count(*) FILTER (WHERE d.earned_status='PENDING')  AS pending
FROM bs_earned_run_decision d
LEFT JOIN bs_player p ON p.id = d.responsible_pitcher_id
WHERE d.game_id = :gid
GROUP BY d.responsible_pitcher_id, p.name
ORDER BY d.responsible_pitcher_id;

-- [5] 差异对照：[3] 存的 er vs [4] 引擎算的 earned，逐投手对齐
--     若 earned>0 但统计 er=0（如朱哲彬），说明是“有决策但没同步到统计行”→重跑 reconstruct 可自愈
WITH engine AS (
  SELECT responsible_pitcher_id AS pid,
         count(*) FILTER (WHERE earned_status='EARNED')   AS er_calc,
         count(*) FILTER (WHERE earned_status='UNEARNED') AS uer_calc,
         count(*) FILTER (WHERE earned_status='PENDING')  AS p_calc
  FROM bs_earned_run_decision WHERE game_id = :gid
  GROUP BY responsible_pitcher_id
)
SELECT
  s.player_id, p.name AS pitcher_name,
  s.er AS stored_er, coalesce(e.er_calc,0) AS engine_er,
  s.unearned_r AS stored_uer, coalesce(e.uer_calc,0) AS engine_uer,
  (s.er <> coalesce(e.er_calc,0)) AS er_mismatch
FROM bs_game_player_stat s
LEFT JOIN bs_player p ON p.id = s.player_id
LEFT JOIN engine e ON e.pid = s.player_id
WHERE s.game_id = :gid AND s.is_pitcher = 1
ORDER BY er_mismatch DESC, s.player_id;

-- [6] 责任投手归属明细：逐跑者看是谁上垒、谁负责（核对 H 队 康杰克/钱乾 拆分）
SELECT
  pl.inning, pl.half, pl.sequence,
  d.runner_id,  rp.name AS runner_name,
  d.responsible_pitcher_id, pp.name AS resp_pitcher,
  d.reached_base_pitcher_id, rb.name AS reach_pitcher,
  d.runner_origin, d.earned_status, d.unearned_reason
FROM bs_earned_run_decision d
LEFT JOIN bs_earned_run_play pl ON pl.id = d.play_id
LEFT JOIN bs_player rp ON rp.id = d.runner_id
LEFT JOIN bs_player pp ON pp.id = d.responsible_pitcher_id
LEFT JOIN bs_player rb ON rb.id = d.reached_base_pitcher_id
WHERE d.game_id = :gid
ORDER BY pl.inning, pl.half, pl.sequence, d.runner_id;

-- ============================================================
-- 解读指引（跑完把 [1]-[6] 结果回传）：
--  A. [2] engine_play_rows=0 → 237 是纯旧数据（引擎从没跑过）
--        单靠 reconstruct 端点无法重建（缺 play 输入），需前端重放 playEvents 或走 ops SQL 修
--  B. [2] 有 play/decision 且 [5] 显示 stored_er≠engine_er → 统计行没同步
--        直接触发 reconstruct 重算即可自愈（最理想）
--  C. [4]/[6] responsible_pitcher_id 分错人 → 责任归属输入本身就错
--        需 D4 覆写 UI 人工逐笔改，或前端重录
--  D. [6] reached_base_pitcher_id 为空 → 237 早于 D5 上线，无“上垒投手”审计列，只能靠 responsible 推断
-- ============================================================
