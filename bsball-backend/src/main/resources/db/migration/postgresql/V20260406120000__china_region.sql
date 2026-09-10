-- 中国省市区；数据由应用从 classpath:data/china-pca-code.json 导入。
-- Hibernate 会在 Flyway 之前创建 sys_china_region，故不可再建同名异表 bs_china_region（否则会产生两套表与约束冲突）。
DO $china$
BEGIN
    IF to_regclass('public.sys_china_region') IS NULL AND to_regclass('public.bs_china_region') IS NULL THEN
        CREATE TABLE bs_china_region (
            id              BIGSERIAL PRIMARY KEY,
            adcode          VARCHAR(12)  NOT NULL UNIQUE,
            name            VARCHAR(64)  NOT NULL,
            level           SMALLINT     NOT NULL,
            parent_adcode   VARCHAR(12)
        );
        CREATE INDEX idx_bs_china_region_parent ON bs_china_region (parent_adcode);
        CREATE INDEX idx_bs_china_region_level ON bs_china_region (level);
        COMMENT ON TABLE bs_china_region IS '中国行政区划（省/市/区县）';
        COMMENT ON COLUMN bs_china_region.adcode IS '区划代码（统一为6位国标样式）';
        COMMENT ON COLUMN bs_china_region.level IS '1省 2市 3区县';
    ELSIF to_regclass('public.sys_china_region') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_sys_china_region_parent ON sys_china_region (parent_adcode);
        CREATE INDEX IF NOT EXISTS idx_sys_china_region_level ON sys_china_region (level);
    ELSIF to_regclass('public.bs_china_region') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_bs_china_region_parent ON bs_china_region (parent_adcode);
        CREATE INDEX IF NOT EXISTS idx_bs_china_region_level ON bs_china_region (level);
    END IF;
END
$china$;

ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS addr_district_adcode VARCHAR(12);
CREATE INDEX IF NOT EXISTS idx_bs_stadium_district_adcode ON bs_stadium (addr_district_adcode);

COMMENT ON COLUMN bs_stadium.location IS '经纬度为 GCJ-02（国测局/火星坐标），与高德地图一致；仍以 geometry(Point,4326) 存数值';
COMMENT ON COLUMN bs_stadium.addr_district_adcode IS '区县区划代码（6位），省/市/县名称由 bs_china_region 解析';
