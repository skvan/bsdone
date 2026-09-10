-- 球场扩展：简介、交通、大屏幕、布局图、联系电话（参考公开球场介绍页字段）

ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(50);
ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS has_large_screen BOOLEAN;
ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS introduction TEXT;
ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS transportation_info TEXT;
ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS layout_diagram_url VARCHAR(500);

COMMENT ON COLUMN bs_stadium.contact_phone IS '联系电话';
COMMENT ON COLUMN bs_stadium.has_large_screen IS '是否具备大型计分屏/大屏幕（空=未填）';
COMMENT ON COLUMN bs_stadium.introduction IS '球场简介';
COMMENT ON COLUMN bs_stadium.transportation_info IS '交通资讯（多行文本）';
COMMENT ON COLUMN bs_stadium.layout_diagram_url IS '球场布局图（上传图片 URL）';
