-- 平台级文章（如隐私协议）tenant_id 为空；与实体、SysArticleService.create 中超管逻辑一致
ALTER TABLE sys_article ALTER COLUMN tenant_id DROP NOT NULL;
