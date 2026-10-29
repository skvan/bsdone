// 逐位置守备统计：按当前守位累加出局与 PO/A/E/DP/捕手专项、换算局数、生成 save-live 载荷
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  accrueActiveFieldingOuts,
  accrueFieldingOut,
  accrueFieldingPlay,
  buildFieldingPositionRows,
  hasFieldingByPosBaseline,
  hasHistoricalFieldingStats,
  normalizeFieldingPosition,
} from '../src/utils/liveGameFieldingByPos.js';

const entry = (outs = 0, po = 0, a = 0, e = 0, dp = 0, pb = 0, catcherCs = 0, catcherSb = 0) => ({
  outs, po, a, e, dp, pb, catcherCs, catcherSb,
});

test('按当前守位累加出局；DH/PH/EP/空守位不计', () => {
  const fielding = {};
  assert.equal(accrueFieldingOut(fielding, 'ss'), true);
  assert.equal(accrueFieldingOut(fielding, 'SS'), true);
  assert.equal(accrueFieldingOut(fielding, '2B'), true);
  assert.equal(accrueFieldingOut(fielding, 'DH'), false);
  assert.equal(accrueFieldingOut(fielding, 'PH'), false);
  assert.equal(accrueFieldingOut(fielding, 'EP'), false);
  assert.equal(accrueFieldingOut(fielding, ''), false);
  assert.deepEqual(fielding.byPos, { SS: entry(2), '2B': entry(1) });
});

test('逐位置 PO/A/E：按守位累计且可与 outs 共用同一条目', () => {
  const fielding = {};
  accrueFieldingOut(fielding, 'SS');
  assert.equal(accrueFieldingPlay(fielding, 'ss', { po: 1 }), true);
  assert.equal(accrueFieldingPlay(fielding, 'SS', { po: 1, a: 2 }), true);
  assert.equal(accrueFieldingPlay(fielding, 'SS', { e: 1 }), true);
  assert.deepEqual(fielding.byPos, { SS: entry(1, 2, 2, 1) });
});

test('逐位置 DP/捕手专项：双杀、捕逸、盗垒阻杀、被盗垒按守位累计', () => {
  const fielding = {};
  assert.equal(accrueFieldingPlay(fielding, 'SS', { dp: 1 }), true);
  assert.equal(accrueFieldingPlay(fielding, 'SS', { dp: 1 }), true);
  assert.equal(accrueFieldingPlay(fielding, 'C', { pb: 2 }), true);
  assert.equal(accrueFieldingPlay(fielding, 'C', { catcherCs: 3 }), true);
  assert.equal(accrueFieldingPlay(fielding, 'C', { catcherSb: 1 }), true);
  assert.deepEqual(fielding.byPos, {
    SS: entry(0, 0, 0, 0, 2),
    C: entry(0, 0, 0, 0, 0, 2, 3, 1),
  });
});

test('逐位置记账：DH/PH/EP/空守位与全零无效；负数不计', () => {
  const fielding = {};
  assert.equal(accrueFieldingPlay(fielding, 'DH', { po: 1 }), false);
  assert.equal(accrueFieldingPlay(fielding, 'PH', { a: 1 }), false);
  assert.equal(accrueFieldingPlay(fielding, 'EP', { e: 1 }), false);
  assert.equal(accrueFieldingPlay(fielding, '', { dp: 1 }), false);
  assert.equal(accrueFieldingPlay(fielding, 'SS', {}), false);
  assert.equal(accrueFieldingPlay(fielding, 'SS', { po: -2, a: -1, e: 0 }), false);
  assert.equal(accrueFieldingPlay(fielding, 'C', { dp: -1, pb: -1, catcherCs: -1, catcherSb: -1 }), false);
  assert.deepEqual(fielding.byPos, undefined);
});

test('兼容旧快照：数字 byPos 视为仅有 outs，缺字段对象归一化后记账', () => {
  const fielding = { byPos: { CF: 3, SS: { outs: 6, po: 2, a: 1, e: 0 } } };
  assert.equal(accrueFieldingPlay(fielding, 'CF', { a: 1 }), true);
  assert.equal(accrueFieldingPlay(fielding, 'SS', { dp: 1 }), true);
  assert.deepEqual(fielding.byPos, {
    CF: entry(3, 0, 1, 0),
    SS: entry(6, 2, 1, 0, 1),
  });
  assert.equal(accrueFieldingOut(fielding, 'CF'), true);
  assert.deepEqual(fielding.byPos.CF, entry(4, 0, 1, 0));
});

test('守位归一化', () => {
  assert.equal(normalizeFieldingPosition(' cf '), 'CF');
  assert.equal(normalizeFieldingPosition(null), '');
});

test('载荷：按球员×位置展开，先发首个守位标记 GS，替补不标', () => {
  const players = [
    { id: 108, stats: { fielding: { byPos: { SS: entry(9, 3, 4, 0, 1), '2B': entry(3, 1, 1) } } } },
    { id: 111, stats: { fielding: { byPos: { SS: entry(6, 2, 0, 1) } } } },
    { id: 112, stats: { fielding: {} } },
  ];
  const rows = buildFieldingPositionRows(1, players, [108]);
  assert.deepEqual(rows, [
    { teamId: 1, playerId: 108, position: 'SS', outs: 9, po: 3, a: 4, e: 0, dp: 1, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: true },
    { teamId: 1, playerId: 108, position: '2B', outs: 3, po: 1, a: 1, e: 0, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: false },
    { teamId: 1, playerId: 111, position: 'SS', outs: 6, po: 2, a: 0, e: 1, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: false },
  ]);
});

test('载荷：捕手位置输出 PB/CS/SB，DP 随所属守位输出', () => {
  const players = [{
    id: 102,
    stats: { fielding: { byPos: { C: entry(9, 9, 0, 0, 0, 2, 1, 3), SS: entry(3, 2, 1, 0, 1) } } },
  }];
  assert.deepEqual(buildFieldingPositionRows(1, players, [102]), [
    { teamId: 1, playerId: 102, position: 'C', outs: 9, po: 9, a: 0, e: 0, dp: 0, pb: 2, catcherCs: 1, catcherSb: 3, fieldingGs: true },
    { teamId: 1, playerId: 102, position: 'SS', outs: 3, po: 2, a: 1, e: 0, dp: 1, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: false },
  ]);
});

test('载荷：零出局但有 E/DP 的位置保留行；旧快照缺字段按 0 展开', () => {
  const players = [
    { id: 108, stats: { fielding: { byPos: { SS: 9, '2B': entry(0, 0, 0, 1), '3B': entry(0, 0, 0, 0, 1) } } } },
    { id: 111, stats: { fielding: { byPos: { CF: 3, SS: { outs: 6, po: 2, a: 0, e: 1 } } } } },
  ];
  assert.deepEqual(buildFieldingPositionRows(1, players, [108]), [
    { teamId: 1, playerId: 108, position: 'SS', outs: 9, po: 0, a: 0, e: 0, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: true },
    { teamId: 1, playerId: 108, position: '2B', outs: 0, po: 0, a: 0, e: 1, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: false },
    { teamId: 1, playerId: 108, position: '3B', outs: 0, po: 0, a: 0, e: 0, dp: 1, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: false },
    { teamId: 1, playerId: 111, position: 'CF', outs: 3, po: 0, a: 0, e: 0, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: false },
    { teamId: 1, playerId: 111, position: 'SS', outs: 6, po: 2, a: 0, e: 1, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: false },
  ]);
});

test('载荷：仅守备投手（fpId 不在打序）也标记 GS', () => {
  const players = [{ id: 1, stats: { fielding: { byPos: { P: entry(6, 1, 0) } } } }];
  const rows = buildFieldingPositionRows(1, players, [108, 1]);
  assert.deepEqual(rows, [
    { teamId: 1, playerId: 1, position: 'P', outs: 6, po: 1, a: 0, e: 0, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0, fieldingGs: true },
  ]);
});

test('载荷：缺省/非法输入安全', () => {
  assert.deepEqual(buildFieldingPositionRows(1, null, null), []);
  assert.deepEqual(
    buildFieldingPositionRows(1, [{ id: 1, stats: { fielding: { byPos: { SS: entry(0) } } } }], []),
    []
  );
});

test('在场守备员按当前守位累计；换位后新位置继续累计', () => {
  const pitcher = { id: 1, stats: { fielding: {} } };
  const shortstop = { id: 2, position: 'SS', stats: { fielding: {} } };
  const secondBase = { id: 3, position: '2B', stats: { fielding: {} } };
  accrueActiveFieldingOuts(pitcher, [pitcher, shortstop, secondBase], (p) => p.position);
  assert.deepEqual(pitcher.stats.fielding.byPos, { P: entry(1) });
  assert.deepEqual(shortstop.stats.fielding.byPos, { SS: entry(1) });
  assert.deepEqual(secondBase.stats.fielding.byPos, { '2B': entry(1) });

  shortstop.position = '2B';
  secondBase.position = 'SS';
  accrueActiveFieldingOuts(pitcher, [shortstop, secondBase], (p) => p.position);
  assert.deepEqual(shortstop.stats.fielding.byPos, { SS: entry(1), '2B': entry(1) });
  assert.deepEqual(secondBase.stats.fielding.byPos, { '2B': entry(1), SS: entry(1) });
});

test('逐位置基线：仅当存在 byPos 才发送 payload', () => {
  assert.equal(hasFieldingByPosBaseline([{ id: 1, stats: { fielding: {} } }]), false);
  assert.equal(hasFieldingByPosBaseline([{ id: 1, stats: { fielding: { byPos: {} } } }]), true);
  assert.equal(hasFieldingByPosBaseline(null), false);
});

test('历史守备局数：Do 重建路径据此判定基线不可信', () => {
  assert.equal(hasHistoricalFieldingStats([{ defInn: 0 }, {}]), false);
  assert.equal(hasHistoricalFieldingStats([{ defInn: 1.2 }]), true);
  assert.equal(hasHistoricalFieldingStats(null), false);
});
