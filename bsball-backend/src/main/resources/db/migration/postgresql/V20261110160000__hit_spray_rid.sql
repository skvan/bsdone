-- 击球落点客户端幂等键：前端 rid 落库，服务端按 (game_id, rid) 去重
-- 兼容历史数据：旧记录 rid 为空；唯一索引为部分索引，不约束 NULL。
ALTER TABLE bs_hit_spray ADD COLUMN IF NOT EXISTS rid VARCHAR(64);

COMMENT ON COLUMN bs_hit_spray.rid IS '客户端幂等键（前端 rid，用于重复上传去重）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_hit_spray_game_rid
    ON bs_hit_spray (game_id, rid)
    WHERE rid IS NOT NULL;
