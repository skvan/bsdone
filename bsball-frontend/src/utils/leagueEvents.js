// 联盟下辖赛事过滤（Issue #329）：leagueId 归一比对 + 开始日期倒序；不改动原数组
export function filterEventsByLeague(events, leagueId) {
  const target = Number(leagueId);
  if (!Number.isFinite(target) || target <= 0) return [];
  if (!Array.isArray(events)) return [];
  return events
    .filter((ev) => ev && Number(ev.leagueId) === target)
    .sort((a, b) => String(b.startDate || '').localeCompare(String(a.startDate || '')));
}
