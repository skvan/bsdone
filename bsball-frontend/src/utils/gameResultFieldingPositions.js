// 结果录入页逐位置守备状态工具（Issue #701 后续）
// 职责：把 /game/{id}/fielding-positions 明细按球员分组、算合计回写球员级统计、
// 生成保存 payload、给守位下拉过滤选项、校验行内输入。
// 说明：positionOptions 只过滤已用守位与 DH/PH/EP，比赛模式白名单由页面负责。
import { outsToInnings } from './inningsFormat.js';

/** 计数据字段（不含 outs/defInn/fieldingGs：前者单独换算局数，后者取并集） */
export const POSITION_CREDIT_KEYS = ['po', 'a', 'e', 'dp', 'pb', 'catcherSb', 'catcherCs'];
const EXCLUDED_POSITIONS = new Set(['DH', 'PH', 'EP']);

// 非负整数（输入框字符串/浮点/空值都收敛到合法整数，未知字段归零）
const int = (value) => Number.isFinite(Number(value)) ? Math.max(0, Math.floor(Number(value))) : 0;
// 校验用：未填按 0，其余保持原值，避免 NaN 误判（`int` 会把负数吞成 0）
const countOrZero = (value) => (value === undefined || value === null || value === '' ? 0 : Number(value));
// 出场（GS）归一：真值 1；其余 0
const isFieldingGs = (row) => row?.fieldingGs === true || Number(row?.fieldingGs) === 1;
const normalizePosition = (value) => String(value ?? '').trim().toUpperCase();

/**
 * 逐位置明细按 teamId_playerId 分组，返回普通对象 Record（Task 5 直接按键取用）。
 * 非法 teamId/playerId/position 的行丢弃；计数据缺栏归零。
 */
export function groupFieldingPositionRows(rows) {
  const map = {};
  for (const row of rows ?? []) {
    const teamId = Number(row?.teamId);
    const playerId = Number(row?.playerId);
    const position = normalizePosition(row?.position);
    if (!Number.isInteger(teamId) || !Number.isInteger(playerId) || !position) continue;
    const key = `${teamId}_${playerId}`;
    if (!map[key]) map[key] = [];
    map[key].push({
      position,
      outs: int(row.outs),
      po: int(row.po), a: int(row.a), e: int(row.e), dp: int(row.dp),
      pb: int(row.pb), catcherSb: int(row.catcherSb), catcherCs: int(row.catcherCs),
      fieldingGs: isFieldingGs(row)
    });
  }
  return map;
}

/** 逐位置合计：outs 与各计数据求和（不含 fieldingGs） */
export function sumFieldingPositionRows(rows) {
  const sum = { outs: 0, po: 0, a: 0, e: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0 };
  for (const row of rows ?? []) {
    sum.outs += int(row.outs);
    for (const key of POSITION_CREDIT_KEYS) sum[key] += int(row[key]);
  }
  return sum;
}

/**
 * 把逐位置合计就地写回球员级统计：计数据取合计，INN 由 outs 换算，GS 取任一行出场。
 */
export function applyFieldingPositionTotals(stats, rows) {
  if (!stats) return;
  const sum = sumFieldingPositionRows(rows);
  for (const key of POSITION_CREDIT_KEYS) stats[key] = sum[key];
  stats.defInn = outsToInnings(sum.outs);
  stats.fieldingGs = (rows ?? []).some((row) => isFieldingGs(row)) ? 1 : 0;
}

/**
 * 保存 payload：只带 dirty 球员（teamId_playerId），rows 缺省为 []（后端视为清空该球员全部守位）。
 */
export function buildFieldingPlayerParts(dirtyKeys, byPlayer) {
  return [...(dirtyKeys ?? [])].map((key) => {
    const [teamId, playerId] = String(key).split('_').map(Number);
    const rows = byPlayer?.[key] ?? [];
    return {
      teamId,
      playerId,
      rows: rows.map((row) => ({
        position: String(row.position ?? '').toUpperCase(),
        outs: int(row.outs),
        po: int(row.po), a: int(row.a), e: int(row.e), dp: int(row.dp),
        pb: int(row.pb), catcherSb: int(row.catcherSb), catcherCs: int(row.catcherCs)
      }))
    };
  });
}

/** 可选守位：全量守位中排除 DH/PH/EP 与本球员已用守位（模式白名单不在此处理） */
export function positionOptions(allPositions, rows) {
  const used = new Set((rows ?? []).map((row) => normalizePosition(row.position)));
  return (allPositions ?? [])
    .map((position) => normalizePosition(position))
    .filter((position) => position && !EXCLUDED_POSITIONS.has(position) && !used.has(position));
}

/**
 * 单球员逐位置行校验：返回错误文案，通过返回 null。
 * 顺序：守位合法 → 守位不重复 → outs/计数据 0..999；缺栏按 0。
 */
export function validateFieldingPositionRows(rows, allPositions) {
  const allowed = new Set((allPositions ?? []).map((position) => String(position).toUpperCase()));
  const used = new Set();
  for (const row of rows ?? []) {
    const position = normalizePosition(row.position);
    if (!position || EXCLUDED_POSITIONS.has(position) || (allowed.size && !allowed.has(position))) {
      return '守位无效';
    }
    if (used.has(position)) return '守位重复';
    used.add(position);
    const outs = countOrZero(row.outs);
    if (!Number.isInteger(outs) || outs < 0 || outs > 999) return '出局数超出范围';
    for (const key of POSITION_CREDIT_KEYS) {
      const value = countOrZero(row[key]);
      if (!Number.isInteger(value) || value < 0 || value > 999) return '数值超出范围';
    }
  }
  return null;
}
