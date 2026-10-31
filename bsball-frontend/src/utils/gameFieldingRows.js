// 比赛详情守备表行构建：按本场首次上场守位归组（含投手），组按先发打线顺序排序，同组替补缩进，缺失数据补 0
import { normalizeFieldingPosition, resolveFirstFieldPosition } from './gamePlayerStat.js';
import { formatInningsPitched } from './inningsFormat.js';

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
    firstPosition: normalizeFieldingPosition(row?.firstPosition),
    // 先发守备位（新数据）；DH/替补为空，旧数据为空时用回退规则
    startingPosition: normalizeFieldingPosition(row?.startingPosition)
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

function compareWithinGroup(a, b) {
  const ar = starterRank(a), br = starterRank(b);
  if (ar !== br) return ar - br;
  const al = listOrderOf(a), bl = listOrderOf(b);
  if (al !== bl) return al - bl;
  const ab = battingOrderOf(a), bb = battingOrderOf(b);
  if (ab !== bb) return ab - bb;
  const ap = playerIdOf(a), bp = playerIdOf(b);
  if (ap !== bp) return ap - bp;
  return String(a.playerId ?? '').localeCompare(String(b.playerId ?? ''));
}

/** 旧数据回退：有棒次的先发优先于替补；新数据由 startingPosition 直接决定组首 */
function starterRank(row) {
  return battingOrderOf(row) !== Number.POSITIVE_INFINITY ? 0 : 1;
}

/** 组首：优先取本守位的先发守备者（startingPosition=该守位）；无该字段时按旧回退规则 */
function chooseGroupHead(members) {
  const starters = members.filter((row) => row.startingPosition && row.startingPosition === row.fieldPosition);
  const pool = starters.length ? starters : members;
  return pool.slice().sort(compareWithinGroup)[0];
}

function groupSortKey(head) {
  const bo = Number(head?.battingOrder);
  const hasBattingOrder = Number.isFinite(bo) && bo > 0;
  return {
    hasBattingOrder: hasBattingOrder ? 0 : 1,
    battingOrder: hasBattingOrder ? bo : Number.POSITIVE_INFINITY,
    listOrder: listOrderOf(head),
    positionRank: positionRank(head?.fieldPosition),
    fieldPosition: String(head?.fieldPosition ?? '')
  };
}

function compareGroups(a, b) {
  if (a.hasBattingOrder !== b.hasBattingOrder) return a.hasBattingOrder - b.hasBattingOrder;
  if (a.battingOrder !== b.battingOrder) return a.battingOrder - b.battingOrder;
  if (a.listOrder !== b.listOrder) return a.listOrder - b.listOrder;
  if (a.positionRank !== b.positionRank) return a.positionRank - b.positionRank;
  return a.fieldPosition.localeCompare(b.fieldPosition);
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
  // 每个首次守位一组；组按先发打线顺序（先发守备者的棒次）排序，无棒次的纯投手组排最后
  const groups = new Map();
  for (const row of byPlayer.values()) {
    const key = row.fieldPosition || '';
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(row);
  }
  const ordered = [];
  for (const members of groups.values()) {
    const head = chooseGroupHead(members);
    const orderedMembers = [head, ...members.filter((row) => row !== head).sort(compareWithinGroup)];
    // 组首为该守位先发/首个上场者，其后（替补，含后上场的先发打者）缩进
    orderedMembers.forEach((row, index) => { row.subRow = index > 0; });
    ordered.push({ members: orderedMembers, sortKey: groupSortKey(head) });
  }
  ordered.sort((a, b) => compareGroups(a.sortKey, b.sortKey));
  return ordered.flatMap((group) => group.members);
}

/**
 * 守备表 Totals 汇总：计数项求和；INN 取全队最大守备局数（各球员守备局不叠加）。
 * DP 按现有球队守备口径汇总球员 DP 记分（与球队守备榜一致；双杀会按参与人数重复计入）。
 * INN 取 max 在全员轮换/末局换替补时可能低于全队实际守备局数，属于展示口径。
 */
export function buildFieldingTotals(rows, fields = []) {
  const list = Array.isArray(rows) ? rows : [];
  const totals = {};
  for (const field of fields) {
    const key = field?.key;
    if (!key) continue;
    if (key === 'defInn') {
      totals[key] = list.reduce((max, row) => Math.max(max, Number(row?.defInn) || 0), 0);
      continue;
    }
    totals[key] = list.reduce((sum, row) => sum + (Number(row?.[key]) || 0), 0);
  }
  return totals;
}

/**
 * 守备表 Totals 行：按 el-table 列的 prop 映射合计值，首列 playerName→Totals，POS 等非合计列留空。
 * 不依赖列下标，字段显隐/排序后仍与对应列对齐。
 */
export function buildFieldingSummaryCells(columns, totals) {
  const list = Array.isArray(columns) ? columns : [];
  const values = totals || {};
  return list.map((column) => {
    const key = column?.property;
    if (key === 'playerName') return 'Totals';
    if (!key || !(key in values)) return '';
    const value = values[key] ?? 0;
    return key === 'defInn' ? formatInningsPitched(value) : String(value);
  });
}
