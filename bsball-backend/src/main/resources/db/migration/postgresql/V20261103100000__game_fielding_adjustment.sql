-- 赛后守备补记：失误导致本应完成的出局未发生时，记录员复盘补记的助杀 A。
-- 独立于 ER 判定表；统计事实仍写回 bs_game_player_stat.a/tc 与 bs_game_fielding_pos_stat.a。
CREATE TABLE IF NOT EXISTS bs_game_fielding_adjustment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    game_id BIGINT NOT NULL,
    source_event_id VARCHAR(128) NOT NULL,
    source_sequence INTEGER,
    inning INTEGER,
    top_bottom VARCHAR(8),
    team_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    position VARCHAR(8) NOT NULL,
    assist_count INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(16) NOT NULL,
    source_text VARCHAR(512),
    note VARCHAR(200),
    revoked_by BIGINT,
    revoked_at TIMESTAMP,
    invalid_reason VARCHAR(200),
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

-- 同一 Play、同一守备员只保留一条补记（不区分守位）；撤销后重新补记复用原行。
CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_game_fielding_adjustment_source
    ON bs_game_fielding_adjustment (game_id, source_event_id, player_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_bs_game_fielding_adjustment_game_status
    ON bs_game_fielding_adjustment (game_id, status);
