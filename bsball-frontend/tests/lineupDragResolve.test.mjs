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
  // 无 battingRowRects（缺行信息）→ index = null（不参与重排）
  assert.deepEqual(resolveDropTarget(150, 400, layout), { kind: 'batting', index: null });
  assert.deepEqual(resolveDropTarget(400, 400, layout), { kind: 'bench' });
  assert.deepEqual(resolveDropTarget(600, 100, layout), { kind: 'pool' });
});

test('不落入任何可释放区域 = outside（拖离选中区域即移除，无需离开画布）', () => {
  assert.deepEqual(resolveDropTarget(150, 150, layout), { kind: 'outside' }); // 画布内草地
  assert.deepEqual(resolveDropTarget(700, 700, layout), { kind: 'outside' }); // 页面空白
});

test('容差命中卡槽（触屏放大 1.2 倍）', () => {
  // P 矩形 y∈[100,140]、中心 y=120、半高 20 → 容差上界 y = 120 + 20×1.2 = 144
  // 探针取 144：已出精确区、恰在容差上界内 —— 真正走 hitTestSlot 的容差分支
  assert.deepEqual(resolveDropTarget(130, 144, layout), { kind: 'slot', code: 'P' });
});

test('layout 缺省字段安全（无任何 rect = outside）', () => {
  assert.deepEqual(resolveDropTarget(10, 10, {}), { kind: 'outside' });
  assert.deepEqual(resolveDropTarget(10, 10, { battingRect: rect(0, 0, 50, 50) }), { kind: 'batting', index: null });
});

// 打线行插入位（gap）：按「其余行」纵向中点计数（页面取 rect 时已排除被拖行本身）
const rowRect = (top, height = 40) => ({ top, height });
const withRows = (rows) => ({ ...layout, battingRowRects: rows });

test('打线行 gap：严格越过中点才计一行（中点 320/380/440）', () => {
  const l = withRows([rowRect(300), rowRect(360), rowRect(420)]);
  assert.deepEqual(resolveDropTarget(150, 310, l), { kind: 'batting', index: 0 });
  assert.deepEqual(resolveDropTarget(150, 320, l), { kind: 'batting', index: 0 }); // 恰在中点：不计
  assert.deepEqual(resolveDropTarget(150, 321, l), { kind: 'batting', index: 1 });
  assert.deepEqual(resolveDropTarget(150, 430, l), { kind: 'batting', index: 2 });
  assert.deepEqual(resolveDropTarget(150, 500, l), { kind: 'batting', index: 3 }); // 末尾 gap = others.length
});

test('打线行 gap：无可计行 = 0；非数组/缺字段 = null；卡槽命中优先不受影响', () => {
  assert.deepEqual(resolveDropTarget(150, 400, withRows([])), { kind: 'batting', index: 0 });
  assert.deepEqual(resolveDropTarget(150, 400, { ...layout, battingRowRects: null }), { kind: 'batting', index: null });
  assert.deepEqual(resolveDropTarget(120, 110, withRows([rowRect(300)])), { kind: 'slot', code: 'P' });
});
