// 击出球飞行轨迹单测（纯函数，不触碰 DOM）
// 运行：node --test tests/battedBallFlight.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  BAT_SHAPES,
  BAT_SHAPE_COLORS,
  bipCodeShape,
  battedShapeOf,
  battedShapeColor,
  battedGeo,
  battedPointAt,
  battedPathD,
} from '../src/utils/battedBallFlight.js';

test('六类球种参数齐全，且强袭滚地/短打/高飞不再与同类共用参数', () => {
  assert.deepEqual(Object.keys(BAT_SHAPES).sort(), ['b', 'fly', 'g', 'hg', 'ld', 'pop']);
  // 滚地系：同为贴地直线，速度各异（强袭更快、短打更慢）
  assert.equal(BAT_SHAPES.g[0], 0);
  assert.equal(BAT_SHAPES.hg[0], 0);
  assert.equal(BAT_SHAPES.b[0], 0);
  assert.ok(BAT_SHAPES.hg[1] < BAT_SHAPES.g[1], '强袭滚地应快于普通滚地');
  assert.ok(BAT_SHAPES.b[1] > BAT_SHAPES.g[1], '短打应慢于普通滚地');
  // 高飞系：内野高飞弧线应比飞球更高、滞空更久
  assert.ok(BAT_SHAPES.pop[0] > BAT_SHAPES.fly[0], '内野高飞弧线应高于飞球');
  assert.ok(BAT_SHAPES.pop[1] > BAT_SHAPES.fly[1], '内野高飞滞空应久于飞球');
});

test('bipCodeShape 解析球种段', () => {
  assert.equal(bipCodeShape('bip:fly:1b'), 'fly');
  assert.equal(bipCodeShape('bip:hg:bo:1'), 'hg');
  assert.equal(bipCodeShape('bip:b:sac:e'), 'b');
  assert.equal(bipCodeShape('其他'), null);
  assert.equal(bipCodeShape(null), null);
});

test('battedShapeOf：已知球种取表，未知/缺失按平飞兜底', () => {
  assert.deepEqual(battedShapeOf('bip:g:1b'), { k: 0, ms: 450 });
  assert.deepEqual(battedShapeOf('bip:pop:bo:3'), { k: 0.6, ms: 1100 });
  assert.deepEqual(battedShapeOf('bip:xx:1b'), { k: 0.14, ms: 600 });
  assert.deepEqual(battedShapeOf(undefined), { k: 0.14, ms: 600 });
});

test('轨迹配色：滚地系统一黄色；平飞/飞球/高飞各自独立且非黄色', () => {
  assert.equal(BAT_SHAPE_COLORS.g, '#FFD700');
  assert.equal(battedShapeColor('bip:g:1b'), '#FFD700');
  assert.equal(battedShapeColor('bip:hg:o1'), '#FFD700');
  assert.equal(battedShapeColor('bip:b:o1'), '#FFD700');
  const ld = battedShapeColor('bip:ld:1b');
  const fly = battedShapeColor('bip:fly:1b');
  const pop = battedShapeColor('bip:pop:1b');
  for (const c of [ld, fly, pop]) assert.notEqual(c, '#FFD700', '非滚地系不应为黄色');
  assert.equal(new Set([ld, fly, pop]).size, 3, '平飞/飞球/高飞颜色应互不相同');
  assert.equal(battedShapeColor('bip:xx:1b'), ld, '未知球种回退平飞色');
});

test('battedGeo：k=0 控制点为弦中点（退化直线）；k>0 沿法线抬高 k*弦长', () => {
  assert.deepEqual(battedGeo(240, 355, 0), { cx: 240, cy: 455 });
  assert.deepEqual(battedGeo(240, 355, 1), { cx: 240, cy: 255 });
  // 负 k 视为 0（与原录入页 Math.max(0,k) 一致）
  assert.deepEqual(battedGeo(240, 355, -1), { cx: 240, cy: 455 });
});

test('battedPointAt：端点固定为本垒与落点，直线轨迹中点为弦中点', () => {
  assert.deepEqual(battedPointAt(100, 300, 0.32, 0), { x: 240, y: 555 });
  assert.deepEqual(battedPointAt(100, 300, 0.32, 1), { x: 100, y: 300 });
  const mid = battedPointAt(240, 355, 0, 0.5);
  assert.equal(mid.x, 240);
  assert.equal(mid.y, 455);
});

test('battedPathD：k=0 画直线，k>0 画二次贝塞尔', () => {
  assert.equal(battedPathD(352.5, 445.5, 0), 'M 240 555 L 352.5 445.5');
  const geo = battedGeo(352.5, 445.5, 0.32);
  assert.equal(battedPathD(352.5, 445.5, 0.32), `M 240 555 Q ${geo.cx} ${geo.cy} 352.5 445.5`);
});
