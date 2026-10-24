// 比赛详情守备表行构建：按打线顺序展示全部守备球员，缺失数据补 0
const EXCLUDED_POSITIONS = new Set(['P', 'DH', 'PH']);
const FIELD_KEYS = ['fieldingGs', 'po', 'a', 'e', 'dp', 'defInn', 'pb', 'catcherSb', 'catcherCs'];

function orderOf(row) {
  const batting = Number(row?.battingOrder);
  if (Number.isFinite(batting) && batting > 0) return batting;
  const list = Number(row?.listOrder);
  if (Number.isFinite(list) && list >= 0) return list;
  return Number.POSITIVE_INFINITY;
}

function toDisplayRow(row) {
  const next = { ...row, position: String(row?.position ?? '').trim().toUpperCase() };
  for (const key of FIELD_KEYS) next[key] = Number(next[key]) || 0;
  return next;
}

function isFieldingRow(row) {
  return !EXCLUDED_POSITIONS.has(row.position);
}

function compareRows(a, b) {
  const ao = orderOf(a), bo = orderOf(b);
  if (Number.isFinite(ao) && Number.isFinite(bo) && ao !== bo) return ao - bo;
  if (Number.isFinite(ao) !== Number.isFinite(bo)) return Number.isFinite(ao) ? -1 : 1;
  const ap = Number(a.playerId), bp = Number(b.playerId);
  if (Number.isFinite(ap) && Number.isFinite(bp) && ap !== bp) return ap - bp;
  return String(a.playerId ?? '').localeCompare(String(b.playerId ?? ''));
}

export function buildFieldingDisplayRows(stats, teamId) {
  const rows = (Array.isArray(stats) ? stats : [])
    .filter((row) => String(row?.teamId) === String(teamId))
    .map(toDisplayRow)
    .filter(isFieldingRow);
  rows.sort(compareRows);
  return rows;
}
