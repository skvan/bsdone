-- 微信 JSSDK 签名接口登记并授权全部角色（含游客角色 guest）
-- 背景：接口已实现；生产环境实测匿名访问返回 403。即使部署环境用 APP_GUEST_WHITELIST
--       覆盖了默认值、未包含 /wechat，仍可通过 sys_api + guest 角色授权放行。
-- 口径对齐 /hit-spray/game/:gameId（GET）：幂等，WHERE NOT EXISTS 防重复。

INSERT INTO sys_api (path, method, description, api_group, created_at)
SELECT '/wechat/jssdk-config', 'GET', '获取微信 JSSDK 签名配置', '认证', now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_api
    WHERE path = '/wechat/jssdk-config' AND method = 'GET' AND deleted_at IS NULL
);

INSERT INTO sys_role_api (role_id, api_id, created_at)
SELECT r.id, a.id, now()
FROM sys_role r
JOIN sys_api a ON a.path = '/wechat/jssdk-config' AND a.method = 'GET' AND a.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_api ra
      WHERE ra.role_id = r.id AND ra.api_id = a.id AND ra.deleted_at IS NULL
  );
