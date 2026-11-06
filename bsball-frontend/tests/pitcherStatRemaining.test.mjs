import { test } from 'node:test';
import assert from 'node:assert/strict';
import { nonZeroPitcherFields } from '../src/utils/pitcherStatRemaining.js';

test('诊断：列出投手行剩余 PA/FO/NP 等非零字段', () => {
  const fields = nonZeroPitcherFields(
    { pitchPa: 11, fo: 3, np: 0, pitchBf: 0, pitchR: 0, er: 0 },
    { po: 0, e: 0 }
  );
  assert.deepEqual(fields.map((field) => `${field.label}=${field.value}`), ['PA=11', 'FO=3']);
});

test('诊断：守备字段从守备对象补足，统计对象优先', () => {
  const fields = nonZeroPitcherFields(
    { po: 0, tc: 0, pitchPa: 1 },
    { po: 2, tc: 4, e: 1 }
  );
  assert.deepEqual(
    fields.map((field) => `${field.label}=${field.value}`),
    ['PA=1', 'TC=4', 'PO=2', 'E=1']
  );
});

test('诊断：全零返回空数组', () => {
  assert.deepEqual(nonZeroPitcherFields({ np: 0, pitchPa: 0 }, { po: 0 }), []);
});
