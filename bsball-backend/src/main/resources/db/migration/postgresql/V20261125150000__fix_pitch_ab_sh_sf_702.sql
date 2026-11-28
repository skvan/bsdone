-- Issue #702：投手自由击球数（pitch_ab）历史值回填
--
-- 口径：AB = max(0, BF − BB − HBP − SH − SF)，BB 含 IBB（不额外单减）；
--       旧数据分项缺失时回退 BB+HBP 合计 pitch_bb_hp（与 PlayerStatsMapper.pitchAbFromBfCol 完全一致）。
--
-- 回填规则（只改可判定为“自动推算”的库值）：
--   1) 库值等于旧公式（BF − BB − HBP，不减 SH/SF）且本行 SH/SF 存在 → 视为历史自动值，按新公式回填；
--   2) 库值为 0 但新公式 > 0 → 视为旧默认值/旧口径产物，按新公式回填；
--   3) 其余非空值 → 视为人工覆写，保留不动；
--   4) NULL → 不动（查询期仍按新公式推算，不在此强制落库）。
--
-- 人工值边界（已知局限）：库表没有持久的人工标记，无法完全区分“历史自动值”和“恰好等于旧公式的人工值”；
-- 后者会被规则 1 修正。规则 2 同理无法排除“人工明确填 0”的极少数场景，已由用户拍板接受。
--
-- 注意：
--   * 本迁移不可自动回滚。执行前请备份 bs_game_player_stat（至少 pitch_ab/pitch_bf/pitch_bb/pitch_hbp/
--     pitch_bb_hp/pitch_sh/pitch_sf 列），必要时用备份恢复；
--   * 生产执行前建议先跑下方只读审计 SQL 评估影响行数并抽样核对。
--
-- 只读审计（默认注释，不执行）：
-- SELECT COUNT(*) AS legacy_sh_sf_rows
--   FROM bs_game_player_stat
--  WHERE pitch_ab IS NOT NULL
--    AND pitch_ab = GREATEST(0, COALESCE(pitch_bf,0) - (CASE WHEN COALESCE(pitch_bb,0) + COALESCE(pitch_hbp,0) > 0 THEN COALESCE(pitch_bb,0) ELSE GREATEST(COALESCE(pitch_bb_hp,0) - COALESCE(pitch_hbp,0), 0) END) - COALESCE(pitch_hbp,0))
--    AND (COALESCE(pitch_sh,0) <> 0 OR COALESCE(pitch_sf,0) <> 0);
-- SELECT COUNT(*) AS zero_default_rows
--   FROM bs_game_player_stat
--  WHERE pitch_ab = 0
--    AND GREATEST(0, COALESCE(pitch_bf,0) - (CASE WHEN COALESCE(pitch_bb,0) + COALESCE(pitch_hbp,0) > 0 THEN COALESCE(pitch_bb,0) ELSE GREATEST(COALESCE(pitch_bb_hp,0) - COALESCE(pitch_hbp,0), 0) END) - COALESCE(pitch_hbp,0) - COALESCE(pitch_sh,0) - COALESCE(pitch_sf,0)) > 0;
-- SELECT s.id, s.game_id, s.player_id, s.pitch_ab, s.pitch_bf, s.pitch_bb, s.pitch_hbp, s.pitch_bb_hp, s.pitch_sh, s.pitch_sf
--   FROM bs_game_player_stat s
--  WHERE s.pitch_ab IS NOT NULL
--    AND (
--          (s.pitch_ab = GREATEST(0, COALESCE(s.pitch_bf,0) - (CASE WHEN COALESCE(s.pitch_bb,0) + COALESCE(s.pitch_hbp,0) > 0 THEN COALESCE(s.pitch_bb,0) ELSE GREATEST(COALESCE(s.pitch_bb_hp,0) - COALESCE(s.pitch_hbp,0), 0) END) - COALESCE(s.pitch_hbp,0))
--           AND (COALESCE(s.pitch_sh,0) <> 0 OR COALESCE(s.pitch_sf,0) <> 0))
--       OR (s.pitch_ab = 0
--           AND GREATEST(0, COALESCE(s.pitch_bf,0) - (CASE WHEN COALESCE(s.pitch_bb,0) + COALESCE(s.pitch_hbp,0) > 0 THEN COALESCE(s.pitch_bb,0) ELSE GREATEST(COALESCE(s.pitch_bb_hp,0) - COALESCE(s.pitch_hbp,0), 0) END) - COALESCE(s.pitch_hbp,0) - COALESCE(s.pitch_sh,0) - COALESCE(s.pitch_sf,0)) > 0)
--        )
--  ORDER BY s.game_id, s.player_id
--  LIMIT 50;

UPDATE bs_game_player_stat
   SET pitch_ab = GREATEST(0, COALESCE(pitch_bf,0) - (CASE WHEN COALESCE(pitch_bb,0) + COALESCE(pitch_hbp,0) > 0 THEN COALESCE(pitch_bb,0) ELSE GREATEST(COALESCE(pitch_bb_hp,0) - COALESCE(pitch_hbp,0), 0) END) - COALESCE(pitch_hbp,0) - COALESCE(pitch_sh,0) - COALESCE(pitch_sf,0))
 WHERE pitch_ab IS NOT NULL
   AND (
        (pitch_ab = GREATEST(0, COALESCE(pitch_bf,0) - (CASE WHEN COALESCE(pitch_bb,0) + COALESCE(pitch_hbp,0) > 0 THEN COALESCE(pitch_bb,0) ELSE GREATEST(COALESCE(pitch_bb_hp,0) - COALESCE(pitch_hbp,0), 0) END) - COALESCE(pitch_hbp,0))
         AND (COALESCE(pitch_sh,0) <> 0 OR COALESCE(pitch_sf,0) <> 0))
     OR (pitch_ab = 0
         AND GREATEST(0, COALESCE(pitch_bf,0) - (CASE WHEN COALESCE(pitch_bb,0) + COALESCE(pitch_hbp,0) > 0 THEN COALESCE(pitch_bb,0) ELSE GREATEST(COALESCE(pitch_bb_hp,0) - COALESCE(pitch_hbp,0), 0) END) - COALESCE(pitch_hbp,0) - COALESCE(pitch_sh,0) - COALESCE(pitch_sf,0)) > 0)
       );
