// 门户赛事数据组合式 —— 行为移植自编译产物 useEventGames chunk（逐字）
import { computed, ref, onMounted } from 'vue';
import { fetchData, fetchAllPages } from '../api/request';

// 从赛事/比赛记录提取年份（入口 S）
export function getEventYear(item) {
  const text = item.gameday || item.gameTime || '';
  if (!text) return null;
  const year = parseInt(String(text).slice(0, 4), 10);
  return isNaN(year) ? null : year;
}

const GAME_STATUSES = ['scheduled', 'live', 'final', 'postponed', 'cancelled'];

function normalizeGameStatus(value) {
  if (value == null || String(value).trim() === '') return 'scheduled';
  const lower = String(value).trim().toLowerCase();
  return GAME_STATUSES.includes(lower) ? lower : 'scheduled';
}

// 赛事/比赛/球队全量加载（入口 M）
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
      const [gamesRes, teamsRes, eventsRes] = await Promise.all([
        fetchAllPages('/api/game/list', {}),
        fetchData('/api/team/select-options'),
        fetchAllPages('/api/event/list', {})
      ]);
      if (gamesRes.error) throw new Error(gamesRes.error);
      if (teamsRes.error) throw new Error(teamsRes.error);
      if (eventsRes.error) throw new Error(eventsRes.error);
      const teams = teamsRes.data ?? [];
      teamsFull.value = teams;
      const teamNameById = Object.fromEntries(teams.map((t) => [t.id, t.name]));
      gamesWithMeta.value = gamesRes.list.map((g) => ({ ...g, homeName: teamNameById[g.homeTeamId], awayName: teamNameById[g.awayTeamId] }));
      events.value = eventsRes.list.sort((a, b) => {
        const left = a.startDate || '';
        return (b.startDate || '').localeCompare(left);
      });
    } catch (e) {
      error.value = e?.message ?? '加载失败';
    } finally {
      loading.value = false;
    }
  }
  onMounted(load);
  return { loading, error, events, gamesWithMeta, teamsFull, retry: load };
}

// 按赛事分组 + 多维过滤（入口 T）
export function computeEventGroups(eventsRef, gamesRef, eventIdRef, yearsRef, teamIdsRef, statusRef, gameModeRef) {
  return computed(() => {
    const byEvent = new Map();
    eventsRef.value.forEach((ev) => byEvent.set(ev.id, { ...ev, games: [] }));
    let list = gamesRef.value;
    if (eventIdRef.value) list = list.filter((g) => g.eventId === eventIdRef.value);
    if (yearsRef?.value && yearsRef.value.length > 0) {
      const years = yearsRef.value;
      list = list.filter((g) => {
        const year = getEventYear(g);
        return year != null && years.includes(year);
      });
    }
    if (teamIdsRef?.value && teamIdsRef.value.length > 0) {
      const teamIds = new Set(teamIdsRef.value.map((v) => Number(v)).filter((v) => Number.isFinite(v) && v > 0));
      list = list.filter((g) => teamIds.has(g.homeTeamId) || teamIds.has(g.awayTeamId));
    }
    const status = statusRef?.value;
    if (status != null && status !== '') list = list.filter((g) => normalizeGameStatus(g.status) === status);
    const gameMode = gameModeRef?.value;
    if (gameMode) list = list.filter((g) => (byEvent.get(g.eventId)?.gameMode || 'BASEBALL') === gameMode);
    list.forEach((g) => {
      const group = byEvent.get(g.eventId);
      if (group) group.games.push(g);
    });
    let groups = Array.from(byEvent.values());
    if (gameMode) groups = groups.filter((g) => g.games.length > 0);
    groups.sort((a, b) => {
      const left = a.startDate || '';
      return (b.startDate || '').localeCompare(left);
    });
    return groups;
  });
}
