-- 修复「租户编码软删后复用」返回 500（Issue #356）
--
-- 背景：sys_tenant 为软删除；sys_tenant.code 上存在非部分唯一索引
-- （ux_sys_tenant_code，历史库另有 idx_sys_tenant_code）。新建租户只校验
-- deleted_at IS NULL 的编码，允许复用已删编码，但 INSERT 会撞旧唯一索引
-- 23505 -> 全局异常 -> HTTP 500「服务器繁忙」。
--
-- 本迁移幂等：先确保「仅未软删行生效」的部分唯一索引存在，再删除覆盖 (code)
-- 的非部分唯一约束/索引（兼容不同命名与形态），不修改业务数据。
-- 不要修改 V20260410160000 的已应用内容（Flyway checksum）。

DO $$
DECLARE
    r record;
    v_tbl regclass := to_regclass('public.sys_tenant');
BEGIN
    IF v_tbl IS NULL THEN
        RETURN;
    END IF;

    -- 先建部分唯一索引：若存量有重复的非软删编码，整个迁移回滚，旧索引不会被提前破坏
    CREATE UNIQUE INDEX IF NOT EXISTS ux_sys_tenant_code_active
        ON public.sys_tenant (code)
        WHERE deleted_at IS NULL;

    -- 删除覆盖 (code) 的唯一约束（不触碰主键）
    FOR r IN
        SELECT c.conname
        FROM pg_constraint c
        WHERE c.conrelid = v_tbl
          AND c.contype = 'u'
          AND (
              SELECT array_agg(a.attname::text ORDER BY a.attname)
              FROM pg_attribute a
              WHERE a.attrelid = c.conrelid
                AND a.attnum = ANY(c.conkey)
          ) = ARRAY['code']
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', v_tbl, r.conname);
    END LOOP;

    -- 删除覆盖 (code) 的非部分唯一索引（历史库的 idx_sys_tenant_code / ux_sys_tenant_code 即此形态）
    FOR r IN
        SELECT ic.relname AS index_name
        FROM pg_index i
        JOIN pg_class ic ON ic.oid = i.indexrelid
        WHERE i.indrelid = v_tbl
          AND i.indisunique
          AND NOT i.indisprimary
          AND i.indpred IS NULL
          AND (
              SELECT array_agg(a.attname::text ORDER BY a.attname)
              FROM pg_attribute a
              WHERE a.attrelid = v_tbl
                AND a.attnum = ANY(i.indkey)
          ) = ARRAY['code']
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS public.%I', r.index_name);
    END LOOP;
END $$;
