// 比赛详情守备表行构建：按本场首次上场守位归组（含投手），组按先发打线顺序排序，同组替补缩进，缺失数据补 0
import { normalizeFieldingPosition, resolveFirstFieldPosition } from './gamePlayerStat.js';
import { formatInningsPitched, inningsToOuts, outsToInnings } from './inningsFormat.js';

const POSITION_ORDER = ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF'];
const BATTING_ONLY_POSITIONS = new Set(['DH', 'PH', 'EP']);
const BREAKDOWN_COUNT_KEYS = ['po', 'a', 'e', 'dp', 'pb', 'catcherSb', 'catcherCs'];
const FIELD_KEYS = ['fieldingGs', ...BREAKDOWN_COUNT_KEYS, 'defInn'];

function countOf(value) {
  return Math.max(0, Math.floor(Number(value) || 0));
}

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
  return BREAKDOWN_COUNT_KEYS.reduce((sum, key) => sum + (Number(row?.[key]) || 0), 0);
}

function toDisplayRow(row) {
  const next = {
    ...row,
    position: String(row?.position ?? '').trim().toUpperCase(),
    // 只认「首次上场守位」事实；旧数据为空时由 isDisplayable 的守备证据决定是否回退
    firstPosition: normalizeFieldingPosition(row?.firstPosition),
    // 先发守备位（新数据）；DH/替补为空，旧数据为空时用回退规则
    startingPosition: normalizeFieldingPosition(row?.startingPosition),
    // 区分「已知无先发守位（DH/替补）」与「旧数据没有该字段」
    startingPositionKnown: row?.startingPosition != null,
    kind: 'player',
    breakdown: [],
    multiPosition: false,
    open: false
  };
  next.fieldPosition = next.firstPosition;
  for (const key of FIELD_KEYS) next[key] = Number(next[key]) || 0;
  return next;
}

/** 单条逐位置记录归一化；旧快照的数字值视为只有 outs */
function normalizePositionEntry(raw) {
  const position = normalizeFieldingPosition(raw?.position);
  if (!position || BATTING_ONLY_POSITIONS.has(position)) return null;
  const value = raw && typeof raw === 'object' ? raw : { outs: raw };
  const entry = {
    position,
    outs: countOf(value.outs),
    po: countOf(value.po),
    a: countOf(value.a),
    e: countOf(value.e),
    dp: countOf(value.dp),
    pb: countOf(value.pb),
    catcherSb: countOf(value.catcherSb),
    catcherCs: countOf(value.catcherCs),
    fieldingGs: value.fieldingGs === true || Number(value.fieldingGs) === 1
  };
  const activity = entry.outs + BREAKDOWN_COUNT_KEYS.reduce((sum, key) => sum + entry[key], 0);
  return activity > 0 ? entry : null;
}

/** 从统计行收集逐位置记录：赛后接口数组优先，实时观赛用快照 byPos 对象 */
function collectPositionEntries(row) {
  const raw = [];
  if (Array.isArray(row?.fieldingPositions)) {
    for (const item of row.fieldingPositions) {
      if (!item) continue;
      if (item.teamId != null && String(item.teamId) !== String(row?.teamId)) continue;
      if (item.playerId != null && String(item.playerId) !== String(row?.playerId)) continue;
      raw.push(item);
    }
  } else if (row?.fieldingByPos && typeof row.fieldingByPos === 'object') {
    for (const [position, value] of Object.entries(row.fieldingByPos)) {
      raw.push(value && typeof value === 'object' ? { ...value, position } : { position, outs: value });
    }
  }
  const byPosition = new Map();
  for (const item of raw) {
    const entry = normalizePositionEntry(item);
    if (!entry) continue;
    const current = byPosition.get(entry.position);
    if (!current) {
      byPosition.set(entry.position, entry);
      continue;
    }
    for (const key of ['outs', ...BREAKDOWN_COUNT_KEYS]) current[key] += entry[key];
    current.fieldingGs = current.fieldingGs || entry.fieldingGs;
  }
  return [...byPosition.values()];
}

/** 明细 GS：有 startingPosition 时按先发守位判定；旧数据回退逐位置 fieldingGs */
function breakdownGs(row, entry) {
  if (row.startingPositionKnown) {
    return row.startingPosition && entry.position === row.startingPosition ? 1 : 0;
  }
  return entry.fieldingGs ? 1 : 0;
}

/** 起始守位优先，其余保持首次触达顺序 */
function orderBreakdown(entries, row) {
  if (!row.startingPositionKnown || !row.startingPosition) return entries;
  const index = entries.findIndex((entry) => entry.position === row.startingPosition);
  if (index <= 0) return entries;
  return [entries[index], ...entries.slice(0, index), ...entries.slice(index + 1)];
}

function toBreakdownRow(row, entry) {
  const next = {
    ...row,
    kind: 'breakdown',
    position: entry.position,
    fieldPosition: entry.position,
    startingPosition: '',
    startingPositionKnown: false,
    fieldingGs: breakdownGs(row, entry),
    po: entry.po,
    a: entry.a,
    e: entry.e,
    dp: entry.dp,
    defInn: outsToInnings(entry.outs),
    pb: entry.pb,
    catcherSb: entry.catcherSb,
    catcherCs: entry.catcherCs,
    outs: entry.outs,
    breakdown: [],
    multiPosition: false,
    subRow: false,
    open: false
  };
  delete next.fieldingPositions;
  delete next.fieldingByPos;
  return next;
}

/** 明细各项之和必须与球员汇总行一致，才允许展开，避免历史/过期明细覆盖汇总 */
function breakdownMatches(row, breakdown) {
  for (const key of BREAKDOWN_COUNT_KEYS) {
    const total = breakdown.reduce((sum, item) => sum + item[key], 0);
    if ((Number(row?.[key]) || 0) !== total) return false;
  }
  const outs = breakdown.reduce((sum, item) => sum + item.outs, 0);
  return inningsToOuts(Number(row?.defInn) || 0) === outs;
}

function attachBreakdown(row) {
  const entries = collectPositionEntries(row);
  if (entries.length < 2) return row;
  const breakdown = orderBreakdown(entries, row).map((entry) => toBreakdownRow(row, entry));
  if (!breakdownMatches(row, breakdown)) return row;
  row.breakdown = breakdown;
  row.multiPosition = true;
  if (!row.fieldPosition) row.fieldPosition = breakdown[0].position;
  return row;
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
    // 先挂逐位置明细：首次守位缺失时可用明细首项兜底归组
    attachBreakdown(display);
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

/** 把赛后逐位置接口结果按 teamId+playerId 挂到统计行，供 buildFieldingDisplayRows 展开 */
export function attachFieldingPositions(stats, positions) {
  const list = Array.isArray(stats) ? stats : [];
  const byPlayer = new Map();
  for (const position of Array.isArray(positions) ? positions : []) {
    if (!position) continue;
    const key = `${String(position.teamId)}:${String(position.playerId)}`;
    if (!byPlayer.has(key)) byPlayer.set(key, []);
    byPlayer.get(key).push(position);
  }
  if (!byPlayer.size) return list;
  for (const row of list) {
    const items = byPlayer.get(`${String(row?.teamId)}:${String(row?.playerId)}`);
    if (items?.length) row.fieldingPositions = items;
  }
  return list;
}

/** 展开/折叠：多守位球员在主行后插入逐位置明细行，不改动分组与 Totals 基础行 */
export function expandFieldingRows(rows, expandedPlayerIds) {
  const source = expandedPlayerIds instanceof Set
    ? expandedPlayerIds
    : new Set(Array.isArray(expandedPlayerIds) ? expandedPlayerIds : []);
  const expanded = new Set([...source].map((id) => String(id)));
  const list = [];
  for (const row of Array.isArray(rows) ? rows : []) {
    const open = !!row.multiPosition && expanded.has(String(row.playerId));
    list.push({
      ...row,
      kind: 'player',
      open,
      _rowKey: `player-${row.playerId}`
    });
    if (!open) continue;
    for (const item of row.breakdown) {
      list.push({
        ...item,
        open: false,
        _rowKey: `breakdown-${row.playerId}-${item.position}`
      });
    }
  }
  return list;
}

/**
 * 守备表 Totals 汇总：计数项求和；INN 取全队最大守备局数（各球员守备局不叠加）。
 * DP 按现有球队守备口径汇总球员 DP 记分（与球队守备榜一致；双杀会按参与人数重复计入）。
 * INN 取 max 在全员轮换/末局换替补时可能低于全队实际守备局数，属于展示口径。
 * 展开明细行（kind=breakdown）不参与合计，避免重复统计。
 */
export function buildFieldingTotals(rows, fields = []) {
  const list = Array.isArray(rows) ? rows.filter((row) => row?.kind !== 'breakdown') : [];
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
