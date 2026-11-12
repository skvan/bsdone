-- 赛前阵容提交（电子打序单）
-- 语义：一场比赛每队一条；球队负责人赛前编辑先发打线/守位/先发投手并保存，
-- 办赛方实时录入页在空侧自动带出、已有编辑时提示手动载入。阵容只作赛前来源，
-- 办赛方开始录入后仍以 bs_game.live_snapshot_json 为准。

CREATE TABLE IF NOT EXISTS bs_game_lineup_submission (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT NOT NULL,
    event_id    BIGINT NOT NULL,
    game_id     BIGINT NOT NULL,
    team_id     BIGINT NOT NULL,
    lineup_json TEXT NOT NULL,
    complete    BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT, deleted_at TIMESTAMP
);
COMMENT ON TABLE bs_game_lineup_submission IS '赛前阵容提交（电子打序单）';
COMMENT ON COLUMN bs_game_lineup_submission.tenant_id IS '租户ID';
COMMENT ON COLUMN bs_game_lineup_submission.event_id IS '赛事ID';
COMMENT ON COLUMN bs_game_lineup_submission.game_id IS '比赛ID';
COMMENT ON COLUMN bs_game_lineup_submission.team_id IS '球队ID';
COMMENT ON COLUMN bs_game_lineup_submission.lineup_json IS '本队赛前阵容 JSON（打线/替补/先发投手/DH池）';
COMMENT ON COLUMN bs_game_lineup_submission.complete IS '前端标记阵容是否完整（展示用）';
COMMENT ON COLUMN bs_game_lineup_submission.created_by IS '创建人';
COMMENT ON COLUMN bs_game_lineup_submission.created_at IS '创建时间';
COMMENT ON COLUMN bs_game_lineup_submission.updated_by IS '更新人';
COMMENT ON COLUMN bs_game_lineup_submission.updated_at IS '更新时间';
COMMENT ON COLUMN bs_game_lineup_submission.deleted_by IS '删除人';
COMMENT ON COLUMN bs_game_lineup_submission.deleted_at IS '删除时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_game_lineup_submission_game_team
    ON bs_game_lineup_submission (game_id, team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_game_lineup_submission_game
    ON bs_game_lineup_submission (game_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_game_lineup_submission_event_team
    ON bs_game_lineup_submission (event_id, team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_game_lineup_submission_tenant
    ON bs_game_lineup_submission (tenant_id) WHERE deleted_at IS NULL;
