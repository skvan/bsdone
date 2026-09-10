-- 在 Hibernate 启动前执行：仅当 sys_notice 已存在时修复 view_count（空库由 Hibernate 建表 + @ColumnDefault）
-- 注意：勿在 DO 块内使用 EXECUTE 动态 SQL，Druid WallFilter 会误报 “sql injection violation”
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables t
        WHERE t.table_schema = current_schema()
          AND t.table_name = 'sys_notice'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns c
            WHERE c.table_schema = current_schema()
              AND c.table_name = 'sys_notice'
              AND c.column_name = 'view_count'
        ) THEN
            ALTER TABLE sys_notice ADD COLUMN view_count BIGINT DEFAULT 0;
        END IF;

        UPDATE sys_notice SET view_count = 0 WHERE view_count IS NULL;

        ALTER TABLE sys_notice ALTER COLUMN view_count SET DEFAULT 0;
        ALTER TABLE sys_notice ALTER COLUMN view_count SET NOT NULL;

        COMMENT ON COLUMN sys_notice.view_count IS '浏览次数（前台详情访问累计）';
    END IF;
END
$flyway$;
