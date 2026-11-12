// 换人模式·守备轮转速记单测（解析闭环 / 位置占用 / 置换表）
// 运行：node --test tests/liveSubRotation.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  ROTATION_POSITION_BY_DIGIT,
  buildFieldRotationMoves,
  formatFieldRotation,
  parseFieldRotation,
} from '../src/utils/liveSubRotation.js';

const fullLineup = () => [
  { id: 101, position: 'P' },
  { id: 102, position: 'C' },
  { id: 103, position: '1B' },
  { id: 104, position: '2B' },
  { id: 105, position: '3B' },
  { id: 106, position: 'SS' },
  { id: 107, position: 'LF' },
  { id: 108, position: 'CF' },
  { id: 109, position: 'RF' },
];

/* ---------------- parseFieldRotation ---------------- */

test('合法闭环：1341 → 1→3→4→1', () => {
  const r = parseFieldRotation('1341');
  assert.equal(r.ok, true);
  assert.deepEqual(r.digits, ['1', '3', '4']);
  assert.deepEqual(r.codes, ['P', '1B', '2B']);
});

test('两位互换：121 → 1↔2', () => {
  const r = parseFieldRotation('121');
  assert.equal(r.ok, true);
  assert.deepEqual(r.codes, ['P', 'C']);
});

test('全守位闭环：1234567891', () => {
  const r = parseFieldRotation('1234567891');
  assert.equal(r.ok, true);
  assert.deepEqual(r.codes, ['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF']);
});

test('允许首尾空白与空格分隔', () => {
  assert.equal(parseFieldRotation(' 1 3 4 1 ').ok, true);
});

test('拒绝空输入', () => {
  const r = parseFieldRotation('');
  assert.equal(r.ok, false);
  assert.match(r.error, /请输入/);
});

test('拒绝非 1-9 字符（含 0 与大写字母）', () => {
  for (const raw of ['1301', '13A1', '1-3-4-1', '1001']) {
    const r = parseFieldRotation(raw);
    assert.equal(r.ok, false, raw);
    assert.match(r.error, /1-9/);
  }
});

test('拒绝不足 3 位', () => {
  for (const raw of ['1', '11']) {
    const r = parseFieldRotation(raw);
    assert.equal(r.ok, false, raw);
    assert.match(r.error, /至少/);
  }
});

test('拒绝非闭环（首尾不同）', () => {
  const r = parseFieldRotation('134');
  assert.equal(r.ok, false);
  assert.match(r.error, /首尾/);
});

test('拒绝中间重复位置', () => {
  for (const raw of ['1331', '1311', '12121']) {
    const r = parseFieldRotation(raw);
    assert.equal(r.ok, false, raw);
    assert.match(r.error, /只能出现一次/);
  }
});

test('数字→位置码映射固定', () => {
  assert.deepEqual(ROTATION_POSITION_BY_DIGIT, {
    1: 'P', 2: 'C', 3: '1B', 4: '2B', 5: '3B', 6: 'SS', 7: 'LF', 8: 'CF', 9: 'RF',
  });
});

/* ---------------- buildFieldRotationMoves ---------------- */

test('1341 置换表：1→3→4→1', () => {
  const r = buildFieldRotationMoves(['1', '3', '4'], fullLineup());
  assert.equal(r.ok, true);
  assert.deepEqual(r.moves, [
    { id: 101, from: 'P', to: '1B' },
    { id: 103, from: '1B', to: '2B' },
    { id: 104, from: '2B', to: 'P' },
  ]);
});

test('121 互换：双方换位', () => {
  const r = buildFieldRotationMoves(['1', '2'], fullLineup());
  assert.equal(r.ok, true);
  assert.deepEqual(r.moves, [
    { id: 101, from: 'P', to: 'C' },
    { id: 102, from: 'C', to: 'P' },
  ]);
});

test('位置无人：返回缺位错误（含号位与人名）', () => {
  const lineup = fullLineup().filter((p) => p.position !== '3B');
  const r = buildFieldRotationMoves(['1', '3', '5'], lineup);
  assert.equal(r.ok, false);
  assert.match(r.error, /5号位（三垒手）/);
});

test('位置重复：返回重复错误', () => {
  const lineup = [...fullLineup(), { id: 999, position: '1B' }];
  const r = buildFieldRotationMoves(['1', '3'], lineup);
  assert.equal(r.ok, false);
  assert.match(r.error, /3号位（一垒手）/);
});

test('空位置条目被忽略，不影响占用校验', () => {
  const lineup = [...fullLineup(), { id: 200, position: '' }, { id: 201, position: null }];
  const r = buildFieldRotationMoves(['3', '4'], lineup);
  assert.equal(r.ok, true);
  assert.equal(r.moves.length, 2);
});

test('非法数字数组直接拒绝', () => {
  for (const digits of [[], ['1'], ['1', '0'], ['a', 'b']]) {
    const r = buildFieldRotationMoves(digits, fullLineup());
    assert.equal(r.ok, false, JSON.stringify(digits));
  }
});

test('重复位置（绕过解析层直接调用）也被拒绝', () => {
  const r = buildFieldRotationMoves(['1', '3', '1'], fullLineup());
  assert.equal(r.ok, false);
  assert.match(r.error, /只能出现一次/);
});

test('轮转路径文案', () => {
  assert.equal(formatFieldRotation(['P', '1B', '2B']), '投手 → 一垒手 → 二垒手 → 投手');
  assert.equal(formatFieldRotation([]), '');
});
