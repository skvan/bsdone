// 开始录入快照：先发守备位映射（阵容/板凳/守备投手行）
// 运行：node --test tests/lineupStartingPosition.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildStartingBenchRows,
  buildStartingLineupRows,
  buildStartingPitcherRow
} from '../src/utils/lineupStartingPosition.js';

test('先发阵容：守位写入 startingPosition，DH/PH/EP 为空', () => {
  const rows = [
    { id: 1, position: 'lf' },
    { id: 2, position: 'DH' },
    { id: 3, position: 'P' },
    { id: 4, position: 'PH' },
    { id: 5, position: 'EP' }
  ];
  const out = buildStartingLineupRows(rows);
  assert.deepEqual(out.map((r) => r.startingPosition), ['LF', '', 'P', '', '']);
  assert.equal(rows[0].startingPosition, undefined, '不修改入参');
});

test('板凳：只有指定的先发投手补 P，其余替补为空', () => {
  const bench = [{ id: 10, position: '' }, { id: 11, position: 'P' }];
  const out = buildStartingBenchRows(bench, 11);
  assert.deepEqual(out.map((r) => r.startingPosition), ['', 'P']);
  assert.deepEqual(buildStartingBenchRows(bench, null).map((r) => r.startingPosition), ['', '']);
});

test('守备投手行：DH 制纯投手只在 roster 中也能生成 P 行', () => {
  const roster = [{ id: 21, name: '李投', number: '21', position: 'P' }];
  const row = buildStartingPitcherRow([], [], roster, 21, 7);
  assert.ok(row, '应从 roster 找到先发投手');
  assert.equal(row.id, 21);
  assert.equal(row.teamId, 7);
  assert.equal(row.position, 'P');
  assert.equal(row.startingPosition, 'P');
  assert.equal(buildStartingPitcherRow([], [], roster, null, 7), null);
  assert.equal(buildStartingPitcherRow([], [], roster, 99, 7), null);
});
