-- 统一修订台账：结果补录页手工统计修改按字段留痕；领域审计表继续保留。
CREATE TABLE IF NOT EXISTS bs_game_stat_adjustment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    game_id BIGINT NOT NULL,
    scope VARCHAR(32) NOT NULL,
    action VARCHAR(32) NOT NULL,
    source_type VARCHAR(32),
    source_id BIGINT,
    team_id BIGINT,
    player_id BIGINT NOT NULL,
    is_pitcher INTEGER,
    field_key VARCHAR(64),
    field_label VARCHAR(64),
    before_value VARCHAR(128),
    after_value VARCHAR(128),
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(200),
    operator_id BIGINT,
    operated_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_bs_game_stat_adjustment_game
    ON bs_game_stat_adjustment (tenant_id, game_id, operated_at);

CREATE INDEX IF NOT EXISTS ix_bs_game_stat_adjustment_player
    ON bs_game_stat_adjustment (game_id, player_id, is_pitcher);
