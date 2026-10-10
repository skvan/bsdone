// 阵容行数组的不变量与置换（纯函数，node 可测）
// 行对象形状与旧件一致：{id,name,number,teamId,position,stats}；空行 = id<0 占位行
export const PLACEHOLDER_BASE = -10000;

/** 与旧件 M() 同形状的空白统计 */
export function emptyStats() {
  return {
    batting: { ab: 0, r: 0, h: 0, rbi: 0, bb: 0, hbp: 0, so: 0, so_swing: 0, so_looking: 0, sf: 0, sh: 0, sb: 0, cs: 0, doubles: 0, triples: 0, hr: 0 },
    pitching: { ip: 0, np: 0, pitchH: 0, er: 0, pitchBbHp: 0, pitchSo: 0, pitchHr: 0, pitchInsideParkHr: 0, wp: 0, bk: 0, pk: 0 },
    fielding: { po: 0, a: 0, e: 0 },
  };
}

export function makePlaceholderRow(teamId, index) {
  return { id: PLACEHOLDER_BASE - Number(index || 0), name: '', number: '', teamId, position: '', stats: emptyStats() };
}

export function isPlaceholder(row) {
  return row != null && typeof row.id === 'number' && row.id < 0;
}

export function posOf(row) {
  return String(row?.position ?? '').trim().toUpperCase();
}

export function makeRow(player, teamId, position) {
  return { ...player, teamId, position: String(position ?? '').toUpperCase(), stats: player.stats ?? emptyStats() };
}

export function hasPlayer(list, playerId) {
  return (list ?? []).some((p) => p != null && String(p.id) === String(playerId));
}

export function findRowIndex(rows, playerId) {
  return rows.findIndex((r) => !isPlaceholder(r) && String(r.id) === String(playerId));
}

export function slotRow(rows, code) {
  const c = String(code ?? '').toUpperCase();
  return rows.find((r) => !isPlaceholder(r) && posOf(r) === c) ?? null;
}

/** 非空行保持原相对顺序在前，补足占位空行至 max(baseCount, filled) 且不超过 maxCount */
export function normalizeRows(list, baseCount, maxCount, teamId) {
  const filled = (list ?? []).filter((r) => !isPlaceholder(r)).slice(0, maxCount);
  const out = [...filled];
  const target = Math.max(baseCount, filled.length);
  let i = 0;
  while (out.length < target) out.push(makePlaceholderRow(teamId, out.length + i++));
  return out;
}

/** 旧件 ke() 特例：长度=baseCount 且位置全空且校验因「未指定守备位置」失败 → msg 置空 */
export function applyKeSpecial(validation, rows, baseCount) {
  const allEmpty = rows.length > 0 && rows.every((r) => !String(r.position ?? '').trim());
  if (rows.length === baseCount && allEmpty && !validation.ok && String(validation.msg ?? '').includes('未指定守备位置')) {
    return { ok: false, msg: undefined, errorRowIndices: [] };
  }
  return validation;
}
