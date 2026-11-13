-- 击球落点重复行清理（2026-11-10）
--
-- 数据来源：通过生产只读 API 全量扫描 177 场比赛的 /hit-spray/game/:id，
-- 按内容键（比赛/球队/局/半局/棒次/球员/结果/BIP/打点/坐标/区域/深度/出局数/垒况）
-- 分组发现 33 组重复（每组 2 行），删除每组 id 较大的一笔，共 33 行。
-- 影响场次：207, 228, 233, 251, 253, 255, 257, 260, 261, 270, 271。
--
-- 安全措施：
--   1) 删除前把待删行完整备份到 bs_hit_spray_dedupe_backup_20261110；
--   2) 删除时要求保留行仍存在、未软删，且 15 个内容字段与待删行完全一致；
--   3) Flyway 事务失败自动回滚；备份表可用于人工复核/恢复。
-- 复核：SELECT * FROM bs_hit_spray_dedupe_backup_20261110 ORDER BY id;

CREATE TABLE IF NOT EXISTS bs_hit_spray_dedupe_backup_20261110 AS
SELECT *
FROM bs_hit_spray
WHERE id IN (565, 566, 823, 824, 825, 826, 827, 913, 1295, 1315, 1316, 1317, 1318, 1319, 1444, 1531, 1532, 1533, 1534, 1535, 1536, 1537, 1590, 1591, 1624, 1724, 1725, 1760, 1761, 1762, 1763, 1769, 1797);

COMMENT ON TABLE bs_hit_spray_dedupe_backup_20261110 IS '2026-11-10 击球落点重复行清理备份（来源：生产只读 API 扫描，33 组）';

WITH pairs(keep_id, delete_id) AS (
  VALUES
    (562, 565),
    (563, 566),
    (818, 823),
    (819, 824),
    (820, 825),
    (821, 826),
    (822, 827),
    (912, 913),
    (1293, 1295),
    (1310, 1315),
    (1311, 1316),
    (1312, 1317),
    (1313, 1318),
    (1314, 1319),
    (1443, 1444),
    (1522, 1531),
    (1523, 1532),
    (1524, 1533),
    (1525, 1534),
    (1526, 1535),
    (1527, 1536),
    (1528, 1537),
    (1587, 1590),
    (1588, 1591),
    (1623, 1624),
    (1635, 1724),
    (1636, 1725),
    (1756, 1760),
    (1757, 1761),
    (1758, 1762),
    (1759, 1763),
    (1768, 1769),
    (1796, 1797)
)
DELETE FROM bs_hit_spray d
USING pairs p, bs_hit_spray k
WHERE d.id = p.delete_id
  AND k.id = p.keep_id
  AND k.deleted_at IS NULL
  AND d.game_id = k.game_id
  AND d.team_id IS NOT DISTINCT FROM k.team_id
  AND d.inning IS NOT DISTINCT FROM k.inning
  AND d.half IS NOT DISTINCT FROM k.half
  AND d.batter_order IS NOT DISTINCT FROM k.batter_order
  AND d.player_id IS NOT DISTINCT FROM k.player_id
  AND d.result_code IS NOT DISTINCT FROM k.result_code
  AND d.bip_code IS NOT DISTINCT FROM k.bip_code
  AND d.rbi IS NOT DISTINCT FROM k.rbi
  AND d.sprayx IS NOT DISTINCT FROM k.sprayx
  AND d.sprayy IS NOT DISTINCT FROM k.sprayy
  AND d.spray_zone IS NOT DISTINCT FROM k.spray_zone
  AND d.spray_depth IS NOT DISTINCT FROM k.spray_depth
  AND d.outs_before IS NOT DISTINCT FROM k.outs_before
  AND d.runners_before IS NOT DISTINCT FROM k.runners_before;
