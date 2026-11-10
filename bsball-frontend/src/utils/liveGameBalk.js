// 投手犯规（Balk）与不合法投球（Illegal Pitch）分流规则（2026-11-10 口径）
// 棒球 OBR：有跑者 Balk（推进一垒、不记坏球、记 BK）；无跑者 Illegal Pitch（记一坏球、不记 BK）。
// 垒球 WBSC：没有 Balk；Illegal Pitch 记一坏球，有跑者时所有跑者各进一垒，不记 BK。

export const BALK_LOG_KIND = "balk";
export const ILLEGAL_PITCH_LOG_KIND = "illegal_pitch_ball";

/**
 * 按赛事制和垒上状态决定本次「投手犯规 / 不合法投球」的记账动作。
 * @param {{sport?: string, runnersOnBase?: boolean}} params
 * @returns {{advanceRunners: boolean, countPitcherBalk: boolean, logKind: string}}
 */
export function resolveBalkAction({ sport = "BASEBALL", runnersOnBase = false } = {}) {
  const runnersAdvance = !!runnersOnBase;
  const isBalk = runnersAdvance && String(sport ?? "").toUpperCase() !== "SOFTBALL";
  return {
    advanceRunners: runnersAdvance,
    countPitcherBalk: isBalk,
    logKind: isBalk ? BALK_LOG_KIND : ILLEGAL_PITCH_LOG_KIND,
  };
}
