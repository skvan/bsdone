import { test } from 'node:test';
import assert from 'node:assert/strict';
import { activeFielders, addOutToInnings } from '../src/utils/liveGameFieldingIp.js';
import { computeCatchBackFieldingStats } from '../src/utils/liveGameChainStats.js';

test('守备局数按每三出局进一局累计', () => {
  assert.equal(addOutToInnings(0), 0.1);
  assert.equal(addOutToInnings(0.1), 0.2);
  assert.equal(addOutToInnings(0.2), 1);
  assert.equal(addOutToInnings(1.2), 2);
});

test('只从打序筛选实际守备员，即使合法位置清单含 DH 也排除 DH', () => {
  const positions = new Set(['P', 'C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'XF', 'DH']);
  const lineup = [
    { id: 1, position: 'P' },
    { id: 2, position: 'SS' },
    { id: 3, position: 'XF' },
    { id: 4, position: 'DH' },
    { id: 5, position: '' },
  ];
  assert.deepEqual(activeFielders(lineup, positions).map(({ id }) => id), [1, 2, 3]);
  assert.deepEqual(activeFielders(null, positions), []);
});

test('接杀后回传触杀：接杀者 PO+1/A+1，触杀者 PO+1', () => {
  assert.deepEqual(computeCatchBackFieldingStats('C', '2B'), [
    { pos: 'C', po: 1, a: 1 },
    { pos: '2B', po: 1, a: 0 },
  ]);
});

test('同一名守备员接杀后回传并完成触杀时两次 PO 与一次 A 均保留', () => {
  assert.deepEqual(computeCatchBackFieldingStats('C', 'C'), [
    { pos: 'C', po: 2, a: 1 },
  ]);
});
