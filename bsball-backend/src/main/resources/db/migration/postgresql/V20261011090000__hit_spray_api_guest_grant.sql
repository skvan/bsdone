-- 打击落点回放开放到门户（游客可读）：注册 GET /hit-spray/game/:gameId 并授权全部角色（含游客角色 guest）
-- 背景：未注册接口对登录用户默认放行；游客（匿名）访问需先注册 sys_api 且经 sys_role_api 授权。
-- 授权口径对齐既有 /game/:id（GET），覆盖全部 7 个角色；幂等：WHERE NOT EXISTS 防重复。

INSERT INTO sys_api (path, method, description, api_group, created_at)
SELECT '/hit-spray/game/:gameId', 'GET', '查询比赛击球落点', '比赛管理', now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_api
    WHERE path = '/hit-spray/game/:gameId' AND method = 'GET' AND deleted_at IS NULL
);

INSERT INTO sys_role_api (role_id, api_id, created_at)
SELECT r.id, a.id, now()
FROM sys_role r
JOIN sys_api a ON a.path = '/hit-spray/game/:gameId' AND a.method = 'GET' AND a.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_api ra
      WHERE ra.role_id = r.id AND ra.api_id = a.id AND ra.deleted_at IS NULL
  );
