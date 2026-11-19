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
  assert.equal(stats.tc, 3, 'TC 同步为 PO+A+E，避免保存前台账出现 transient 值');
});

test('汇总不覆盖球员级 GS：已有值保留，缺省才按 rows 推导', () => {
  // 球员手改 GS=1，逐位置行均无 GS：保持 1（GS 保持球员级口径）
  const manual = { fieldingGs: 1 };
  applyFieldingPositionTotals(manual, [{ position: '3B', outs: 9 }]);
  assert.equal(manual.fieldingGs, 1);

  // _fieldingOnly 等新建对象未带 GS：按 rows 推导
  const derived = {};
  applyFieldingPositionTotals(derived, [{ position: '3B', outs: 9, fieldingGs: true }]);
  assert.equal(derived.fieldingGs, 1);

  // 已有 0 也是有效值，不被 rows 覆盖
  const zero = { fieldingGs: 0 };
  applyFieldingPositionTotals(zero, [{ position: '3B', outs: 9, fieldingGs: true }]);
  assert.equal(zero.fieldingGs, 0);

  // 缺省且 rows 无 GS：0
  const empty = {};
  applyFieldingPositionTotals(empty, [{ position: '3B', outs: 9 }]);
  assert.equal(empty.fieldingGs, 0);
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
