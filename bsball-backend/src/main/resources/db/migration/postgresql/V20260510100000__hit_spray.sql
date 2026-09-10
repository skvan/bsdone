-- 击球落点记录表（与 Hibernate 实体 HitSpray / ddl-auto 列名一致；表可能已由 Hibernate 先创建）
CREATE TABLE IF NOT EXISTS bs_hit_spray (
    id              BIGSERIAL PRIMARY KEY,
    game_id         BIGINT,
    player_id       BIGINT,
    team_id         BIGINT,
    inning          SMALLINT,
    half            VARCHAR(255),
    batter_order    SMALLINT,
    outs_before     SMALLINT,
    runners_before  VARCHAR(255),
    result_code     VARCHAR(255),
    bip_code        VARCHAR(255),
    rbi             SMALLINT,
    sprayx          NUMERIC(38, 2),
    sprayy          NUMERIC(38, 2),
    spray_zone      VARCHAR(255),
    spray_depth     VARCHAR(255),
    recorded_at     TIMESTAMP(6),
    created_by      BIGINT,
    created_at      TIMESTAMP(6),
    updated_by      BIGINT,
    updated_at      TIMESTAMP(6),
    deleted_by      BIGINT,
    deleted_at      TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_hit_spray_game ON bs_hit_spray (game_id);
CREATE INDEX IF NOT EXISTS idx_hit_spray_player ON bs_hit_spray (player_id);
CREATE INDEX IF NOT EXISTS idx_hit_spray_team_game ON bs_hit_spray (team_id, game_id);
CREATE INDEX IF NOT EXISTS idx_hit_spray_result ON bs_hit_spray (result_code);
CREATE INDEX IF NOT EXISTS idx_hit_spray_zone ON bs_hit_spray (spray_zone);

COMMENT ON TABLE bs_hit_spray IS '击球落点记录表';
COMMENT ON COLUMN bs_hit_spray.game_id IS '比赛ID';
COMMENT ON COLUMN bs_hit_spray.player_id IS '击球员ID';
COMMENT ON COLUMN bs_hit_spray.team_id IS '球队ID';
COMMENT ON COLUMN bs_hit_spray.inning IS '局数';
COMMENT ON COLUMN bs_hit_spray.half IS '上下半局';
COMMENT ON COLUMN bs_hit_spray.batter_order IS '棒次';
COMMENT ON COLUMN bs_hit_spray.outs_before IS '出局数（击球前）';
COMMENT ON COLUMN bs_hit_spray.runners_before IS '垒况（如"1,3"表示一三垒有人）';
COMMENT ON COLUMN bs_hit_spray.result_code IS '结果代码（H1/H2/H3/HR/OUT_F等）';
COMMENT ON COLUMN bs_hit_spray.bip_code IS 'BIP详细代码（bip:fly:1b等）';
COMMENT ON COLUMN bs_hit_spray.rbi IS '打点';
COMMENT ON COLUMN bs_hit_spray.sprayx IS '落点X坐标（viewBox 0-480）';
COMMENT ON COLUMN bs_hit_spray.sprayy IS '落点Y坐标（viewBox 0-640）';
COMMENT ON COLUMN bs_hit_spray.spray_zone IS '落点区域（IF/LF/LCF/CF/RCF/RF）';
COMMENT ON COLUMN bs_hit_spray.spray_depth IS '落点深度（浅/中/深）';
COMMENT ON COLUMN bs_hit_spray.recorded_at IS '记录时间';
