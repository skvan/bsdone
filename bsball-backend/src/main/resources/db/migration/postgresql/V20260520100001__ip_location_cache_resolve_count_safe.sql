-- 兜底：若某环境 Hibernate 先执行曾留下可空的 resolve_count，或列缺失，与实体 NOT NULL DEFAULT 0 对齐

ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS resolve_count INTEGER;
UPDATE ip_location_cache SET resolve_count = 0 WHERE resolve_count IS NULL;
ALTER TABLE ip_location_cache ALTER COLUMN resolve_count SET DEFAULT 0;
ALTER TABLE ip_location_cache ALTER COLUMN resolve_count SET NOT NULL;
