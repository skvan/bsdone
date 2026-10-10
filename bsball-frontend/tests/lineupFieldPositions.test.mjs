// 卡槽几何与命中判定单测
// 运行：node --test tests/lineupFieldPositions.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  FIELD_VIEWBOX, FIELD_SLOTS, DH_SLOT, ALL_SLOT_CODES,
  slotDef, isFieldCode, hitTestSlot,
} from '../src/utils/lineupFieldPositions.js';

test('10 个卡槽：9 守备位 + DH，代码唯一', () => {
  assert.equal(FIELD_SLOTS.length, 9);
  assert.equal(DH_SLOT.code, 'DH');
  assert.equal(ALL_SLOT_CODES.length, 10);
  assert.equal(new Set(ALL_SLOT_CODES).size, 10);
});

test('坐标全部落在 viewBox 内', () => {
  for (const s of [...FIELD_SLOTS, DH_SLOT]) {
    assert.ok(s.x >= 0 && s.x <= FIELD_VIEWBOX.width, `${s.code} x 越界`);
    assert.ok(s.y >= 0 && s.y <= FIELD_VIEWBOX.height, `${s.code} y 越界`);
  }
});

test('isFieldCode：守备位 true、DH false', () => {
  assert.equal(isFieldCode('P'), true);
  assert.equal(isFieldCode('SS'), true);
  assert.equal(isFieldCode('DH'), false);
  assert.equal(isFieldCode('EP'), false);
});

test('slotDef：已知返回定义、未知返回 null', () => {
  assert.equal(slotDef('CF').label, '中外野手');
  assert.equal(slotDef('NOPE'), null);
});

test('hitTestSlot：矩形内精确命中', () => {
  const rects = [
    { code: 'P', left: 100, top: 100, width: 60, height: 40 },
    { code: 'C', left: 100, top: 200, width: 60, height: 40 },
  ];
  assert.equal(hitTestSlot(110, 110, rects), 'P');
  assert.equal(hitTestSlot(110, 210, rects), 'C');
});

test('hitTestSlot：容差内命中（tolerance 1.2 倍外扩）', () => {
  const rects = [{ code: 'P', left: 100, top: 100, width: 60, height: 40 }];
  // 中心 (130,120)，半高 20 × 1.2 = 24 → y=140 仍在容差内
  assert.equal(hitTestSlot(130, 140, rects), 'P');
  // y=150 超出容差
  assert.equal(hitTestSlot(130, 150, rects), null);
});

test('hitTestSlot：多个候选时取中心距离最近者', () => {
  const rects = [
    { code: 'P', left: 100, top: 100, width: 60, height: 40 },
    { code: 'C', left: 100, top: 130, width: 60, height: 40 },
  ];
  // y=128 距 P 中心(120) 8，距 C 中心(150) 22 → P
  assert.equal(hitTestSlot(130, 128, rects), 'P');
  // y=160 距 C 中心 10，距 P 中心 40（均超精确区）→ C
  assert.equal(hitTestSlot(130, 160, rects), 'C');
});

test('hitTestSlot：空候选/无命中返回 null', () => {
  assert.equal(hitTestSlot(0, 0, []), null);
  assert.equal(hitTestSlot(0, 0, [{ code: 'P', left: 100, top: 100, width: 60, height: 40 }]), null);
});
