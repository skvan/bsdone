-- =====================================================================
-- 放宽 bs_earned_run_decision.runner_origin 约束
-- ---------------------------------------------------------------------
-- 背景：判定引擎支持 RunnerOrigin 全枚举（NORMAL/ERROR/TIE_BREAK/
--       WILD_PITCH/PASSED_BALL/INTERFERENCE），而旧约束仅允许前三者，
--       暴投/捕逸/妨碍来源的合法判定落库时会违反约束，导致整个半局重建失败
--       （比赛 226 实测：PASSED_BALL 等来源一旦出现即 500）。
-- 说明：与 #149（投手 ER/UER 重建引擎）二阶段的“修正 runner_origin
--       constraint”方向一致；若后续迁移再次重定义该约束，以后者为准。
-- =====================================================================

ALTER TABLE IF EXISTS bs_earned_run_decision
    DROP CONSTRAINT IF EXISTS ck_earned_run_decision_origin;

ALTER TABLE IF EXISTS bs_earned_run_decision
    ADD CONSTRAINT ck_earned_run_decision_origin
        CHECK (runner_origin IN ('NORMAL', 'ERROR', 'TIE_BREAK', 'WILD_PITCH', 'PASSED_BALL', 'INTERFERENCE'));
