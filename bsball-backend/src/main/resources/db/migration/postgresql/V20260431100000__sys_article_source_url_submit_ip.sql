-- 文章：原文链接、保存时客户端 IP 与归属地（后台可查）
ALTER TABLE sys_article ADD COLUMN IF NOT EXISTS source_url VARCHAR(2000);
ALTER TABLE sys_article ADD COLUMN IF NOT EXISTS submit_ip VARCHAR(128);
ALTER TABLE sys_article ADD COLUMN IF NOT EXISTS submit_ip_region VARCHAR(512);
COMMENT ON COLUMN sys_article.source_url IS '原文链接（外链）';
COMMENT ON COLUMN sys_article.submit_ip IS '发表/保存时客户端 IP（服务端解析）';
COMMENT ON COLUMN sys_article.submit_ip_region IS '发表/保存时 IP 归属地展示（服务端解析）';
