// #478 投手登板序：沿用既有顺序、缺省追加、统计行回填、行排序
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizePitcherOrder,
  maxPitcherOrder,
  assignPitcherOrder,
  seedPitcherOrders,
  orderPitcherRows,
} from '../src/utils/liveGamePitcherOrder.js';

test('已有登板序原样保留；新投手追加最大序号', () => {
  const starter = { id: 200, pitcherOrder: 1 };
  const reliever = { id: 201 };
  const copies = [starter, { id: 200 }, reliever];
  assert.equal(assignPitcherOrder(copies, 200), 1);
  assert.equal(copies[1].pitcherOrder, 1, '同 id 副本都要写入');
  assert.equal(assignPitcherOrder(copies, 201), 2);
  assert.equal(reliever.pitcherOrder, 2);
});

test('无任何既有顺序时首名投手为 1；非法值归一为 0', () => {
  const pitcher = { id: 200 };
  assert.equal(assignPitcherOrder([pitcher], 200), 1);
  assert.equal(normalizePitcherOrder(0), 0);
  assert.equal(normalizePitcherOrder('x'), 0);
  assert.equal(normalizePitcherOrder('3'), 3);
});

test('统计行回填登板序，不覆盖缺失行', () => {
  const players = [{ id: 200 }, { id: 201 }, { id: 202, pitcherOrder: 9 }];
  const changed = seedPitcherOrders(players, [
    { playerId: 200, pitcherOrder: 1 },
    { playerId: 201, pitcherOrder: 2 },
    { playerId: 202, pitcherOrder: 0 },
  ]);
  assert.equal(changed, 2);
  assert.equal(players[0].pitcherOrder, 1);
  assert.equal(players[1].pitcherOrder, 2);
  assert.equal(players[2].pitcherOrder, 9, '统计行无有效值时保留原值');
  assert.equal(maxPitcherOrder(players), 9);
});

test('投手行按登板序排序；缺失者追加在末尾并写入序号', () => {
  const reliever = { id: 201, pitcherOrder: 2 };
  const starter = { id: 200, pitcherOrder: 1 };
  const unknown = { id: 202 };
  const { rows, orderById } = orderPitcherRows([reliever, unknown, starter]);
  assert.deepEqual(rows.map((row) => row.id), [200, 201, 202]);
  assert.equal(unknown.pitcherOrder, 3);
  assert.equal(orderById.get(201), 2);
});

test('缺少顺序时追加到最大序号之后，不改变已有顺序', () => {
  const a = { id: 300 };
  const b = { id: 301 };
  const c = { id: 302, pitcherOrder: 5 };
  const { rows } = orderPitcherRows([a, b, c]);
  assert.deepEqual(rows.map((row) => row.id), [302, 300, 301]);
  assert.deepEqual([a.pitcherOrder, b.pitcherOrder, c.pitcherOrder], [6, 7, 5]);
});
