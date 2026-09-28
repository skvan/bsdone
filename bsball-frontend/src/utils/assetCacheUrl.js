// 资源缓存版本 URL —— 行为移植自编译产物 assetCacheUrl chunk（逐字）
export function parseCacheTimestamp(updatedAt, fallback) {
  const text = (updatedAt || fallback || '').trim();
  if (!text) return null;
  const ts = Date.parse(text);
  return Number.isFinite(ts) ? ts : null;
}

export function appendCacheVersion(url, timestamp) {
  if (!url || timestamp == null || !Number.isFinite(timestamp) || url.startsWith('data:')) return url;
  return `${url}${url.includes('?') ? '&' : '?'}t=${Math.trunc(timestamp)}`;
}
