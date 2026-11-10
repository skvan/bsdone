// 「上一棒内推进（牺牲打）」补记规则（2026-11-10 口径）
// 只有上一棒已经记成击球出局时才能补记牺牲打；补记后回减 1 个 AB。
// - 飞球/平飞出局且跑者回本垒：SF
// - 滚地/触杀出局：SH（跑者回本垒时同时记 RBI）
// - 已经记过 SF/SH、安打、保送、触身、失误、野选等结果不补记。

const FLY_OUT_CODES = new Set(["F", "L", "OUT_F", "OUT_L"]);
const GROUND_OUT_CODES = new Set(["G", "TAG", "OUT_G", "OUT_TAG"]);
const ALREADY_SACRIFICE_CODES = new Set(["SF", "SH"]);

/**
 * 判定一次「上一棒内推进（牺牲打）」是否可以补记牺牲打。
 * @param {{resultCode?: string, toBase?: number}} params
 * @returns {{resultCode: 'SF'|'SH', creditRbi: boolean}|null}
 */
export function resolveSacrificeCredit({ resultCode, toBase } = {}) {
  const code = String(resultCode ?? "").trim().toUpperCase();
  if (!code || ALREADY_SACRIFICE_CODES.has(code)) return null;
  const reachedHome = Number(toBase) === 4;
  if (FLY_OUT_CODES.has(code)) {
    // 飞球出局但跑者没有得分，只是普通高飞推进，不记 SF
    return reachedHome ? { resultCode: "SF", creditRbi: true } : null;
  }
  if (GROUND_OUT_CODES.has(code)) {
    return { resultCode: "SH", creditRbi: reachedHome };
  }
  return null;
}
