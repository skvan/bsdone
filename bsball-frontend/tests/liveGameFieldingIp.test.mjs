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
