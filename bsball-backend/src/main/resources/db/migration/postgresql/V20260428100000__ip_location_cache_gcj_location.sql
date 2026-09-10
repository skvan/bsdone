-- IP 缓存：近似位置点（统一 GCJ-02，与球场坐标语义一致）；百度 BD-09 在后端转换后写入

ALTER TABLE ip_location_cache ADD COLUMN IF NOT EXISTS location geometry(Point,4326);

COMMENT ON COLUMN ip_location_cache.location IS 'IP 近似位置（GCJ-02）；百度接口为 BD-09 经 ChinaCoordTransform 转换后入库；高德为矩形质心；腾讯为接口返回点';

CREATE INDEX IF NOT EXISTS idx_ip_location_cache_location_gist ON ip_location_cache USING GIST (location)
    WHERE location IS NOT NULL;
