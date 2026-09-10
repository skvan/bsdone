-- 管理端列表：创建时间、管理端「重新解析」累计次数
-- 注意：先加可空列再回填再 NOT NULL，避免已有数据行导致「column contains null values」

ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS resolve_count INTEGER;

UPDATE ip_location_cache SET resolve_count = 0 WHERE resolve_count IS NULL;
ALTER TABLE ip_location_cache ALTER COLUMN resolve_count SET DEFAULT 0;
ALTER TABLE ip_location_cache ALTER COLUMN resolve_count SET NOT NULL;

UPDATE ip_location_cache SET created_at = fetched_at WHERE created_at IS NULL;

COMMENT ON COLUMN ip_location_cache.created_at IS '首次写入缓存的时间';
COMMENT ON COLUMN ip_location_cache.resolve_count IS '管理端「重新解析」成功累计次数';
