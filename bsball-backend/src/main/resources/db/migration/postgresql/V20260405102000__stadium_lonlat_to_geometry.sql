-- 若曾误用双精度列存经纬度，迁回 PostGIS geometry（需已有 postgis 扩展）。
DO $$
BEGIN
    IF to_regclass('public.bs_stadium') IS NULL THEN
        RETURN;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'bs_stadium' AND column_name = 'longitude'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'bs_stadium' AND column_name = 'location'
    ) THEN
        ALTER TABLE bs_stadium ADD COLUMN location geometry(Point, 4326);
        UPDATE bs_stadium SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)
        WHERE longitude IS NOT NULL AND latitude IS NOT NULL;
        DROP INDEX IF EXISTS idx_bs_stadium_coords;
        ALTER TABLE bs_stadium DROP COLUMN longitude;
        ALTER TABLE bs_stadium DROP COLUMN latitude;
        CREATE INDEX IF NOT EXISTS idx_bs_stadium_location ON bs_stadium USING GIST (location);
    END IF;
END $$;
