-- 超级管理员登录日志需要记录 tenant_id=0（全局态），该值不在 sys_tenant 中。
-- 登录日志不再对 tenant_id 做外键约束，仅保留非空约束与索引。

ALTER TABLE sys_login_log
    DROP CONSTRAINT IF EXISTS sys_login_log_tenant_id_fkey;
