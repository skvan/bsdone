// 打击落点回放：打者/球队筛选项构建（纯函数，便于单测）
//
// 数据来源：
//   * stats：GET /game/{id}/stats 的球员统计行（含替补；替补 battingOrder 为空、listOrder 有值）
//   * records：GET /hit-spray/game/{id} 的落点记录（历史 teamId=0，需要按上/下半局推导球队）
//
// 棒次补全规则（#717）：
//   1. 候选 = 位置球员；投手若 battingOrder 非空或 pa > 0（二刀流/取消 DH）也纳入，其余投手排除；
//   2. 按 listOrder 升序（再按 playerId）排列；
//   3. battingOrder 非空的行为棒次锚点；若该球员在落点记录中有 batterOrder，优先用记录修正；
//   4. 无棒次的替补继承其前面最近锚点的棒次；前面没有锚点则取后面最近锚点；都没有则标记为“替补”。

const UNKNOWN_ORDER = Number.MAX_SAFE_INTEGER;

function toNumber(value) {
  if (value === null || value === undefined || value === '') return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

/**
 * 落点记录归属球队：优先记录自身 teamId；历史数据为 0 时按上/下半局推导（top=客队，bottom=主队）。
 */
export function resolveRecordTeamId(record, homeTeamId, awayTeamId) {
  const own = toNumber(record?.teamId);
  if (own !== null && own > 0) return own;
  if (record?.half === 'top') return toNumber(awayTeamId);
  if (record?.half === 'bottom') return toNumber(homeTeamId);
  return null;
}

/**
 * 球员姓名：playersMap 复合键 `teamId:playerId` 优先，回退单键，再回退 `球员#ID`。
 */
export function playerDisplayName(playersMap, teamId, playerId) {
  const map = playersMap ?? {};
  const player = map[`${teamId}:${playerId}`] ?? map[playerId] ?? map[String(playerId)];
  return player?.name ?? `球员#${playerId}`;
}

/**
 * 球队名称：props.teams 优先，缺失时回退 `球队#ID`。
 */
export function teamDisplayName(teams, teamId) {
  const team = (teams ?? []).find((t) => toNumber(t?.id) === Number(teamId));
  return team?.name ?? `球队#${teamId}`;
}

function teamRecordStats(teamId, records, homeTeamId, awayTeamId) {
  const counts = new Map();
  const orders = new Map();
  for (const record of records ?? []) {
    if (resolveRecordTeamId(record, homeTeamId, awayTeamId) !== teamId) continue;
    const playerId = toNumber(record?.playerId);
    if (playerId === null) continue;
    counts.set(playerId, (counts.get(playerId) ?? 0) + 1);
    const order = toNumber(record?.batterOrder);
    if (order !== null && !orders.has(playerId)) orders.set(playerId, order);
  }
  return { counts, orders };
}

function isBatterCandidate(row) {
  if (Number(row?.isPitcher) !== 1) return true;
  return row?.battingOrder !== null && row?.battingOrder !== undefined
    || (toNumber(row?.pa) ?? 0) > 0;
}

function compareRosterRows(a, b) {
  const oa = toNumber(a?.listOrder) ?? UNKNOWN_ORDER;
  const ob = toNumber(b?.listOrder) ?? UNKNOWN_ORDER;
  if (oa !== ob) return oa - ob;
  return (toNumber(a?.playerId) ?? 0) - (toNumber(b?.playerId) ?? 0);
}

function compareOptions(a, b) {
  const sa = a.battingOrder ?? UNKNOWN_ORDER;
  const sb = b.battingOrder ?? UNKNOWN_ORDER;
  if (sa !== sb) return sa - sb;
  if (a.substitute !== b.substitute) return a.substitute ? 1 : -1;
  const la = a.listOrder ?? UNKNOWN_ORDER;
  const lb = b.listOrder ?? UNKNOWN_ORDER;
  if (la !== lb) return la - lb;
  return a.playerId - b.playerId;
}

function buildLabel({ slot, substitute, recordCount, name, teamPrefix }) {
  const orderText = slot !== null ? `第${slot}棒 ` : '替补 ';
  let label = `${teamPrefix}${orderText}${name}`;
  if (substitute && slot !== null) label += '（替补）';
  if (recordCount === 0) label += '（无落点）';
  return label;
}

function toOption(entry, context) {
  const { playersMap, teams, teamId, useTeamPrefix } = context;
  const name = playerDisplayName(playersMap, teamId, entry.playerId);
  const teamPrefix = useTeamPrefix ? `[${teamDisplayName(teams, teamId)}] ` : '';
  return {
    key: `${teamId}:${entry.playerId}`,
    label: buildLabel({ ...entry, name, teamPrefix }),
    teamId,
    playerId: entry.playerId,
    battingOrder: entry.slot,
    substitute: entry.substitute,
    listOrder: entry.listOrder,
    recordCount: entry.recordCount,
  };
}

function assignSlots(rows) {
  const entries = rows.map((row) => ({
    playerId: toNumber(row.playerId),
    directSlot: toNumber(row.recordOrder) ?? toNumber(row.battingOrder),
    substitute: toNumber(row.battingOrder) === null,
    listOrder: toNumber(row.listOrder) ?? UNKNOWN_ORDER,
    recordCount: toNumber(row.recordCount) ?? 0,
    slot: null,
  }));
  let anchor = null;
  for (const entry of entries) {
    if (entry.directSlot !== null) {
      entry.slot = entry.directSlot;
      anchor = entry.directSlot;
    } else if (anchor !== null) {
      entry.slot = anchor;
    }
  }
  anchor = null;
  for (let i = entries.length - 1; i >= 0; i -= 1) {
    const entry = entries[i];
    if (entry.slot !== null) {
      anchor = entry.slot;
    } else if (anchor !== null) {
      entry.slot = anchor;
    }
  }
  return entries;
}

/**
 * 单队打者选项：
 *   - stats 有该队球员时按 stats 构建（含无落点球员与替补）；
 *   - stats 缺失该队时退回“由落点记录反推”的旧逻辑，避免白屏。
 */
function buildTeamBatters(teamId, { stats, records, homeTeamId, awayTeamId }) {
  const { counts, orders } = teamRecordStats(teamId, records, homeTeamId, awayTeamId);
  const statRows = (stats ?? []).filter((row) => toNumber(row?.teamId) === teamId);
  if (!statRows.length) {
    return [...counts.entries()].map(([playerId, recordCount]) => ({
      playerId,
      slot: orders.get(playerId) ?? null,
      substitute: false,
      listOrder: UNKNOWN_ORDER,
      recordCount,
    }));
  }
  const rows = statRows
    .filter(isBatterCandidate)
    .slice()
    .sort(compareRosterRows)
    .map((row) => {
      const playerId = toNumber(row.playerId);
      return {
        playerId,
        battingOrder: toNumber(row.battingOrder),
        recordOrder: orders.get(playerId) ?? null,
        listOrder: toNumber(row.listOrder) ?? UNKNOWN_ORDER,
        recordCount: counts.get(playerId) ?? 0,
      };
    });
  const rosterIds = new Set(rows.map((row) => row.playerId));
  // 防御：stats 缺失但落点记录里存在的球员，补在末尾（有棒次按记录棒次，否则“替补”）
  for (const [playerId, recordCount] of counts.entries()) {
    if (rosterIds.has(playerId)) continue;
    rows.push({
      playerId,
      battingOrder: null,
      recordOrder: orders.get(playerId) ?? null,
      listOrder: UNKNOWN_ORDER,
      recordCount,
    });
  }
  return assignSlots(rows).filter((entry) => entry.playerId !== null);
}

function allTeamIds({ stats, records, homeTeamId, awayTeamId }) {
  const ids = [];
  const add = (id) => {
    if (id !== null && !ids.includes(id)) ids.push(id);
  };
  add(toNumber(homeTeamId));
  add(toNumber(awayTeamId));
  for (const row of stats ?? []) add(toNumber(row?.teamId));
  for (const record of records ?? []) add(resolveRecordTeamId(record, homeTeamId, awayTeamId));
  return ids;
}

/**
 * 球队下拉：由 props.teams 生成（即使某队 0 落点也能选到），缺失时用落点记录兜底。
 */
export function buildTeamOptions({ teams, records, homeTeamId, awayTeamId } = {}) {
  const seen = new Map();
  for (const team of teams ?? []) {
    const id = toNumber(team?.id);
    if (id === null || seen.has(id)) continue;
    seen.set(id, { id, name: team?.name ?? `球队#${id}` });
  }
  for (const record of records ?? []) {
    const id = resolveRecordTeamId(record, homeTeamId, awayTeamId);
    if (id === null || seen.has(id)) continue;
    seen.set(id, { id, name: `球队#${id}` });
  }
  return [...seen.values()];
}

/**
 * 打者下拉（平铺球员）：teamKey 为具体球队时只列该队；为 all 时合并两队并加 `[队名] ` 前缀。
 *
 * @returns {Array<{key:string,label:string,teamId:number,playerId:number,battingOrder:number|null,
 *   substitute:boolean,listOrder:number,recordCount:number}>}
 */
export function buildBatterOptions({
  stats, records, playersMap, teams, teamKey, homeTeamId, awayTeamId,
} = {}) {
  const allRecords = records ?? [];
  const scopedTeam = toNumber(teamKey);
  const teamIds = scopedTeam !== null
    ? [scopedTeam]
    : allTeamIds({ stats, records: allRecords, homeTeamId, awayTeamId });
  const useTeamPrefix = scopedTeam === null && teamIds.length > 1;
  const context = { playersMap, teams, useTeamPrefix };
  const options = [];
  for (const teamId of teamIds) {
    const entries = buildTeamBatters(teamId, {
      stats, records: allRecords, homeTeamId, awayTeamId,
    });
    const teamOptions = entries
      .map((entry) => toOption(entry, { ...context, teamId }))
      .sort(compareOptions);
    options.push(...teamOptions);
  }
  return options;
}
