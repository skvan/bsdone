-- 球员表：列 college 重命名为 education（业务含义「学历」）
DO $flyway$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'bs_player'
      AND column_name = 'college'
  ) THEN
    ALTER TABLE bs_player RENAME COLUMN college TO education;
  END IF;
END
$flyway$;

COMMENT ON COLUMN bs_player.education IS '学历';
