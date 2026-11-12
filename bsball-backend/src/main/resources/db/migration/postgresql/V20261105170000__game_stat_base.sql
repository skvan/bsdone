-- 比赛统计基座快照：首次 final 保存时保留当时统计行与逐位置守备，后续人工修订不覆盖。
-- 历史场次没有基座记录时由接口标记 LEGACY，不伪造原始值。
CREATE TABLE IF NOT EXISTS bs_game_stat_base (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    game_id BIGINT NOT NULL,
    team_id BIGINT,
    player_id BIGINT NOT NULL,
    is_pitcher INTEGER,
    source VARCHAR(16) NOT NULL,
    stat_json TEXT,
    fielding_pos_json TEXT,
    captured_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_game_stat_base_player
    ON bs_game_stat_base (game_id, player_id, COALESCE(is_pitcher, 0));

CREATE INDEX IF NOT EXISTS ix_bs_game_stat_base_game
    ON bs_game_stat_base (tenant_id, game_id);
