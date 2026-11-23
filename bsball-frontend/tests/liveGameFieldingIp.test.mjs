import { test } from 'node:test';
import assert from 'node:assert/strict';
import { activeFielders, addOutToInnings } from '../src/utils/liveGameFieldingIp.js';
import { inningsToOuts } from '../src/utils/inningsFormat.js';
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

test('接杀后回传触杀结算：接杀者仅记 A+1（PO 已在接杀时入账），触杀者记 PO+1', () => {
  assert.deepEqual(computeCatchBackFieldingStats('C', '2B'), [
    { pos: 'C', po: 0, a: 1 },
    { pos: '2B', po: 1, a: 0 },
  ]);
});

test('同一名守备员接杀后回传并完成触杀：结算补记 A+1 与 PO+1（连同接杀 PO 最终 2 PO 1 A）', () => {
  assert.deepEqual(computeCatchBackFieldingStats('C', 'C'), [
    { pos: 'C', po: 1, a: 1 },
  ]);
});

import {
  capFieldingByPosOuts,
  creditOutsWithinLimit,
  outsRemainingInHalfInning,
  reconcileTeamFieldingInnings,
  teamDefensiveOutsSoFar
} from '../src/utils/liveGameFieldingIp.js';

const away237 = () => ([
  { id: 164, name: '吴昌言', position: '3B', stats: { fielding: { ip: 3.2 } } },
  { id: 1, name: '吴景风', position: 'CF', stats: { fielding: { ip: 3.2 } } },
  { id: 294, name: '张森溢', position: '1B', stats: { fielding: { ip: 3.2 } } },
  { id: 2, name: '翁仲甫', position: 'C', stats: { fielding: { ip: 3.2 } } },
  { id: 295, name: '农善伟', position: 'LF', stats: { fielding: { ip: 3.2 } } },
  { id: 7, name: '岳童凯', position: 'DH', stats: { fielding: { ip: 0 } } },
  { id: 6, name: '徐丞', position: 'SS', stats: { fielding: { ip: 3.2 } } },
  { id: 12, name: '马飞龙', position: 'RF', stats: { fielding: { ip: 1.2 } } },
  { id: 298, name: '林永祥', position: '2B', stats: { fielding: { ip: 3.2 } } },
]);

test('守备出局数按攻守侧与当前出局数换算', () => {
  assert.equal(teamDefensiveOutsSoFar({ inning: 4, isTop: false, outs: 0, side: 'away' }), 9);
  assert.equal(teamDefensiveOutsSoFar({ inning: 4, isTop: false, outs: 0, side: 'home' }), 12);
  assert.equal(teamDefensiveOutsSoFar({ inning: 4, isTop: true, outs: 0, side: 'away' }), 9);
  assert.equal(teamDefensiveOutsSoFar({ inning: 4, isTop: true, outs: 2, side: 'home' }), 11);
  assert.equal(teamDefensiveOutsSoFar({ inning: 1, isTop: true, outs: 0, side: 'away' }), 0);
});

test('237 场对账：超过球队守备出局数的 3.2 回退到 3.0，低于上限的 1.2/投手/DH 不动', () => {
  const lineup = away237();
  const result = reconcileTeamFieldingInnings({
    side: 'away',
    gameState: { inning: 4, isTop: false, outs: 0 },
    lineup,
    pitcherId: 27
  });
  assert.equal(result.teamOuts, 9);
  assert.equal(result.maxOuts, 11);
  assert.equal(result.overflow, 2);
  for (const id of [164, 1, 294, 2, 295, 6, 298]) {
    assert.equal(lineup.find((player) => player.id === id).stats.fielding.ip, 3, `player ${id}`);
  }
  assert.equal(lineup.find((player) => player.id === 12).stats.fielding.ip, 1.2);
  assert.equal(lineup.find((player) => player.id === 7).stats.fielding.ip, 0);
  assert.equal(result.changed.length, 7);
});

test('客队上半局进攻时同样按已完成守备局对账', () => {
  const lineup = away237();
  const result = reconcileTeamFieldingInnings({
    side: 'away',
    gameState: { inning: 4, isTop: true, outs: 0 },
    lineup,
    pitcherId: 27
  });
  assert.equal(result.teamOuts, 9);
  assert.equal(result.overflow, 2);
  assert.equal(lineup.find((player) => player.id === 164).stats.fielding.ip, 3);
  assert.equal(lineup.find((player) => player.id === 12).stats.fielding.ip, 1.2);
});

test('正常半局不误扣：当前 2 出局时野手 0.2 保持不变', () => {
  const lineup = [
    { id: 1, name: 'A', position: 'SS', stats: { fielding: { ip: 0.2 } } },
    { id: 2, name: 'B', position: 'RF', stats: { fielding: { ip: 0.2 } } }
  ];
  const result = reconcileTeamFieldingInnings({
    side: 'away',
    gameState: { inning: 1, isTop: false, outs: 2 },
    lineup,
    pitcherId: 9
  });
  assert.equal(result.overflow, 0);
  assert.equal(lineup[0].stats.fielding.ip, 0.2);
  assert.equal(lineup[1].stats.fielding.ip, 0.2);
});

test('对账只减不增，且溢出不扣到投手行', () => {
  const lineup = [
    { id: 1, name: 'SS', position: 'SS', stats: { fielding: { ip: 2.2 } } },
    { id: 9, name: 'P', position: 'P', stats: { fielding: { ip: 2 } } }
  ];
  const result = reconcileTeamFieldingInnings({
    side: 'away',
    gameState: { inning: 3, isTop: false, outs: 0 },
    lineup,
    pitcherId: 9
  });
  assert.equal(result.overflow, 2);
  assert.equal(lineup[0].stats.fielding.ip, 2);
  assert.equal(lineup[1].stats.fielding.ip, 2);
});

test('撤销栈混合快照（3 局下 0 出局、野手 2.2）对账回 2.0，低局数球员不动', () => {
  const lineup = away237().map((player) => ({
    ...player,
    stats: { fielding: { ip: player.id === 12 ? 0.2 : player.position === 'DH' ? 0 : 2.2 } }
  }));
  const result = reconcileTeamFieldingInnings({
    side: 'away',
    gameState: { inning: 3, isTop: false, outs: 0 },
    lineup,
    pitcherId: 27
  });
  assert.equal(result.teamOuts, 6);
  assert.equal(result.overflow, 2);
  for (const id of [164, 1, 294, 2, 295, 6, 298]) {
    assert.equal(lineup.find((player) => player.id === id).stats.fielding.ip, 2, `player ${id}`);
  }
  assert.equal(lineup.find((player) => player.id === 12).stats.fielding.ip, 0.2);
});

// #699 防守局数 4.1 异常：本半局出局上限 + 逐位置出局同步收敛
const emptyPos = (outs) => ({ outs, po: 0, a: 0, e: 0, dp: 0, pb: 0, catcherCs: 0, catcherSb: 0 });

test('#699 本半局剩余出局：三出局后不再累计守备局数', () => {
  assert.equal(outsRemainingInHalfInning(0), 3);
  assert.equal(outsRemainingInHalfInning(1), 2);
  assert.equal(outsRemainingInHalfInning(2), 1);
  assert.equal(outsRemainingInHalfInning(3), 0);
  assert.equal(outsRemainingInHalfInning(9), 0);
  // 2 出局双杀只记 1 个；1 出局三杀只记 2 个；0 出局双杀照常记 2 个
  assert.equal(creditOutsWithinLimit(2, 2), 1);
  assert.equal(creditOutsWithinLimit(3, 1), 2);
  assert.equal(creditOutsWithinLimit(2, 0), 2);
  assert.equal(creditOutsWithinLimit(1, 3), 0);
});

test('#699 逐位置出局按球队合法出局数截断，保留逐位置结构', () => {
  // 单个球员全场逐位置合计不会超过球队守备出局数；多守位时合计多记 1 个
  const fielding = { byPos: { P: emptyPos(7), SS: emptyPos(6) } };
  const result = capFieldingByPosOuts(fielding, 12);
  assert.equal(result.before, 13);
  assert.equal(result.after, 12);
  assert.equal(result.removed, 1);
  assert.equal(fielding.byPos.P.outs, 7);
  assert.equal(fielding.byPos.SS.outs, 5);
  // 旧快照数字值：需要扣减时转成标准对象
  const legacy = { byPos: { SS: 13 } };
  const legacyResult = capFieldingByPosOuts(legacy, 12);
  assert.equal(legacyResult.removed, 1);
  assert.equal(legacy.byPos.SS.outs, 12);
  assert.equal(legacy.byPos.SS.po, 0);
});

test('#699 对账：多记的出局同时从 byPos、fielding.ip、pitching.ip 扣回（4.1 → 4）', () => {
  const lineup = [
    { id: 9, name: '投手', position: 'P', stats: { fielding: { ip: 4.1, byPos: { P: emptyPos(13) } }, pitching: { ip: 4.1 } } },
    { id: 1, name: '游击', position: 'SS', stats: { fielding: { ip: 4.1, byPos: { SS: emptyPos(13) } } } },
    { id: 7, name: 'DH', position: 'DH', stats: { fielding: { ip: 0 } } }
  ];
  // 主队第 4 局下半结束（12 个守备出局）后直接结束：第 5 局上半、0 出局
  const result = reconcileTeamFieldingInnings({
    side: 'home',
    gameState: { inning: 5, isTop: true, outs: 0 },
    lineup,
    pitcherId: 9
  });
  assert.equal(result.teamOuts, 12);
  assert.equal(lineup[0].stats.fielding.byPos.P.outs, 12);
  assert.equal(lineup[1].stats.fielding.byPos.SS.outs, 12);
  assert.equal(lineup[0].stats.fielding.ip, 4);
  assert.equal(lineup[1].stats.fielding.ip, 4);
  assert.equal(lineup[0].stats.pitching.ip, 4);
  assert.equal(inningsToOuts(lineup[0].stats.fielding.ip), 12);
  assert.equal(inningsToOuts(lineup[1].stats.fielding.ip), 12);
  assert.ok(result.changed.some((item) => item.kind === 'byPos' && item.playerId === 1));
  assert.ok(result.changed.some((item) => item.kind === 'pitching.ip' && item.playerId === 9));
});

test('#699 对账：DH 制只守备投手不在打线时也收敛 P 行与投球局数', () => {
  const lineup = [
    { id: 1, name: '游击', position: 'SS', stats: { fielding: { ip: 4.1, byPos: { SS: emptyPos(13) } } } }
  ];
  const fpRow = {
    id: 27, name: '投手', position: 'P',
    stats: { fielding: { ip: 4.1, byPos: { P: emptyPos(13) } }, pitching: { ip: 4.1 } }
  };
  reconcileTeamFieldingInnings({
    side: 'home',
    gameState: { inning: 5, isTop: true, outs: 0 },
    lineup,
    pitcherId: 27,
    extraPlayers: [fpRow]
  });
  assert.equal(fpRow.stats.fielding.byPos.P.outs, 12);
  assert.equal(fpRow.stats.fielding.ip, 4);
  assert.equal(fpRow.stats.pitching.ip, 4);
});

test('#699 对账：合法范围内的 byPos / IP 保持不变', () => {
  const lineup = [
    { id: 1, name: '游击', position: 'SS', stats: { fielding: { ip: 4, byPos: { SS: emptyPos(12) } } } },
    { id: 9, name: '投手', position: 'P', stats: { fielding: { ip: 3.1, byPos: { P: emptyPos(10) } }, pitching: { ip: 3.1 } } }
  ];
  const result = reconcileTeamFieldingInnings({
    side: 'home',
    gameState: { inning: 5, isTop: true, outs: 0 },
    lineup,
    pitcherId: 9
  });
  assert.equal(result.changed.length, 0);
  assert.equal(lineup[0].stats.fielding.ip, 4);
  assert.equal(lineup[1].stats.fielding.ip, 3.1);
  assert.equal(lineup[1].stats.pitching.ip, 3.1);
});

test('后上场球员合法局数低于球队守备出局数时不被误扣', () => {
  const lineup = [
    { id: 1, name: '打满', position: 'SS', stats: { fielding: { ip: 3.2 } } },
    { id: 2, name: '后上', position: 'RF', stats: { fielding: { ip: 0.1 } } },
    { id: 9, name: '投手', position: 'P', stats: { fielding: { ip: 3.2 } } }
  ];
  const result = reconcileTeamFieldingInnings({
    side: 'away',
    gameState: { inning: 4, isTop: false, outs: 0 },
    lineup,
    pitcherId: null
  });
  assert.equal(result.teamOuts, 9);
  assert.equal(lineup[0].stats.fielding.ip, 3);
  assert.equal(lineup[1].stats.fielding.ip, 0.1);
  assert.equal(lineup[2].stats.fielding.ip, 3.2);
  assert.equal(result.changed.length, 1);
});
