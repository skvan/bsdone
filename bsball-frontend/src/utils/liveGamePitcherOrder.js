// 投手登板序（#478）：沿用已持久化的首次登板顺序，新投手追加最大序号。
// 不再按“当前投手优先”重排，避免换投后 pitcherOrder 倒序。

export function normalizePitcherOrder(value) {
  const order = Number(value);
  return Number.isInteger(order) && order > 0 ? order : 0;
}

export function maxPitcherOrder(players) {
  let max = 0;
  for (const player of Array.isArray(players) ? players : []) {
    const order = normalizePitcherOrder(player?.pitcherOrder);
    if (order > max) max = order;
  }
  return max;
}

/** 给指定球员及其同 id 副本写入登板序；已有顺序优先，缺失则追加到最大序号之后。 */
export function assignPitcherOrder(players, playerId) {
  const id = Number(playerId);
  if (!Number.isInteger(id) || id <= 0) return 0;
  const list = Array.isArray(players) ? players : [];
  let order = 0;
  for (const player of list) {
    if (Number(player?.id) !== id) continue;
    order = normalizePitcherOrder(player.pitcherOrder);
    if (order) break;
  }
  if (!order) order = maxPitcherOrder(list) + 1;
  for (const player of list) {
    if (Number(player?.id) === id) player.pitcherOrder = order;
  }
  return order;
}

/** 用后端统计行的 pitcherOrder 回填本场球员（快照/重建路径）。 */
export function seedPitcherOrders(players, statRows) {
  const list = Array.isArray(players) ? players : [];
  const orders = new Map();
  for (const row of Array.isArray(statRows) ? statRows : []) {
    const id = Number(row?.playerId);
    const order = normalizePitcherOrder(row?.pitcherOrder);
    if (Number.isInteger(id) && id > 0 && order > 0) orders.set(id, order);
  }
  let changed = 0;
  for (const player of list) {
    const order = orders.get(Number(player?.id));
    if (!order || normalizePitcherOrder(player.pitcherOrder) === order) continue;
    player.pitcherOrder = order;
    changed++;
  }
  return changed;
}

/** 按登板序排序投手行；缺失者按给定顺序追加（通常仅新投手兜底）。 */
export function orderPitcherRows(pitchers) {
  const list = Array.isArray(pitchers) ? pitchers : [];
  const orderById = new Map();
  let max = 0;
  for (const pitcher of list) {
    const id = Number(pitcher?.id);
    if (!Number.isInteger(id) || id <= 0 || orderById.has(id)) continue;
    const order = normalizePitcherOrder(pitcher.pitcherOrder);
    if (order > max) max = order;
    if (order > 0) orderById.set(id, order);
  }
  for (const pitcher of list) {
    const id = Number(pitcher?.id);
    if (!Number.isInteger(id) || id <= 0 || orderById.has(id)) continue;
    const order = ++max;
    orderById.set(id, order);
    pitcher.pitcherOrder = order;
  }
  const rows = list
    .filter((pitcher) => orderById.has(Number(pitcher?.id)))
    .sort((a, b) => orderById.get(Number(a.id)) - orderById.get(Number(b.id)));
  return { rows, orderById };
}
