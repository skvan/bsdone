-- 球场顶部介绍图（门户横幅，可与布局图区分）

ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS intro_image_url VARCHAR(500);

COMMENT ON COLUMN bs_stadium.intro_image_url IS '球场介绍横幅图（上传 URL）';
