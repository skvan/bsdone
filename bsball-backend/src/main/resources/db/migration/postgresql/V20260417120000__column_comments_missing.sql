-- 补齐历史迁移中新增列、但未写 COMMENT ON 的表字段（便于 DBA / 文档工具展示）
-- PostgreSQL 专用；COMMENT 重复执行会覆盖为相同文本，无妨

-- ========== 租户隔离列（批量 ALTER 时未写注释） ==========
COMMENT ON COLUMN bs_stadium.tenant_id IS '数据归属租户';
COMMENT ON COLUMN bs_player.tenant_id IS '数据归属租户';
COMMENT ON COLUMN bs_coach.tenant_id IS '数据归属租户';
COMMENT ON COLUMN bs_personnel_change.tenant_id IS '数据归属租户';
COMMENT ON COLUMN bs_game_player_stat.tenant_id IS '数据归属租户';
COMMENT ON COLUMN portal_visit_hit.tenant_id IS '打点所属租户';

COMMENT ON COLUMN bs_event.tenant_id IS '数据归属租户';
COMMENT ON COLUMN bs_game.tenant_id IS '数据归属租户';
-- bs_news：空库无此表；旧库由 V20260420120000 合并入 sys_article 后 DROP，此处不再 COMMENT
COMMENT ON COLUMN sys_notice.tenant_id IS '数据归属租户';
-- sys_announcement：空库无此表；V20260420120000 将其 RENAME 为 sys_notice，此处不再 COMMENT

COMMENT ON COLUMN bs_league.tenant_id IS '数据归属租户';
COMMENT ON COLUMN bs_team.tenant_id IS '数据归属租户';

-- ========== sys_tenant：表级仅有注释，补列注释 ==========
COMMENT ON COLUMN sys_tenant.id IS '主键';
COMMENT ON COLUMN sys_tenant.name IS '租户名称';
COMMENT ON COLUMN sys_tenant.code IS '租户编码（URL 段，唯一）';
COMMENT ON COLUMN sys_tenant.status IS '状态：0 禁用 1 启用';
COMMENT ON COLUMN sys_tenant.sort IS '排序';
COMMENT ON COLUMN sys_tenant.created_by IS '创建人用户 ID';
COMMENT ON COLUMN sys_tenant.created_at IS '创建时间';
COMMENT ON COLUMN sys_tenant.updated_by IS '更新人用户 ID';
COMMENT ON COLUMN sys_tenant.updated_at IS '更新时间';
COMMENT ON COLUMN sys_tenant.deleted_by IS '删除人用户 ID';
COMMENT ON COLUMN sys_tenant.deleted_at IS '删除时间（软删除）';

-- ========== sys_user_tenant ==========
COMMENT ON COLUMN sys_user_tenant.id IS '主键';
COMMENT ON COLUMN sys_user_tenant.user_id IS '用户 ID';
COMMENT ON COLUMN sys_user_tenant.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_user_tenant.created_by IS '创建人用户 ID';
COMMENT ON COLUMN sys_user_tenant.created_at IS '创建时间';
COMMENT ON COLUMN sys_user_tenant.updated_by IS '更新人用户 ID';
COMMENT ON COLUMN sys_user_tenant.updated_at IS '更新时间';
COMMENT ON COLUMN sys_user_tenant.deleted_by IS '删除人用户 ID';
COMMENT ON COLUMN sys_user_tenant.deleted_at IS '删除时间（软删除）';

-- ========== sys_data_scope ==========
COMMENT ON COLUMN sys_data_scope.id IS '主键';
COMMENT ON COLUMN sys_data_scope.user_id IS '用户 ID';
COMMENT ON COLUMN sys_data_scope.tenant_id IS '租户 ID';
COMMENT ON COLUMN sys_data_scope.ref_id IS '联盟或球队 ID（与 scope_type 配合）';
COMMENT ON COLUMN sys_data_scope.created_by IS '创建人用户 ID';
COMMENT ON COLUMN sys_data_scope.created_at IS '创建时间';
COMMENT ON COLUMN sys_data_scope.updated_by IS '更新人用户 ID';
COMMENT ON COLUMN sys_data_scope.updated_at IS '更新时间';
COMMENT ON COLUMN sys_data_scope.deleted_by IS '删除人用户 ID';
COMMENT ON COLUMN sys_data_scope.deleted_at IS '删除时间（软删除）';

-- ========== bs_stadium：早期脚本未对审计列注释 ==========
COMMENT ON COLUMN bs_stadium.id IS '主键';
COMMENT ON COLUMN bs_stadium.created_by IS '创建人用户 ID';
COMMENT ON COLUMN bs_stadium.created_at IS '创建时间';
COMMENT ON COLUMN bs_stadium.updated_by IS '更新人用户 ID';
COMMENT ON COLUMN bs_stadium.updated_at IS '更新时间';
COMMENT ON COLUMN bs_stadium.deleted_by IS '删除人用户 ID';
COMMENT ON COLUMN bs_stadium.deleted_at IS '删除时间（软删除）';

-- ========== bs_stadium_home_team：关联与审计 ==========
COMMENT ON COLUMN bs_stadium_home_team.id IS '主键';
COMMENT ON COLUMN bs_stadium_home_team.stadium_id IS '球场 ID';
COMMENT ON COLUMN bs_stadium_home_team.team_id IS '球队 ID';
COMMENT ON COLUMN bs_stadium_home_team.created_by IS '创建人用户 ID';
COMMENT ON COLUMN bs_stadium_home_team.created_at IS '创建时间';
COMMENT ON COLUMN bs_stadium_home_team.updated_by IS '更新人用户 ID';
COMMENT ON COLUMN bs_stadium_home_team.updated_at IS '更新时间';
COMMENT ON COLUMN bs_stadium_home_team.deleted_by IS '删除人用户 ID';
COMMENT ON COLUMN bs_stadium_home_team.deleted_at IS '删除时间（软删除）';
