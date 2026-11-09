// 击球落点记录载荷构建（表 bs_hit_spray，见 docs/tech/数据模型设计/统计实体.md）
// 2026-10-10（issue #279）：从 LiveGame.js 内联推送逻辑提取为纯函数，
// 供各结果分支（含对话框路径）共用，字段语义与提取前逐字一致
// 2026-11-09：新增 rid（客户端唯一键）与「已送出剔除」纯函数，用于修复
// 「落点记录送出后被撤销/重进现场的快照还原回队列，再次批量送出」造成的重复落点

// 守位码（对应 LiveGame io() 的 ya）：fieldPos 仅接受守位码
const FIELD_POS_CODES = new Set(['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF']);
// 落点区域码（对应 LiveGame io() 的 Pu ∪ ya，返回码本身）
const ZONE_CODES = new Set([...FIELD_POS_CODES, 'LCF', 'RCF', 'IF']);

/** 解析落点区域：优先取守备位（sprayFieldPos），否则取区域（sprayZone）；无效返回 undefined */
export function resolveSprayZone(sprayZone, sprayFieldPos) {
  const pos = String(sprayFieldPos ?? '')
    .trim()
    .toUpperCase();
  if (pos && FIELD_POS_CODES.has(pos)) return pos;
  const zone = String(sprayZone ?? '')
    .trim()
    .toUpperCase();
  return zone && ZONE_CODES.has(zone) ? zone : undefined;
}

/**
 * 构建一条落点记录；无坐标 / 区域无效 / 无击球员时返回 null（调用方不入队）
 * @param {object} state 录入现场快照（击球动作发生前）
 * @param {string} resultCode 结果码（H1/H2/H3/HR/IPHR/OUT_G/OUT_F/OUT_L/OUT_TAG/SF/SH/E/FC/DP/TP）
 * @param {number} rbi 打点（沿用既有口径：仅 HR/IPHR 传实际值，其余传 0）
 * @param {object} spray 落点信息（sprayX/sprayY/sprayZone/sprayFieldPos/sprayDepth/bipCode）
 */
export function buildHitSprayRecord(state, resultCode, rbi, spray) {
  const x = spray?.sprayX;
  const y = spray?.sprayY;
  // 0 是合法 SVG 坐标（边线/墙顶/本垒一线），不能按 falsy 当成“无坐标”。
  if (!Number.isFinite(x) || !Number.isFinite(y)) return null;
  const zone = resolveSprayZone(spray.sprayZone, spray.sprayFieldPos);
  if (!zone) return null;
  const batter = state.batter;
  if (!batter) return null;
  const occupied = [1, 2, 3].filter((base) => state.runners?.[base]);
  return {
    gameId: state.gameId ?? 0,
    playerId: Number(batter.id),
    teamId: state.teamId ?? 0,
    inning: state.inning,
    half: state.isTop ? "top" : "bottom",
    batterOrder: state.batterOrder,
    outsBefore: state.outs,
    runnersBefore: occupied.length > 0 ? occupied.join(",") : undefined,
    resultCode,
    bipCode: spray.bipCode,
    rbi,
    sprayX: x,
    sprayY: y,
    sprayZone: zone,
    sprayDepth: spray.sprayDepth,
  };
}

// 客户端唯一键：记录入队时生成，随结构化克隆一起进撤销/重做快照。
// 只有字符串型 rid 能在副本之间对上同一笔记录（快照是 structuredClone，不保留对象引用）。
export function newSprayRid() {
  const cryptoApi = globalThis.crypto;
  if (typeof cryptoApi?.randomUUID === 'function') return cryptoApi.randomUUID();
  return `r-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

/** 送出前剥掉 rid：bs_hit_spray 没有这一列，后端 DTO 也不认这个字段 */
export function toHitSprayPayload(record) {
  if (!record || typeof record !== 'object') return record;
  const { rid, ...payload } = record;
  return payload;
}

/**
 * 从队列/快照队列中剔除已送出的记录；没有 rid 的历史记录一律保留
 * （部署前的快照里没有 rid，无法判定，按原样保留交由数据清理处理）
 */
export function pruneSentSprayRecords(records, sentRids) {
  if (!Array.isArray(records)) return [];
  if (!(sentRids instanceof Set) || sentRids.size === 0) return records;
  return records.filter((record) => !record?.rid || !sentRids.has(record.rid));
}
