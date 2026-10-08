CREATE TABLE IF NOT EXISTS bs_earned_run_play (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    inning INTEGER NOT NULL,
    half VARCHAR(6) NOT NULL,
    sequence INTEGER NOT NULL,
    source_event_id VARCHAR(128) NOT NULL,
    actual_outs_added INTEGER NOT NULL DEFAULT 0,
    reconstructed_outs_added INTEGER NOT NULL DEFAULT 0,
    ruling_pending BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_earned_run_play_game FOREIGN KEY (game_id) REFERENCES bs_game(id),
    CONSTRAINT ck_earned_run_play_inning CHECK (inning > 0),
    CONSTRAINT ck_earned_run_play_half CHECK (half IN ('top', 'bottom')),
    CONSTRAINT ck_earned_run_play_sequence CHECK (sequence > 0),
    CONSTRAINT ck_earned_run_play_actual_outs CHECK (actual_outs_added BETWEEN 0 AND 3),
    CONSTRAINT ck_earned_run_play_reconstructed_outs
        CHECK (reconstructed_outs_added BETWEEN 0 AND 3),
    CONSTRAINT uk_earned_run_play_sequence UNIQUE (game_id, inning, half, sequence),
    CONSTRAINT uk_earned_run_play_source UNIQUE (game_id, source_event_id)
);

CREATE TABLE IF NOT EXISTS bs_earned_run_decision (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    play_id BIGINT NOT NULL,
    runner_id BIGINT NOT NULL,
    responsible_pitcher_id BIGINT NOT NULL,
    runner_origin VARCHAR(16) NOT NULL,
    earned_status VARCHAR(16) NOT NULL,
    unearned_reason VARCHAR(32),
    overridden_by BIGINT,
    overridden_at TIMESTAMP WITH TIME ZONE,
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_earned_run_decision_game FOREIGN KEY (game_id) REFERENCES bs_game(id),
    CONSTRAINT fk_earned_run_decision_play
        FOREIGN KEY (play_id) REFERENCES bs_earned_run_play(id) ON DELETE CASCADE,
    CONSTRAINT ck_earned_run_decision_origin
        CHECK (runner_origin IN ('NORMAL', 'ERROR', 'TIE_BREAK')),
    CONSTRAINT ck_earned_run_decision_status
        CHECK (earned_status IN ('EARNED', 'UNEARNED', 'PENDING')),
    CONSTRAINT ck_earned_run_decision_reason CHECK (
        (earned_status = 'UNEARNED' AND unearned_reason IS NOT NULL)
        OR (earned_status <> 'UNEARNED' AND unearned_reason IS NULL)
    ),
    CONSTRAINT ck_earned_run_decision_override CHECK (
        (overridden_by IS NULL AND overridden_at IS NULL)
        OR (overridden_by IS NOT NULL AND overridden_at IS NOT NULL)
    ),
    CONSTRAINT uk_earned_run_decision_runner UNIQUE (play_id, runner_id)
);

CREATE INDEX IF NOT EXISTS idx_earned_run_play_half
    ON bs_earned_run_play (tenant_id, game_id, inning, half, sequence);

CREATE INDEX IF NOT EXISTS idx_earned_run_decision_pitcher
    ON bs_earned_run_decision (tenant_id, game_id, responsible_pitcher_id, earned_status);

COMMENT ON TABLE bs_earned_run_play IS '自责分重建逐球事件';
COMMENT ON TABLE bs_earned_run_decision IS '逐得分自责分判定及责任投手';
COMMENT ON COLUMN bs_earned_run_play.reconstructed_outs_added IS '无失误假想局面的出局数增量';
COMMENT ON COLUMN bs_earned_run_decision.unearned_reason IS '非自责原因或人工覆核原因';
