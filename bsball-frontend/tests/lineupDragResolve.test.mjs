// 落点解析单测（纯函数，不触碰 DOM/interactjs）
// 运行：node --test tests/lineupDragResolve.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolveDropTarget } from '../src/composables/useLineupDrag.js';

const rect = (left, top, width = 60, height = 40) => ({ left, top, width, height });
const layout = {
  slotRects: [{ code: 'P', ...rect(100, 100) }, { code: 'C', ...rect(100, 200) }],
  battingRect: rect(0, 300, 300, 200),
  benchRect: rect(320, 300, 200, 200),
  poolRect: rect(540, 0, 200, 600),
};

test('命中卡槽优先于区域', () => {
  assert.deepEqual(resolveDropTarget(120, 110, layout), { kind: 'slot', code: 'P' });
  assert.deepEqual(resolveDropTarget(120, 210, layout), { kind: 'slot', code: 'C' });
});

test('落到打线列表 / 替补区 / 池', () => {
  assert.deepEqual(resolveDropTarget(150, 400, layout), { kind: 'batting' });
  assert.deepEqual(resolveDropTarget(400, 400, layout), { kind: 'bench' });
  assert.deepEqual(resolveDropTarget(600, 100, layout), { kind: 'pool' });
});

test('都不命中返回 null', () => {
  assert.equal(resolveDropTarget(700, 700, layout), null);
});

test('容差命中卡槽（触屏放大 1.2 倍）', () => {
  // P 矩形 y∈[100,140]、中心 y=120、半高 20 → 容差上界 y = 120 + 20×1.2 = 144
  // 探针取 144：已出精确区、恰在容差上界内 —— 真正走 hitTestSlot 的容差分支
  assert.deepEqual(resolveDropTarget(130, 144, layout), { kind: 'slot', code: 'P' });
});

test('layout 缺省字段安全', () => {
  assert.equal(resolveDropTarget(10, 10, {}), null);
  assert.deepEqual(resolveDropTarget(10, 10, { battingRect: rect(0, 0, 50, 50) }), { kind: 'batting' });
});
