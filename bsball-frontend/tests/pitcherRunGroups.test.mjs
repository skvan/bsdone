// 投手 R/ER 表格分组排序：客队在前、主队在后；队内按登板序升序，空值最后。
// 运行：node --test tests/pitcherRunGroups.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildPitcherRunTable } from '../src/utils/pitcherRunGroups.js';

const row = (playerId, teamId, pitcherOrder) => ({
  playerId,
  teamId,
  pitcherOrder,
  playerName: `P${playerId}`
});

test('客队在前、主队在后，队内按登板序升序，空登板序最后', () => {
  const { groups, tableRows } = buildPitcherRunTable([
    row(61, 3, 2),
    row(68, 3, 1),
    row(102, 5, 4),
    row(112, 5, 1),
    row(105, 5, null)
  ], {
    homeTeamId: 3,
    awayTeamId: 5,
    teamNameById: new Map([['3', '野钻'], ['5', 'Hustlers']])
  });

  assert.deepEqual(groups.map((group) => group.side), ['away', 'home']);
  assert.deepEqual(groups[0].rows.map((item) => item.playerId), [112, 102, 105]);
  assert.deepEqual(groups[1].rows.map((item) => item.playerId), [68, 61]);
  assert.deepEqual(tableRows.map((item) => item._teamRowspan), [3, 0, 0, 2, 0]);
  assert.deepEqual(tableRows.map((item) => item._teamStart), [true, false, false, true, false]);
  assert.equal(tableRows[0]._teamName, 'Hustlers');
  assert.equal(tableRows[3]._teamName, '野钻');
});

test('没有主客队信息时按球队聚合，未知球队排最后', () => {
  const { groups } = buildPitcherRunTable([
    row(1, 9, 1),
    row(2, 7, 1),
    row(3, null, null)
  ], {});

  assert.deepEqual(groups.map((group) => group.teamId), [7, 9, null]);
  assert.deepEqual(groups.map((group) => group.side), ['other', 'other', 'other']);
  assert.equal(groups[2].teamName, '待定');
});

test('登板序相同按球员 id 稳定排序，不修改原始行', () => {
  const input = [row(9, 5, 1), row(3, 5, 1)];
  const { groups } = buildPitcherRunTable(input, { awayTeamId: 5 });

  assert.deepEqual(groups[0].rows.map((item) => item.playerId), [3, 9]);
  assert.deepEqual(input.map((item) => item.playerId), [9, 3]);
});

test('空输入返回空结构', () => {
  assert.deepEqual(buildPitcherRunTable(null, {}), { groups: [], tableRows: [] });
  assert.deepEqual(buildPitcherRunTable([], {}), { groups: [], tableRows: [] });
});
