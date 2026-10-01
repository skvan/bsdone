-- 账号权限重构（批次 3b 收敛 / #154）：唯一约束重构（部分唯一索引）
--
-- 背景（批 3a 质量评审沉淀①⑤）：
--   ① bs_team_manager 现有约束 ux_bs_team_manager_team_user 为「非部分」唯一键（team_id, user_id）——
--      remove（软删 deleted_at）后对同一 (team_id, user_id) 再 assign：判存 deleted_at IS NULL 看不到软删行，
--      于是 INSERT 撞非部分唯一键 → 23505 → 500。修复：改为「部分唯一索引」（仅 deleted_at IS NULL 生效）。
--   ⑤ bs_player.user_id 无唯一约束（createSelfProfile 与邀请并发路径均可能双建档）——
--      与「一人一档」语义对齐，加部分唯一索引（user_id IS NOT NULL AND deleted_at IS NULL）。
--
-- 幂等：DROP CONSTRAINT IF EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS；禁用 DO $$ 块
--   （空库时表结构由实体 ddl-auto 兜底，本脚本仅做约束口径校正）。
--
-- ============================================================================
-- 存量预检 SQL（本机冒烟前由控制器执行；任一查询返回非空 = 存在历史重复，须先人工去重再迁移，
-- 否则 CREATE UNIQUE INDEX 会因 23505 失败）：
--
-- ① bs_team_manager 存量重复（同一 team_id + user_id，且两侧均未软删）：
--   SELECT team_id, user_id, COUNT(*) AS cnt
--   FROM bs_team_manager
--   WHERE deleted_at IS NULL
--   GROUP BY team_id, user_id
--   HAVING COUNT(*) > 1;
--
-- ② bs_player 存量重复（同一 user_id 非空，且均未软删）：
--   SELECT user_id, COUNT(*) AS cnt
--   FROM bs_player
--   WHERE user_id IS NOT NULL AND deleted_at IS NULL
--   GROUP BY user_id
--   HAVING COUNT(*) > 1;
-- ============================================================================

-- ① bs_team_manager：去掉非部分唯一键，改立部分唯一索引（软删行不参与唯一性判定）
ALTER TABLE bs_team_manager DROP CONSTRAINT IF EXISTS ux_bs_team_manager_team_user;

CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_team_manager_team_user_active
    ON bs_team_manager (team_id, user_id)
    WHERE deleted_at IS NULL;

COMMENT ON INDEX ux_bs_team_manager_team_user_active IS '球队负责人部分唯一：同队同人仅一条未软删记录（软删后可重新 assign）';

-- ② bs_player：一人一档（部分唯一：user_id 非空且未软删）
CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_player_user_active
    ON bs_player (user_id)
    WHERE user_id IS NOT NULL AND deleted_at IS NULL;

COMMENT ON INDEX ux_bs_player_user_active IS '球员档案一人一档：同 user_id 仅一条未软删档案';
