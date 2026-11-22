-- 击球落点同打席重复行清理（2026-11-22，Issue #717）
--
-- 背景：赛后补录/改判会生成新的客户端 rid，服务端旧逻辑只按 (game_id, rid) 幂等，
-- 旧 rid 行未删除，导致同一打席出现多条落点。读取侧按“同一打席保留最新”后，
-- 生产 game 282 由 31 行收敛为 20 行，主队安打 1、客队 4，与官方技术统计一致。
--
-- 数据来源：2026-11-22 通过生产只读 API 全量扫描 193 场 /hit-spray/game/:id，
-- 按打席键（game_id + inning + half + batter_order + player_id + outs_before +
-- COALESCE(runners_before,'')，team_id 历史恒为 0 不参与）分组。
-- 受影响场次：202, 216, 251, 261, 282，共 15 组、18 条旧行。
--
-- 保留规则：每组保留 createdAt 最新（同时间取 id 最大）的行，软删其余活跃行。
-- 采用软删除（deleted_at），不物理删除、不建备份表，可随时回滚恢复；
-- 不使用 PL/pgSQL（DO $$），避免 Druid WallFilter 解析失败（沿用 V20260419100000 约定）。
-- 不新增唯一索引：历史数据尚未全量核验，且写入侧 JPA 落库顺序可能先 insert 后 update。
--
-- 已知差异（不是本次清理造成，不伪造数据）：
--   * 216 官方主队 H=7，原始落点主队 H=7 中 1 条为重复行，去重后为 6：另有 1 支原有安打从未记录落点。
--   * 202 主队官方 H=2，原始落点无主队安打：2 支原有安打缺少落点记录。
--
-- 迁移后校验（应返回 0 行）：
--   SELECT game_id, inning, half, batter_order, player_id, outs_before, COALESCE(runners_before,'')
--   FROM bs_hit_spray
--   WHERE deleted_at IS NULL AND game_id IN (202, 216, 251, 261, 282)
--   GROUP BY 1,2,3,4,5,6,7
--   HAVING COUNT(*) > 1;
-- 5 场活跃行数（202/216/251/261/282）应为 11/33/14/42/20，合计 120。
--
-- 回滚：
--   UPDATE bs_hit_spray
--   SET deleted_at = NULL, deleted_by = NULL, updated_at = NOW()
--   WHERE id IN (492,493,720,721,722,1294,1637,2068,2069,2070,2071,2072,2073,2074,2079,2080,2264,2267)
--     AND deleted_at IS NOT NULL;

WITH pairs(keep_id, delete_id) AS (
  VALUES
    (494, 492),
    (494, 493),
    (723, 720),
    (724, 721),
    (725, 722),
    (1296, 1294),
    (1726, 1637),
    (2259, 2068),
    (2260, 2069),
    (2261, 2070),
    (2262, 2071),
    (2263, 2072),
    (2265, 2073),
    (2265, 2264),
    (2266, 2074),
    (2268, 2079),
    (2268, 2267),
    (2269, 2080)
)
UPDATE bs_hit_spray d
SET deleted_at = NOW(),
    deleted_by = COALESCE((SELECT id FROM sys_user WHERE username = 'admin' AND deleted_at IS NULL LIMIT 1), 1),
    updated_at = NOW()
FROM pairs p, bs_hit_spray k
WHERE d.id = p.delete_id
  AND k.id = p.keep_id
  AND d.deleted_at IS NULL
  AND k.deleted_at IS NULL
  AND d.game_id = k.game_id
  AND d.inning IS NOT DISTINCT FROM k.inning
  AND d.half IS NOT DISTINCT FROM k.half
  AND d.batter_order IS NOT DISTINCT FROM k.batter_order
  AND d.player_id IS NOT DISTINCT FROM k.player_id
  AND d.outs_before IS NOT DISTINCT FROM k.outs_before
  AND COALESCE(d.runners_before, '') = COALESCE(k.runners_before, '');
