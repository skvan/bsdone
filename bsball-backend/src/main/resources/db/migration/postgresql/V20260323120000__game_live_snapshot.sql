-- 实时录入界面完整快照（阵容、局面、事件日志等），用于断线恢复，替代浏览器 localStorage
-- 空库时表可能尚不存在（Hibernate 稍后建表），仅当 bs_game 已存在时补列。
DO $flyway$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables t
        WHERE t.table_schema = current_schema()
          AND t.table_name = 'bs_game'
    ) THEN
        ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS live_snapshot_json TEXT;
        COMMENT ON COLUMN bs_game.live_snapshot_json IS '实时录入快照 JSON（前端 v=1 结构）';
    END IF;
END
$flyway$;
