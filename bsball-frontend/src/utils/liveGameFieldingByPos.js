// 逐位置守备局数：按出局累计 outs（3 outs = 1.0 局），供 save-live 与按位置排行使用
const NON_FIELDING_POSITIONS = new Set(['DH', 'PH', 'EP']);

export function normalizeFieldingPosition(value) {
  return String(value ?? '').trim().toUpperCase();
}

export function isFieldingPosition(position) {
  const pos = normalizeFieldingPosition(position);
  return !!pos && !NON_FIELDING_POSITIONS.has(pos);
}

function countOf(value) {
  const n = Math.floor(Number(value) || 0);
  return n > 0 ? n : 0;
}

/** 取或升级某守位的逐位置条目；旧快照的数字值视为仅有 outs */
function byPosEntry(fielding, position) {
  const byPos = fielding.byPos ?? (fielding.byPos = {});
  const current = byPos[position];
  if (current && typeof current === 'object') {
    current.outs = countOf(current.outs);
    current.po = countOf(current.po);
    current.a = countOf(current.a);
    current.e = countOf(current.e);
    return current;
  }
  const entry = { outs: countOf(current), po: 0, a: 0, e: 0 };
  byPos[position] = entry;
  return entry;
}

/** 给球员 fielding 统计按当前守位 +1 出局；返回是否计入（DH/PH/EP/空守位不计） */
export function accrueFieldingOut(fielding, position) {
  if (!fielding) return false;
  const pos = normalizeFieldingPosition(position);
  if (!isFieldingPosition(pos)) return false;
  byPosEntry(fielding, pos).outs += 1;
  return true;
}

/** DH 制投手不在打线时 Hn 里是深拷贝；P 位记账必须落回活体投手行 */
export function resolveFieldingCreditTarget(position, player, livePitcher) {
  return normalizeFieldingPosition(position) === 'P' && livePitcher ? livePitcher : player;
}

/** 按当前守位累计 PO/A/E；返回是否计入（非法守位或全零不计） */
export function accrueFieldingPlay(fielding, position, { po = 0, a = 0, e = 0 } = {}) {
  if (!fielding) return false;
  const pos = normalizeFieldingPosition(position);
  if (!isFieldingPosition(pos)) return false;
  const addPo = countOf(po);
  const addA = countOf(a);
  const addE = countOf(e);
  if (addPo + addA + addE === 0) return false;
  const entry = byPosEntry(fielding, pos);
  entry.po += addPo;
  entry.a += addA;
  entry.e += addE;
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
    for (const [rawPos, rawEntry] of Object.entries(byPos)) {
      const position = normalizeFieldingPosition(rawPos);
      if (!isFieldingPosition(position)) continue;
      if (firstPos == null) firstPos = position;
      const entry = rawEntry && typeof rawEntry === 'object' ? rawEntry : { outs: rawEntry };
      const outs = countOf(entry.outs);
      const po = countOf(entry.po);
      const a = countOf(entry.a);
      const e = countOf(entry.e);
      if (outs + po + a + e === 0) continue;
      rows.push({
        teamId: Number(teamId),
        playerId,
        position,
        outs,
        po,
        a,
        e,
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
