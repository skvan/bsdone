// 系统管理 API —— 行为与端点移植自编译产物 system chunk（B0 契约）
import { get, post, put, del, fetchList, fetchListResult, rewriteUrl, isSuccessCode } from './request';
import { resolveToken } from './tokenStorage';

const BASE = '/api/sys';

export const userApi = {
  list: (params) => fetchList(`${BASE}/user/list`, params),
  get: (id) => get(`${BASE}/user/${id}`),
  create: (data) => post(`${BASE}/user/create`, data),
  update: (id, data) => put(`${BASE}/user/update/${id}`, data),
  delete: (id) => del(`${BASE}/user/delete/${id}`)
};

export const roleApi = {
  list: (params) => fetchList(`${BASE}/role/list`, params),
  assignOptions: (params) => fetchList(`${BASE}/role/assign-options`, params),
  get: (id) => get(`${BASE}/role/${id}`),
  create: (data) => post(`${BASE}/role/create`, data),
  update: (id, data) => put(`${BASE}/role/update/${id}`, data),
  delete: (id) => del(`${BASE}/role/delete/${id}`)
};

export const menuApi = {
  list: () => get(`${BASE}/menu/list`),
  create: (data) => post(`${BASE}/menu/create`, data),
  update: (id, data) => put(`${BASE}/menu/update/${id}`, data),
  delete: (id) => del(`${BASE}/menu/delete/${id}`)
};

export const apiResourceApi = {
  list: (params) => fetchList(`${BASE}/api/list`, params),
  all: () => get(`${BASE}/api/all`),
  get: (id) => get(`${BASE}/api/${id}`),
  create: (data) => post(`${BASE}/api/create`, data),
  update: (id, data) => put(`${BASE}/api/update/${id}`, data),
  delete: (id) => del(`${BASE}/api/delete/${id}`)
};

export const dictTypeApi = {
  list: (params) => fetchList(`${BASE}/dict/type/list`, params),
  create: (data) => post(`${BASE}/dict/type/create`, data),
  update: (id, data) => put(`${BASE}/dict/type/update/${id}`, data),
  delete: (id) => del(`${BASE}/dict/type/delete/${id}`)
};

export const dictDataApi = {
  list: (params) => fetchList(`${BASE}/dict/data/list`, params),
  create: (data) => post(`${BASE}/dict/data/create`, data),
  update: (id, data) => put(`${BASE}/dict/data/update/${id}`, data),
  delete: (id) => del(`${BASE}/dict/data/delete/${id}`)
};

export const articleApi = {
  platformList: (params) => fetchList(`${BASE}/article/platform/list`, params),
  list: (params) => fetchList(`${BASE}/article/list`, params),
  get: (id) => get(`${BASE}/article/${id}`),
  create: (data) => post(`${BASE}/article/create`, data),
  update: (id, data) => put(`${BASE}/article/update/${id}`, data),
  delete: (id) => del(`${BASE}/article/delete/${id}`)
};

export const noticeApi = {
  list: (params) => fetchList(`${BASE}/notice/list`, params),
  get: (id) => get(`${BASE}/notice/${id}`),
  create: (data) => post(`${BASE}/notice/create`, data),
  update: (id, data) => put(`${BASE}/notice/update/${id}`, data),
  delete: (id) => del(`${BASE}/notice/delete/${id}`),
  fetchToShow: (target) => {
    const params = { page: 1, pageSize: 5, target };
    return fetchList(`${BASE}/notice/list`, params);
  }
};

export const resourceApi = {
  list: (params) => fetchList(`${BASE}/resource/list`, params),
  create: (data) => post(`${BASE}/resource/create`, data),
  delete: (id) => del(`${BASE}/resource/delete/${id}`)
};

export const mediaIconApi = {
  list: (params) => fetchList(`${BASE}/media-icon/list`, params),
  get: (id) => get(`${BASE}/media-icon/${id}`),
  create: (data) => post(`${BASE}/media-icon/create`, data),
  update: (id, data) => put(`${BASE}/media-icon/update/${id}`, data),
  delete: (id) => del(`${BASE}/media-icon/delete/${id}`)
};

export const mediaGalleryApi = {
  list: (params) => fetchList(`${BASE}/media-gallery/list`, params),
  get: (id) => get(`${BASE}/media-gallery/${id}`),
  create: (data) => post(`${BASE}/media-gallery/create`, data),
  update: (id, data) => put(`${BASE}/media-gallery/update/${id}`, data),
  delete: (id) => del(`${BASE}/media-gallery/delete/${id}`)
};

export const loginLogApi = {
  list: (params) => fetchList(`${BASE}/login-log/list`, params)
};

export const operationLogApi = {
  list: (params) => fetchList(`${BASE}/operation-log/list`, params)
};

export const portalMonitorApi = {
  devtoolsReportList: (params) => fetchList(`${BASE}/portal/devtools-report/list`, params),
  visitHitList: (params) => fetchList(`${BASE}/portal/visit-hit/list`, params),
  feedbackList: (params) => fetchList(`${BASE}/portal/feedback/list`, params),
  feedbackReply: (data) => post(`${BASE}/portal/feedback/reply`, data)
};

export const ipLocationCacheApi = {
  page: (params) => fetchList(`${BASE}/ip-location-cache/page`, params),
  lbsProviders: () => get(`${BASE}/ip-location-cache/lbs-providers`).then((data) => data ?? []),
  refresh: (ip, provider) => {
    const query = new URLSearchParams({ ip });
    if (provider != null && provider !== '') query.set('provider', provider);
    return post(`${BASE}/ip-location-cache/refresh?${query.toString()}`, {}).then((body) => body.data);
  }
};

function tenantQuery(tenantId) {
  return tenantId == null ? '' : `?tenantId=${tenantId}`;
}

export const ipAccessPolicyApi = {
  getPolicy: (tenantId) =>
    get(`${BASE}/ip-access-policy${tenantQuery(tenantId)}`).then(
      (data) => data ?? { mode: 'off', bypassPaths: '', trustXForwardedFor: true, ipAccessFilterEnabled: true }
    ),
  savePolicy: (data, tenantId) => put(`${BASE}/ip-access-policy${tenantQuery(tenantId)}`, data),
  listRules: (tenantId) => fetchList(`${BASE}/ip-access-rule/list`, tenantId != null ? { tenantId } : undefined),
  createRule: (data, tenantId) => post(`${BASE}/ip-access-rule/create${tenantQuery(tenantId)}`, data),
  updateRule: (id, data) => put(`${BASE}/ip-access-rule/update/${id}`, data),
  deleteRule: (id) => del(`${BASE}/ip-access-rule/delete/${id}`)
};

export const monitorApi = {
  getDatasourceUrl: () => get(`${BASE}/monitor/datasource/url`).then((data) => data ?? { url: '', tip: '' }),
  getServerInfo: () => get(`${BASE}/monitor/server`).then((data) => data ?? {}),
  getCacheInfo: () => get(`${BASE}/monitor/cache`).then((data) => data ?? {}),
  getCacheKeys: (cacheName, pattern, limit) => {
    const query = new URLSearchParams({ cacheName });
    if (pattern != null && pattern !== '') query.set('pattern', pattern);
    if (limit != null && limit > 0) query.set('limit', String(limit));
    return get(`${BASE}/monitor/cache/keys?${query}`).then((data) => data ?? { keys: [], total: 0 });
  },
  getCacheValue: (cacheName, key) =>
    get(`${BASE}/monitor/cache/value?cacheName=${encodeURIComponent(cacheName)}&key=${encodeURIComponent(key)}`).then(
      (data) => data ?? { cacheName: '', key: '', value: '', message: '' }
    ),
  removeCacheKey: (cacheName, key) =>
    del(`${BASE}/monitor/cache/key?cacheName=${encodeURIComponent(cacheName)}&key=${encodeURIComponent(key)}`),
  clearCache: (cacheName) => del(`${BASE}/monitor/cache/clear?cacheName=${encodeURIComponent(cacheName)}`)
};

export const tenantApi = {
  list: (params) => fetchList(`${BASE}/tenant/list`, params),
  get: (id) => get(`${BASE}/tenant/${id}`),
  create: (data) => post(`${BASE}/tenant/create`, data),
  update: (id, data) => put(`${BASE}/tenant/update/${id}`, data),
  delete: (id) => del(`${BASE}/tenant/delete/${id}`),
  scopeOptions: (tenantId) => get(`${BASE}/tenant/scope-options?tenantId=${tenantId}`)
};

// 资源上传（multipart，单独走 fetch）
export async function uploadResource(file) {
  const url = rewriteUrl('/api/sys/resource/upload');
  const form = new FormData();
  form.append('file', file);
  const headers = {};
  const token = resolveToken();
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(url, { method: 'POST', body: form, headers });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.message || body.msg || `上传失败: ${res.status}`);
  if (!isSuccessCode(body.code)) throw new Error(body.msg || body.message || '上传失败');
  const data = body.data;
  return {
    url: data?.url ?? data?.path ?? '',
    path: data?.path,
    name: data?.name ?? file.name
  };
}

// 供组合式函数复用的轻量列表（同原 fetchListResult 语义）
export { fetchListResult };
