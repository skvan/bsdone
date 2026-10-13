-- 电子秩序册（Issue #310）：赛事参赛登记 + 报名球员名单
-- 语义：办赛方邀请球队（invited）→ 球队确认/谢绝（confirmed/declined）→ 提交报名名单（bs_event_roster_player）
--       → 办赛方锁定（roster_locked）后球队不可修改；办赛方移除=软删登记+名单行（先解锁）。
-- 注册类数据不设 platform_owned 标记（沿用 bs_player_team 先例）。

CREATE TABLE IF NOT EXISTS bs_event_team (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT NOT NULL,
    event_id            BIGINT NOT NULL,
    team_id             BIGINT NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'invited',
    invited_by          BIGINT,
    invited_at          TIMESTAMP,
    confirmed_at        TIMESTAMP,
    declined_at         TIMESTAMP,
    roster_submitted_at TIMESTAMP,
    roster_locked       BOOLEAN NOT NULL DEFAULT FALSE,
    locked_by           BIGINT,
    locked_at           TIMESTAMP,
    remark              VARCHAR(500),
    created_by BIGINT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT, deleted_at TIMESTAMP
);
COMMENT ON TABLE bs_event_team IS '赛事参赛登记';
COMMENT ON COLUMN bs_event_team.tenant_id IS '租户ID';
COMMENT ON COLUMN bs_event_team.event_id IS '赛事ID';
COMMENT ON COLUMN bs_event_team.team_id IS '球队ID';
COMMENT ON COLUMN bs_event_team.status IS '状态：invited/confirmed/declined';
COMMENT ON COLUMN bs_event_team.invited_by IS '邀请人';
COMMENT ON COLUMN bs_event_team.invited_at IS '邀请时间';
COMMENT ON COLUMN bs_event_team.confirmed_at IS '确认时间';
COMMENT ON COLUMN bs_event_team.declined_at IS '谢绝时间';
COMMENT ON COLUMN bs_event_team.roster_submitted_at IS '最近提交名单时间';
COMMENT ON COLUMN bs_event_team.roster_locked IS '名单锁定标记';
COMMENT ON COLUMN bs_event_team.locked_by IS '锁定人';
COMMENT ON COLUMN bs_event_team.locked_at IS '锁定时间';
COMMENT ON COLUMN bs_event_team.remark IS '备注';
COMMENT ON COLUMN bs_event_team.created_by IS '创建人';
COMMENT ON COLUMN bs_event_team.created_at IS '创建时间';
COMMENT ON COLUMN bs_event_team.updated_by IS '更新人';
COMMENT ON COLUMN bs_event_team.updated_at IS '更新时间';
COMMENT ON COLUMN bs_event_team.deleted_by IS '删除人';
COMMENT ON COLUMN bs_event_team.deleted_at IS '删除时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_event_team_event_team ON bs_event_team (event_id, team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_team_event_status ON bs_event_team (event_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_team_team ON bs_event_team (team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_team_tenant ON bs_event_team (tenant_id) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS bs_event_roster_player (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT NOT NULL,
    event_id   BIGINT NOT NULL,
    team_id    BIGINT NOT NULL,
    player_id  BIGINT NOT NULL,
    number     VARCHAR(64),
    sort       INTEGER NOT NULL DEFAULT 0,
    created_by BIGINT, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_by BIGINT, deleted_at TIMESTAMP
);
COMMENT ON TABLE bs_event_roster_player IS '秩序册报名球员';
COMMENT ON COLUMN bs_event_roster_player.tenant_id IS '租户ID';
COMMENT ON COLUMN bs_event_roster_player.event_id IS '赛事ID';
COMMENT ON COLUMN bs_event_roster_player.team_id IS '球队ID';
COMMENT ON COLUMN bs_event_roster_player.player_id IS '球员ID';
COMMENT ON COLUMN bs_event_roster_player.number IS '报名时背号快照（展示用）';
COMMENT ON COLUMN bs_event_roster_player.sort IS '排序';
COMMENT ON COLUMN bs_event_roster_player.created_by IS '创建人';
COMMENT ON COLUMN bs_event_roster_player.created_at IS '创建时间';
COMMENT ON COLUMN bs_event_roster_player.updated_by IS '更新人';
COMMENT ON COLUMN bs_event_roster_player.updated_at IS '更新时间';
COMMENT ON COLUMN bs_event_roster_player.deleted_by IS '删除人';
COMMENT ON COLUMN bs_event_roster_player.deleted_at IS '删除时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_event_roster_event_team_player ON bs_event_roster_player (event_id, team_id, player_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_roster_event_team ON bs_event_roster_player (event_id, team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_roster_player ON bs_event_roster_player (player_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_event_roster_tenant ON bs_event_roster_player (tenant_id) WHERE deleted_at IS NULL;
