-- 长内容：sys_notice -> sys_article；原组件短通知：sys_announcement -> sys_notice；合并 bs_news 后删除

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'sys_notice')
     AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'sys_article') THEN
    ALTER TABLE sys_notice RENAME TO sys_article;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'sys_announcement') THEN
    ALTER TABLE sys_announcement RENAME TO sys_notice;
  END IF;
END $$;

COMMENT ON TABLE sys_article IS '文章（资讯、协议等长内容；tenant_id 空为平台级文档）';
COMMENT ON TABLE sys_notice IS '系统通知（短消息、组件/MQ 推送）';

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'bs_news') THEN
    INSERT INTO sys_article (
      tenant_id, title, publish_target, summary, cover, content, type, author, content_type,
      attachments, is_pinned, pinned_at, show_in_carousel, carousel_order, status, view_count,
      created_by, created_at, updated_by, updated_at, deleted_by, deleted_at
    )
    SELECT
      tenant_id,
      title,
      'portal',
      NULL,
      cover,
      content,
      COALESCE(category, 'news'),
      author,
      COALESCE(content_type, 'html'),
      NULL,
      0,
      NULL,
      0,
      COALESCE(sort, 0),
      1,
      0,
      created_by,
      created_at,
      updated_by,
      updated_at,
      deleted_by,
      deleted_at
    FROM bs_news
    WHERE deleted_at IS NULL;
    DROP TABLE bs_news;
  END IF;
END $$;

-- sys_api 路径随接口重命名（path 不含 /api 前缀）
UPDATE sys_api SET path = replace(path, '/sys/notice/', '/sys/article/') WHERE path LIKE '/sys/notice/%';
UPDATE sys_api SET path = replace(path, '/sys/announcement', '/sys/notice') WHERE path LIKE '/sys/announcement%';
