// 打击落点回放打者/球队选项（#717）
// 运行：node --test tests/gameDetailHitReplayBatters.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildBatterOptions,
  buildTeamOptions,
  playerDisplayName,
  resolveRecordTeamId,
} from '../src/utils/gameDetailHitReplayBatters.js';

const TEAMS_282 = [
  { id: 1, name: '暴力羊' },
  { id: 6, name: '客队' },
];

function stat(teamId, playerId, battingOrder, listOrder, extra = {}) {
  return {
    teamId,
    playerId,
    battingOrder,
    listOrder,
    isPitcher: 0,
    pa: 0,
    h: 0,
    ...extra,
  };
}

// 生产 game 282（2026-11-22 快照）：主队 15 行（含 2 投手）、客队 13 行（含 3 投手）
const STATS_282 = [
  stat(1, 84, 1, 0, { pa: 3 }),
  stat(1, 26, null, 0, { isPitcher: 1 }),
  stat(1, 21, null, 1, { isPitcher: 1 }),
  stat(1, 1406, null, 1),
  stat(1, 1, 2, 2, { pa: 3 }),
  stat(1, 23, 3, 3, { pa: 2, h: 1 }),
  stat(1, 16, null, 4, { pa: 1 }),
  stat(1, 3, 4, 5, { pa: 2 }),
  stat(1, 5, null, 6, { pa: 1 }),
  stat(1, 2, 5, 7, { pa: 3 }),
  stat(1, 6, 6, 8, { pa: 2 }),
  stat(1, 22, 7, 9, { pa: 2 }),
  stat(1, 8, 8, 10, { pa: 1 }),
  stat(1, 7, null, 11, { pa: 1 }),
  stat(1, 14, 9, 12, { pa: 2 }),
  stat(6, 164, 1, 0, { pa: 3, h: 1 }),
  stat(6, 334, null, 0, { isPitcher: 1 }),
  stat(6, 179, 2, 1, { pa: 3 }),
  stat(6, 333, null, 1, { isPitcher: 1 }),
  stat(6, 153, 3, 2, { pa: 2 }),
  stat(6, 167, null, 2, { isPitcher: 1 }),
  stat(6, 166, null, 3, { pa: 1 }),
  stat(6, 136, 4, 4, { pa: 3 }),
  stat(6, 1362, 5, 5, { pa: 3, h: 1 }),
  stat(6, 152, 6, 6, { pa: 3 }),
  stat(6, 329, 7, 7, { pa: 3, h: 1 }),
  stat(6, 143, 8, 8, { pa: 2 }),
  stat(6, 135, 9, 9, { pa: 2, h: 1 }),
];

// 按打席键保留最新后的 20 条落点（历史 teamId=0，靠 half 推导球队）
const RECORDS_282 = [
  { id: 2027, teamId: 0, half: 'top', inning: 1, batterOrder: 1, playerId: 164 },
  { id: 2028, teamId: 0, half: 'top', inning: 1, batterOrder: 2, playerId: 179 },
  { id: 2040, teamId: 0, half: 'bottom', inning: 1, batterOrder: 4, playerId: 3 },
  { id: 2041, teamId: 0, half: 'bottom', inning: 1, batterOrder: 5, playerId: 2 },
  { id: 2056, teamId: 0, half: 'top', inning: 2, batterOrder: 7, playerId: 329 },
  { id: 2057, teamId: 0, half: 'top', inning: 2, batterOrder: 8, playerId: 143 },
  { id: 2058, teamId: 0, half: 'top', inning: 2, batterOrder: 9, playerId: 135 },
  { id: 2059, teamId: 0, half: 'top', inning: 2, batterOrder: 2, playerId: 179 },
  { id: 2060, teamId: 0, half: 'top', inning: 2, batterOrder: 3, playerId: 153 },
  { id: 2061, teamId: 0, half: 'top', inning: 2, batterOrder: 5, playerId: 1362 },
  { id: 2067, teamId: 0, half: 'bottom', inning: 2, batterOrder: 7, playerId: 22 },
  { id: 2259, teamId: 0, half: 'bottom', inning: 2, batterOrder: 3, playerId: 23 },
  { id: 2260, teamId: 0, half: 'top', inning: 3, batterOrder: 7, playerId: 329 },
  { id: 2261, teamId: 0, half: 'top', inning: 3, batterOrder: 9, playerId: 135 },
  { id: 2262, teamId: 0, half: 'top', inning: 3, batterOrder: 1, playerId: 164 },
  { id: 2263, teamId: 0, half: 'bottom', inning: 3, batterOrder: 6, playerId: 6 },
  { id: 2265, teamId: 0, half: 'top', inning: 4, batterOrder: 2, playerId: 179 },
  { id: 2266, teamId: 0, half: 'top', inning: 4, batterOrder: 5, playerId: 1362 },
  { id: 2268, teamId: 0, half: 'bottom', inning: 4, batterOrder: 2, playerId: 1 },
  { id: 2269, teamId: 0, half: 'bottom', inning: 4, batterOrder: 5, playerId: 2 },
];

function options282(teamKey) {
  return buildBatterOptions({
    stats: STATS_282,
    records: RECORDS_282,
    playersMap: {},
    teams: TEAMS_282,
    teamKey,
    homeTeamId: 1,
    awayTeamId: 6,
  });
}

function toMap(options) {
  return Object.fromEntries(options.map((option) => [option.key, option]));
}

test('282 全队合并：主队 13 项、客队 10 项，投手排除且带队名前缀', () => {
  const options = options282('all');
  assert.equal(options.length, 23);
  assert.equal(options.filter((o) => o.teamId === 1).length, 13);
  assert.equal(options.filter((o) => o.teamId === 6).length, 10);

  const map = toMap(options);
  assert.equal(map['1:21'], undefined, '主队投手 21 应排除');
  assert.equal(map['1:26'], undefined, '主队投手 26 应排除');
  assert.equal(map['6:334'], undefined, '客队投手 334 应排除');
  assert.equal(map['6:333'], undefined, '客队投手 333 应排除');
  assert.equal(map['6:167'], undefined, '客队投手 167 应排除');

  assert.match(map['1:23'].label, /^\[暴力羊\] 第3棒 /);
  assert.match(map['6:164'].label, /^\[客队\] 第1棒 /);
});

test('282 主队：替补按 listOrder 继承最近锚点棒次，首发无落点仍出现', () => {
  const options = options282('1');
  assert.equal(options.length, 13);
  assert.deepEqual(
    options.map((o) => o.battingOrder),
    [1, 1, 2, 3, 3, 4, 4, 5, 6, 7, 8, 8, 9],
  );

  const map = toMap(options);
  assert.equal(map['1:1406'].battingOrder, 1);
  assert.equal(map['1:16'].battingOrder, 3);
  assert.equal(map['1:5'].battingOrder, 4);
  assert.equal(map['1:7'].battingOrder, 8);
  for (const key of ['1:1406', '1:16', '1:5', '1:7']) {
    assert.equal(map[key].substitute, true, `${key} 应标记替补`);
    assert.match(map[key].label, /（替补）/);
  }
  // 同棒次先发在前、替补在后
  assert.equal(options[0].playerId, 84);
  assert.equal(options[1].playerId, 1406);

  // 首发 84/8/14 即使 0 落点也出现，并标注“无落点”
  for (const key of ['1:84', '1:8', '1:14']) {
    assert.equal(map[key].recordCount, 0);
    assert.match(map[key].label, /（无落点）/);
  }
  // 2 局下 3 棒 23 只有 1 条落点
  assert.equal(map['1:23'].recordCount, 1);
  assert.match(map['1:23'].label, /第3棒/);
});

test('282 客队：10 项且替补 166 归入第3棒（先发中外野 153 的槽位）', () => {
  const options = options282('6');
  assert.equal(options.length, 10);
  const map = toMap(options);
  assert.equal(map['6:166'].battingOrder, 3);
  assert.equal(map['6:166'].substitute, true);
  assert.equal(map['6:164'].recordCount, 2);
  assert.equal(map['6:179'].recordCount, 3);
  assert.equal(map['6:1362'].recordCount, 2);
});

test('216 场次：battingOrder 10/11（EP/DH）保留在统计来源中', () => {
  const stats = [];
  for (let i = 1; i <= 9; i += 1) stats.push(stat(1, 100 + i, i, i - 1, { pa: 3 }));
  stats.push(stat(1, 201, 10, 9, { pa: 2 }));
  stats.push(stat(1, 202, 11, 10, { pa: 2 }));

  const options = buildBatterOptions({
    stats, records: [], playersMap: {}, teams: TEAMS_282,
    teamKey: '1', homeTeamId: 1, awayTeamId: 6,
  });

  const map = toMap(options);
  assert.equal(options.length, 11);
  assert.equal(map['1:201'].battingOrder, 10);
  assert.equal(map['1:202'].battingOrder, 11);
});

test('261 场次：同槽两个替补都保留，并按 listOrder 排序', () => {
  const stats = [
    stat(1, 1, 1, 0),
    stat(1, 2, 2, 1),
    stat(1, 3, 3, 2),
    stat(1, 4, 4, 3),
    stat(1, 61, null, 4),
    stat(1, 67, null, 5),
    stat(1, 5, 5, 6),
  ];

  const options = buildBatterOptions({
    stats, records: [], playersMap: {}, teams: TEAMS_282,
    teamKey: '1', homeTeamId: 1, awayTeamId: 6,
  });

  const subs = options.filter((o) => o.substitute);
  assert.deepEqual(subs.map((o) => o.playerId), [61, 67]);
  assert.deepEqual(subs.map((o) => o.battingOrder), [4, 4]);
});

test('记录 batterOrder 优先于 stats.battingOrder（记录是实际打席事实）', () => {
  const stats = [
    stat(1, 900, 3, 0, { pa: 3 }),
    stat(1, 903, null, 1, { pa: 1 }),
    stat(1, 901, 4, 2, { pa: 3 }),
  ];
  // 记录显示 900 实际打第 2 棒，与 stats 的第 3 棒冲突
  const records = [
    { teamId: 0, half: 'bottom', inning: 1, batterOrder: 2, playerId: 900 },
  ];

  const options = buildBatterOptions({
    stats, records, playersMap: {}, teams: TEAMS_282,
    teamKey: '1', homeTeamId: 1, awayTeamId: 6,
  });

  const map = toMap(options);
  assert.equal(map['1:900'].battingOrder, 2, '应以记录 batterOrder=2 修正 stats=3');
  assert.match(map['1:900'].label, /第2棒/);
  // 记录修正后的棒次同样作为替补继承锚点
  assert.equal(map['1:903'].battingOrder, 2);
  assert.equal(map['1:901'].battingOrder, 4);
  assert.deepEqual(options.map((o) => o.battingOrder), [2, 2, 4]);
});

test('二刀流/取消 DH：isPitcher=1 但 pa>0 或 battingOrder 非空的球员保留', () => {
  const stats = [
    stat(1, 500, 7, 2, { isPitcher: 1, pa: 2 }),
    stat(1, 501, null, 3, { isPitcher: 1, pa: 0 }),
    stat(1, 502, 8, 4),
    stat(1, 503, 9, 5, { isPitcher: 1 }),
  ];

  const map = toMap(buildBatterOptions({
    stats, records: [], playersMap: {}, teams: TEAMS_282,
    teamKey: '1', homeTeamId: 1, awayTeamId: 6,
  }));

  assert.ok(map['1:500'], '投手有打席应保留');
  assert.equal(map['1:501'], undefined, '未上场且无落点的投手应排除');
  assert.ok(map['1:503'], '投手带 battingOrder 应保留');
});

test('stats 为空时退回由落点记录反推（不白屏、不标无落点）', () => {
  const records = [
    { teamId: 0, half: 'bottom', batterOrder: 4, playerId: 9 },
    { teamId: 0, half: 'top', batterOrder: 2, playerId: 10 },
  ];
  const playersMap = { '1:9': { name: '甲' }, '6:10': { name: '乙' } };

  const options = buildBatterOptions({
    stats: [], records, playersMap, teams: TEAMS_282,
    teamKey: 'all', homeTeamId: 1, awayTeamId: 6,
  });

  const map = toMap(options);
  assert.equal(options.length, 2);
  assert.equal(map['1:9'].battingOrder, 4);
  assert.equal(map['6:10'].battingOrder, 2);
  assert.match(map['1:9'].label, /第4棒 甲/);
  assert.ok(!map['1:9'].label.includes('无落点'));
});

test('球队下拉：props.teams 优先，某队 0 落点也可选；缺失时用记录兜底', () => {
  const fromTeams = buildTeamOptions({ teams: TEAMS_282, records: [], homeTeamId: 1, awayTeamId: 6 });
  assert.deepEqual(fromTeams.map((t) => t.id), [1, 6]);

  const fromRecords = buildTeamOptions({
    teams: [],
    records: [
      { teamId: 0, half: 'bottom' },
      { teamId: 0, half: 'top' },
    ],
    homeTeamId: 1,
    awayTeamId: 6,
  });
  assert.deepEqual(fromRecords.map((t) => t.id), [1, 6]);
});

test('记录球队解析：自身 teamId 优先，teamId=0 时按上下半局推导', () => {
  assert.equal(resolveRecordTeamId({ teamId: 5, half: 'top' }, 1, 6), 5);
  assert.equal(resolveRecordTeamId({ teamId: 0, half: 'top' }, 1, 6), 6);
  assert.equal(resolveRecordTeamId({ teamId: 0, half: 'bottom' }, 1, 6), 1);
  assert.equal(resolveRecordTeamId({ teamId: null, half: 'bottom' }, 1, 6), 1);
});

test('球员姓名：复合键优先，回退单键，再回退球员#ID', () => {
  assert.equal(playerDisplayName({ '1:9': { name: '甲' } }, 1, 9), '甲');
  assert.equal(playerDisplayName({ 9: { name: '乙' } }, 1, 9), '乙');
  assert.equal(playerDisplayName({}, 1, 9), '球员#9');
});
