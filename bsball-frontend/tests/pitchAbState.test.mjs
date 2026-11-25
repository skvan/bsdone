// 投手 AB 状态判定（#702）：历史库值动态修正 + 人工覆写保留
// 运行：node --test tests/pitchAbState.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  inferPitchAbState,
  syncAutoPitchAb,
  toSavePitchAb,
} from '../src/utils/pitchAbState.js';
import { calcAtBatsFromBf, legacyAtBatsFromBf } from '../src/utils/gamePlayerStat.js';

// BF10 / BB2 / HBP1 / SH1 / SF1 → 新 5，旧 7
const base = { pitchBf: 10, pitchBb: 2, pitchHbp: 1, pitchSh: 1, pitchSf: 1 };

test('新旧公式基准：新 5，旧 7', () => {
  assert.equal(calcAtBatsFromBf(base), 5);
  assert.equal(legacyAtBatsFromBf(base), 7);
});

test('库值为空 → 新公式，自动', () => {
  assert.deepEqual(inferPitchAbState({ ...base }), { pitAb: 5, abManual: false });
  assert.deepEqual(inferPitchAbState({ ...base, pitchAb: null }), { pitAb: 5, abManual: false });
});

test('库值 = 旧公式且 SH/SF>0 → 按新公式修正，自动', () => {
  assert.deepEqual(inferPitchAbState({ ...base, pitchAb: 7 }), { pitAb: 5, abManual: false });
});

test('库值 = 新公式 → 保持，自动', () => {
  assert.deepEqual(inferPitchAbState({ ...base, pitchAb: 5 }), { pitAb: 5, abManual: false });
});

test('库值为人工不同值 → 保留，人工', () => {
  assert.deepEqual(inferPitchAbState({ ...base, pitchAb: 6 }), { pitAb: 6, abManual: true });
  // 下限截断
  assert.deepEqual(inferPitchAbState({ ...base, pitchAb: -3 }), { pitAb: 0, abManual: true });
});

test('SH/SF=0 时旧公式与新公式等价，不改写库值', () => {
  const noSac = { pitchBf: 10, pitchBb: 2, pitchHbp: 1, pitchSh: 0, pitchSf: 0 };
  assert.equal(calcAtBatsFromBf(noSac), 7);
  assert.deepEqual(inferPitchAbState({ ...noSac, pitchAb: 7 }), { pitAb: 7, abManual: false });
  assert.deepEqual(inferPitchAbState({ ...noSac, pitchAb: 9 }), { pitAb: 9, abManual: true });
});

test('旧 BBHP 合计回退同样参与判定', () => {
  const legacyBbHp = { pitchBf: 10, pitchBb: 0, pitchHbp: 0, pitchBbHp: 3, pitchSh: 1, pitchSf: 1 };
  assert.equal(legacyAtBatsFromBf(legacyBbHp), 7);
  assert.equal(calcAtBatsFromBf(legacyBbHp), 5);
  assert.deepEqual(inferPitchAbState({ ...legacyBbHp, pitchAb: 7 }), { pitAb: 5, abManual: false });
});

test('库值 0 且新公式>0 → 视为旧默认值，按新公式自动（#702 回填同步）', () => {
  assert.deepEqual(inferPitchAbState({ ...base, pitchAb: 0 }), { pitAb: 5, abManual: false });
  assert.equal(toSavePitchAb({ ...base, pitchAb: 0 }), 5);
});

test('库值 0 且公式=0 → 保持 0，自动（不误伤）', () => {
  const zero = { pitchBf: 0, pitchBb: 0, pitchHbp: 0, pitchSh: 0, pitchSf: 0 };
  assert.deepEqual(inferPitchAbState({ ...zero, pitchAb: 0 }), { pitAb: 0, abManual: false });
  // 分项把公式压到 0（BB/HBP/SH/SF 覆盖 BF）同样保持 0
  const floored = { pitchBf: 2, pitchBb: 1, pitchHbp: 1, pitchSh: 1, pitchSf: 0 };
  assert.deepEqual(inferPitchAbState({ ...floored, pitchAb: 0 }), { pitAb: 0, abManual: false });
});

test('会话内已标记人工（abManual=true）→ 直接保留当前值', () => {
  assert.deepEqual(inferPitchAbState({ ...base, pitAb: 9, abManual: true }), { pitAb: 9, abManual: true });
});

test('syncAutoPitchAb：自动行随分项重算，人工行不动', () => {
  const auto = { ...base, pitAb: 99, abManual: false };
  syncAutoPitchAb(auto);
  assert.equal(auto.pitAb, 5);

  const manual = { ...base, pitAb: 99, abManual: true };
  syncAutoPitchAb(manual);
  assert.equal(manual.pitAb, 99);

  // 未标记的行按自动处理
  const fresh = { ...base };
  syncAutoPitchAb(fresh);
  assert.equal(fresh.pitAb, 5);
});

test('toSavePitchAb：人工→clamp 人工值；自动→当前新公式（不写默认 0）', () => {
  assert.equal(toSavePitchAb({ ...base, pitchAb: 7 }), 5);
  assert.equal(toSavePitchAb({ ...base, pitchAb: 6 }), 5);
  assert.equal(toSavePitchAb({ ...base, pitchAb: 6, abManual: true, pitAb: 6 }), 6);
  // 新行默认 pitAb=0 且无其它分项时保存 0；有 BF 时保存公式值而非 0
  assert.equal(toSavePitchAb({ pitAb: 0 }), 0);
  assert.equal(toSavePitchAb({ ...base, pitAb: 0 }), 5);
});

test('toSavePitchAb：改过 BF/SH/SF 后不因旧库值误判人工（#702 跟修）', () => {
  // 原始库值 7 已不等于当前旧公式，但未标记人工 → 必须按当前分项算新公式 6，而非 5/7
  assert.equal(toSavePitchAb({ ...base, pitchAb: 7, abManual: false, pitchBf: 11, pitAb: 5 }), 6);
  // 人工标记仍以人工值为准（即便分项公式结果不同）
  assert.equal(toSavePitchAb({ ...base, pitchBf: 11, pitAb: 9, abManual: true }), 9);
  assert.equal(toSavePitchAb({ ...base, pitchBf: 11, pitAb: null, abManual: true }), 0);
});
