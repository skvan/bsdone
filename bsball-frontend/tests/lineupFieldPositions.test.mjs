// 卡槽几何与命中判定单测
// 运行：node --test tests/lineupFieldPositions.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  FIELD_VIEWBOX, FIELD_SLOTS, DH_SLOT, XF_SLOT, ALL_SLOT_CODES, SLOT_SIZE, slotsForMode,
  slotDef, isFieldCode, hitTestSlot,
} from '../src/utils/lineupFieldPositions.js';

test('卡槽全集：9 守备位 + XF + DH，代码唯一；slotsForMode 按赛制取列', () => {
  assert.equal(FIELD_SLOTS.length, 9);
  assert.equal(XF_SLOT.code, 'XF');
  assert.equal(XF_SLOT.label, '自由人');
  assert.equal(DH_SLOT.code, 'DH');
  assert.equal(ALL_SLOT_CODES.length, 11);
  assert.equal(new Set(ALL_SLOT_CODES).size, 11);
  const baseball = slotsForMode('BASEBALL');
  assert.equal(baseball.length, 10);
  assert.equal(baseball.at(-1).code, 'DH');
  assert.ok(!baseball.some((s) => s.code === 'XF'));
  const softball = slotsForMode('SOFTBALL');
  assert.equal(softball.length, 11);
  assert.deepEqual(softball.slice(-2).map((s) => s.code), ['XF', 'DH']);
});

test('坐标全部落在 viewBox 内', () => {
  for (const s of [...FIELD_SLOTS, XF_SLOT, DH_SLOT]) {
    assert.ok(s.x >= 0 && s.x <= FIELD_VIEWBOX.width, `${s.code} x 越界`);
    assert.ok(s.y >= 0 && s.y <= FIELD_VIEWBOX.height, `${s.code} y 越界`);
  }
});

test('卡槽不重叠不变量：任意球场宽度下（含极窄移动端）任意两卡槽矩形不相交（含垒球 XF）', () => {
  const slots = [...FIELD_SLOTS, XF_SLOT, DH_SLOT];
  const slotAt = (w) => Math.min(
    Math.min(SLOT_SIZE.max, Math.max(SLOT_SIZE.min, SLOT_SIZE.ratio * w)),
    SLOT_SIZE.guardRatio * w,
  );
  for (let w = 150; w <= 2400; w += 1) {
    const slot = slotAt(w);
    const scale = w / FIELD_VIEWBOX.width;
    for (let i = 0; i < slots.length; i += 1) {
      for (let j = i + 1; j < slots.length; j += 1) {
        const dx = Math.abs(slots[i].x - slots[j].x) * scale;
        const dy = Math.abs(slots[i].y - slots[j].y) * scale;
        assert.ok(
          dx >= slot || dy >= slot,
          `球场 ${w}px 时 ${slots[i].code}-${slots[j].code} 卡槽重叠（dx=${dx.toFixed(1)}, dy=${dy.toFixed(1)}, slot=${slot.toFixed(1)}）`,
        );
      }
    }
  }
});

test('isFieldCode：守备位（含垒球 XF）true、DH false', () => {
  assert.equal(isFieldCode('P'), true);
  assert.equal(isFieldCode('SS'), true);
  assert.equal(isFieldCode('XF'), true);
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
  // 中心 (130,120)，半高 20 × 1.2 = 24 → 容差区 y∈[96,144]
  // y=140 恰是精确区下边缘（top+height=140），走精确命中分支返回 P
  assert.equal(hitTestSlot(130, 140, rects), 'P');
  // y=144 恰在容差上界（dy=24 未超过 20×1.2=24），才是真正的容差命中探针
  assert.equal(hitTestSlot(130, 144, rects), 'P');
  // y=150 超出容差（dy=30 > 24）
  assert.equal(hitTestSlot(130, 150, rects), null);
});

test('hitTestSlot：多个候选重叠时按精确区归属（取数组首个精确命中）', () => {
  const rects = [
    { code: 'P', left: 100, top: 100, width: 60, height: 40 }, // 精确 y∈[100,140]
    { code: 'C', left: 100, top: 130, width: 60, height: 40 }, // 精确 y∈[130,170]
  ];
  // (130,128) 落在 P 的精确区 y∈[100,140] 内，精确分支按数组顺序返回首个命中 P
  assert.equal(hitTestSlot(130, 128, rects), 'P');
  // (130,160) 不在 P 精确区（>140），落在 C 的精确区 y∈[130,170] 内 → C
  assert.equal(hitTestSlot(130, 160, rects), 'C');
});

test('hitTestSlot：多候选均在容差区、由中心距离最近者决出', () => {
  const rects = [
    { code: 'P', left: 100, top: 100, width: 60, height: 40 }, // 精确 y∈[100,140]，容差 y∈[96,144]，中心 y=120
    { code: 'C', left: 100, top: 144, width: 60, height: 40 }, // 精确 y∈[144,184]，容差 y∈[140,188]，中心 y=164
  ];
  // (130,141)：两侧精确区之外；到 P 中心 21、到 C 中心 23 → P
  assert.equal(hitTestSlot(130, 141, rects), 'P');
  // (130,143)：到 P 中心 23、到 C 中心 21 → C
  assert.equal(hitTestSlot(130, 143, rects), 'C');
});

test('hitTestSlot：空候选/无命中返回 null', () => {
  assert.equal(hitTestSlot(0, 0, []), null);
  assert.equal(hitTestSlot(0, 0, [{ code: 'P', left: 100, top: 100, width: 60, height: 40 }]), null);
});
