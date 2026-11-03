-- 守备聚合去重查找：按比赛+球员+球队定位 is_pitcher=0 的球员行（#446 关联）
CREATE INDEX IF NOT EXISTS idx_bs_game_player_stat_game_player_role
    ON bs_game_player_stat(game_id, player_id, team_id, is_pitcher);
