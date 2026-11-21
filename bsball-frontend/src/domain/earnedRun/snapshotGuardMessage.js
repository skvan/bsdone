// 后端稳定原因码 → 本地化文案 key（#713）。文案可以改，reason 不变；
// 不认识的原因码由调用方回退服务器 msg。
const SNAPSHOT_GUARD_REASON_KEYS = {
  snapshot_run_total_regress: 'liveGame.snapshotKeys.reason.runTotalRegress',
  snapshot_event_count_shrink_without_intent: 'liveGame.snapshotKeys.reason.shrinkWithoutIntent',
  reconstruct_guard_partial_denominator: 'liveGame.snapshotKeys.reason.partialDenominator'
};

export function snapshotGuardReasonKey(reason) {
  return reason ? SNAPSHOT_GUARD_REASON_KEYS[reason] ?? null : null;
}
