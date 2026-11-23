import { inningsToOuts, outsToInnings } from './inningsFormat.js';

/** Return innings pitched after recording one out using baseball's .0/.1/.2 notation. */
export function addOutToInnings(ip) {
  return outsToInnings(inningsToOuts(Number(ip)) + 1);
}

/** DH and other non-fielding lineup slots must not receive defensive innings. */
export function activeFielders(players, validPositions, normalizePosition = (position) => String(position ?? '').trim().toUpperCase()) {
  if (!Array.isArray(players)) return [];
  return players.filter((player) => {
    const position = normalizePosition(player?.position);
    return position !== 'DH' && validPositions.has(position);
  });
}

export const DEFAULT_FIELDING_POSITIONS = new Set([
  'P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF', 'XF1', 'XF2'
]);

/**
 * 该队截至目前已完成的守备出局数。
 * 客队上半局进攻、下半局守备；主队上半局守备、下半局进攻。
 * 仅用于“只减不增”的对账，强制换局等提前结束的半局按 3 出局估算，
 * 结果最多是不触发下调，不会把正常值抬高。
 */
export function teamDefensiveOutsSoFar({ inning, isTop, outs, side }) {
  const currentInning = Math.max(1, Number(inning) || 1);
  const currentOuts = Math.min(3, Math.max(0, Number(outs) || 0));
  const completed = (currentInning - 1) * 3;
  if (side === 'away') return completed + (isTop ? 0 : currentOuts);
  if (side === 'home') return completed + (isTop ? currentOuts : 3);
  return 0;
}

const BATTING_ONLY_POSITIONS = new Set(['DH', 'PH', 'EP']);

/** #699：本半局还能记的出局数（每半局 3 出局上限） */
export function outsRemainingInHalfInning(outs) {
  const current = Math.max(0, Math.min(3, Math.floor(Number(outs) || 0)));
  return 3 - current;
}

/** #699：把一次事件需要记的出局数按本半局剩余出局数截断 */
export function creditOutsWithinLimit(requested, outs) {
  const wanted = Math.max(0, Math.floor(Number(requested) || 0));
  return Math.min(wanted, outsRemainingInHalfInning(outs));
}

function entryOutsOf(entry) {
  const value = entry && typeof entry === 'object' ? entry.outs : entry;
  return Math.max(0, Math.floor(Number(value) || 0));
}

/**
 * #699：把逐位置出局合计压到 capOuts 以内（按守位插入顺序从后往前扣）。
 * 后端 def_inn 由 fieldingPositions 各行 outs 合计决定，因此必须收敛 byPos.outs，
 * 只改 fielding.ip 不足以修复多记的出局。旧快照数字值在需要扣减时转成标准对象。
 */
export function capFieldingByPosOuts(fielding, capOuts) {
  const byPos = fielding && typeof fielding === 'object' ? fielding.byPos : null;
  const cap = Math.max(0, Math.floor(Number(capOuts) || 0));
  const result = { before: 0, after: 0, removed: 0, positions: [] };
  if (!byPos || typeof byPos !== 'object') return result;
  const positions = Object.keys(byPos).filter((position) =>
    !BATTING_ONLY_POSITIONS.has(position) && byPos[position] != null);
  result.before = positions.reduce((sum, position) => sum + entryOutsOf(byPos[position]), 0);
  let excess = Math.max(0, result.before - cap);
  for (let index = positions.length - 1; index >= 0 && excess > 0; index--) {
    const position = positions[index];
    const raw = byPos[position];
    const entry = raw && typeof raw === 'object'
      ? raw
      : (byPos[position] = { outs: entryOutsOf(raw), po: 0, a: 0, e: 0, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0 });
    const current = entryOutsOf(entry);
    const cut = Math.min(current, excess);
    if (cut <= 0) continue;
    entry.outs = current - cut;
    excess -= cut;
    result.removed += cut;
    result.positions.push({ position, before: current, after: current - cut });
  }
  result.after = result.before - result.removed;
  return result;
}

/**
 * 守备局数对账：野手 IP 不可能超过该队已完成的守备出局数。
 * 若因撤销/重开/流程中断导致整队野手多记，把整队溢出量从当前在场野手统一扣回。
 * 投手行（pitcherId）由独立字段同步，跳过，避免把只多记野手的场景反向扣到投手。
 */
export function reconcileTeamFieldingInnings({
  side,
  gameState,
  lineup,
  pitcherId,
  extraPlayers = [],
  validPositions = DEFAULT_FIELDING_POSITIONS,
  normalizePosition = (position) => String(position ?? '').trim().toUpperCase()
} = {}) {
  const empty = { teamOuts: 0, maxOuts: 0, overflow: 0, changed: [] };
  if (!Array.isArray(lineup) || !gameState) return empty;
  const positions = validPositions instanceof Set ? validPositions : new Set(validPositions ?? DEFAULT_FIELDING_POSITIONS);
  const teamOuts = teamDefensiveOutsSoFar({ ...gameState, side });
  const pitcher = pitcherId == null ? null : Number(pitcherId);
  const candidates = lineup.filter((player) => {
    if (!player?.stats?.fielding) return false;
    const id = Number(player.id);
    if (Number.isFinite(pitcher) && Number.isFinite(id) && id === pitcher) return false;
    const position = normalizePosition(player.position);
    // 投手行由独立字段同步：非 DH 赛制下 O/te 可能为空，按位置再兜底排除，避免误扣投手。
    return position !== 'DH' && position !== 'P' && positions.has(position);
  });
  const outsOf = (player) => inningsToOuts(Number(player.stats.fielding.ip) || 0);
  const maxOuts = candidates.reduce((max, player) => Math.max(max, outsOf(player)), 0);
  const overflow = Math.max(0, maxOuts - teamOuts);
  // #699：逐位置出局是后端 def_inn 的唯一来源，先按球队合法出局数收敛 byPos，
  // 再让 fielding.ip / pitching.ip 与之一致（只减不增）。含投手行与 DH 制场外投手。
  const changed = [];
  const players = [];
  const seenStats = new Set();
  for (const player of [...lineup, ...(Array.isArray(extraPlayers) ? extraPlayers : [])]) {
    if (!player?.stats?.fielding || seenStats.has(player.stats)) continue;
    seenStats.add(player.stats);
    players.push(player);
  }
  for (const player of players) {
    const fielding = player.stats.fielding;
    const byPosResult = capFieldingByPosOuts(fielding, teamOuts);
    if (byPosResult.removed > 0) {
      changed.push({
        playerId: player.id, name: player.name, kind: 'byPos',
        before: byPosResult.before, after: byPosResult.after, positions: byPosResult.positions
      });
    }
    // 有逐位置基线才反向收敛 IP；旧快照只有 fielding.ip 时保留原有野手下调逻辑
    if (byPosResult.before > 0) {
      const beforeIp = Number(fielding.ip) || 0;
      if (inningsToOuts(beforeIp) > byPosResult.after) {
        fielding.ip = outsToInnings(byPosResult.after);
        changed.push({ playerId: player.id, name: player.name, kind: 'fielding.ip', before: beforeIp, after: fielding.ip });
      }
      const pitching = player.stats.pitching;
      const pitcherEntry = fielding.byPos?.P;
      if (pitching && pitcherEntry && typeof pitcherEntry === 'object') {
        const pitchOuts = inningsToOuts(Number(pitching.ip) || 0);
        const pOuts = entryOutsOf(pitcherEntry);
        if (pitchOuts > pOuts) {
          const beforePitch = Number(pitching.ip) || 0;
          pitching.ip = outsToInnings(pOuts);
          changed.push({ playerId: player.id, name: player.name, kind: 'pitching.ip', before: beforePitch, after: pitching.ip });
        }
      }
    }
  }
  if (overflow <= 0 && changed.length === 0) return { ...empty, teamOuts, maxOuts };
  for (const player of candidates) {
    const outs = outsOf(player);
    // 只下调超过球队已完成守备出局数的球员；后上场球员的合法局数低于该上限时保持不动。
    if (outs <= teamOuts) continue;
    const before = Number(player.stats.fielding.ip) || 0;
    const after = outsToInnings(teamOuts);
    player.stats.fielding.ip = after;
    changed.push({ playerId: player.id, name: player.name, before, after });
  }
  return { teamOuts, maxOuts, overflow, changed };
}
