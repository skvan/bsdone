-- 图标：Font Class、位移、填充色、分段样式 JSON；图库：名称、描述

ALTER TABLE sys_media_icon
    ADD COLUMN IF NOT EXISTS font_symbol VARCHAR(128),
    ADD COLUMN IF NOT EXISTS offset_x DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS offset_y DOUBLE PRECISION DEFAULT 0,
    ADD COLUMN IF NOT EXISTS fill_color VARCHAR(32) DEFAULT 'currentColor',
    ADD COLUMN IF NOT EXISTS part_styles_json TEXT;

COMMENT ON COLUMN sys_media_icon.font_symbol IS 'Font Class / 图标类名（如 icon-home），租户内唯一，可与业务 CSS 引用';
COMMENT ON COLUMN sys_media_icon.offset_x IS '展示平移 X（px）';
COMMENT ON COLUMN sys_media_icon.offset_y IS '展示平移 Y（px）';
COMMENT ON COLUMN sys_media_icon.fill_color IS '默认填充色，如 #333333 或 currentColor';
COMMENT ON COLUMN sys_media_icon.part_styles_json IS '分段图形样式 JSON 数组：[{index,scale,rotateDeg,translateX,translateY,fill}]';

UPDATE sys_media_icon
SET font_symbol = name
WHERE font_symbol IS NULL OR btrim(font_symbol) = '';

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_media_icon_tenant_font_symbol_alive
    ON sys_media_icon (tenant_id, font_symbol)
    WHERE deleted_at IS NULL AND font_symbol IS NOT NULL AND btrim(font_symbol) <> '';

ALTER TABLE sys_media_gallery_item
    ADD COLUMN IF NOT EXISTS name VARCHAR(256),
    ADD COLUMN IF NOT EXISTS description TEXT;

COMMENT ON COLUMN sys_media_gallery_item.name IS '图片名称';
COMMENT ON COLUMN sys_media_gallery_item.description IS '图片描述';

UPDATE sys_media_gallery_item
SET name = COALESCE(NULLIF(btrim(title), ''), '未命名')
WHERE name IS NULL;
