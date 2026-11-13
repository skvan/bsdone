-- 赛事积分榜/对阵（Issue #665）：注册公开 GET 并授权全部角色（含游客），
-- 参赛球队分组 PUT 授权给拥有「参赛球队」按钮权限的角色。
-- 幂等：INSERT ... WHERE NOT EXISTS 防重复；新装环境由 InitDataRunner 再兜底。

INSERT INTO sys_api (path, method, description, api_group, created_at)
SELECT '/event/:eventId/tournament', 'GET', '赛事积分榜与对阵', '赛事管理', now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_api
    WHERE path = '/event/:eventId/tournament' AND method = 'GET' AND deleted_at IS NULL
);

INSERT INTO sys_api (path, method, description, api_group, created_at)
SELECT '/event/:eventId/teams/:teamId/group', 'PUT', '设置参赛球队分组', '赛事管理', now()
WHERE NOT EXISTS (
    SELECT 1 FROM sys_api
    WHERE path = '/event/:eventId/teams/:teamId/group' AND method = 'PUT' AND deleted_at IS NULL
);

INSERT INTO sys_role_api (role_id, api_id, created_at)
SELECT r.id, a.id, now()
FROM sys_role r
JOIN sys_api a ON a.path = '/event/:eventId/tournament' AND a.method = 'GET' AND a.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_api ra
      WHERE ra.role_id = r.id AND ra.api_id = a.id AND ra.deleted_at IS NULL
  );

INSERT INTO sys_role_api (role_id, api_id, created_at)
SELECT r.id, a.id, now()
FROM sys_role r
JOIN sys_role_menu rm ON rm.role_id = r.id AND rm.deleted_at IS NULL
JOIN sys_menu m ON m.id = rm.menu_id AND m.permission = 'business:event:teams' AND m.deleted_at IS NULL
JOIN sys_api a ON a.path = '/event/:eventId/teams/:teamId/group' AND a.method = 'PUT' AND a.deleted_at IS NULL
WHERE r.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_api ra
      WHERE ra.role_id = r.id AND ra.api_id = a.id AND ra.deleted_at IS NULL
  );
