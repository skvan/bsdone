// 比赛详情守备表行构建：按本场首次上场守位归组（含投手），同组替补缩进，缺失数据补 0
import { normalizeFieldingPosition, resolveFirstFieldPosition } from './gamePlayerStat.js';

const POSITION_ORDER = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF'];
const BATTING_ONLY_POSITIONS = new Set(['DH', 'PH', 'EP']);
const FIELD_KEYS = ['fieldingGs', 'po', 'a', 'e', 'dp', 'defInn', 'pb', 'catcherSb', 'catcherCs'];

/** 组内顺序：listOrder（换人史紧跟原棒次）优先，其次棒次，再次球员 id */
function listOrderOf(row) {
  const list = Number(row?.listOrder);
  return Number.isFinite(list) && list >= 0 ? list : Number.POSITIVE_INFINITY;
}

function battingOrderOf(row) {
  const batting = Number(row?.battingOrder);
  return Number.isFinite(batting) && batting > 0 ? batting : Number.POSITIVE_INFINITY;
}

function playerIdOf(row) {
  const id = Number(row?.playerId);
  return Number.isFinite(id) ? id : Number.POSITIVE_INFINITY;
}

function fieldingActivity(row) {
  return ['po', 'a', 'e', 'dp', 'pb', 'catcherSb', 'catcherCs']
    .reduce((sum, key) => sum + (Number(row?.[key]) || 0), 0);
}

function toDisplayRow(row) {
  const next = {
    ...row,
    position: String(row?.position ?? '').trim().toUpperCase(),
    // 只认「首次上场守位」事实；旧数据为空时由 isDisplayable 的守备证据决定是否回退
    firstPosition: normalizeFieldingPosition(row?.firstPosition)
  };
  next.fieldPosition = next.firstPosition;
  for (const key of FIELD_KEYS) next[key] = Number(next[key]) || 0;
  return next;
}

function isDisplayable(row) {
  // 投手即使零守备数据也必须在表内；DH/PH/EP/未上场者没有首个守位，不进入
  if (row.fieldPosition) return true;
  if (!row.position) {
    // 历史脏数据：位置为空但有守备动作的异常行保留在末尾，避免丢数据
    return fieldingActivity(row) > 0;
  }
  if (BATTING_ONLY_POSITIONS.has(row.position)) return false;
  if (row.position === 'P') {
    // 只有真正进入投手角色（投手行/有投球或守备记录）的 P 才纳入；
    // 仅因板凳名单带着首选守位 P 的人不显示。
    return !!row.isPitcher
      || Number(row.ip) > 0
      || Number(row.np) > 0
      || Number(row.defInn) > 0
      || fieldingActivity(row) > 0;
  }
  // 旧数据/未记录首个守位：有守备证据才按当前守位兜底，避免代打/代跑（继承守位但未防守）被误纳入
  return fieldingActivity(row) > 0
    || Number(row.defInn) > 0
    || Number(row.fieldingGs) > 0
    || battingOrderOf(row) !== Number.POSITIVE_INFINITY;
}

function positionRank(position) {
  if (!position) return 100;
  const index = POSITION_ORDER.indexOf(position);
  return index >= 0 ? index : 90;
}

function compareRows(a, b) {
  const pa = positionRank(a.fieldPosition);
  const pb = positionRank(b.fieldPosition);
  if (pa !== pb) return pa - pb;
  if (a.fieldPosition !== b.fieldPosition) {
    return String(a.fieldPosition).localeCompare(String(b.fieldPosition));
  }
  const al = listOrderOf(a), bl = listOrderOf(b);
  if (al !== bl) return al - bl;
  const ab = battingOrderOf(a), bb = battingOrderOf(b);
  if (ab !== bb) return ab - bb;
  const ap = playerIdOf(a), bp = playerIdOf(b);
  if (ap !== bp) return ap - bp;
  return String(a.playerId ?? '').localeCompare(String(b.playerId ?? ''));
}

/** 同一球员可能同时有打者行与投手行（二刀流）：保留守备动作更完整的一行 */
function preferRow(candidate, current) {
  const ca = fieldingActivity(candidate), cb = fieldingActivity(current);
  if (ca !== cb) return ca > cb;
  const cl = listOrderOf(candidate), dl = listOrderOf(current);
  if (cl !== dl) return cl < dl;
  // 完全同分时保留先出现的一行
  return false;
}

export function buildFieldingDisplayRows(stats, teamId) {
  const byPlayer = new Map();
  for (const row of (Array.isArray(stats) ? stats : [])) {
    if (String(row?.teamId) !== String(teamId)) continue;
    const display = toDisplayRow(row);
    if (!isDisplayable(display)) continue;
    // 旧数据没有首个守位：用当前守位兜底显示（仅对已通过守备证据校验的行）
    if (!display.fieldPosition) display.fieldPosition = resolveFirstFieldPosition(display);
    const key = String(display.playerId ?? '');
    const prev = byPlayer.get(key);
    if (!prev || preferRow(display, prev)) byPlayer.set(key, display);
  }
  const rows = [...byPlayer.values()].sort(compareRows);
  let previousPosition = '';
  for (const row of rows) {
    // 每个守位组首行为先发/首个上场者，其余（替补）缩进
    row.subRow = !!row.fieldPosition && row.fieldPosition === previousPosition;
    if (row.fieldPosition) previousPosition = row.fieldPosition;
  }
  return rows;
}
