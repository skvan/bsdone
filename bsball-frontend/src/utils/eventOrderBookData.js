import { buildOrderBookSections } from './orderBook.js';

/**
 * 门户报名名单每次下载都重新取数；request 注入便于验证不会跨次缓存。
 * 只对“已确认且已提交名单”的球队拉取 roster，失败单队保留占位。
 */
export async function loadEventOrderBookData(eventId, request, timestamp = Date.now()) {
  if (typeof request !== 'function') throw new TypeError('request is required');
  const suffix = `_ts=${encodeURIComponent(String(timestamp))}`;
  const teamsResult = await request(`/api/event/${eventId}/teams?${suffix}`);
  if (teamsResult?.error) throw new Error(String(teamsResult.error));
  const teams = Array.isArray(teamsResult?.data) ? teamsResult.data : [];
  const toFetch = teams.filter((x) => x?.status === 'confirmed' && x?.rosterSubmittedAt != null);
  const rosterMap = {};

  await Promise.all(toFetch.map(async (team) => {
    try {
      const result = await request(`/api/event/${eventId}/teams/${team.teamId}/roster?${suffix}`);
      rosterMap[team.teamId] = result?.error ? { failed: true } : { rows: result?.data ?? [] };
    } catch {
      rosterMap[team.teamId] = { failed: true };
    }
  }));

  return { sections: buildOrderBookSections(teams, rosterMap) };
}
