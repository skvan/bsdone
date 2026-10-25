// LiveGame.js 得分挂接/台账消费——静态回归断言（#204）
// 运行：node --test tests/liveGamePitchAttach.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const pick = (startMark, endMark) => {
  const i = src.indexOf(startMark);
  assert.ok(i >= 0, startMark + ' 存在');
  const j = src.indexOf(endMark, i);
  assert.ok(j > i, endMark + ' 在 ' + startMark + ' 之后');
  return src.slice(i, j);
};

test('Qr()：playEvent 组装前消费台账并在推入后同步重建（防决策重复）', () => {
  const body = pick('function Qr(){', 'function syncEarnedRunHalfInning(');
  assert.match(body, /const scoringEntries=takeAllRuns\(runLedger\)/,
    'Qr 必须先一次性取走台账得分');
  assert.match(body, /f\.playEvents\.push\(E\),syncEarnedRunHalfInning\(E\)/,
    'push(E) 后紧跟同步重建');
  assert.doesNotMatch(body, /Jl\(\)/, '旧的整表清空入口不得残留');
});

test('wp() 回本垒分支统一经 ls 记账并按推进原因判定来源', () => {
  const body = pick('function wp(e,t,a,s,r,rbiNote){', 'function fr(e){');
  assert.match(body, /,g=advanceOrigin\(r\)/, '推进原因统一换算为得分来源');
  assert.match(body, /a===4\?\(ls\(1,\{runner:e,pitcher:p,origin:g\}\)/,
    '回本垒分支须走 ls 并带上归一后的来源');
  assert.doesNotMatch(body, /Ct\(1\)/, '禁止旁路加分');
  assert.doesNotMatch(body, /addLivePitcherEarnedRuns/, '投手失分随台账统一记账');
  assert.match(body, /const p=Se\.value/, '当前投手来源仍为 Se.value');
});

test('Nd() 覆盖三项「上一棒内推进」变体', () => {
  const i = src.indexOf('function Nd(u){');
  assert.ok(i >= 0, 'Nd 存在');
  const body = src.slice(i, src.indexOf('}', i) + 1);
  for (const id of ['"safe_last_play"', '"safe_last_play_sac"', '"safe_last_play_rbi"']) {
    assert.ok(body.includes(id), 'Nd 应包含 ' + id);
  }
});

test('既有挂接不被破坏：lc 失误得分仍带 origin=ERROR；wf/creditLastBatterRbi 原样', () => {
  assert.match(src, /ls\(1,\{runner:s,pitcher:w\?\?void 0,origin:"ERROR",runCause:"ERROR"\}\)/,
    'lc 的失误得分挂接仍在');
  assert.match(src, /function wf\(e\)\{/, 'wf 存在');
  assert.match(src, /function creditLastBatterRbi\(\)\{/, '#206 记账函数存在');
});

test('自检：台账仅在允许的消费点撤销或清空（xf/恢复/重置）', () => {
  assert.doesNotMatch(src, /Jl\(\)/, '旧的整表清空入口不得残留');
  assert.match(src, /voidCurrentPlayRuns\(runLedger\)/, '第三出局撤销走当前动作边界');
  assert.match(src, /clearRuns\(runLedger\)/, '恢复/重置路径清空台账');
  const takeCount = (src.match(/takeAllRuns\(runLedger\)/g) ?? []).length;
  assert.ok(takeCount >= 1, '打席落定必须消费台账：' + takeCount);
});
