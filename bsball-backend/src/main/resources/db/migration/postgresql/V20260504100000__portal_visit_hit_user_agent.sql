ALTER TABLE portal_visit_hit
    ADD COLUMN IF NOT EXISTS user_agent varchar(512);

COMMENT ON COLUMN portal_visit_hit.user_agent IS '客户端 User-Agent（列表摘要由前端解析展示）';
