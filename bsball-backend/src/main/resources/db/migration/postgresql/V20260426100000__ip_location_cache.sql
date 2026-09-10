-- 高德 IP 归属地服务端缓存（全站共用；按 fetched_at 可做定期刷新）

CREATE TABLE IF NOT EXISTS ip_location_cache (
    ip           VARCHAR(128) PRIMARY KEY,
    region_text  VARCHAR(512) NOT NULL,
    fetched_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE ip_location_cache IS 'IP→归属地文本缓存（高德），减少重复外呼';
COMMENT ON COLUMN ip_location_cache.ip IS '客户端 IP（规范化后主键）';
COMMENT ON COLUMN ip_location_cache.region_text IS '省市区等展示文案';
COMMENT ON COLUMN ip_location_cache.fetched_at IS '最近一次从高德解析成功的时间';
