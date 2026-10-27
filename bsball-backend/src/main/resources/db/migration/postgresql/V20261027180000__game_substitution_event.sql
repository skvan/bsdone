-- 实时录入换人事务：一次“换人模式”提交写一组净变化事件，作为箱线图/审计/重建的显式依据。
-- 统计事实仍在 bs_game_player_stat；本表只记录 who→whom、槽位、守位与事务分组。
CREATE TABLE IF NOT EXISTS bs_game_substitution_event (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    tenant_id BIGINT,
    transaction_id VARCHAR(64) NOT NULL,
    inning INTEGER,
    top_bottom VARCHAR(8),
    sequence INTEGER NOT NULL,
    change_seq INTEGER NOT NULL,
    change_type VARCHAR(24) NOT NULL,
    slot INTEGER,
    out_player_id BIGINT,
    in_player_id BIGINT,
    out_position VARCHAR(8),
    in_position VARCHAR(8),
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

-- 同一场同一事务只落一次：前端重试/重复保存不会产生重复换人。
CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_game_substitution_event_tx
    ON bs_game_substitution_event (game_id, transaction_id, change_seq)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_bs_game_substitution_event_game_seq
    ON bs_game_substitution_event (game_id, team_id, sequence);
