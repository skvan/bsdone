// 击球落点拖拽范围单测（纯函数，不触碰 DOM）
// 运行：node --test tests/hitSprayField.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { HIT_SPOT_DRAG_BOUNDS, clampHitSpotToField } from '../src/utils/hitSprayField.js';

test('拖拽范围覆盖扇形左右两翼（旧 0–480 矩形外的边角不再被夹住）', () => {
  // 对应 bug 复现点：CF 拖向左/右外野边角，旧实现被夹到 x=0 / x=480。
  assert.deepEqual(clampHitSpotToField(-120, 120), { x: -120, y: 120 });
  assert.deepEqual(clampHitSpotToField(600, 120), { x: 600, y: 120 });
  assert.deepEqual(clampHitSpotToField(-179.5, 165.8), { x: -179.5, y: 165.8 });
  assert.deepEqual(clampHitSpotToField(659.5, 165.8), { x: 659.5, y: 165.8 });
});

test('界外接杀与捕手后方落点保持原坐标', () => {
  // 两条边线外侧的界外区域（接杀可能发生）。
  assert.deepEqual(clampHitSpotToField(-60, 330), { x: -60, y: 330 });
  assert.deepEqual(clampHitSpotToField(540, 330), { x: 540, y: 330 });
  // 捕手后方高飞球。
  assert.deepEqual(clampHitSpotToField(240, 620), { x: 240, y: 620 });
});

test('坐标 0 是合法落点，不会被当成越界夹走', () => {
  assert.deepEqual(clampHitSpotToField(0, 120), { x: 0, y: 120 });
  assert.deepEqual(clampHitSpotToField(240, 0), { x: 240, y: 0 });
});

test('拖出可用范围时贴边停住', () => {
  const { minX, maxX, minY, maxY } = HIT_SPOT_DRAG_BOUNDS;
  assert.deepEqual(clampHitSpotToField(-500, 120), { x: minX, y: 120 });
  assert.deepEqual(clampHitSpotToField(900, 120), { x: maxX, y: 120 });
  assert.deepEqual(clampHitSpotToField(240, -100), { x: 240, y: minY });
  assert.deepEqual(clampHitSpotToField(240, 900), { x: 240, y: maxY });
});

test('范围覆盖扇形上沿与捕手后方，且与场地视图同量级', () => {
  assert.ok(HIT_SPOT_DRAG_BOUNDS.minX <= -179.5, '左翼极点应在范围内');
  assert.ok(HIT_SPOT_DRAG_BOUNDS.maxX >= 659.5, '右翼极点应在范围内');
  assert.ok(HIT_SPOT_DRAG_BOUNDS.minY <= -17.3, '外野墙顶应在范围内');
  assert.ok(HIT_SPOT_DRAG_BOUNDS.maxY > 590, '捕手后方应保留可拖空间');
});

test('非有限坐标回落到本垒附近，不写出 NaN', () => {
  assert.deepEqual(clampHitSpotToField(NaN, 120), { x: 240, y: 120 });
  assert.deepEqual(clampHitSpotToField(240, Infinity), { x: 240, y: 555 });
});
