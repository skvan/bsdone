// 逐位置守备局数：按出局累计 outs（3 outs = 1.0 局），供 save-live 与按位置排行使用
const NON_FIELDING_POSITIONS = new Set(['DH', 'PH', 'EP']);

export function normalizeFieldingPosition(value) {
  return String(value ?? '').trim().toUpperCase();
}

export function isFieldingPosition(position) {
  const pos = normalizeFieldingPosition(position);
  return !!pos && !NON_FIELDING_POSITIONS.has(pos);
}

/** 给球员 fielding 统计按当前守位 +1 出局；返回是否计入（DH/PH/EP/空守位不计） */
export function accrueFieldingOut(fielding, position) {
  if (!fielding) return false;
  const pos = normalizeFieldingPosition(position);
  if (!isFieldingPosition(pos)) return false;
  const byPos = fielding.byPos ?? (fielding.byPos = {});
  byPos[pos] = (Number(byPos[pos]) || 0) + 1;
  return true;
}

/** 生成 save-live 的 fieldingPositions（球员 × 位置）；先发的首个守位标 GS */
export function buildFieldingPositionRows(teamId, players, starterIds) {
  const rows = [];
  const starters = new Set((Array.isArray(starterIds) ? starterIds : []).map((id) => Number(id)));
  for (const player of Array.isArray(players) ? players : []) {
    const playerId = Number(player?.id);
    if (!Number.isFinite(playerId) || playerId <= 0) continue;
    const byPos = player?.stats?.fielding?.byPos;
    if (!byPos || typeof byPos !== 'object') continue;
    let firstPos = null;
    for (const [rawPos, rawOuts] of Object.entries(byPos)) {
      const position = normalizeFieldingPosition(rawPos);
      if (!isFieldingPosition(position)) continue;
      if (firstPos == null) firstPos = position;
      const outs = Math.floor(Number(rawOuts) || 0);
      if (outs <= 0) continue;
      rows.push({
        teamId: Number(teamId),
        playerId,
        position,
        outs,
        fieldingGs: starters.has(playerId) && position === firstPos,
      });
    }
  }
  return rows;
}

/** 每个守备出局：投手按 P、在场野手按当前守位各 +1；同 id 只计一次 */
export function accrueActiveFieldingOuts(pitcher, fielders, positionOf = (p) => p?.position) {
  const seen = new Set();
  const add = (player, position) => {
    const id = Number(player?.id);
    if (!Number.isFinite(id) || id <= 0 || seen.has(String(id))) return;
    const stats = player?.stats;
    if (!stats) return;
    seen.add(String(id));
    accrueFieldingOut(stats.fielding ?? (stats.fielding = {}), position);
  };
  add(pitcher, 'P');
  for (const fielder of Array.isArray(fielders) ? fielders : []) add(fielder, positionOf(fielder));
}

/** 是否存在逐位置基线（任一球员 stats.fielding.byPos 为对象）；无基线时不发 key，避免覆盖历史数据 */
export function hasFieldingByPosBaseline(players) {
  return (Array.isArray(players) ? players : []).some((p) => {
    const byPos = p?.stats?.fielding?.byPos;
    return !!byPos && typeof byPos === 'object';
  });
}

/** 历史统计行是否已有守备局数（Do 重建路径据此判断逐位置基线是否可信） */
export function hasHistoricalFieldingInnings(statRows) {
  return (Array.isArray(statRows) ? statRows : []).some((row) => (Number(row?.defInn) || 0) > 0);
}
