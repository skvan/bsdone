-- 租户 / 用户 / 角色：描述与租赁期
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS description VARCHAR(2000);
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS lease_start_date DATE;
ALTER TABLE sys_tenant ADD COLUMN IF NOT EXISTS lease_end_date DATE;

COMMENT ON COLUMN sys_tenant.description IS '租户说明/备注';
COMMENT ON COLUMN sys_tenant.lease_start_date IS '租赁开始日期';
COMMENT ON COLUMN sys_tenant.lease_end_date IS '租赁结束日期';

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS description VARCHAR(500);
COMMENT ON COLUMN sys_user.description IS '用户说明/备注';

ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS description VARCHAR(500);
COMMENT ON COLUMN sys_role.description IS '角色说明/备注';

-- 下线「数据范围」后台菜单（数据范围仍由 sys_data_scope + 接口维护，仅去掉侧栏入口）
DELETE FROM sys_role_menu WHERE menu_id IN (SELECT id FROM sys_menu WHERE path = '/admin/user-data-scope');
DELETE FROM sys_menu WHERE path = '/admin/user-data-scope';
