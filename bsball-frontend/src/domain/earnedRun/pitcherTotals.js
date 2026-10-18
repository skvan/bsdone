// 重建接口返回的“库内绝对总量”回显：只覆盖，不累加，刷新后仍幂等。
export function applyAbsolutePitcherTotals(players, totals) {
  const byPitcher = new Map();
  for (const [pitcherId, total] of Object.entries(totals ?? {})) {
    const id = Number(pitcherId);
    if (Number.isFinite(id) && total) byPitcher.set(id, total);
  }
  if (byPitcher.size === 0) return 0;

  let applied = 0;
  for (const player of players ?? []) {
    const id = Number(player?.id);
    const total = byPitcher.get(id);
    if (!Number.isFinite(id) || !total || !player?.stats?.pitching) continue;
    player.stats.pitching.pitchR = nonNegative(total.runs);
    player.stats.pitching.er = nonNegative(total.earnedRuns);
    player.stats.pitching.unearnedR = nonNegative(total.unearnedRuns);
    player.stats.pitching.pendingR = nonNegative(total.pendingRuns);
    applied += 1;
  }
  return applied;
}

function nonNegative(value) {
  const number = Number(value);
  return Number.isFinite(number) ? Math.max(0, number) : 0;
}
