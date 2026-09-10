-- 门户访问打点：IP 解析后的省级名称（与 ECharts / DataV 中国地图 feature.name 对齐，便于聚合展示）
ALTER TABLE portal_visit_hit ADD COLUMN IF NOT EXISTS visit_province VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_portal_visit_hit_tenant_date_province
    ON portal_visit_hit (tenant_id, hit_date, visit_province);

COMMENT ON COLUMN portal_visit_hit.visit_province IS '访问 IP 解析的省级行政区名称（如 广东省、北京市），未识别则空';
