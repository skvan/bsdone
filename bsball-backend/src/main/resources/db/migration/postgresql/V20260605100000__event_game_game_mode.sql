-- 赛事 / 比赛增加 game_mode 字段，区分棒球（BASEBALL）与垒球（SOFTBALL）

ALTER TABLE bs_event ADD COLUMN IF NOT EXISTS game_mode VARCHAR(20) DEFAULT 'BASEBALL';
COMMENT ON COLUMN bs_event.game_mode IS '比赛模式: BASEBALL 棒球 / SOFTBALL 垒球';

ALTER TABLE bs_game ADD COLUMN IF NOT EXISTS game_mode VARCHAR(20) DEFAULT 'BASEBALL';
COMMENT ON COLUMN bs_game.game_mode IS '比赛模式: BASEBALL 棒球 / SOFTBALL 垒球';
