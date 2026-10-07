// LiveGame.js 得分挂接/悬挂列表消费——静态回归断言（#204）
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

test('Qr()：playEvent 组装后立即清空悬挂得分列表（防决策重复）', () => {
  const body = pick('function Qr(){', 'function syncEarnedRunHalfInning(');
  assert.match(body, /f\.playEvents\.push\(E\),Jl\(\),syncEarnedRunHalfInning\(E\)/,
    'push(E) 之后必须紧跟 Jl() 再同步重建');
  assert.doesNotMatch(body, /f\.playEvents\.push\(E\),syncEarnedRunHalfInning\(E\)/,
    '旧的「推入后不清空」形态不得残留');
});

test('wp() 回本垒分支：「上一棒内推进」族挂接 ER 决策（origin=NORMAL）', () => {
  const body = pick('function wp(e,t,a,s,r,rbiNote){', 'function fr(e){');
  assert.match(body, /Nd\(r\)\?ls\(1,\{runner:e,pitcher:p,erDelta:1,origin:"NORMAL"\}\):Ct\(1\)/,
    '回本垒分支须以 Nd(r) 判定「上一棒内推进」族并 ls 挂接（#250 同步：非该族走 Ct(1) 二选一）');
  assert.match(body, /addLivePitcherEarnedRuns\(p\)/, '#250 后投手失分统计仍无条件下沉');
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
  assert.match(src, /ls\(1,\{runner:s,pitcher:w\?\?void 0,erDelta:1,origin:"ERROR"\}\)/,
    'lc 的失误得分挂接仍在');
  assert.match(src, /function wf\(e\)\{/, 'wf 存在');
  assert.match(src, /function creditLastBatterRbi\(\)\{/, '#206 记账函数存在');
});

test('自检：悬挂列表仅在允许的消费点清空（Qr/xf/结果处理/恢复/重置）', () => {
  const count = (src.match(/Jl\(\)/g) ?? []).length;
  assert.ok(count >= 5, 'Jl() 调用点数量异常：' + count);
  assert.ok(!/function Qr\(\)\{[\s\S]*?\}[\s\S]{0,400}?Jl\(\)[\s\S]{0,600}?function Qr/.test(src), '不应存在重复定义');
});
