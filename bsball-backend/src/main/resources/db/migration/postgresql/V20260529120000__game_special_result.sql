DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bs_game' AND column_name = 'is_special_result') THEN
        ALTER TABLE bs_game ADD COLUMN is_special_result BOOLEAN DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bs_game' AND column_name = 'show_remark_in_card') THEN
        ALTER TABLE bs_game ADD COLUMN show_remark_in_card BOOLEAN DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'bs_game' AND column_name = 'include_stats_in_ranking') THEN
        ALTER TABLE bs_game ADD COLUMN include_stats_in_ranking BOOLEAN DEFAULT TRUE;
    END IF;
END $$;

COMMENT ON COLUMN bs_game.is_special_result IS '是否为特殊比赛（如不足人数判负等）';
COMMENT ON COLUMN bs_game.show_remark_in_card IS '是否将备注显示在比赛结果卡片中';
COMMENT ON COLUMN bs_game.include_stats_in_ranking IS '是否将本场比赛球员数据计入统计排行';
