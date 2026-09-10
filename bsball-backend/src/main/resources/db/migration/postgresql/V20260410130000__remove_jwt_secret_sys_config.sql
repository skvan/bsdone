-- JWT 密钥改为 Spring 配置 app.jwt.secret，不再使用 sys_config
DELETE FROM sys_config WHERE config_key = 'jwtSecret';
