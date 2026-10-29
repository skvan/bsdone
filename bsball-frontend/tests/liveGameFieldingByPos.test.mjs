// 逐位置守备局数：按当前守位累加出局、换算局数、生成 save-live 载荷
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  accrueActiveFieldingOuts,
  accrueFieldingOut,
  buildFieldingPositionRows,
  hasFieldingByPosBaseline,
  hasHistoricalFieldingInnings,
  normalizeFieldingPosition,
} from '../src/utils/liveGameFieldingByPos.js';

test('按当前守位累加出局；DH/PH/EP/空守位不计', () => {
  const fielding = {};
  assert.equal(accrueFieldingOut(fielding, 'ss'), true);
  assert.equal(accrueFieldingOut(fielding, 'SS'), true);
  assert.equal(accrueFieldingOut(fielding, '2B'), true);
  assert.equal(accrueFieldingOut(fielding, 'DH'), false);
  assert.equal(accrueFieldingOut(fielding, 'PH'), false);
  assert.equal(accrueFieldingOut(fielding, 'EP'), false);
  assert.equal(accrueFieldingOut(fielding, ''), false);
  assert.deepEqual(fielding.byPos, { SS: 2, '2B': 1 });
});

test('守位归一化', () => {
  assert.equal(normalizeFieldingPosition(' cf '), 'CF');
  assert.equal(normalizeFieldingPosition(null), '');
});

test('载荷：按球员×位置展开，先发首个守位标记 GS，替补不标', () => {
  const players = [
    { id: 108, stats: { fielding: { byPos: { SS: 9, '2B': 3 } } } },
    { id: 111, stats: { fielding: { byPos: { SS: 6 } } } },
    { id: 112, stats: { fielding: {} } },
  ];
  const rows = buildFieldingPositionRows(1, players, [108]);
  assert.deepEqual(rows, [
    { teamId: 1, playerId: 108, position: 'SS', outs: 9, fieldingGs: true },
    { teamId: 1, playerId: 108, position: '2B', outs: 3, fieldingGs: false },
    { teamId: 1, playerId: 111, position: 'SS', outs: 6, fieldingGs: false },
  ]);
});

test('载荷：仅守备投手（fpId 不在打序）也标记 GS', () => {
  const players = [{ id: 1, stats: { fielding: { byPos: { P: 6 } } } }];
  const rows = buildFieldingPositionRows(1, players, [108, 1]);
  assert.deepEqual(rows, [{ teamId: 1, playerId: 1, position: 'P', outs: 6, fieldingGs: true }]);
});

test('载荷：缺省/非法输入安全', () => {
  assert.deepEqual(buildFieldingPositionRows(1, null, null), []);
  assert.deepEqual(buildFieldingPositionRows(1, [{ id: 1, stats: { fielding: { byPos: { SS: 0 } } } }], []), []);
});

test('在场守备员按当前守位累计；换位后新位置继续累计', () => {
  const pitcher = { id: 1, stats: { fielding: {} } };
  const shortstop = { id: 2, position: 'SS', stats: { fielding: {} } };
  const secondBase = { id: 3, position: '2B', stats: { fielding: {} } };
  accrueActiveFieldingOuts(pitcher, [pitcher, shortstop, secondBase], (p) => p.position);
  assert.deepEqual(pitcher.stats.fielding.byPos, { P: 1 });
  assert.deepEqual(shortstop.stats.fielding.byPos, { SS: 1 });
  assert.deepEqual(secondBase.stats.fielding.byPos, { '2B': 1 });

  shortstop.position = '2B';
  secondBase.position = 'SS';
  accrueActiveFieldingOuts(pitcher, [shortstop, secondBase], (p) => p.position);
  assert.deepEqual(shortstop.stats.fielding.byPos, { SS: 1, '2B': 1 });
  assert.deepEqual(secondBase.stats.fielding.byPos, { '2B': 1, SS: 1 });
});

test('逐位置基线：仅当存在 byPos 才发送 payload', () => {
  assert.equal(hasFieldingByPosBaseline([{ id: 1, stats: { fielding: {} } }]), false);
  assert.equal(hasFieldingByPosBaseline([{ id: 1, stats: { fielding: { byPos: {} } } }]), true);
  assert.equal(hasFieldingByPosBaseline(null), false);
});

test('历史守备局数：Do 重建路径据此判定基线不可信', () => {
  assert.equal(hasHistoricalFieldingInnings([{ defInn: 0 }, {}]), false);
  assert.equal(hasHistoricalFieldingInnings([{ defInn: 1.2 }]), true);
  assert.equal(hasHistoricalFieldingInnings(null), false);
});
