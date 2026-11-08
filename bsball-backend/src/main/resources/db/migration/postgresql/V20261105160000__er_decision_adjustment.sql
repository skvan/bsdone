-- ER 逐分判定人工改派/作废：保留原始判定与操作审计，有效责任投手由本表 ACTIVE 行决定。
-- 稳定键使用 source_event_id + runner_id，避免半局重建后 play/decision 行 ID 变化导致调整丢失。
CREATE TABLE IF NOT EXISTS bs_er_decision_adjustment (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    game_id BIGINT NOT NULL,
    decision_id BIGINT,
    source_event_id VARCHAR(255) NOT NULL,
    runner_id BIGINT NOT NULL,
    action VARCHAR(16) NOT NULL,
    target_pitcher_id BIGINT,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(200),
    revoked_by BIGINT,
    revoked_at TIMESTAMP,
    created_by BIGINT,
    created_at TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP,
    deleted_by BIGINT,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_bs_er_decision_adjustment_game
    ON bs_er_decision_adjustment (game_id, status);

CREATE INDEX IF NOT EXISTS ix_bs_er_decision_adjustment_source
    ON bs_er_decision_adjustment (tenant_id, game_id, source_event_id, runner_id);

CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_er_decision_adjustment_active
    ON bs_er_decision_adjustment (tenant_id, game_id, source_event_id, runner_id)
    WHERE status = 'ACTIVE';
