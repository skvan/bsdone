-- 公开个人主页接口登记并授权全部角色（含游客 guest）
-- 背景：/player/{playerId}/clubhouse 三个 GET 是门户免登录只读能力；
--       已有环境的 InitDataRunner 首装授权不会再跑，必须用迁移补齐。
-- 口径对齐 /wechat/jssdk-config 与 /hit-spray/game/:gameId：幂等，WHERE NOT EXISTS 防重复。

INSERT INTO sys_api (path, method, description, api_group, created_at)
SELECT '/player/:playerId/clubhouse', 'GET', '公开个人主页聚合数据', '球员管理', now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_api
    WHERE path = '/player/:playerId/clubhouse' AND method = 'GET' AND deleted_at IS NULL
);

INSERT INTO sys_api (path, method, description, api_group, created_at)
SELECT '/player/:playerId/clubhouse/milestones', 'GET', '公开个人主页生涯里程碑', '球员管理', now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_api
    WHERE path = '/player/:playerId/clubhouse/milestones' AND method = 'GET' AND deleted_at IS NULL
);

INSERT INTO sys_api (path, method, description, api_group, created_at)
SELECT '/player/:playerId/clubhouse/sprays', 'GET', '公开个人主页打击落点', '球员管理', now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_api
    WHERE path = '/player/:playerId/clubhouse/sprays' AND method = 'GET' AND deleted_at IS NULL
);

INSERT INTO sys_role_api (role_id, api_id, created_at)
SELECT r.id, a.id, now()
FROM sys_role r
JOIN sys_api a ON a.path = '/player/:playerId/clubhouse' AND a.method = 'GET' AND a.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_api ra
      WHERE ra.role_id = r.id AND ra.api_id = a.id AND ra.deleted_at IS NULL
  );

INSERT INTO sys_role_api (role_id, api_id, created_at)
SELECT r.id, a.id, now()
FROM sys_role r
JOIN sys_api a ON a.path = '/player/:playerId/clubhouse/milestones' AND a.method = 'GET' AND a.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_api ra
      WHERE ra.role_id = r.id AND ra.api_id = a.id AND ra.deleted_at IS NULL
  );

INSERT INTO sys_role_api (role_id, api_id, created_at)
SELECT r.id, a.id, now()
FROM sys_role r
JOIN sys_api a ON a.path = '/player/:playerId/clubhouse/sprays' AND a.method = 'GET' AND a.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_api ra
      WHERE ra.role_id = r.id AND ra.api_id = a.id AND ra.deleted_at IS NULL
  );
