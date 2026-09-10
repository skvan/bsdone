-- 行政区划归为系统基础数据：bs_china_region → sys_china_region（幂等）

DO $$
BEGIN
    IF to_regclass('public.bs_china_region') IS NOT NULL
       AND to_regclass('public.sys_china_region') IS NULL THEN
        ALTER TABLE bs_china_region RENAME TO sys_china_region;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
               WHERE c.relkind = 'i' AND n.nspname = 'public' AND c.relname = 'idx_bs_china_region_parent') THEN
        ALTER INDEX idx_bs_china_region_parent RENAME TO idx_sys_china_region_parent;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
               WHERE c.relkind = 'i' AND n.nspname = 'public' AND c.relname = 'idx_bs_china_region_level') THEN
        ALTER INDEX idx_bs_china_region_level RENAME TO idx_sys_china_region_level;
    END IF;
END $$;

-- 须限定约束所属表：空库时 Hibernate 已建 sys_china_region，Flyway 可能另建 bs_china_region，
-- 此时 bs_china_region_pkey 在 bs_china_region 上，对 sys_china_region 执行 RENAME 会报错。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE c.conname = 'bs_china_region_pkey'
          AND r.relname = 'sys_china_region'
          AND n.nspname = 'public'
    ) THEN
        ALTER TABLE sys_china_region RENAME CONSTRAINT bs_china_region_pkey TO sys_china_region_pkey;
    END IF;
    IF EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE c.conname = 'bs_china_region_adcode_key'
          AND r.relname = 'sys_china_region'
          AND n.nspname = 'public'
    ) THEN
        ALTER TABLE sys_china_region RENAME CONSTRAINT bs_china_region_adcode_key TO sys_china_region_adcode_key;
    END IF;
END $$;

-- 不显式重命名 id 序列：避免与列默认值 nextval('...') 解析不一致；保留 bs_china_region_id_seq 亦可正常工作

DO $$
BEGIN
    IF to_regclass('public.sys_china_region') IS NOT NULL THEN
        COMMENT ON TABLE sys_china_region IS '中国行政区划（省/市/区县/乡镇街道等），系统基础数据';
        COMMENT ON COLUMN sys_china_region.adcode IS '区划代码：省2扩6、地市4扩6；区县多6位；直筒子市下属镇街9～12位';
    END IF;
    IF to_regclass('public.bs_stadium') IS NOT NULL THEN
        COMMENT ON COLUMN bs_stadium.addr_district_adcode IS '末级区划代码（见 sys_china_region），镇街可为9～12位';
    END IF;
END $$;
