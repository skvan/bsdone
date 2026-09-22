// 资源缓存参数工具 —— 行为移植自编译产物 assetCacheUrl chunk
export function parseCacheTime(value, fallback) {
  const raw = (value || fallback || '').trim();
  if (!raw) return null;
  const parsed = Date.parse(raw);
  return Number.isFinite(parsed) ? parsed : null;
}

export function appendCacheBust(url, cacheTime) {
  if (!url || cacheTime == null || !Number.isFinite(cacheTime) || url.startsWith('data:')) return url;
  return `${url}${url.includes('?') ? '&' : '?'}t=${Math.trunc(cacheTime)}`;
}
