// 门户统计页大小——行为保真移植自编译产物入口 chunk index-ByOnov1B.js
// 逐字对应：Kn=存储键、Yr=选项列表、Kr=合法集合、$n=解析
export const STATS_PAGE_SIZE_KEY = 'bsball.portal.stats.pageSize';
export const STATS_PAGE_SIZE_OPTIONS = [10, 20, 30, 50, 100];
const VALID_PAGE_SIZES = new Set(STATS_PAGE_SIZE_OPTIONS);

export function parseStatsPageSize(value) {
  if (value == null || value === '') return 20;
  const parsed = parseInt(value, 10);
  return VALID_PAGE_SIZES.has(parsed) ? parsed : 20;
}
