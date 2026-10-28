// 替补席拖拽换人·纯函数层单测（列表构建 / 空席文案 / 落点解析 / 合法性矩阵 / 可落目标）
// 运行：node --test tests/liveSubDrag.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildLiveBenchItems,
  liveBenchEmptyText,
  resolveLiveSubDropHits,
  liveSubDropAction,
  liveSubTargetKinds,
} from '../src/utils/liveSubDrag.js';

const rect = (left, top, width = 40, height = 40) => ({ left, top, width, height });

// 模拟场地：P 左上、C 与打者本垒圈重叠（真实布局中捕手标签贴近本垒）
const layout = {
  fielderRects: [
    { code: 'P', rect: rect(240, 300) },
    { code: 'C', rect: rect(220, 535) },
  ],
  runnerRects: [
    { base: 1, rect: rect(320, 420) },
    { base: 2, rect: rect(240, 200) },
  ],
  batterRect: rect(220, 535),
  benchRects: [{ side: 'defense', playerId: 199, rect: rect(20, 300) }],
};

/* ---------------- buildLiveBenchItems ---------------- */

test('棒球：过滤已换下不可再上场的球员', () => {
  const bench = [
    { id: 1, name: '甲', number: 5 },
    { id: 2, name: '乙', number: 7 },
  ];
  assert.deepEqual(buildLiveBenchItems(bench, [2], 'BASEBALL'), [{ id: 1, name: '甲', number: 5 }]);
});

test('棒球：unavailable 与 bench 的 id 类型不一致（字符串/数字）仍能过滤', () => {
  const bench = [{ id: 2, name: '乙', number: 7 }];
  assert.deepEqual(buildLiveBenchItems(bench, ['2'], 'BASEBALL'), []);
});

test('垒球：不过滤已换下球员（可再上场）', () => {
  const bench = [{ id: 1, name: '甲', number: 5 }];
  assert.deepEqual(buildLiveBenchItems(bench, [1], 'SOFTBALL'), [{ id: 1, name: '甲', number: 5 }]);
});

test('空/缺省输入安全', () => {
  assert.deepEqual(buildLiveBenchItems(null, [1], 'BASEBALL'), []);
  assert.deepEqual(buildLiveBenchItems([], null, 'BASEBALL'), []);
});

test('替补席按背号升序，空背号排最后', () => {
  const bench = [
    { id: 1, name: '甲', number: '10' },
    { id: 2, name: '乙', number: '2' },
    { id: 3, name: '丙', number: '' }
  ];
  assert.deepEqual(buildLiveBenchItems(bench, [], 'BASEBALL').map((p) => p.id), [2, 1, 3]);
  assert.deepEqual(buildLiveBenchItems(bench, [], 'SOFTBALL').map((p) => p.id), [2, 1, 3]);
});

/* ---------------- liveBenchEmptyText ---------------- */

test('空席文案：棒球注明已换下不可再上场，垒球不带该说明', () => {
  assert.equal(liveBenchEmptyText('BASEBALL'), '板凳席无可用球员（已换下球员不可再上场）');
  assert.equal(liveBenchEmptyText('SOFTBALL'), '板凳席无可用球员');
});

/* ---------------- resolveLiveSubDropHits ---------------- */

test('命中守备员 / 垒位 / 打者', () => {
  assert.deepEqual(resolveLiveSubDropHits(250, 310, layout), [{ kind: 'fielder', code: 'P' }]);
  assert.deepEqual(resolveLiveSubDropHits(330, 430, layout), [{ kind: 'base', base: 1 }]);
  assert.deepEqual(resolveLiveSubDropHits(250, 210, layout), [{ kind: 'base', base: 2 }]);
  assert.deepEqual(resolveLiveSubDropHits(240, 555, layout), [
    { kind: 'fielder', code: 'C' },
    { kind: 'batter' },
  ]);
});

test('打者与捕手标签重叠：返回有序多命中（守备员在前）', () => {
  // (240,555) 同时落在 C 的矩形与打者矩形内 —— 调用方按来源过滤取第一个可落项
  const hits = resolveLiveSubDropHits(240, 555, layout);
  assert.deepEqual(hits, [{ kind: 'fielder', code: 'C' }, { kind: 'batter' }]);
});

test('命中替补席卡片：kind=bench + side/playerId', () => {
  assert.deepEqual(resolveLiveSubDropHits(40, 310, layout), [{ kind: 'bench', side: 'defense', playerId: 199 }]);
});

test('未落入任何目标 = 空数组', () => {
  assert.deepEqual(resolveLiveSubDropHits(10, 10, layout), []);
  assert.deepEqual(resolveLiveSubDropHits(10, 10, {}), []);
  assert.deepEqual(resolveLiveSubDropHits(10, 10, null), []);
});

test('容差命中（触屏放大）：恰在精确区外、容差界内仍命中', () => {
  // C 矩形 x∈[220,260]，中心 x=240、半宽 20 → 容差 1.2 → 上界 264
  assert.deepEqual(resolveLiveSubDropHits(262, 555, layout), [
    { kind: 'fielder', code: 'C' },
    { kind: 'batter' },
  ]);
  assert.deepEqual(resolveLiveSubDropHits(266, 555, layout), []); // 超出容差 = 无命中
});

/* ---------------- liveSubDropAction ---------------- */

test('合法性矩阵：进攻替补 → 打者/垒位（代打/代跑/设置跑者）', () => {
  assert.equal(liveSubDropAction('offenseBench', { kind: 'batter' }), 'pinchHit');
  assert.equal(liveSubDropAction('offenseBench', { kind: 'base', base: 1 }, { baseOccupied: true }), 'pinchRun');
  assert.equal(liveSubDropAction('offenseBench', { kind: 'base', base: 1 }, { baseOccupied: false }), 'setRunner');
});

test('合法性矩阵：守备替补 → 守备员（换守备/换投）', () => {
  assert.equal(liveSubDropAction('defenseBench', { kind: 'fielder', code: 'P' }), 'fieldReplace');
  assert.equal(liveSubDropAction('defenseBench', { kind: 'fielder', code: 'SS' }), 'fieldReplace');
});

test('合法性矩阵：守备员 → 守备员（互换守备位；同一人无效）/ 守备员 → 替补席（下场）', () => {
  assert.equal(liveSubDropAction('fielder', { kind: 'fielder', code: 'SS' }), 'fieldSwap');
  assert.equal(liveSubDropAction('fielder', { kind: 'fielder', code: 'SS' }, { samePlayer: true }), null);
  assert.equal(liveSubDropAction('fielder', { kind: 'bench', side: 'defense', playerId: 199 }), 'benchSwap');
});

test('非法组合一律无效：进攻↔守备员、守备↔垒位/打者、守备员↔打者', () => {
  assert.equal(liveSubDropAction('offenseBench', { kind: 'fielder', code: 'P' }), null);
  assert.equal(liveSubDropAction('defenseBench', { kind: 'base', base: 1 }, { baseOccupied: true }), null);
  assert.equal(liveSubDropAction('defenseBench', { kind: 'batter' }), null);
  assert.equal(liveSubDropAction('fielder', { kind: 'batter' }), null);
  assert.equal(liveSubDropAction('defenseBench', { kind: 'bench', side: 'defense', playerId: 199 }), null);
  assert.equal(liveSubDropAction('fielder', { kind: 'base', base: 1 }), null);
  assert.equal(liveSubDropAction('unknown', { kind: 'fielder', code: 'P' }), null);
});

/* ---------------- liveSubTargetKinds ---------------- */

test('可落目标：进攻席=垒位+打者；守备席=守备员；守备员=守备员+替补席', () => {
  assert.deepEqual(liveSubTargetKinds('offenseBench'), ['base', 'batter']);
  assert.deepEqual(liveSubTargetKinds('defenseBench'), ['fielder']);
  assert.deepEqual(liveSubTargetKinds('fielder'), ['fielder', 'bench']);
  assert.deepEqual(liveSubTargetKinds('unknown'), []);
});
