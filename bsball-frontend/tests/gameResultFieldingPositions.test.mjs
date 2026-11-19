// 结果录入页逐位置守备状态工具（Task 3）：分组 / 汇总 / 保存 payload / 守位选项 / 校验。
// 运行：node --test tests/gameResultFieldingPositions.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  groupFieldingPositionRows,
  sumFieldingPositionRows,
  applyFieldingPositionTotals,
  buildFieldingPlayerParts,
  positionOptions,
  validateFieldingPositionRows
} from '../src/utils/gameResultFieldingPositions.js';

test('分组：按 teamId_playerId 聚合，未知字段归零', () => {
  const grouped = groupFieldingPositionRows([
    { teamId: 1, playerId: 21, position: '3B', outs: 9, po: 1, a: 2 },
    { teamId: 1, playerId: 21, position: 'P', outs: 3, po: 0 }
  ]);
  assert.equal(grouped['1_21'].length, 2);
  assert.equal(grouped['1_21'][1].e, 0);
});

test('汇总与回写：合计 = 逐位置之和，INN 按出局换算', () => {
  const rows = [
    { position: '3B', outs: 9, po: 1, a: 2, e: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0 },
    { position: 'P', outs: 3, po: 0, a: 0, e: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0 }
  ];
  assert.deepEqual(sumFieldingPositionRows(rows), { outs: 12, po: 1, a: 2, e: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0 });
  const stats = {};
  applyFieldingPositionTotals(stats, rows);
  assert.equal(stats.po, 1);
  assert.equal(stats.defInn, 4);
});

test('保存 payload：只带 dirty 球员，空数组表示清空', () => {
  const byPlayer = { '1_21': [{ position: '3B', outs: 9, po: 1 }] };
  const parts = buildFieldingPlayerParts(['1_21', '2_9'], byPlayer);
  assert.deepEqual(parts[0], { teamId: 1, playerId: 21, rows: [{ position: '3B', outs: 9, po: 1, a: 0, e: 0, dp: 0, pb: 0, catcherSb: 0, catcherCs: 0 }] });
  assert.deepEqual(parts[1], { teamId: 2, playerId: 9, rows: [] });
});

test('守位选项：排除已用与 DH/PH/EP', () => {
  assert.deepEqual(
    positionOptions(['P', 'C', '1B', 'DH', 'EP'], [{ position: 'P' }]),
    ['C', '1B']
  );
});

test('校验：重复守位与越界', () => {
  assert.match(validateFieldingPositionRows([{ position: 'SS', outs: 3 }, { position: 'SS', outs: 6 }]), /重复/);
  assert.match(validateFieldingPositionRows([{ position: 'SS', outs: 3, po: 1000 }]), /范围/);
  assert.equal(validateFieldingPositionRows([{ position: 'SS', outs: 3, po: 1 }]), null);
});
