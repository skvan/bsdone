// 个人驾驶舱 API —— 固定布局 v3-fixed
import { get, post, put } from './request';

function queryString(params) {
  const query = new URLSearchParams();
  if (params?.teamId != null && params.teamId !== '') query.set('teamId', String(params.teamId));
  if (params?.eventId != null && params.eventId !== '') query.set('eventId', String(params.eventId));
  return query.toString();
}

export const clubhouseApi = {
  dashboard: (params) => {
    const qs = queryString(params);
    return get(`/api/account/clubhouse${qs ? `?${qs}` : ''}`).then((res) => res?.data ?? null);
  },
  saveConfig: (config) => put('/api/account/clubhouse/config', config).then((res) => res?.data ?? null),
  resetConfig: () => post('/api/account/clubhouse/config/reset', {}).then((res) => res?.data ?? null)
};
