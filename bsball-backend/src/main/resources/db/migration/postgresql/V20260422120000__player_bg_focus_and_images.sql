-- 球员背景：分端焦点配置（JSON）+ 多背景图（JSON 数组）
ALTER TABLE public.bs_player
    ADD COLUMN IF NOT EXISTS bg_focus_config JSONB,
    ADD COLUMN IF NOT EXISTS bg_images JSONB;

COMMENT ON COLUMN public.bs_player.bg_focus_config IS '背景图分端配置（JSON）';
COMMENT ON COLUMN public.bs_player.bg_images IS '背景图列表（JSON 数组，URL 字符串）';
