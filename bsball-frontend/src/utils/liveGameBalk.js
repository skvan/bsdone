// 投手犯规（Balk）与不合法投球（Illegal Pitch）分流规则（2026-11-10 口径）
// - 垒上有人：Balk，所有跑者各进一垒；打者不记坏球；投手记 BK。
// - 垒上无人：Illegal Pitch，打者记一坏球；不记 BK；满四坏按保送收尾（由坏球计数流程处理）。

export const BALK_LOG_KIND = "balk";
export const ILLEGAL_PITCH_LOG_KIND = "illegal_pitch_ball";

/**
 * 按垒上是否有人决定本次「投手犯规 / 不合法投球」的记账动作。
 * @param {{runnersOnBase?: boolean}} params
 * @returns {{advanceRunners: boolean, logKind: string}} advanceRunners 为 true 时即按 Balk 记账（推进跑者并给投手记 BK）
 */
export function resolveBalkAction({ runnersOnBase = false } = {}) {
  if (runnersOnBase) {
    return { advanceRunners: true, logKind: BALK_LOG_KIND };
  }
  return { advanceRunners: false, logKind: ILLEGAL_PITCH_LOG_KIND };
}
