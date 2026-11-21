// 失误上垒传球链助杀口径（#695）：
// 链上除最后一人（接球/触垒者）外，每人记 1 次助杀 A；
// 与失误责任守备员对比，失误者不计 A（可有多名失误者）。

function normalizePosition(value) {
  return String(value ?? '').trim().toUpperCase();
}

/**
 * 计算失误传球链应记助杀的守备位置。
 * @param {string[]} errorFielders 失误责任守备员位置（如 ['1B']）
 * @param {string[]} chain 传球链位置（按球路顺序，最后一位为接球/触垒者，如 ['2B','1B']）
 * @returns {string[]} 应记 A 的位置（去重、保持链上顺序）
 */
export function computeErrorChainAssists(errorFielders, chain) {
  const errors = new Set((Array.isArray(errorFielders) ? errorFielders : []).map(normalizePosition).filter(Boolean));
  const positions = (Array.isArray(chain) ? chain : []).map(normalizePosition).filter(Boolean);
  if (positions.length < 2) return [];
  const assists = [];
  for (let i = 0; i < positions.length - 1; i += 1) {
    const pos = positions[i];
    if (errors.has(pos) || assists.includes(pos)) continue;
    assists.push(pos);
  }
  return assists;
}

/**
 * 失误传球链链尾接杀口径：链尾不是失误责任者时，链尾仍记 1 次接杀 PO。
 * （链尾为失误责任者时不记 PO，由失误记账覆盖。）
 * @param {string[]} errorFielders 失误责任守备员位置
 * @param {string[]} chain 传球链位置（最后一位为接球/触垒者）
 * @returns {string|null} 应记 PO 的位置；不满足条件返回 null
 */
export function computeErrorChainPutout(errorFielders, chain) {
  const errors = new Set((Array.isArray(errorFielders) ? errorFielders : []).map(normalizePosition).filter(Boolean));
  const positions = (Array.isArray(chain) ? chain : []).map(normalizePosition).filter(Boolean);
  if (positions.length < 2) return null;
  const last = positions[positions.length - 1];
  return errors.has(last) ? null : last;
}
