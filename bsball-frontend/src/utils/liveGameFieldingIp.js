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
  if (overflow <= 0) return { ...empty, teamOuts, maxOuts };
  const changed = [];
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
