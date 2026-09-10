-- 种子租户 id=1 原为 code='default'，与前端默认门户路径 /{tenantCode}/ 使用的 bs-ball（VITE_DEFAULT_TENANT_CODE 默认）不一致，
-- 导致 checkTenantSlug('bs-ball') 永远 missing、整站表现为 404。
-- 注意：URL 段「default」为前端保留 slug，不能作为租户路径使用。
UPDATE sys_tenant
SET code = 'bs-ball',
    updated_at = NOW()
WHERE id = 1
  AND code = 'default'
  AND NOT EXISTS (
    SELECT 1 FROM sys_tenant t2 WHERE t2.code = 'bs-ball' AND t2.id <> 1
  );
