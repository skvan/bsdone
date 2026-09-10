-- 记录 Vue Router 路由名，与 path（fullPath）区分展示

ALTER TABLE portal_devtools_report
    ADD COLUMN IF NOT EXISTS route_name VARCHAR(256);

COMMENT ON COLUMN portal_devtools_report.route_name IS 'Vue Router 路由 name（可能为空）';
