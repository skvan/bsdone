// 电子秩序册（Issue #310）：阵容池过滤纯函数（受限赛事剔除名单外球员；未受限原样放行）
export function isInPool(playerId, pool) {
  if (playerId == null) return true;
  if (pool?.restricted !== true) return true;
  return (pool.players ?? []).some((p) => String(p.id) === String(playerId));
}

/** 取「在池内才保留」的 id（用于守备投手/DH 池等单值字段的过滤）。 */
export function pickPoolId(playerId, pool) {
  return isInPool(playerId, pool) ? playerId : null;
}

/**
 * 过滤阵容/替补中的名单外球员。
 * - pool.restricted !== true 时原样返回（removed 恒空）；
 * - 占位行（id 为空或 ≤0，如 -10000 系列）原样保留、不记为 removed（它们不代表具体球员）；
 * - 其余名单外球员剔除并回传 removed（id/name 供提示）。
 */
export function filterOutOfRoster(rows, bench, pool) {
  if (pool?.restricted !== true) {
    return { keptRows: rows ?? [], keptBench: bench ?? [], removed: [] };
  }
  const ids = new Set((pool.players ?? []).map((p) => String(p.id)));
  const removed = [];
  const keep = (player) => {
    if (player == null) return false;
    const rawId = player.id;
    if (rawId == null || !(Number(rawId) > 0)) return true; // 占位行放行
    if (ids.has(String(rawId))) return true;
    removed.push({ id: rawId, name: player.name });
    return false;
  };
  return {
    keptRows: (rows ?? []).filter(keep),
    keptBench: (bench ?? []).filter(keep),
    removed
  };
}

/**
 * 过滤阵容模板：槽位中的名单外球员置空（留位待选）、替补与先发投手剔除；
 * 返回 removedCount 供提示。未受限时原样返回。
 */
export function filterTemplateByPool(template, pool) {
  const slots = Array.isArray(template?.slots) ? template.slots : [];
  const bench = Array.isArray(template?.benchPlayerIds) ? template.benchPlayerIds : [];
  const pitcherId = template?.startingPitcherPlayerId ?? null;
  if (pool?.restricted !== true) {
    return { slots, benchPlayerIds: bench, pitcherId, removedCount: 0 };
  }
  const ids = new Set((pool.players ?? []).map((p) => String(p.id)));
  let removedCount = 0;
  const keptSlots = slots.map((slot) => {
    const pid = slot?.playerId;
    if (pid != null && !ids.has(String(pid))) {
      removedCount += 1;
      return { ...slot, playerId: null };
    }
    return slot;
  });
  const keptBench = bench.filter((id) => {
    if (ids.has(String(id))) return true;
    removedCount += 1;
    return false;
  });
  let keptPitcher = pitcherId;
  if (pitcherId != null && !ids.has(String(pitcherId))) {
    removedCount += 1;
    keptPitcher = null;
  }
  return { slots: keptSlots, benchPlayerIds: keptBench, pitcherId: keptPitcher, removedCount };
}
