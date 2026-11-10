// 投手犯规 / 不合法投球纯函数规则（Issue #651 / #653）
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolveBalkAction, BALK_LOG_KIND, ILLEGAL_PITCH_LOG_KIND } from '../src/utils/liveGameBalk.js';

test('棒球垒上有人：Balk，推进跑者、不记坏球、记投手 BK，日志为投手犯规', () => {
  const action = resolveBalkAction({ sport: 'BASEBALL', runnersOnBase: true });
  assert.deepEqual(action, {
    advanceRunners: true,
    countPitcherBalk: true,
    logKind: BALK_LOG_KIND,
  });
});

test('棒球垒上无人：Illegal Pitch，记一坏球、不推进、不记 BK', () => {
  const action = resolveBalkAction({ sport: 'BASEBALL', runnersOnBase: false });
  assert.deepEqual(action, {
    advanceRunners: false,
    countPitcherBalk: false,
    logKind: ILLEGAL_PITCH_LOG_KIND,
  });
});

test('缺省按棒球处理，垒上无人时避免误记 BK 或误推进跑者', () => {
  assert.deepEqual(resolveBalkAction(), {
    advanceRunners: false,
    countPitcherBalk: false,
    logKind: ILLEGAL_PITCH_LOG_KIND,
  });
});

test('垒球垒上有人：Illegal Pitch，记一坏球并推进所有跑者，不记 BK', () => {
  const action = resolveBalkAction({ sport: 'SOFTBALL', runnersOnBase: true });
  assert.deepEqual(action, {
    advanceRunners: true,
    countPitcherBalk: false,
    logKind: ILLEGAL_PITCH_LOG_KIND,
  });
});

test('垒球垒上无人：Illegal Pitch，记一坏球、不推进、不记 BK', () => {
  const action = resolveBalkAction({ sport: 'SOFTBALL', runnersOnBase: false });
  assert.deepEqual(action, {
    advanceRunners: false,
    countPitcherBalk: false,
    logKind: ILLEGAL_PITCH_LOG_KIND,
  });
});

test('sport 大小写不敏感', () => {
  assert.equal(resolveBalkAction({ sport: 'softball', runnersOnBase: true }).countPitcherBalk, false);
  assert.equal(resolveBalkAction({ sport: 'Softball', runnersOnBase: true }).logKind, ILLEGAL_PITCH_LOG_KIND);
});
