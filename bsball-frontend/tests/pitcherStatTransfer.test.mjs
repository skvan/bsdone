// 投手行转记合并规则：目标已有值优先，原投手非零数据补入，原行清零。
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  PITCHER_TRANSFER_FIELDS,
  PITCHER_FIELDING_TRANSFER_FIELDS,
  mergePitcherStats,
  mergePitcherFieldingStats,
} from '../src/utils/pitcherStatTransfer.js';

test('转记：目标缺失的 NP/PA/pitAb 从原投手补入，已有 IP/R/ER 保持', () => {
  const source = { np: 27, pitchPa: 11, pitAb: 9, ip: 0, pitchR: 0, er: 0 };
  const target = { np: 0, pitchPa: 0, pitAb: 0, ip: 1.2, pitchR: 3, er: 3 };

  const result = mergePitcherStats(source, target);

  assert.deepEqual(
    { np: target.np, pitchPa: target.pitchPa, pitAb: target.pitAb, ip: target.ip, pitchR: target.pitchR, er: target.er },
    { np: 27, pitchPa: 11, pitAb: 9, ip: 1.2, pitchR: 3, er: 3 }
  );
  assert.equal(result.moved, 3);
  assert.equal(result.cleared, true);
  for (const key of PITCHER_TRANSFER_FIELDS) {
    assert.equal(Number(source[key]) || 0, 0, `原投手 ${key} 应清零`);
  }
});

test('转记：目标已有非零值优先，原投手仍被清零', () => {
  const source = { np: 27, pitchH: 2 };
  const target = { np: 5, pitchH: 0 };

  const result = mergePitcherStats(source, target);

  assert.equal(target.np, 5);
  assert.equal(target.pitchH, 2);
  assert.equal(result.moved, 1);
  assert.equal(result.cleared, true);
  assert.equal(source.np, 0);
  assert.equal(source.pitchH, 0);
});

test('转记：原投手无数据时不标记已清零', () => {
  const source = { np: 0, ip: 0 };
  const target = { np: 0, ip: 1 };

  const result = mergePitcherStats(source, target);

  assert.deepEqual(result, { moved: 0, cleared: false });
  assert.equal(target.ip, 1);
});

test('守备转记：原投手 E/PO 并入目标并清零', () => {
  const source = { po: 2, a: 1, e: 1, tc: 4, fieldingGs: 1, defInn: 1 };
  const target = { po: 0, a: 0, e: 0, tc: 0, fieldingGs: 0, defInn: 0 };

  const result = mergePitcherFieldingStats(source, target);

  assert.equal(target.po, 2);
  assert.equal(target.a, 1);
  assert.equal(target.e, 1);
  assert.equal(target.tc, 4);
  assert.equal(result.cleared, true);
  for (const key of PITCHER_FIELDING_TRANSFER_FIELDS) {
    assert.equal(Number(source[key]) || 0, 0, `原投手守备 ${key} 应清零`);
  }
});
