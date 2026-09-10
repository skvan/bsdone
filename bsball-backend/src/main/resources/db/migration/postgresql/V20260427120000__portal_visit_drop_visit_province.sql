-- 省级统计改由 portal_visit_hit.ip JOIN ip_location_cache（避免与缓存重复存储）
DROP INDEX IF EXISTS idx_portal_visit_hit_tenant_date_province;
ALTER TABLE portal_visit_hit DROP COLUMN IF EXISTS visit_province;

-- 加速与 ip_location_cache(ip) 的关联（缓存表主键已为 ip）
CREATE INDEX IF NOT EXISTS idx_portal_visit_hit_ip ON portal_visit_hit (ip);
