-- 个人展示守位：本人自助维护（SELF 通道），仅用于个人资料与个人主场展示。
-- 球队名册守位仍以 bs_player.positions（由球队经历镜像）为准，比赛统计与排行不读取本列。
ALTER TABLE bs_player
    ADD COLUMN IF NOT EXISTS profile_positions TEXT;

COMMENT ON COLUMN bs_player.profile_positions IS
    '个人展示守位（JSON 数组）：本人自助维护，仅用于个人资料/个人主场展示，不参与比赛统计与排行';
