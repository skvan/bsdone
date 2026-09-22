// 赛事数据组合式 —— 行为移植自编译产物 useEventGames chunk
// 提供：赛事+比赛+球队选项加载（useEventGames）、按赛事聚合过滤（useGamesByEvent）、年份提取（gameYearOf）
import { computed, onMounted, ref } from 'vue';
import { fetchAllPages, fetchResult } from '../api/request';

export function gameYearOf(game) {
  const raw = game.gameday || game.gameTime || '';
  if (!raw) return null;
  const year = parseInt(String(raw).slice(0, 4), 10);
  return Number.isNaN(year) ? null : year;
}

const GAME_STATUSES = ['scheduled', 'live', 'final', 'postponed', 'cancelled'];

export function normalizeGameStatus(status) {
  if (status == null || String(status).trim() === '') return 'scheduled';
  const s = String(status).trim().toLowerCase();
  return GAME_STATUSES.includes(s) ? s : 'scheduled';
}

export function useEventGames() {
  const loading = ref(true);
  const error = ref(null);
  const events = ref([]);
  const gamesWithMeta = ref([]);
  const teamsFull = ref([]);

  async function load() {
    loading.value = true;
    error.value = null;
    try {
      const [games, teamOptions, eventList] = await Promise.all([
        fetchAllPages('/api/game/list', {}),
        fetchResult('/api/team/select-options'),
        fetchAllPages('/api/event/list', {})
      ]);
      if (games.error) throw new Error(games.error);
      if (teamOptions.error) throw new Error(teamOptions.error);
      if (eventList.error) throw new Error(eventList.error);
      const teams = teamOptions.data ?? [];
      teamsFull.value = teams;
      const nameById = Object.fromEntries(teams.map((t) => [t.id, t.name]));
      gamesWithMeta.value = games.list.map((g) => ({
        ...g,
        homeName: nameById[g.homeTeamId],
        awayName: nameById[g.awayTeamId]
      }));
      events.value = eventList.list.sort((a, b) => (b.startDate || '').localeCompare(a.startDate || ''));
    } catch (e) {
      error.value = e?.message ?? '加载失败';
    } finally {
      loading.value = false;
    }
  }

  onMounted(load);
  return { loading, error, events, gamesWithMeta, teamsFull, retry: load };
}

// 按赛事聚合比赛（支持：指定赛事/年份集合/球队集合/状态/比赛模式过滤，倒序排列）
export function useGamesByEvent(events, gamesWithMeta, eventIdFilter, yearsFilter, teamIdsFilter, statusFilter, gameModeFilter) {
  return computed(() => {
    const byEvent = new Map();
    events.value.forEach((e) => byEvent.set(e.id, { ...e, games: [] }));
    let list = gamesWithMeta.value;
    if (eventIdFilter?.value) list = list.filter((g) => g.eventId === eventIdFilter.value);
    if (yearsFilter?.value?.length) {
      const years = yearsFilter.value;
      list = list.filter((g) => {
        const year = gameYearOf(g);
        return year != null && years.includes(year);
      });
    }
    if (teamIdsFilter?.value?.length) {
      const teamIds = new Set(teamIdsFilter.value.map((id) => Number(id)).filter((id) => Number.isFinite(id) && id > 0));
      list = list.filter((g) => teamIds.has(g.homeTeamId) || teamIds.has(g.awayTeamId));
    }
    const status = statusFilter?.value;
    if (status != null && status !== '') list = list.filter((g) => normalizeGameStatus(g.status) === status);
    const mode = gameModeFilter?.value;
    if (mode) list = list.filter((g) => (byEvent.get(g.eventId)?.gameMode || 'BASEBALL') === mode);
    list.forEach((g) => {
      const entry = byEvent.get(g.eventId);
      if (entry) entry.games.push(g);
    });
    let result = Array.from(byEvent.values());
    if (mode) result = result.filter((e) => e.games.length > 0);
    return result.sort((a, b) => (b.startDate || '').localeCompare(a.startDate || ''));
  });
}
