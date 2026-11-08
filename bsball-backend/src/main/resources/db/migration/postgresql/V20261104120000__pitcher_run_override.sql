-- 赛后复核：投手 R/ER 人工覆写。
-- 与 ER 引擎判定并列：APPROVED 行按比赛+投手唯一；重建半局或改判定时按规则失效，
-- 重算投手统计时优先应用人工值，撤销后恢复引擎绝对值。
CREATE TABLE IF NOT EXISTS bs_pitcher_run_override (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT,
    game_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    runs_allowed INTEGER NOT NULL,
    earned_runs INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
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

-- 同一比赛、同一投手只保留一条生效中的人工覆写；撤销/失效后重新覆写新建审计行。
CREATE UNIQUE INDEX IF NOT EXISTS ux_bs_pitcher_run_override_active
    ON bs_pitcher_run_override (game_id, player_id)
    WHERE status = 'APPROVED' AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_bs_pitcher_run_override_game_status
    ON bs_pitcher_run_override (game_id, status);
