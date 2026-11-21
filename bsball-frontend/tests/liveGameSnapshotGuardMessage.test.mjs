// #713：后端稳定 reason → 前端本地化文案 key 的映射
// 运行：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"
import test from 'node:test';
import assert from 'node:assert/strict';
import { snapshotGuardReasonKey } from '../src/domain/earnedRun/snapshotGuardMessage.js';

test('snapshotGuardReasonKey：已知 reason 映射到 liveGame 文案 key', () => {
  assert.equal(snapshotGuardReasonKey('snapshot_run_total_regress'),
    'liveGame.snapshotKeys.reason.runTotalRegress');
  assert.equal(snapshotGuardReasonKey('snapshot_event_count_shrink_without_intent'),
    'liveGame.snapshotKeys.reason.shrinkWithoutIntent');
  assert.equal(snapshotGuardReasonKey('reconstruct_guard_partial_denominator'),
    'liveGame.snapshotKeys.reason.partialDenominator');
});

test('snapshotGuardReasonKey：未知/空 reason 返回 null（调用方回退服务器 msg）', () => {
  assert.equal(snapshotGuardReasonKey('some_future_reason'), null);
  assert.equal(snapshotGuardReasonKey(''), null);
  assert.equal(snapshotGuardReasonKey(null), null);
  assert.equal(snapshotGuardReasonKey(undefined), null);
});
