-- 逐位置守备统计：每场每队每球员每守位一行，outs 为守备出局数（3 outs = 1 局）。
-- 作为按位置排行与后续逐位置守备指标（PO/A/E 等）的事实来源；bs_game_player_stat.def_inn 为球员级汇总。
CREATE TABLE IF NOT EXISTS bs_game_fielding_pos_stat (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    position VARCHAR(8) NOT NULL,
    outs INTEGER NOT NULL DEFAULT 0,
    fielding_gs BOOLEAN NOT NULL DEFAULT FALSE,
    source VARCHAR(16) NOT NULL DEFAULT 'LIVE',
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_game_fielding_pos_stat
    ON bs_game_fielding_pos_stat (game_id, team_id, player_id, position)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_bs_game_fielding_pos_stat_game
    ON bs_game_fielding_pos_stat (game_id);

CREATE INDEX IF NOT EXISTS ix_bs_game_fielding_pos_stat_player_pos
    ON bs_game_fielding_pos_stat (tenant_id, player_id, position);

CREATE INDEX IF NOT EXISTS ix_bs_game_fielding_pos_stat_rank
    ON bs_game_fielding_pos_stat (tenant_id, position, player_id);
