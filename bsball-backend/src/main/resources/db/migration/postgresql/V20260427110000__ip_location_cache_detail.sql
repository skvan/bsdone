-- IP 归属地缓存：结构化字段 + 数据来源 + 高德矩形范围（PostGIS）

ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS province VARCHAR(64);
ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS city VARCHAR(128);
ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS adcode VARCHAR(12);
ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS lbs_provider VARCHAR(16);
ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS rectangle geometry(Polygon, 4326);

COMMENT ON TABLE ip_location_cache IS 'IP→归属地缓存（多源 LBS），减少重复外呼';
COMMENT ON COLUMN ip_location_cache.region_text IS '省市区等展示文案（与业务展示一致）';
COMMENT ON COLUMN ip_location_cache.province IS '省级行政区名称';
COMMENT ON COLUMN ip_location_cache.city IS '市级行政区名称';
COMMENT ON COLUMN ip_location_cache.adcode IS '行政区划代码（与数据源一致，多为 6 位国标）';
COMMENT ON COLUMN ip_location_cache.lbs_provider IS '数据来源：amap / baidu / tencent';
COMMENT ON COLUMN ip_location_cache.rectangle IS 'IP 可能位置矩形范围；高德返回两点对角线解析为 Polygon；坐标系与接口一致（GCJ-02 数值按 SRID 4326 存几何，与球场表一致）';
COMMENT ON COLUMN ip_location_cache.fetched_at IS '最近一次解析成功时间';

CREATE INDEX IF NOT EXISTS idx_ip_location_cache_rectangle_gist ON ip_location_cache USING GIST (rectangle)
    WHERE rectangle IS NOT NULL;
