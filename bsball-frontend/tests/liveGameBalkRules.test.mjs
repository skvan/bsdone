// 投手犯规 / 不合法投球纯函数规则（Issue #651）
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolveBalkAction, BALK_LOG_KIND, ILLEGAL_PITCH_LOG_KIND } from '../src/utils/liveGameBalk.js';

test('垒上有人：Balk，推进跑者、记投手 BK，日志为投手犯规', () => {
  const action = resolveBalkAction({ runnersOnBase: true });
  assert.deepEqual(action, { advanceRunners: true, logKind: BALK_LOG_KIND });
});

test('垒上无人：Illegal Pitch，不推进跑者、不记 BK，日志为不合法投球', () => {
  const action = resolveBalkAction({ runnersOnBase: false });
  assert.deepEqual(action, { advanceRunners: false, logKind: ILLEGAL_PITCH_LOG_KIND });
});

test('缺省按垒上无人处理，避免误记 BK 或误推进跑者', () => {
  assert.deepEqual(resolveBalkAction(), { advanceRunners: false, logKind: ILLEGAL_PITCH_LOG_KIND });
});
