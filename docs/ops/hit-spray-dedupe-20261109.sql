-- 落点回放重复行清理（2026-11-09）
--
-- 背景：录入页的落点队列（hitSprayQueue）在批量送出成功后只清空了内存队列，
-- 但撤销/重做快照与现场快照（undoHistory）里仍带着这些记录。撤销、重做或重进现场
-- 把这些记录放回队列后，下一次 flush 又整批插入 bs_hit_spray，于是同一次击球出现
-- 两笔内容完全相同的落点。打者 PA/AB 只按打席统计，不受影响；受影响的是落点回放、
-- 热区图与「共 N 次击球落点」的计数。
--
-- 现状：前端已在 fix/hit-spray-duplicate-queue 修复（送出成功的记录记 rid 并从快照剔除），
-- 本脚本只负责清理修复前已经写进库的重复行。
--
-- 注意：HitSpray 实体没有 @SQLRestriction，软删（deleted_at 非空）的行 API 仍会返回，
-- 所以这里必须物理删除，不能只写 deleted_at。
--
-- 用法：
--   1) 先执行【第 1 段】只读清单，人工确认每组确实同一次击球（同局同棒次同内容）；
--   2) 再执行【第 3 段】，事务默认以 ROLLBACK 结尾——看 RETURNING 结果与清单一致后，
--      把最后一行的 ROLLBACK 改成 COMMIT 再跑一次。
--   已知案例（game 271，5 局上第 5 棒金天皓，失误上垒·游击·深）：保留 id 1796，删除 id 1797。

-- ===== 1. 只读清单：内容完全相同的重复行（保留组内 id 最小的一笔） =====
SELECT
    t.dup_group_size,
    t.rn,
    t.id,
    t.game_id,
    t.inning,
    t.half,
    t.batter_order,
    t.player_id,
    t.team_id,
    t.result_code,
    t.bip_code,
    t.rbi,
    t.sprayx,
    t.sprayy,
    t.spray_zone,
    t.spray_depth,
    t.outs_before,
    t.runners_before,
    t.created_at
FROM (
    SELECT
        h.*,
        count(*) OVER w AS dup_group_size,
        row_number() OVER w AS rn
    FROM bs_hit_spray h
    WHERE h.deleted_at IS NULL
    WINDOW w AS (
        PARTITION BY
            h.game_id,
            h.team_id,
            h.inning,
            h.half,
            h.batter_order,
            h.player_id,
            coalesce(h.result_code, ''),
            coalesce(h.bip_code, ''),
            coalesce(h.rbi, -1),
            coalesce(h.sprayx, -1),
            coalesce(h.sprayy, -1),
            coalesce(h.spray_zone, ''),
            coalesce(h.spray_depth, ''),
            coalesce(h.outs_before, -1),
            coalesce(h.runners_before, '')
        ORDER BY h.id
    )
) t
WHERE t.dup_group_size > 1
ORDER BY t.game_id, t.inning, t.half, t.batter_order, t.player_id, t.rn;

-- ===== 2. 只读汇总：每场会删掉几笔、保留几笔 =====
SELECT
    t.game_id,
    count(*) FILTER (WHERE t.rn > 1) AS rows_to_delete,
    count(*) FILTER (WHERE t.rn = 1)  AS rows_to_keep,
    count(*)                          AS rows_total
FROM (
    SELECT
        h.id,
        h.game_id,
        row_number() OVER (
            PARTITION BY
                h.game_id, h.team_id, h.inning, h.half, h.batter_order, h.player_id,
                coalesce(h.result_code, ''), coalesce(h.bip_code, ''), coalesce(h.rbi, -1),
                coalesce(h.sprayx, -1), coalesce(h.sprayy, -1),
                coalesce(h.spray_zone, ''), coalesce(h.spray_depth, ''),
                coalesce(h.outs_before, -1), coalesce(h.runners_before, '')
            ORDER BY h.id
        ) AS rn
    FROM bs_hit_spray h
    WHERE h.deleted_at IS NULL
) t
GROUP BY t.game_id
HAVING count(*) FILTER (WHERE t.rn > 1) > 0
ORDER BY t.game_id;

-- ===== 3. 删除（先以 ROLLBACK 演练，确认后再改 COMMIT） =====
BEGIN;

WITH ranked AS (
    SELECT
        h.id,
        row_number() OVER (
            PARTITION BY
                h.game_id, h.team_id, h.inning, h.half, h.batter_order, h.player_id,
                coalesce(h.result_code, ''), coalesce(h.bip_code, ''), coalesce(h.rbi, -1),
                coalesce(h.sprayx, -1), coalesce(h.sprayy, -1),
                coalesce(h.spray_zone, ''), coalesce(h.spray_depth, ''),
                coalesce(h.outs_before, -1), coalesce(h.runners_before, '')
            ORDER BY h.id
        ) AS rn
    FROM bs_hit_spray h
    WHERE h.deleted_at IS NULL
)
DELETE FROM bs_hit_spray h
USING ranked r
WHERE h.id = r.id
  AND r.rn > 1
RETURNING
    h.id,
    h.game_id,
    h.inning,
    h.half,
    h.batter_order,
    h.player_id,
    h.result_code,
    h.sprayx,
    h.sprayy,
    h.created_at;

-- 人工核对上面的 RETURNING 与第 1 段清单一致后，把下一行改成 COMMIT;
ROLLBACK;

-- ===== 4. 删完复查：应返回 0 行 =====
SELECT count(*) AS remaining_duplicate_rows
FROM (
    SELECT
        count(*) OVER (
            PARTITION BY
                h.game_id, h.team_id, h.inning, h.half, h.batter_order, h.player_id,
                coalesce(h.result_code, ''), coalesce(h.bip_code, ''), coalesce(h.rbi, -1),
                coalesce(h.sprayx, -1), coalesce(h.sprayy, -1),
                coalesce(h.spray_zone, ''), coalesce(h.spray_depth, ''),
                coalesce(h.outs_before, -1), coalesce(h.runners_before, '')
        ) AS c
    FROM bs_hit_spray h
    WHERE h.deleted_at IS NULL
) t
WHERE t.c > 1;
