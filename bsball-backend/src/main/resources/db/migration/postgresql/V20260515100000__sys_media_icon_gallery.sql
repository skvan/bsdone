-- SVG 图标库、图库（按租户；软删除时允许同名重建）

CREATE TABLE IF NOT EXISTS sys_media_icon (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES sys_tenant (id),
    name            VARCHAR(128) NOT NULL,
    svg_content     TEXT NOT NULL,
    tags            VARCHAR(500),
    scale           DOUBLE PRECISION NOT NULL DEFAULT 1,
    rotate_deg      DOUBLE PRECISION NOT NULL DEFAULT 0,
    remark          VARCHAR(500),
    created_by      BIGINT,
    created_at      TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP,
    deleted_by      BIGINT,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_media_icon_tenant ON sys_media_icon (tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_media_icon_tenant_name_alive
    ON sys_media_icon (tenant_id, name) WHERE deleted_at IS NULL;

COMMENT ON TABLE sys_media_icon IS 'SVG 图标库（名称租户内唯一，存 SVG 源码与展示用变换）';
COMMENT ON COLUMN sys_media_icon.name IS '图标唯一名称（英文/数字/下划线等）';
COMMENT ON COLUMN sys_media_icon.svg_content IS 'SVG 源码';
COMMENT ON COLUMN sys_media_icon.tags IS '标签，逗号分隔';
COMMENT ON COLUMN sys_media_icon.scale IS '展示缩放';
COMMENT ON COLUMN sys_media_icon.rotate_deg IS '展示旋转角度';

CREATE TABLE IF NOT EXISTS sys_media_gallery_item (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL REFERENCES sys_tenant (id),
    image_url       VARCHAR(1024) NOT NULL,
    tags            VARCHAR(500),
    title           VARCHAR(256),
    sort_order      INTEGER NOT NULL DEFAULT 0,
    created_by      BIGINT,
    created_at      TIMESTAMP,
    updated_by      BIGINT,
    updated_at      TIMESTAMP,
    deleted_by      BIGINT,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_media_gallery_tenant ON sys_media_gallery_item (tenant_id);

COMMENT ON TABLE sys_media_gallery_item IS '图库图片（标签筛选，供文章轮播等选图）';
COMMENT ON COLUMN sys_media_gallery_item.image_url IS '图片访问 URL（通常来自本地上传）';
