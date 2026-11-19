// 结果录入守备合并口径（Issue #701）：人工修订的球员级值不能被逐位置合计回滚。
// 运行：node --test tests/gameResultFieldingMerge701.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  hasResultFieldingActivity,
  mergeResultFieldingRows
} from '../src/utils/gameResultFieldingMerge.js';
import { createBatterStatDefaults } from '../src/utils/gamePlayerStat.js';

const games = fs.readFileSync(new URL('../src/views/admin/Games.js', import.meta.url), 'utf8');

const TEAM_ID = 1;

function buildRows(rows) {
  return rows.map((row, index) => ({ _rowId: index + 1, teamId: TEAM_ID, ...row }));
}

function buildContext({ rows, positions, stats, statPlayerIds }) {
  const statsByKey = {};
  for (const row of rows) {
    const key = `k_${row.id}_${row._rowId}`;
    if (stats[row.id]) statsByKey[key] = { ...createBatterStatDefaults, ...stats[row.id] };
  }
  return {
    teamId: TEAM_ID,
    rows,
    positionRows: positions,
    statsByKey,
    hasPlayerStatRow: (playerId) => statPlayerIds.includes(Number(playerId)),
    isPitcherRow: (row) => Number(row.isPitcher) === 1,
    keyOf: (row) => `k_${row.id}_${row._rowId}`,
    createFieldingOnlyRow: (playerId) => ({
      id: playerId,
      name: `#${playerId}`,
      teamId: TEAM_ID,
      _rowId: 99,
      _fieldingOnly: true
    })
  };
}

test('球员级守备已有数据：逐位置合计不得覆盖人工修订值', () => {
  const rows = buildRows([{ id: 14, name: '梁大鹏', position: 'SS', isPitcher: 0 }]);
  const stats = {
    14: { position: 'SS', firstPosition: 'SS', battingOrder: 2, listOrder: 1, po: 1, a: 0, e: 1, defInn: 4 }
  };
  const ctx = buildContext({
    rows,
    positions: [{ teamId: TEAM_ID, playerId: 14, position: 'SS', po: 0, a: 0, e: 2, outs: 12 }],
    stats,
    statPlayerIds: [14]
  });
  mergeResultFieldingRows(ctx);
  const merged = ctx.statsByKey[ctx.keyOf(rows[0])];
  assert.equal(merged.po, 1, '人工修订的 PO 不应被逐位置合计(0)覆盖');
  assert.equal(merged.e, 1, '人工修订的 E 不应被逐位置合计(2)覆盖');
  assert.equal(rows[0]._fieldingOnly, undefined, '有统计行的球员不应变成守备专用行');
});

test('球员级守备全零（旧数据）：用逐位置合计补全，保留 #560 显示', () => {
  const rows = buildRows([{ id: 44, name: '姚智鹏', position: 'P', isPitcher: 1 }]);
  const stats = {
    44: { position: 'P', firstPosition: 'P', po: 0, a: 0, e: 0, defInn: 0 }
  };
  const ctx = buildContext({
    rows,
    positions: [{ teamId: TEAM_ID, playerId: 44, position: 'P', po: 0, a: 2, e: 0, outs: 12, fieldingGs: true }],
    stats,
    statPlayerIds: [44]
  });
  mergeResultFieldingRows(ctx);
  const merged = ctx.statsByKey[ctx.keyOf(rows[0])];
  assert.equal(merged.a, 2, '全零统计行应由逐位置合计补 A');
  assert.equal(merged.defInn, 4, '全零统计行应由逐位置 outs 补 INN');
  assert.equal(merged.fieldingGs, 1);
});

test('仅存在于逐位置表的球员：补守备专用行并写入合计', () => {
  const rows = buildRows([{ id: 44, name: '姚智鹏', position: 'P', isPitcher: 1 }]);
  const ctx = buildContext({
    rows,
    positions: [{ teamId: TEAM_ID, playerId: 164, position: '1B', po: 10, a: 1, e: 0, dp: 1, outs: 12, fieldingGs: true }],
    stats: {},
    statPlayerIds: []
  });
  mergeResultFieldingRows(ctx);
  const added = rows.find((row) => row.id === 164);
  assert.ok(added, '仅逐位置存在的球员应补一行');
  assert.equal(added._fieldingOnly, true);
  assert.equal(added._fieldPosition, '1B');
  const merged = ctx.statsByKey[ctx.keyOf(added)];
  assert.deepEqual(
    { po: merged.po, a: merged.a, e: merged.e, dp: merged.dp, defInn: merged.defInn },
    { po: 10, a: 1, e: 0, dp: 1, defInn: 4 }
  );
});

test('人工值与逐位置合计一致时照常显示，并保留首个守位', () => {
  const rows = buildRows([{ id: 21, name: '王译圣', position: 'P', firstPosition: '3B', isPitcher: 0 }]);
  const stats = {
    21: { position: 'P', firstPosition: '3B', po: 1, a: 2, e: 0, defInn: 4 }
  };
  const ctx = buildContext({
    rows,
    positions: [
      { teamId: TEAM_ID, playerId: 21, position: '3B', po: 1, a: 2, e: 0, outs: 9 },
      { teamId: TEAM_ID, playerId: 21, position: 'P', po: 0, a: 0, e: 0, outs: 3 }
    ],
    stats,
    statPlayerIds: [21]
  });
  mergeResultFieldingRows(ctx);
  assert.equal(rows[0]._fieldPosition, '3B', 'POS 应优先 firstPosition');
  const merged = ctx.statsByKey[ctx.keyOf(rows[0])];
  assert.equal(merged.po, 1);
  assert.equal(merged.a, 2);
});

test('hasResultFieldingActivity：只认计数据，自动派生的 INN 不算人工修订痕迹', () => {
  assert.equal(hasResultFieldingActivity({}), false);
  assert.equal(hasResultFieldingActivity({ po: 0, a: 0, e: 0, defInn: 0 }), false);
  assert.equal(hasResultFieldingActivity({ e: 1 }), true);
  assert.equal(hasResultFieldingActivity({ defInn: 4 }), false, 'INN 由逐位置自动派生，不能当人工值证据');
});

test('统计行只有 INN、计数据全零：仍用逐位置合计补全，避免旧数据被清零', () => {
  const rows = buildRows([{ id: 44, name: '姚智鹏', position: 'P', isPitcher: 1 }]);
  const stats = {
    44: { position: 'P', firstPosition: 'P', po: 0, a: 0, e: 0, defInn: 4 }
  };
  const ctx = buildContext({
    rows,
    positions: [{ teamId: TEAM_ID, playerId: 44, position: 'P', po: 0, a: 2, e: 0, outs: 12, fieldingGs: true }],
    stats,
    statPlayerIds: [44]
  });
  mergeResultFieldingRows(ctx);
  const merged = ctx.statsByKey[ctx.keyOf(rows[0])];
  assert.equal(merged.a, 2, '计数据全零时必须回落到逐位置合计');
  assert.equal(merged.defInn, 4, '球员级 INN 已有值时应保留，不被逐位置重算覆盖');
});

test('统计行只人工改了 INN、计数据全零：INN 不被逐位置旧值回滚', () => {
  const rows = buildRows([{ id: 45, name: '野手', position: '1B', isPitcher: 0 }]);
  const stats = {
    45: { position: '1B', firstPosition: '1B', battingOrder: 3, listOrder: 2, po: 0, a: 0, e: 0, defInn: 5 }
  };
  const ctx = buildContext({
    rows,
    positions: [{ teamId: TEAM_ID, playerId: 45, position: '1B', po: 0, a: 0, e: 0, outs: 12 }],
    stats,
    statPlayerIds: [45]
  });
  mergeResultFieldingRows(ctx);
  const merged = ctx.statsByKey[ctx.keyOf(rows[0])];
  assert.equal(merged.defInn, 5, '人工修订的 INN 必须保留');
});

test('结果录入页接入合并工具，并装载首守位与投手行守备合计', () => {
  assert.ok(games.includes('mergeResultFieldingRows({'), '结果录入页未调用合并工具');
  assert.ok(games.includes('hasPlayerStatRow:playerId=>p.some('), '未按球员级统计行判定权威来源');
  assert.ok(games.includes('k.firstPosition=v.firstPosition'), '球员统计行未装载首守位');
  assert.ok(games.includes('fd.firstPosition=v.firstPosition'), '投手行未装载首守位');
  assert.ok(games.includes('fd.po=v.po??0'), '投手行守备合计未装载进结果页统计对象');
});
