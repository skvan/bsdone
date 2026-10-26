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

test('同一play内推进不再补记 E（承接失误/暴投/捕逸）', () => {
  assert.equal(shouldCreditError('safe_same_err'), false);
  assert.equal(shouldCreditError('safe_same_err', true), false);
});

test('未记过失误/暴投/捕逸时，“同一play内失误推进”不出现在进垒原因里', () => {
  const reasons = [
    { id: 'safe_sb' },
    { id: 'safe_same_err' },
    { id: 'safe_te' }
  ];
  assert.deepEqual(filterErrorReasons(reasons, false).map((r) => r.id), ['safe_sb', 'safe_te']);
  assert.deepEqual(filterErrorReasons(reasons, true), reasons);
  assert.deepEqual(filterErrorReasons(reasons, false, true), reasons);
  assert.deepEqual(filterErrorReasons(reasons, false, false, true), reasons);
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

  assert.match(src, /shouldCreditError\(t\.advanceFeReasonId\)/, 'ADVANCE_FE 未接判定函数');
  assert.ok(src.includes('filterErrorReasons(s,l.playErrorCredited,l.playWpCredited,l.playPbCredited)'), 'CI 进垒原因未按 WP/PB 过滤');
  assert.ok(src.includes('filterErrorReasons(Wd('), '常规进垒原因未过滤');
  assert.ok(src.includes('l.playWpCredited,l.playPbCredited)'), '常规进垒原因未接 WP/PB 标记');
  assert.ok(src.includes('e.id==="safe_wp"&&!l.playWpCredited'), '暴投重复计数未防');
  assert.ok(src.includes('e.id==="safe_pb"&&!l.playPbCredited'), '捕逸重复计数未防');
  assert.ok(src.includes('l.playWpCredited=!0,l.flow=null'), '暴投投球未置位本 play 标记');
  assert.ok(src.includes('l.playPbCredited=!0'), '捕逸未置位本 play 标记');
  assert.ok(src.includes('const samePlayWpPb=e.id==="safe_same_err"'), '同一 play 承接 WP/PB 未走普通进垒');
  assert.ok(src.includes('advanceReasonId=samePlayWpPb'), '承接 WP/PB 未沿用 WP/PB 来源');
  assert.ok(src.includes('同一play内暴投推进') && src.includes('同一play内捕逸推进'), '承接 WP/PB 日志缺少来源');
  assert.ok(src.includes('zd.has(e.id)&&!samePlayWpPb'), '承接 WP/PB 仍进入失误守备员流程');
  assert.ok(src.includes('if(e==="WP"&&l.playWpCredited)'), '暴投事件菜单仍可重复计数');

  // 所有记 E 的位置统一走 creditFieldingError，由该函数置位本 play 标记
  assert.match(body('function creditFieldingError(player){', 'function Bt('), /playErrorCredited=!0/, '记 E 助手未置位标记');
  for (const call of ['creditFieldingError(r)', 'creditFieldingError(Vt(w))', 'creditFieldingError(Vt(t))', 'creditFieldingError(Vt(b))', 'creditFieldingError(Vt(fc_b))']) {
    assert.ok(src.includes(call), `记 E 位置未使用助手：${call}`);
  }

  // 投球/换局重置；换打席不得重置（BIP 失误后跑者仍在同一 play 内进垒）
  assert.match(body('function Ea(e=1){', 'function $q(){'), /playErrorCredited=!1,l\.playWpCredited=!1,l\.playPbCredited=!1/, '新投球未重置');
  assert.match(body('function Af(){', 'function Of(){'), /playErrorCredited=!1,l\.playWpCredited=!1,l\.playPbCredited=!1/, '换局未重置');
  assert.match(src, /O\.playErrorCredited=!1,O\.playWpCredited=!1,O\.playPbCredited=!1/, '新比赛/清空未重置');
  assert.doesNotMatch(body('function fo(){', 'function Af(){'), /playErrorCredited|playWpCredited|playPbCredited/, '换打席不应重置本 play 标记');

  // 快照恢复
  assert.match(src, /O\.playErrorCredited=pe\.playErrorCredited\?\?!1/, '历史快照恢复未还原标记');
  assert.match(src, /O\.playWpCredited=pe\.playWpCredited\?\?!1/, '历史快照恢复未还原暴投标记');
  assert.match(src, /O\.playPbCredited=pe\.playPbCredited\?\?!1/, '历史快照恢复未还原捕逸标记');
  assert.match(src, /playErrorCredited:!1,playWpCredited:!1,playPbCredited:!1/, '初始状态缺少标记');
});
