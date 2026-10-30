// #530 双杀出局：拖拽持球守备员到垒包逐个记录出局，传球链可继续延长。
// 运行：node --test tests/liveGameDoublePlay.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { resolveDoublePlay, canStartDoublePlay, canStartTriplePlay } from '../src/utils/liveGameDoublePlay.js';

const player = (id, name) => ({ id, name, number: String(id) });
const r1 = player(45, '一垒跑者');
const r2 = player(26, '二垒跑者');
const r3 = player(88, '三垒跑者');
const batter = player(32, '打者');
const runners = (a = null, b = null, c = null) => ({ 1: a, 2: b, 3: c });
const drag = (action, base) => ({ action, base });
const outRows = (result) => result.outs.map((out) => ({
  fielder: out.fielder,
  base: out.base,
  runner: out.runner?.id ?? null,
  action: out.action
}));

test('双杀入口要求一垒有人；仅二垒/三垒有人不算双杀', () => {
  assert.equal(canStartDoublePlay(runners(null, r2)), false);
  assert.equal(canStartDoublePlay(runners(null, null, r3)), false);
  assert.equal(canStartDoublePlay(runners(r1)), true);
  assert.equal(canStartDoublePlay(runners(r1, r2)), true);
  assert.equal(canStartDoublePlay({}), false);
  assert.equal(canStartTriplePlay(runners(r1, r2)), true);
  assert.equal(canStartTriplePlay(runners(r1, null, r3)), false);
  assert.equal(canStartTriplePlay(runners(null, r2, r3)), false);
});

test('6-5-4：6 接球点不算出局，拖 5 到三垒、4 到二垒，打者上一垒', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '4', '4'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 2)],
    runners: runners(r1, r2),
    batter
  });
  assert.equal(result.valid, true);
  assert.equal(result.lastOutStepIndex, 4);
  assert.deepEqual(outRows(result), [
    { fielder: '5', base: 3, runner: 26, action: 'force' },
    { fielder: '4', base: 2, runner: 45, action: 'force' }
  ]);
  assert.equal(result.batterOut, false);
  assert.equal(result.batterSafe, true);
  assert.equal(result.gdp, false);
  assert.deepEqual(result.finalBases, { 1: batter, 2: null, 3: null });
  assert.deepEqual(result.actions.map((action) => [action.pos, action.out, action.base]), [
    ['6', false, null],
    ['5', true, 3],
    ['4', true, 2]
  ]);
});

test('6-4-3：拖 4 到二垒、3 到一垒，打者出局且记 GDP', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '4', '4', '3', '3'],
    stepActions: [null, null, drag('force', 2), null, drag('force', 1)],
    runners: runners(r1, r2),
    batter
  });
  assert.deepEqual(outRows(result), [
    { fielder: '4', base: 2, runner: 45, action: 'force' },
    { fielder: '3', base: 1, runner: 32, action: 'force' }
  ]);
  assert.equal(result.batterOut, true);
  assert.equal(result.gdp, true);
  assert.deepEqual(result.finalBases, { 1: null, 2: null, 3: r2 });
});

test('6-5-3：拖 5 到三垒、3 到一垒，一垒跑者进二垒', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '3', '3'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 1)],
    runners: runners(r1, r2),
    batter
  });
  assert.deepEqual(outRows(result), [
    { fielder: '5', base: 3, runner: 26, action: 'force' },
    { fielder: '3', base: 1, runner: 32, action: 'force' }
  ]);
  assert.equal(result.gdp, true);
  assert.deepEqual(result.finalBases, { 1: null, 2: r1, 3: null });
});

test('满垒 6-2-3：拖 2 到本垒、3 到一垒，其余跑者各进一垒', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '2', '2', '3', '3'],
    stepActions: [null, null, drag('force', 4), null, drag('force', 1)],
    runners: runners(r1, r2, r3),
    batter
  });
  assert.deepEqual(outRows(result), [
    { fielder: '2', base: 4, runner: 88, action: 'force' },
    { fielder: '3', base: 1, runner: 32, action: 'force' }
  ]);
  assert.equal(result.gdp, true);
  assert.deepEqual(result.finalBases, { 1: null, 2: r1, 3: r2 });
});

test('满垒 6-5-4：拖 5 到三垒、4 到二垒，打者上一垒，三垒跑者留三垒', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '4', '4'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 2)],
    runners: runners(r1, r2, r3),
    batter
  });
  assert.equal(result.batterSafe, true);
  assert.equal(result.gdp, false);
  assert.deepEqual(result.finalBases, { 1: batter, 2: null, 3: r3 });
});

test('6-3：先拖 6 到二垒，再拖 3 到一垒', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '6', '3', '3'],
    stepActions: [null, drag('force', 2), null, drag('force', 1)],
    runners: runners(r1),
    batter
  });
  assert.deepEqual(outRows(result), [
    { fielder: '6', base: 2, runner: 45, action: 'force' },
    { fielder: '3', base: 1, runner: 32, action: 'force' }
  ]);
  assert.deepEqual(result.finalBases, { 1: null, 2: null, 3: null });
});

test('4-6-3：4 只是助杀，拖 6 到二垒、3 到一垒', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['4', '6', '6', '3', '3'],
    stepActions: [null, null, drag('force', 2), null, drag('force', 1)],
    runners: runners(r1),
    batter
  });
  assert.deepEqual(outRows(result), [
    { fielder: '6', base: 2, runner: 45, action: 'force' },
    { fielder: '3', base: 1, runner: 32, action: 'force' }
  ]);
  assert.equal(result.actions[0].out, false);
});

test('3-3 独力双杀：同一守备员两次拖到一垒，两个出局', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['3', '3', '3'],
    stepActions: [null, drag('force', 1), drag('tag', 1)],
    runners: runners(r1),
    batter
  });
  assert.equal(result.valid, true);
  assert.deepEqual(result.outs.map((out) => [out.base, out.runner.id, out.action]), [
    [1, batter.id, 'force'],
    [1, r1.id, 'tag']
  ]);
  assert.equal(result.gdp, true);
  assert.deepEqual(result.finalBases, { 1: null, 2: null, 3: null });
});

test('只拖第一个出局时提示还需一个，不提前结算', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '4'],
    stepActions: [null, null, drag('force', 3), null],
    runners: runners(r1, r2),
    batter
  });
  assert.equal(result.valid, false);
  assert.equal(result.reason, 'outs_incomplete');
  assert.equal(result.needsOuts, 1);
  assert.equal(result.outs.length, 1);
  assert.equal(result.lastOutStepIndex, 2);
});

test('一个出局都没拖时不猜出局，提示记录两个出局', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '4'],
    runners: runners(r1, r2),
    batter
  });
  assert.equal(result.valid, false);
  assert.equal(result.reason, 'outs_incomplete');
  assert.equal(result.needsOuts, 2);
  assert.equal(result.outs.length, 0);
});

test('拖到的垒包没有被迫跑者时给出未决项', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '3', '3'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 1)],
    runners: runners(r1),
    batter
  });
  assert.equal(result.valid, false);
  assert.equal(result.reason, 'unresolved');
  assert.equal(result.unresolved.length, 1);
  assert.equal(result.unresolved[0].base, 3);
});

test('满垒 6-4-3 的三垒跑者需要确认得分/留垒', () => {
  const base = {
    type: 'DP',
    steps: ['6', '4', '4', '3', '3'],
    stepActions: [null, null, drag('force', 2), null, drag('force', 1)],
    runners: runners(r1, r2, r3),
    batter
  };
  const pending = resolveDoublePlay(base);
  assert.equal(pending.needsRunner3Decision, true);
  assert.equal(pending.runner3Run, false);

  const safe = resolveDoublePlay({ ...base, runner3Decision: 'safe' });
  assert.equal(safe.needsRunner3Decision, false);
  assert.equal(safe.runner3Run, true);
  assert.deepEqual(safe.finalBases, { 1: null, 2: null, 3: r2 });

  const noscore = resolveDoublePlay({ ...base, runner3Decision: 'noscore' });
  assert.equal(noscore.runner3Run, false);
  assert.deepEqual(noscore.finalBases, { 1: null, 2: r2, 3: r3 });
});

test('第二个出局可改为打者在一垒出局', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '4', '4'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 2)],
    runners: runners(r1, r2),
    batter,
    overrides: [null, { base: 1, runnerKey: 'batter', action: 'force' }]
  });
  assert.deepEqual(outRows(result), [
    { fielder: '5', base: 3, runner: 26, action: 'force' },
    { fielder: '4', base: 1, runner: 32, action: 'force' }
  ]);
  assert.equal(result.batterOut, true);
  assert.deepEqual(result.finalBases, { 1: null, 2: r1, 3: null });
});

test('拖了第三个出局点时提示撤销多余拖拽', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '4', '4', '3', '3'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 2), null, drag('force', 1)],
    runners: runners(r1, r2),
    batter
  });
  assert.equal(result.valid, false);
  assert.equal(result.reason, 'too_many_outs');
  assert.equal(result.outs.length, 2);
  assert.equal(result.lastOutStepIndex, 4);
});

test('第二个出局后仍可延长传球链，统计只取到最后一个出局点', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '4', '4', '3'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 2), null],
    runners: runners(r1, r2),
    batter
  });
  assert.equal(result.valid, true);
  assert.equal(result.lastOutStepIndex, 4);
  assert.deepEqual(result.outs.map((out) => out.fielder), ['5', '4']);
  assert.deepEqual(result.finalBases, { 1: batter, 2: null, 3: null });
  assert.deepEqual(result.actions.map((action) => [action.pos, action.out]), [
    ['6', false], ['5', true], ['4', true], ['3', false]
  ]);
});

test('缺少打者时不结算', () => {
  const result = resolveDoublePlay({
    type: 'DP',
    steps: ['6', '5', '5', '4', '4'],
    stepActions: [null, null, drag('force', 3), null, drag('force', 2)],
    runners: runners(r1, r2),
    batter: null
  });
  assert.equal(result.valid, false);
  assert.equal(result.reason, 'missing_batter');
});

test('LiveGame 接线：入口门槛、拖拽记录、完成记录与落点入队', () => {
  const src = fs
    .readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8')
    .replace(/\r\n/g, '\n');
  for (const needle of [
    'function buildDoublePlayPreview(flow, runner3Decision = null) {',
    'function settleDoublePlayFlow(flow){',
    'function applyDoublePlay(flow,preview){',
    'function editDoublePlayOuts(){',
    'pushHitSpray(type,0,Sr(flow))',
    'completeChainFlow',
    '记录第${dpRecorded+1}个出局',
    'LgCanStartDoublePlay(l.runners)',
    'LgCanStartTriplePlay(l.runners)',
    'preview.lastOutStepIndex',
    'dpOverrides',
    'applyChainFieldingStats(type,statsChain)'
  ]) {
    assert.ok(src.includes(needle), `缺少接线：${needle}`);
  }
  assert.match(src, /function Yt\(e,t\)\{\n\s+if\(t==="DP"&&!LgCanStartDoublePlay\(l\.runners\)\)/, '双杀入口未按一垒有人拦截');
  assert.match(src, /onClick:completeChainFlow/, '完成记录未改走显式完成');
  assert.match(
    src,
    /function Wf\(e\)\{[\s\S]*?if\(!settleChainIfReady\(\)\)\{[\s\S]*?warnFlowBusy\(a\);[\s\S]*?return[\s\S]*?\}[\s\S]*?nextPitchCleanup\(\)[\s\S]*?if\(l\.flow\)\{[\s\S]*?Ro\("P",e\)/,
    'Wf 在双杀链未完成时仍把 P 追加进链'
  );
  assert.match(
    src,
    /case"bip:fly:dp":case"bip:ld:dp":case"bip:pop:dp":J\(/,
    '飞球/平飞/小飞不应进入被迫进垒双杀流程'
  );
});
