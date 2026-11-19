// 结果录入页守备表：逐位置守备与球员级统计的合并口径（Issue #701）
// - 球员统计行已有守备数据：以球员级为权威（人工修订后的最终值），逐位置只补守位展示，
//   不得用逐位置合计覆盖 PO/A/E/DP/PB/CS/SB/INN/GS，否则刷新再保存会把人工值回滚。
// - 球员统计行守备全零（兼容旧数据）或只存在于逐位置表：用逐位置合计补全（#560 口径）。
import { createBatterStatDefaults, resolveFirstFieldPosition } from './gamePlayerStat.js';
import { outsToInnings } from './inningsFormat.js';

const FIELDING_TOTAL_KEYS = ['po', 'a', 'e', 'dp', 'pb', 'catcherSb', 'catcherCs'];
const FIELDING_ONLY_POSITIONS = new Set(['DH', 'PH', 'EP']);

/**
 * 球员统计行是否已有守备计数据（人工修订值是否可信）。
 * 只看 PO/A/E/DP/PB/CS/SB：INN(outs) 由 live 保存按逐位置自动派生，不能当人工修订痕迹；
 * 全部计数据为零时按逐位置补全（#560 旧数据兼容）。
 */
export function hasResultFieldingActivity(stats) {
  if (!stats) return false;
  return FIELDING_TOTAL_KEYS.some((key) => Number(stats[key]) > 0);
}

/**
 * 把逐位置守备合并进结果录入页的球员级统计（就地修改 rows / statsByKey）。
 *
 * @param {object} options
 * @param {number|string} options.teamId 当前球队
 * @param {Array<object>} options.rows 结果页球员行（打者/投手/守备专用行）
 * @param {Array<object>} options.positionRows `/game/{id}/fielding-positions` 明细行
 * @param {object} options.statsByKey 结果页统计对象（按行 key）
 * @param {Function} options.hasPlayerStatRow 判断球员是否有球员级统计行
 * @param {Function} options.isPitcherRow 判断是否投手行
 * @param {Function} options.keyOf 取行统计 key
 * @param {Function} options.createFieldingOnlyRow 创建仅守备行
 */
export function mergeResultFieldingRows({
  teamId,
  rows,
  positionRows,
  statsByKey,
  hasPlayerStatRow,
  isPitcherRow,
  keyOf,
  createFieldingOnlyRow
}) {
  const grouped = new Map();
  for (const item of positionRows ?? []) {
    if (String(item?.teamId) !== String(teamId)) continue;
    const playerId = Number(item.playerId);
    if (!Number.isInteger(playerId) || playerId <= 0) continue;
    if (!grouped.has(playerId)) grouped.set(playerId, []);
    grouped.get(playerId).push(item);
  }
  for (const [playerId, positions] of grouped) {
    let target = rows.find((row) => Number(row.id) === playerId && !isPitcherRow(row))
      || rows.find((row) => Number(row.id) === playerId);
    if (!target) {
      target = createFieldingOnlyRow(playerId);
      rows.push(target);
    }
    const key = keyOf(target);
    if (!(key in statsByKey)) statsByKey[key] = { ...createBatterStatDefaults };
    const stats = statsByKey[key];
    const originalFirst = resolveFirstFieldPosition(stats);
    const firstPosition = positions
      .map((item) => String(item.position ?? '').trim().toUpperCase())
      .find(Boolean) || target.position || '';
    const sum = (name) => positions.reduce((total, item) => total + (Number(item[name]) || 0), 0);
    const positionGs = positions.some(
      (item) => item.fieldingGs === true || Number(item.fieldingGs) === 1
    ) ? 1 : 0;
    if (hasPlayerStatRow(playerId) && hasResultFieldingActivity(stats)) {
      // #701：人工修订后的球员级值必须原样回显，逐位置合计只用于补守位展示。
      stats.position = stats.position || firstPosition;
      stats.firstPosition = originalFirst || stats.firstPosition || firstPosition;
      // INN/GS 单独兜底：旧数据可能只写了逐位置（保存后差额回写会让两边一致）。
      if (!(Number(stats.defInn) > 0)) stats.defInn = outsToInnings(sum('outs'));
      if (!(Number(stats.fieldingGs) > 0)) stats.fieldingGs = positionGs;
    } else {
      stats.position = firstPosition;
      stats.firstPosition = originalFirst || firstPosition;
      for (const name of FIELDING_TOTAL_KEYS) stats[name] = sum(name);
      stats.defInn = Number(stats.defInn) > 0 ? stats.defInn : outsToInnings(sum('outs'));
      stats.fieldingGs = Number(stats.fieldingGs) > 0 ? stats.fieldingGs : positionGs;
    }
    target._fieldPosition = originalFirst || stats.firstPosition || firstPosition;
  }
  for (const row of rows) {
    const key = keyOf(row);
    if (isPitcherRow(row) && !(key in statsByKey)) {
      statsByKey[key] = { ...createBatterStatDefaults, position: row.position ?? 'P' };
    }
    row._fieldPosition = row._fieldPosition
      || resolveFirstFieldPosition(statsByKey[key])
      || (isPitcherRow(row) ? 'P' : '');
  }
  for (const row of rows) {
    const stats = statsByKey[keyOf(row)];
    if (!isPitcherRow(row) && stats && stats.battingOrder == null && stats.listOrder == null) {
      const position = String(stats.position ?? '').trim().toUpperCase();
      if (!FIELDING_ONLY_POSITIONS.has(position)
        && (Number(stats.po) || Number(stats.a) || Number(stats.e) || Number(stats.defInn))) {
        row._fieldingOnly = true;
      }
    }
  }
}
