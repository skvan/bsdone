-- 账号权限重构（批次 3b，Task 3.15b / spec §8.6）：门户角色租户级目录配置覆盖层
-- 语义：门户角色（team_manager / league_organizer）为 tenant_id NULL 平台级（全局共享）；
--       租户管理员可对本租户「租户级覆盖」该角色的目录（菜单/按钮）配置，实施全量替换（空集=启用空覆盖）。
--       bs_tenant_role_menu_config 行存在 = 该租户对该角色启用覆盖（即使 bs_tenant_role_menu 为空）。
--       删除 config 行 = 回落全局（role_menu 全局绑定零改动）。
-- 幂等：CREATE TABLE/INDEX IF NOT EXISTS；禁用 DO $$ 块（空库时列由实体 ddl-auto 创建，本脚本为兜底/补建）。

CREATE TABLE IF NOT EXISTS public.bs_tenant_role_menu_config (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    updated_by  BIGINT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uk_bs_tenant_role_menu_config UNIQUE (tenant_id, role_id)
);

CREATE TABLE IF NOT EXISTS public.bs_tenant_role_menu (
    id         BIGSERIAL PRIMARY KEY,
    config_id  BIGINT NOT NULL REFERENCES public.bs_tenant_role_menu_config (id),
    menu_id    BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bs_tenant_role_menu_config_id ON public.bs_tenant_role_menu (config_id);

COMMENT ON TABLE public.bs_tenant_role_menu_config IS '门户角色租户级目录覆盖配置头：行存在=该租户对该平台门户角色启用覆盖（支持空集）';
COMMENT ON COLUMN public.bs_tenant_role_menu_config.tenant_id IS '租户ID：覆盖生效范围';
COMMENT ON COLUMN public.bs_tenant_role_menu_config.role_id IS '平台级门户角色ID（sys_role.tenant_id IS NULL）';
COMMENT ON COLUMN public.bs_tenant_role_menu_config.updated_by IS '最后更新人';
COMMENT ON TABLE public.bs_tenant_role_menu IS '门户角色租户级目录覆盖明细：该租户对该角色生效的菜单/按钮集合（全量替换）';
COMMENT ON COLUMN public.bs_tenant_role_menu.config_id IS '覆盖配置头ID';
COMMENT ON COLUMN public.bs_tenant_role_menu.menu_id IS '菜单/按钮ID（sys_menu.id）';
