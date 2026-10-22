// 背号排序：数字升序；空背号 / 非数字排最后，同号按姓名稳定排序
function jerseyRank(player) {
  const raw = String(player?.number ?? '').trim();
  if (!raw) return Number.POSITIVE_INFINITY;
  const n = Number(raw);
  return Number.isFinite(n) ? n : Number.POSITIVE_INFINITY;
}

export function compareByJersey(a, b) {
  const ra = jerseyRank(a);
  const rb = jerseyRank(b);
  if (ra !== rb) return ra < rb ? -1 : 1;
  return String(a?.name ?? '').localeCompare(String(b?.name ?? ''), 'zh-Hans-CN');
}

export function sortByJersey(list) {
  return [...(Array.isArray(list) ? list : [])].sort(compareByJersey);
}
