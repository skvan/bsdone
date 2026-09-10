-- 字典类型：合并同 type 的重复行（保留最小 id），并约束未删除行 type 唯一
-- 门户资讯 tab 与 sys_notice.type 一致：news / announcement / notice / publicity
-- 注意：不使用 PL/pgSQL（DO $$），否则 Druid WallFilter 解析会失败导致 Flyway 无法执行

-- 1) 将指向「非保留行」的字典数据归并到该 type 下最小 id（FROM 子句中不可引用待更新表别名，条件放 WHERE）
UPDATE sys_dict_data d
SET dict_type_id = m.keep_id
FROM sys_dict_type cur,
     (SELECT type, MIN(id) AS keep_id
      FROM sys_dict_type
      WHERE deleted_at IS NULL AND type IS NOT NULL AND TRIM(type) <> ''
      GROUP BY type) AS m
WHERE d.dict_type_id = cur.id
  AND cur.deleted_at IS NULL
  AND cur.type = m.type
  AND cur.id <> m.keep_id;

-- 2) 软删除重复的字典类型行（同 type 非最小 id）
UPDATE sys_dict_type t
SET deleted_at = NOW(),
    deleted_by = COALESCE((SELECT id FROM sys_user WHERE username = 'admin' AND deleted_at IS NULL LIMIT 1), 1)
WHERE t.deleted_at IS NULL
  AND EXISTS (
    SELECT 1
    FROM (
      SELECT type, MIN(id) AS keep_id
      FROM sys_dict_type
      WHERE deleted_at IS NULL AND type IS NOT NULL AND TRIM(type) <> ''
      GROUP BY type
    ) AS agg
    WHERE agg.type = t.type AND t.id <> agg.keep_id
  );

-- 3) 合并后同一 dict_type_id 下可能重复 value，保留最小 id
DELETE FROM sys_dict_data d1
WHERE d1.deleted_at IS NULL
  AND EXISTS (
    SELECT 1 FROM sys_dict_data d2
    WHERE d2.dict_type_id = d1.dict_type_id
      AND COALESCE(d2.value, '') = COALESCE(d1.value, '')
      AND d2.deleted_at IS NULL
      AND d2.id < d1.id
  );

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_dict_type_type_active ON sys_dict_type (type) WHERE deleted_at IS NULL;
