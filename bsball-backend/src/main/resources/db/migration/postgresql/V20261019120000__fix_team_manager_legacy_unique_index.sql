-- 修复「球队负责人解除后再次指派」返回 500（2026-10-19 生产实证）
--
-- 生产库的 ux_bs_team_manager_team_user 实际是「非部分唯一索引」而非表约束；
-- V20260930160000 只执行了 DROP CONSTRAINT IF EXISTS，删不掉同名索引，软删行仍占用
-- (team_id, user_id) 唯一键：remove 后再次 assign -> INSERT 23505 -> HTTP 500。
--
-- 本迁移幂等：确保「仅未软删行生效」的部分唯一索引存在，再删除覆盖
-- (team_id, user_id) 的非部分唯一约束/索引（兼容约束与普通索引两种形态），
-- 不修改业务数据。不要修改 V20260930160000 的已应用内容（Flyway checksum）。

DO $$
DECLARE
    r record;
    v_tbl regclass := to_regclass('public.bs_team_manager');
BEGIN
    IF v_tbl IS NULL THEN
        RETURN;
    END IF;

    -- 先建部分唯一索引：若存量有重复的非软删行，整个迁移回滚，旧索引不会被提前破坏
    CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_team_manager_team_user_active
        ON public.bs_team_manager (team_id, user_id)
        WHERE deleted_at IS NULL;

    -- 删除覆盖 (team_id, user_id) 的唯一约束（不触碰主键）
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
          ) = ARRAY['team_id', 'user_id']
    LOOP
        EXECUTE format('ALTER TABLE public.bs_team_manager DROP CONSTRAINT %I', r.conname);
    END LOOP;

    -- 删除覆盖 (team_id, user_id) 的非部分唯一索引（旧生产库即为这种形态）
    FOR r IN
        SELECT ic.relname AS index_name
        FROM pg_index i
        JOIN pg_class t ON t.oid = i.indrelid
        JOIN pg_class ic ON ic.oid = i.indexrelid
        WHERE i.indrelid = v_tbl
          AND i.indisunique
          AND NOT i.indisprimary
          AND i.indpred IS NULL
          AND (
              SELECT array_agg(a.attname::text ORDER BY a.attname)
              FROM pg_attribute a
              WHERE a.attrelid = t.oid
                AND a.attnum = ANY(i.indkey)
          ) = ARRAY['team_id', 'user_id']
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS public.%I', r.index_name);
    END LOOP;
END $$;
