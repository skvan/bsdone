// 比赛详情守备表行构建：全量打线球员、按棒次排序、缺省 0
// 运行：node --test tests/gameFieldingRows.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { buildFieldingDisplayRows } from '../src/utils/gameFieldingRows.js';

const stats = [
  { teamId: 1, playerId: 1, name: '投手', position: 'P', po: 0, battingOrder: 1 },
  { teamId: 1, playerId: 2, name: '捕手', position: 'C', po: 3, a: 1, e: 0, dp: 0, defInn: 3, pb: 1, catcherSb: 7, catcherCs: 0, battingOrder: 4 },
  { teamId: 1, playerId: 3, name: '一垒手', position: '1B', battingOrder: 3 },
  { teamId: 1, playerId: 4, name: '指定打击', position: 'DH', battingOrder: 5 },
  { teamId: 1, playerId: 5, name: '左外野', position: 'lf', battingOrder: 2, po: 1 },
  { teamId: 2, playerId: 6, name: '他队', position: 'SS', battingOrder: 1 }
];

test('守备行：包含全部非投手/非DH打线球员，按棒次排序', () => {
  const rows = buildFieldingDisplayRows(stats, 1);
  assert.deepEqual(rows.map((r) => r.playerId), [5, 3, 2]);
  assert.deepEqual(rows.map((r) => r.position), ['LF', '1B', 'C']);
});

test('守备行：缺失的守备/捕手字段补 0，已有值保留', () => {
  const rows = buildFieldingDisplayRows(stats, 1);
  const first = rows.find((r) => r.playerId === 3);
  assert.equal(first.po, 0);
  assert.equal(first.a, 0);
  assert.equal(first.e, 0);
  assert.equal(first.dp, 0);
  assert.equal(first.defInn, 0);
  assert.equal(first.pb, 0);
  assert.equal(first.catcherSb, 0);
  assert.equal(first.catcherCs, 0);
  const catcher = rows.find((r) => r.playerId === 2);
  assert.equal(catcher.po, 3);
  assert.equal(catcher.defInn, 3);
  assert.equal(catcher.pb, 1);
  assert.equal(catcher.catcherSb, 7);
});

test('守备行：无棒次排最后，且不修改入参', () => {
  const input = [
    { teamId: 1, playerId: 8, position: 'RF' },
    { teamId: 1, playerId: 9, position: 'CF', battingOrder: 1 }
  ];
  const snapshot = JSON.stringify(input);
  const rows = buildFieldingDisplayRows(input, 1);
  assert.deepEqual(rows.map((r) => r.playerId), [9, 8]);
  assert.equal(JSON.stringify(input), snapshot);
});

test('守备行：位置为空但已有守备数据也保留', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 1, playerId: 7, position: '', po: 2, a: 1, battingOrder: 6 }
  ], 1);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].playerId, 7);
  assert.equal(rows[0].po, 2);
  assert.equal(rows[0].position, '');
});

test('守备行：listOrder 从 0 开始可用；都无顺序时按 playerId 稳定排序', () => {
  assert.deepEqual(buildFieldingDisplayRows([
    { teamId: 1, playerId: 8, position: 'RF', listOrder: 0 },
    { teamId: 1, playerId: 9, position: 'CF', battingOrder: 1 }
  ], 1).map((r) => r.playerId), [8, 9]);
  assert.deepEqual(buildFieldingDisplayRows([
    { teamId: 1, playerId: 10, position: 'RF' },
    { teamId: 1, playerId: 9, position: 'CF' }
  ], 1).map((r) => r.playerId), [9, 10]);
});

test('比赛详情守备表接线：使用统一行构建，含 PB/SB/CS/INN 列', () => {
  const detail = fs.readFileSync(new URL('../src/components/admin/GameDetailFieldingTable.vue', import.meta.url), 'utf8');
  assert.match(detail, /buildFieldingDisplayRows/, '详情守备表复用行构建');
  assert.doesNotMatch(detail, /return Number\(row\.po\)/, '不再按有守备数据过滤');
  for (const col of ['PO', 'A', 'E', 'DP', 'INN', 'PB', 'SB', 'CS']) {
    assert.ok(detail.includes(`label="${col}"`), `缺少 ${col} 列`);
  }
  const entry = fs.readFileSync(new URL('../src/components/admin/GameFieldingTable.vue', import.meta.url), 'utf8');
  for (const key of ['pb', 'catcherSb', 'catcherCs']) {
    assert.ok(entry.includes(`key: '${key}'`), `录入页守备表缺少 ${key} 列`);
  }
});
