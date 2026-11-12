-- 投手行转记（赛后补录）：记录源投手行并合并到目标投手，保留前后值审计。
-- 转记与删除源行是两个独立动作；删除时更新 source_deleted_at，不删除审计行。
CREATE TABLE IF NOT EXISTS bs_game_pitcher_transfer (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    game_id BIGINT NOT NULL,
    source_stat_id BIGINT NOT NULL,
    source_team_id BIGINT,
    source_player_id BIGINT NOT NULL,
    target_stat_id BIGINT,
    target_team_id BIGINT,
    target_player_id BIGINT NOT NULL,
    source_before TEXT,
    target_before TEXT,
    target_after TEXT,
    status VARCHAR(16) NOT NULL,
    note VARCHAR(200),
    source_deleted_by BIGINT,
    source_deleted_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_bs_game_pitcher_transfer_game
    ON bs_game_pitcher_transfer (game_id, status);
