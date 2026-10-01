-- 账号权限重构（批次 5，Task ⑭ 其它 delete 复刻）：历史数据处置权——平台资产归还标记
-- 语义：历史数据属平台资产；租户管理员及以下「删除」= 软删（退出租户运营面）+ platform_owned 置位
--       （归还系统租户 / 平台回收），**不改 tenant_id**；仅系统超管拥有最终处置（销毁）权，须审计。
-- 覆盖：教练 / 阵容模板 / 高光时刻 / 球场（league/team/player/event/game 见 V20260930140000）。
-- 用途：仅标记 + 超管资产汇总；不参与任何现有查询过滤。
-- 幂等：ADD COLUMN IF NOT EXISTS；禁用 DO $$ 块。

ALTER TABLE public.bs_coach                 ADD COLUMN IF NOT EXISTS platform_owned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.bs_team_lineup_template  ADD COLUMN IF NOT EXISTS platform_owned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.bs_highlight_moment      ADD COLUMN IF NOT EXISTS platform_owned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE public.bs_stadium               ADD COLUMN IF NOT EXISTS platform_owned BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN public.bs_coach.platform_owned                IS '平台资产归还标记：TRUE=已归还系统租户/平台回收（历史数据处置权，仅超管可最终销毁）';
COMMENT ON COLUMN public.bs_team_lineup_template.platform_owned IS '平台资产归还标记：TRUE=已归还系统租户/平台回收（历史数据处置权，仅超管可最终销毁）';
COMMENT ON COLUMN public.bs_highlight_moment.platform_owned     IS '平台资产归还标记：TRUE=已归还系统租户/平台回收（历史数据处置权，仅超管可最终销毁）';
COMMENT ON COLUMN public.bs_stadium.platform_owned              IS '平台资产归还标记：TRUE=已归还系统租户/平台回收（历史数据处置权，仅超管可最终销毁）';
