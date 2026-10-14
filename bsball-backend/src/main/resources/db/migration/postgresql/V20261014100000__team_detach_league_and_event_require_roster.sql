-- 球队「去联盟归属」改造（Issue #318，2026-10-14）
-- ① bs_team.league_id 全量清空（含软删行）：球队不再隶属任何联盟；清理前先落备份表（回滚=按备份还原）
-- ② bs_event.require_roster：新建赛事"报名制"门槛（决策 11）——存量回填 false（照顾历史比赛），
--    新建赛事由实体默认 true 写入；至少 2 支"已确认且锁定名单"球队才能建比赛
-- 说明：league_id 列保留不删（实体字段保留但业务读写停止，防 ddl-auto 改列）。

-- ① 备份表（幂等：已存在则不再重建）
-- 先对 bs_team 取排他锁，保证「快照 → 清空」之间无并发写入（滚动发布期旧实例若新建带 league_id 的球队，
-- 会被 UPDATE 清空却不在快照里，回滚时无法还原）。EXCLUSIVE 仅阻塞写、放行读，迁移事务内自动释放。
LOCK TABLE bs_team IN EXCLUSIVE MODE;

CREATE TABLE IF NOT EXISTS bs_team_league_backup_20261014 AS
SELECT id, league_id, name, tenant_id, deleted_at, now() AS backed_up_at
FROM bs_team
WHERE league_id IS NOT NULL;

COMMENT ON TABLE bs_team_league_backup_20261014 IS '球队去联盟归属改造（#318）清理前备份：bs_team.league_id 快照，回滚按此表还原';

-- ① 清空归属（全量，含软删行）
UPDATE bs_team SET league_id = NULL WHERE league_id IS NOT NULL;

-- ② 赛事报名制标记（存量一律 false；新建由应用层默认 true）
ALTER TABLE bs_event ADD COLUMN IF NOT EXISTS require_roster BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN bs_event.require_roster IS '报名制：TRUE=至少2支已确认且锁定名单的球队才能建比赛（新建默认；存量回填 FALSE 照顾历史比赛）';
