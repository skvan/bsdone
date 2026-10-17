ALTER TABLE portal_visit_hit
    ADD COLUMN IF NOT EXISTS source varchar(32);

COMMENT ON COLUMN portal_visit_hit.source IS '回流来源（share=微信分享链接 / poster=二维码海报；非法或缺失为 NULL）';
