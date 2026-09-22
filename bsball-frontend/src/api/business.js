// 业务域 API —— 行为与端点移植自编译产物 business chunk（B0 契约）
import { get, post, put, del, fetchList, fetchResult, fetchAllPages } from './request';

export const leagueApi = {
  list: (params) => fetchList('/api/league/list', params),
  get: (id) => get(`/api/league/${id}`),
  create: (data) => post('/api/league/create', data),
  update: (id, data) => put(`/api/league/update/${id}`, data),
  delete: (id) => del(`/api/league/delete/${id}`)
};

export const regionApi = {
  chinaChildren: (parentAdcode) =>
    get(
      parentAdcode
        ? `/api/region/china/children?parentAdcode=${encodeURIComponent(parentAdcode)}`
        : '/api/region/china/children'
    ).then((data) => data ?? [])
};

export const stadiumApi = {
  list: (params) => fetchList('/api/stadium/list', params),
  geoJson: () => get('/api/stadium/geojson'),
  nearby: (lng, lat, radiusMeters = 50000) =>
    get(`/api/stadium/nearby?lng=${lng}&lat=${lat}&radiusMeters=${radiusMeters}`),
  get: (id) => get(`/api/stadium/${id}`),
  create: (data) => post('/api/stadium/create', data),
  update: (id, data) => put(`/api/stadium/update/${id}`, data),
  delete: (id) => del(`/api/stadium/delete/${id}`)
};

export const teamApi = {
  list: (params) => fetchList('/api/team/list', params),
  selectOptions: () => get('/api/team/select-options'),
  get: (id) => get(`/api/team/${id}`),
  create: (data) => post('/api/team/create', data),
  update: (id, data) => put(`/api/team/update/${id}`, data),
  delete: (id) => del(`/api/team/delete/${id}`)
};

export const lineupTemplateApi = {
  listPage: (params) => fetchList('/api/lineup-template/list', params),
  list: (teamId) => get(`/api/team/${teamId}/lineup-template/list`),
  get: (teamId, templateId) => get(`/api/team/${teamId}/lineup-template/${templateId}`),
  create: (teamId, data) => post(`/api/team/${teamId}/lineup-template/create`, data),
  update: (teamId, templateId, data) => put(`/api/team/${teamId}/lineup-template/update/${templateId}`, data),
  delete: (teamId, templateId) => del(`/api/team/${teamId}/lineup-template/delete/${templateId}`),
  copyFromGame: (teamId, data) => post(`/api/team/${teamId}/lineup-template/copy-from-game`, data)
};

export const personnelChangeApi = {
  list: (params) => fetchList('/api/personnel-change/list', params),
  create: (data) => post('/api/personnel-change/create', data)
};

export const highlightMomentApi = {
  list: (params) => fetchList('/api/highlight-moment/list', params),
  create: (data) => post('/api/highlight-moment/create', data),
  update: (id, data) => put(`/api/highlight-moment/update/${id}`, data),
  delete: (id) => del(`/api/highlight-moment/delete/${id}`)
};

export const coachApi = {
  list: (params) => fetchList('/api/coach/list', params),
  selectOptions: () => get('/api/coach/select-options'),
  get: (id) => get(`/api/coach/${id}`),
  create: (data) => post('/api/coach/create', data),
  update: (id, data) => put(`/api/coach/update/${id}`, data),
  delete: (id) => del(`/api/coach/delete/${id}`)
};

export const playerApi = {
  selectOptions: () => get('/api/player/select-options'),
  teamOptions: (teamId) => get(`/api/player/team-options?teamId=${encodeURIComponent(String(teamId))}`),
  list: (params) => {
    const query = { ...params };
    if (query.teamId === undefined || query.teamId === null || query.teamId === '') delete query.teamId;
    if (Array.isArray(query.ids) && query.ids.length) query.ids = query.ids.join(',');
    else delete query.ids;
    return fetchList('/api/player/list', query);
  },
  listByIds: (ids) =>
    ids.length ? fetchList('/api/player/list', { ids: ids.join(',') }) : Promise.resolve({ list: [], total: 0 }),
  checkFullNameDuplicate: async (name, excludeId) => {
    const trimmed = name.trim();
    if (!trimmed) return { duplicate: false };
    const query = new URLSearchParams({ name: trimmed });
    if (excludeId != null) query.set('excludeId', String(excludeId));
    const { data, error } = await fetchResult(`/api/player/check-full-name?${query}`);
    if (error || !data) return { duplicate: false };
    return { duplicate: !!data.duplicate };
  },
  get: (id) => get(`/api/player/${id}`),
  getStats: (id, params) => get(`/api/player/${id}/stats`, params),
  getStatsBySeason: (id, params) => get(`/api/player/${id}/stats/by-season`, params).then((data) => data ?? []),
  getGameLog: (id, options) => {
    const params = { limit: options?.limit ?? 30 };
    if (options?.gameMode) params.gameMode = options.gameMode;
    return get(`/api/player/${id}/stats/game-log`, params).then((data) => data ?? []);
  },
  drillDownBatting: (id, params) => fetchList(`/api/player/${id}/stats/drill-down/batting`, params),
  drillDownPitching: (id, params) => fetchList(`/api/player/${id}/stats/drill-down/pitching`, params),
  drillDownFielding: (id, params) => fetchList(`/api/player/${id}/stats/drill-down/fielding`, params),
  create: (data) => post('/api/player/create', data),
  update: (id, data) => put(`/api/player/update/${id}`, data),
  delete: (id) => del(`/api/player/delete/${id}`),
  deleteBatch: (ids) => post('/api/player/delete-batch', { ids }),
  import: async (data) => (await post('/api/player/import', data)).data
};

// --- playerApi 辅助（缓存/全量/按队拉取） ---

const PLAYERS_BY_TEAM_TTL = 30000;
const playersByTeamCache = new Map();
const playersByTeamInflight = new Map();

export async function fetchPlayersByTeam(teamId) {
  const cached = playersByTeamCache.get(teamId);
  if (cached && Date.now() - cached.ts < PLAYERS_BY_TEAM_TTL) return cached.data;
  let inflight = playersByTeamInflight.get(teamId);
  if (!inflight) {
    inflight = (async () => {
      const list = [];
      let page = 1;
      while (page <= 200) {
        const { list: pageList, total } = await playerApi.list({ teamId, page, pageSize: 100 });
        const rows = pageList ?? [];
        list.push(...rows);
        if (list.length >= (total ?? 0) || rows.length < 100) break;
        page += 1;
      }
      playersByTeamCache.set(teamId, { data: list, ts: Date.now() });
      return list;
    })();
    inflight.finally(() => {
      playersByTeamInflight.delete(teamId);
    });
    playersByTeamInflight.set(teamId, inflight);
  }
  return inflight;
}

export async function fetchAllLeagues() {
  const { list } = await fetchAllPages('/api/league/list');
  return list;
}

export async function fetchAllStadiums() {
  const { list } = await fetchAllPages('/api/stadium/list');
  return list;
}

export async function fetchAllPlayers(params) {
  const { list } = await fetchAllPages('/api/player/list', params);
  return list;
}

export async function fetchGamesByTeam(teamId) {
  const { list, error } = await fetchAllPages('/api/game/list', { teamId });
  if (error) throw new Error(error);
  return list;
}

export const eventApi = {
  list: (params) => fetchList('/api/event/list', params),
  get: (id) => get(`/api/event/${id}`),
  create: (data) => post('/api/event/create', data),
  update: (id, data) => put(`/api/event/update/${id}`, data),
  delete: (id) => del(`/api/event/delete/${id}`),
  importGameResult: (id, data) => post(`/api/event/${id}/import-game-result`, data)
};

export const gameApi = {
  list: (params) => {
    const query = typeof params === 'number' ? { eventId: params } : params;
    const out = {};
    if (query?.eventId != null) out.eventId = query.eventId;
    if (query?.eventIds?.length) out.eventIds = query.eventIds.join(',');
    if (query?.years?.length) out.years = query.years.join(',');
    if (query?.teamId != null) out.teamId = query.teamId;
    if (query?.page != null) out.page = query.page;
    if (query?.pageSize != null) out.pageSize = query.pageSize;
    if (query?.sortProp != null) out.sortProp = query.sortProp;
    if (query?.sortOrder != null) out.sortOrder = query.sortOrder;
    return fetchList('/api/game/list', Object.keys(out).length ? out : undefined);
  },
  get: (id) => get(`/api/game/${id}`),
  create: (data) => post('/api/game/create', data),
  update: (id, data) => put(`/api/game/update/${id}`, data),
  delete: (id) => del(`/api/game/delete/${id}`),
  saveLive: (id, data) => post(`/api/game/${id}/save-live`, data),
  getLiveSnapshot: async (id) => {
    const { data } = await fetchResult(`/api/game/${id}/live-snapshot?_t=${Date.now()}`);
    return data?.snapshotJson ?? '';
  },
  saveLiveSnapshot: (id, snapshotJson) => post(`/api/game/${id}/live-snapshot`, { snapshotJson }),
  saveResult: (id, data) => post(`/api/game/${id}/save-result`, data)
};

export const gameStatsApi = {
  listByGame: (gameId) => fetchList(`/api/game/${gameId}/stats`),
  create: (data) => post('/api/game/stats/create', data),
  update: (id, data) => put(`/api/game/stats/update/${id}`, data),
  delete: (id) => del(`/api/game/stats/delete/${id}`)
};

// 统计榜参数序列化（与编译产物逐字一致）
function statsQuery(params) {
  const parts = [];
  if (params?.eventId != null) parts.push(`eventId=${params.eventId}`);
  if (params?.eventIds?.length) parts.push(`eventIds=${params.eventIds.join(',')}`);
  if (params?.years?.length) parts.push(`years=${params.years.join(',')}`);
  if (params?.teamIds?.length) parts.push(`teamIds=${params.teamIds.join(',')}`);
  else if (params?.teamId != null) parts.push(`teamId=${params.teamId}`);
  if (params?.playerName) parts.push(`playerName=${encodeURIComponent(params.playerName)}`);
  if (params?.position) parts.push(`position=${encodeURIComponent(params.position)}`);
  if (params?.homeAway) parts.push(`homeAway=${encodeURIComponent(params.homeAway)}`);
  if (params?.batterHand) parts.push(`batterHand=${encodeURIComponent(params.batterHand)}`);
  if (params?.pitcherHand) parts.push(`pitcherHand=${encodeURIComponent(params.pitcherHand)}`);
  if (params?.gameMode) parts.push(`gameMode=${encodeURIComponent(params.gameMode)}`);
  if (params?.page != null) parts.push(`page=${params.page}`);
  if (params?.pageSize != null) parts.push(`pageSize=${params.pageSize}`);
  if (params?.sortProp) parts.push(`sortProp=${encodeURIComponent(params.sortProp)}`);
  if (params?.sortOrder) parts.push(`sortOrder=${encodeURIComponent(params.sortOrder)}`);
  return parts.length ? '?' + parts.join('&') : '';
}

function standingsQuery(params) {
  const parts = [];
  if (params?.eventIds?.length) parts.push(`eventIds=${params.eventIds.join(',')}`);
  if (params?.years?.length) parts.push(`years=${params.years.join(',')}`);
  if (params?.gameMode) parts.push(`gameMode=${encodeURIComponent(params.gameMode)}`);
  if (params?.page != null) parts.push(`page=${params.page}`);
  if (params?.pageSize != null) parts.push(`pageSize=${params.pageSize}`);
  return parts.length ? '?' + parts.join('&') : '';
}

export const statsApi = {
  getBatting: (params) => get(`/api/stats/leaders/batting${statsQuery(params)}`),
  getPitching: (params) => get(`/api/stats/leaders/pitching${statsQuery(params)}`),
  getFielding: (params) => get(`/api/stats/leaders/fielding${statsQuery(params)}`),
  getStandings: (params) => get(`/api/stats/standings${standingsQuery(params)}`),
  getTeamBatting: (params) => get(`/api/stats/leaders/team-batting${statsQuery(params)}`),
  getTeamPitching: (params) => get(`/api/stats/leaders/team-pitching${statsQuery(params)}`),
  getTeamFielding: (params) => get(`/api/stats/leaders/team-fielding${statsQuery(params)}`),
  getStarTopList: (params) => {
    const parts = [];
    if (params?.eventId != null) parts.push(`eventId=${params.eventId}`);
    if (params?.eventIds?.length) parts.push(`eventIds=${params.eventIds.join(',')}`);
    if (params?.years?.length) parts.push(`years=${params.years.join(',')}`);
    if (params?.gameMode) parts.push(`gameMode=${encodeURIComponent(params.gameMode)}`);
    if (params?.limit != null) parts.push(`limit=${params.limit}`);
    if (params?.includeMetrics?.length) parts.push(`includeMetrics=${encodeURIComponent(params.includeMetrics.join(','))}`);
    return get(`/api/stats/star/toplist${parts.length ? `?${parts.join('&')}` : ''}`);
  }
};

export const hitSprayApi = {
  save: (data) => post('/api/hit-spray', data),
  saveBatch: (data) => post('/api/hit-spray/batch', data),
  getByGame: (gameId) => get(`/api/hit-spray/game/${gameId}`),
  getByPlayerAndGame: (playerId, gameId) => get(`/api/hit-spray/player/${playerId}/game/${gameId}`),
  getByTeamAndGame: (teamId, gameId) => get(`/api/hit-spray/team/${teamId}/game/${gameId}`)
};
