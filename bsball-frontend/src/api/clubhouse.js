// 个人驾驶舱 API —— 模块化布局 v3-modular（桌面端可调宽高）
import { get, post, put } from './request';

function queryString(params) {
  const query = new URLSearchParams();
  if (params?.teamId != null && params.teamId !== '') query.set('teamId', String(params.teamId));
  if (params?.leagueId != null && params.leagueId !== '') query.set('leagueId', String(params.leagueId));
  if (params?.eventId != null && params.eventId !== '') query.set('eventId', String(params.eventId));
  if (params?.gameMode) query.set('gameMode', String(params.gameMode));
  return query.toString();
}

export const clubhouseApi = {
  dashboard: (params) => {
    const qs = queryString(params);
    return get(`/api/account/clubhouse${qs ? `?${qs}` : ''}`).then((res) => res?.data ?? null);
  },
  milestones: () => get('/api/account/clubhouse/milestones').then((res) => res?.data ?? null),
  sprays: (params) => {
    const qs = queryString(params);
    return get(`/api/account/clubhouse/sprays${qs ? `?${qs}` : ''}`).then((res) => res?.data ?? null);
  },
  saveConfig: (config) => put('/api/account/clubhouse/config', config).then((res) => res?.data ?? null),
  resetConfig: () => post('/api/account/clubhouse/config/reset', {}).then((res) => res?.data ?? null),
  saveShare: (enabled) => put('/api/account/clubhouse/share', { enabled }).then((res) => res?.data ?? null)
};

// 公开个人主页：免登录只读，按球员 ID 取数；范围参数与 /account/clubhouse 一致。
export const publicClubhouseApi = {
  dashboard: (playerId, params) => {
    const qs = queryString(params);
    return get(`/api/player/${playerId}/clubhouse${qs ? `?${qs}` : ''}`).then((res) => res?.data ?? null);
  },
  milestones: (playerId) => get(`/api/player/${playerId}/clubhouse/milestones`).then((res) => res?.data ?? null),
  sprays: (playerId, params) => {
    const qs = queryString(params);
    return get(`/api/player/${playerId}/clubhouse/sprays${qs ? `?${qs}` : ''}`).then((res) => res?.data ?? null);
  }
};
