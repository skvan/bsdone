// #524 守备表展开：明细行插入位置、展开状态与稳定行键
// 运行：node --test tests/gameFieldingExpand.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildFieldingDisplayRows,
  expandFieldingRows
} from '../src/utils/gameFieldingRows.js';

const stats = [
  {
    teamId: 1,
    playerId: 50,
    position: 'P',
    firstPosition: '3B',
    startingPosition: '3B',
    battingOrder: 5,
    po: 1,
    a: 3,
    e: 1,
    dp: 1,
    defInn: 9,
    fieldingPositions: [
      { position: '3B', outs: 15, po: 1, a: 2, e: 1, dp: 1 },
      { position: 'P', outs: 12, po: 0, a: 1 }
    ]
  },
  {
    teamId: 1,
    playerId: 60,
    position: 'CF',
    firstPosition: 'CF',
    startingPosition: 'CF',
    battingOrder: 1,
    po: 2,
    a: 0,
    e: 0,
    dp: 0,
    defInn: 9
  },
  {
    teamId: 1,
    playerId: 70,
    position: 'RF',
    firstPosition: 'LF',
    startingPosition: 'LF',
    battingOrder: 2,
    po: 3,
    a: 2,
    e: 0,
    dp: 0,
    defInn: 9,
    fieldingPositions: [
      { position: 'LF', outs: 18, po: 2, a: 1 },
      { position: 'RF', outs: 9, po: 1, a: 1 }
    ]
  }
];

const baseRows = () => buildFieldingDisplayRows(stats, 1);

test('#524 展开：默认全部折叠，行数不变', () => {
  const flat = expandFieldingRows(baseRows(), new Set());
  assert.deepEqual(flat.map((row) => row.playerId), [60, 70, 50]);
  assert.ok(flat.every((row) => row.kind === 'player'));
  assert.ok(flat.every((row) => row.open === false));
  assert.ok(flat.every((row) => row._rowKey));
});

test('#524 展开：明细行紧跟主行，位于下一名球员之前', () => {
  const flat = expandFieldingRows(baseRows(), new Set(['50']));
  assert.deepEqual(flat.map((row) => `${row.kind}:${row.playerId}:${row.position}`), [
    'player:60:CF',
    'player:70:RF',
    'player:50:P',
    'breakdown:50:3B',
    'breakdown:50:P'
  ]);
  assert.equal(flat[2].open, true);
  assert.equal(flat[3].breakdown.length, 0);
  assert.deepEqual(flat.slice(3).map((row) => row.defInn), [5, 4]);
});

test('#524 展开：多个球员可同时展开，数字/字符串 id 均可', () => {
  const flat = expandFieldingRows(baseRows(), new Set([50, '70']));
  assert.deepEqual(flat.map((row) => `${row.kind}:${row.playerId}:${row.position}`), [
    'player:60:CF',
    'player:70:RF',
    'breakdown:70:LF',
    'breakdown:70:RF',
    'player:50:P',
    'breakdown:50:3B',
    'breakdown:50:P'
  ]);
});

test('#524 展开：无明细球员即使 id 在展开集合中也不插入', () => {
  const flat = expandFieldingRows(baseRows(), new Set(['60']));
  assert.equal(flat.length, 3);
  assert.equal(flat[0].open, false);
});

test('#524 展开：行键唯一，明细行 kind 固定为 breakdown', () => {
  const flat = expandFieldingRows(baseRows(), new Set(['50']));
  const keys = flat.map((row) => row._rowKey);
  assert.equal(new Set(keys).size, keys.length);
  assert.ok(flat.filter((row) => row.kind === 'breakdown').every((row) => row._rowKey.startsWith('breakdown-')));
});
