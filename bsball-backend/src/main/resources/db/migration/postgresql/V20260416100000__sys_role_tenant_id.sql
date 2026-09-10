-- 角色表增加租户维度：NULL=系统级（admin/guest/tenant_admin 等）；非空=某租户自建角色

ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT;
COMMENT ON COLUMN sys_role.tenant_id IS 'NULL=系统级角色；非空=租户自建角色';

ALTER TABLE sys_role DROP CONSTRAINT IF EXISTS fk_sys_role_tenant;
ALTER TABLE sys_role ADD CONSTRAINT fk_sys_role_tenant FOREIGN KEY (tenant_id) REFERENCES sys_tenant (id);

CREATE INDEX IF NOT EXISTS ix_sys_role_tenant_id ON sys_role (tenant_id);
