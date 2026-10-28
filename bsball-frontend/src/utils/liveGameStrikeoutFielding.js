// 三振守备记账（#483）：普通三振捕手 PO；漏接三振传一垒出局捕手 A、守备员 PO。
// 纯函数，便于单测；是否真正落账由 LiveGame 按场上球员是否存在决定。

const ZERO = Object.freeze({ catcherPo: 0, catcherA: 0, fielderPo: 0 });

/**
 * @param {"caught"|"dropped"} kind caught=普通/擦棒被捕三振；dropped=漏接三振
 * @param {string|null} fielderCode 漏接三振完成一垒出局的守备位置（C 表示捕手自己触杀）
 */
export function strikeoutFieldingCredit(kind, fielderCode = null) {
  if (kind === 'caught') {
    return { catcherPo: 1, catcherA: 0, fielderPo: 0 };
  }
  if (kind !== 'dropped') {
    return { ...ZERO };
  }
  const code = String(fielderCode ?? '').trim().toUpperCase();
  if (!code) {
    return { ...ZERO };
  }
  if (code === 'C') {
    return { catcherPo: 1, catcherA: 0, fielderPo: 0 };
  }
  return { catcherPo: 0, catcherA: 1, fielderPo: 1 };
}
