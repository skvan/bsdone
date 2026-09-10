-- 球场主数据：坐标列为 PostGIS geometry(Point,4326)。
-- CREATE EXTENSION 不得写在此处（Flyway 经 Druid 时 Wall 会拦截）；改由 PostgresDatabaseInitializer 原生 JDBC 执行。

CREATE TABLE IF NOT EXISTS bs_stadium (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    short_name   VARCHAR(50),
    short_name_en VARCHAR(80),
    addr_province VARCHAR(50),
    addr_city    VARCHAR(80),
    addr_district VARCHAR(80),
    postal_code  VARCHAR(20),
    address_detail VARCHAR(500),
    location     geometry(Point, 4326),
    level        VARCHAR(16) NOT NULL,
    operating_status VARCHAR(32) NOT NULL,
    capacity_total INTEGER,
    seating_capacity INTEGER,
    record_attendance INTEGER,
    field_distance_left_m INTEGER,
    field_distance_center_m INTEGER,
    field_distance_right_m INTEGER,
    turf_type    VARCHAR(32),
    roof_type    VARCHAR(32),
    created_by   BIGINT,
    created_at   TIMESTAMP,
    updated_by   BIGINT,
    updated_at   TIMESTAMP,
    deleted_by   BIGINT,
    deleted_at   TIMESTAMP
);

COMMENT ON TABLE bs_stadium IS '球场';
COMMENT ON COLUMN bs_stadium.name IS '球场名称';
COMMENT ON COLUMN bs_stadium.short_name IS '简称';
COMMENT ON COLUMN bs_stadium.short_name_en IS '英文简称';
COMMENT ON COLUMN bs_stadium.addr_province IS '省';
COMMENT ON COLUMN bs_stadium.addr_city IS '县/市';
COMMENT ON COLUMN bs_stadium.addr_district IS '乡/镇/区';
COMMENT ON COLUMN bs_stadium.postal_code IS '邮递区号';
COMMENT ON COLUMN bs_stadium.address_detail IS '详细地址';
COMMENT ON COLUMN bs_stadium.location IS '坐标 WGS84（EPSG:4326）';
COMMENT ON COLUMN bs_stadium.level IS '球场级别 A/B/C';
COMMENT ON COLUMN bs_stadium.operating_status IS '运营状态';
COMMENT ON COLUMN bs_stadium.capacity_total IS '容纳人数上限（含站席）';
COMMENT ON COLUMN bs_stadium.seating_capacity IS '纯座席数';
COMMENT ON COLUMN bs_stadium.record_attendance IS '纪录上座（展示）';
COMMENT ON COLUMN bs_stadium.field_distance_left_m IS '左外野距离(米)';
COMMENT ON COLUMN bs_stadium.field_distance_center_m IS '中外野距离(米)';
COMMENT ON COLUMN bs_stadium.field_distance_right_m IS '右外野距离(米)';
COMMENT ON COLUMN bs_stadium.turf_type IS '草皮类型';
COMMENT ON COLUMN bs_stadium.roof_type IS '屋顶类型';

CREATE INDEX IF NOT EXISTS idx_bs_stadium_location ON bs_stadium USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_bs_stadium_deleted_at ON bs_stadium (deleted_at);

-- 依赖 bs_team：空库时 Flyway 先于 Hibernate，若尚无球队表则跳过，关联表由 ddl-auto 后续创建
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'bs_team'
    ) THEN
        CREATE TABLE IF NOT EXISTS bs_stadium_home_team (
            id            BIGSERIAL PRIMARY KEY,
            stadium_id    BIGINT NOT NULL REFERENCES bs_stadium (id) ON DELETE CASCADE,
            team_id       BIGINT NOT NULL REFERENCES bs_team (id) ON DELETE CASCADE,
            effective_from DATE,
            effective_to   DATE,
            sort_order    INTEGER DEFAULT 0,
            created_by    BIGINT,
            created_at    TIMESTAMP,
            updated_by    BIGINT,
            updated_at    TIMESTAMP,
            deleted_by    BIGINT,
            deleted_at    TIMESTAMP
        );

        COMMENT ON TABLE bs_stadium_home_team IS '球场主场球队关联表';
        COMMENT ON COLUMN bs_stadium_home_team.effective_from IS '生效起';
        COMMENT ON COLUMN bs_stadium_home_team.effective_to IS '生效止（空表示仍有效）';
        COMMENT ON COLUMN bs_stadium_home_team.sort_order IS '同球场多队排序';

        CREATE INDEX IF NOT EXISTS idx_bs_stadium_home_team_stadium ON bs_stadium_home_team (stadium_id);
        CREATE INDEX IF NOT EXISTS idx_bs_stadium_home_team_team ON bs_stadium_home_team (team_id);
    END IF;
END
$flyway$;
