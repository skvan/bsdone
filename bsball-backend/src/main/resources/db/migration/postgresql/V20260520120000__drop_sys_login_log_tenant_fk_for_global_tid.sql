-- 登录日志允许写入超级管理员全局态 tenant_id=0。
-- 由于 0 不在 sys_tenant 主表中，需要移除 sys_login_log.tenant_id 外键约束。

ALTER TABLE sys_login_log
    DROP CONSTRAINT IF EXISTS sys_login_log_tenant_id_fkey;
