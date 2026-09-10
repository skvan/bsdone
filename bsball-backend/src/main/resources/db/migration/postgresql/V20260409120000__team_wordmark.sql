-- 球队文字标识（无图，与队徽 logo 图像区分）
ALTER TABLE bs_team ADD COLUMN IF NOT EXISTS wordmark VARCHAR(64);
COMMENT ON COLUMN bs_team.wordmark IS '文字标识（英文或数字等组合）；队徽仍为 logo 图像 URL';
