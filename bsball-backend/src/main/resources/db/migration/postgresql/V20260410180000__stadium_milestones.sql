-- 球场兴建年代、开放日、重要日期记事
ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS construction_era VARCHAR(120);
ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS opened_on DATE;
ALTER TABLE bs_stadium ADD COLUMN IF NOT EXISTS important_dates_note TEXT;

COMMENT ON COLUMN bs_stadium.construction_era IS '兴建年代（自由文本，如 2004~2006）';
COMMENT ON COLUMN bs_stadium.opened_on IS '开放日期';
COMMENT ON COLUMN bs_stadium.important_dates_note IS '重要日期记事（扩建、职棒首战、明星赛、总冠军赛等，多行）';
