-- 整名球员身份转记审计：统计行、逐位置守备、ER 判定；转记与删除源数据分离。
-- status: TRANSFERRED / DELETED（删除源数据是转记后的独立动作）。
CREATE TABLE IF NOT EXISTS bs_game_identity_transfer (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    game_id BIGINT NOT NULL,
    source_player_id BIGINT NOT NULL,
    source_team_id BIGINT,
    target_player_id BIGINT NOT NULL,
    target_team_id BIGINT,
    scopes VARCHAR(128) NOT NULL,
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

CREATE INDEX IF NOT EXISTS ix_bs_game_identity_transfer_game
    ON bs_game_identity_transfer (game_id, status);
