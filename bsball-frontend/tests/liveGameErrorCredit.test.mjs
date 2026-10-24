// 失误记账判定：同一 play 内“同一失误推进”不重复计 E（比赛 236 捕手失误漏记）
// 运行：node --test tests/liveGameErrorCredit.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { shouldCreditError, filterErrorReasons } from '../src/utils/liveGameErrorCredit.js';

test('普通失误原因始终记账', () => {
  for (const reason of ['safe_te', 'safe_fe', 'safe_pk_err', 'safe_diff_err', undefined]) {
    assert.equal(shouldCreditError(reason, false), true);
    assert.equal(shouldCreditError(reason, true), true);
  }
});

test('同一play内失误：本 play 未记过失误时补记，已记过时跳过', () => {
  assert.equal(shouldCreditError('safe_same_err', false), true);
  assert.equal(shouldCreditError('safe_same_err', true), false);
});

test('未记过失误时，“同一play内失误推进”不出现在进垒原因里', () => {
  const reasons = [
    { id: 'safe_sb' },
    { id: 'safe_same_err' },
    { id: 'safe_te' }
  ];
  assert.deepEqual(filterErrorReasons(reasons, false).map((r) => r.id), ['safe_sb', 'safe_te']);
  assert.deepEqual(filterErrorReasons(reasons, true), reasons);
  assert.deepEqual(filterErrorReasons(null, false), null);
});

test('LiveGame 接线：使用判定/过滤函数、记 E 置位、投球/换局重置', () => {
  const src = fs
    .readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8')
    .replace(/\r\n/g, '\n');
  const body = (start, end) => {
    const s = src.indexOf(start);
    const e = src.indexOf(end, s);
    assert.ok(s >= 0 && e > s, `缺少代码段 ${start}`);
    return src.slice(s, e);
  };

  assert.match(src, /shouldCreditError\(t\.advanceFeReasonId,l\.playErrorCredited\)/, 'ADVANCE_FE 未接判定函数');
  assert.ok(src.includes('filterErrorReasons(s,l.playErrorCredited)'), 'CI 进垒原因未过滤');
  assert.ok(src.includes('filterErrorReasons(Wd('), '常规进垒原因未过滤');

  // 所有记 E 的位置统一走 creditFieldingError，由该函数置位本 play 标记
  assert.match(body('function creditFieldingError(player){', 'function Bt('), /playErrorCredited=!0/, '记 E 助手未置位标记');
  for (const call of ['creditFieldingError(r)', 'creditFieldingError(Vt(w))', 'creditFieldingError(Vt(t))', 'creditFieldingError(Vt(b))', 'creditFieldingError(Vt(fc_b))']) {
    assert.ok(src.includes(call), `记 E 位置未使用助手：${call}`);
  }

  // 投球/换局重置；换打席不得重置（BIP 失误后跑者仍在同一 play 内进垒）
  assert.match(body('function Ea(e=1){', 'function $q(){'), /playErrorCredited=!1/, '新投球未重置');
  assert.match(body('function Af(){', 'function Of(){'), /playErrorCredited=!1/, '换局未重置');
  assert.match(src, /O\.playErrorCredited=!1/, '新比赛/清空未重置');
  assert.doesNotMatch(body('function fo(){', 'function Af(){'), /playErrorCredited/, '换打席不应重置本 play 标记');

  // 快照恢复
  assert.match(src, /O\.playErrorCredited=pe\.playErrorCredited\?\?!1/, '历史快照恢复未还原标记');
  assert.match(src, /playErrorCredited:!1/, '初始状态缺少标记');
});
