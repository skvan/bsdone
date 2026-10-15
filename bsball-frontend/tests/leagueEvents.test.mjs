// 单测：联盟下辖赛事过滤（Issue #329）
// 运行：node --test tests/leagueEvents.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { filterEventsByLeague } from '../src/utils/leagueEvents.js';

test('按 leagueId 过滤：数字与字符串归一比对', () => {
  const events = [
    { id: 1, leagueId: 3, startDate: '2026-03-01' },
    { id: 2, leagueId: '3', startDate: '2026-05-01' },
    { id: 3, leagueId: 4, startDate: '2026-04-01' },
    { id: 4, leagueId: null, startDate: '2026-06-01' }
  ];
  const got = filterEventsByLeague(events, '3');
  assert.deepEqual(got.map((e) => e.id), [2, 1]);
  assert.deepEqual(filterEventsByLeague(events, 4).map((e) => e.id), [3]);
});

test('按开始日期倒序；缺 startDate 视为最小排在最后', () => {
  const events = [
    { id: 1, leagueId: 1, startDate: '2026-01-10' },
    { id: 2, leagueId: 1, startDate: '2026-03-10' },
    { id: 3, leagueId: 1 },
    { id: 4, leagueId: 1, startDate: '2025-12-31' }
  ];
  assert.deepEqual(filterEventsByLeague(events, 1).map((e) => e.id), [2, 1, 4, 3]);
});

test('不改动原数组（仅返回过滤副本）', () => {
  const events = [
    { id: 1, leagueId: 1, startDate: '2026-01-01' },
    { id: 2, leagueId: 2, startDate: '2026-02-01' }
  ];
  const snapshot = JSON.stringify(events);
  filterEventsByLeague(events, 1);
  assert.equal(JSON.stringify(events), snapshot);
});

test('边界：非法 leagueId（null/undefined/空串/0/负数/非数）返回空数组', () => {
  const events = [{ id: 1, leagueId: 1, startDate: '2026-01-01' }];
  for (const bad of [null, undefined, '', 0, -1, 'abc', NaN]) {
    assert.deepEqual(filterEventsByLeague(events, bad), [], `leagueId=${String(bad)} 应返回空数组`);
  }
});

test('边界：非数组/空数组输入返回空数组', () => {
  assert.deepEqual(filterEventsByLeague(null, 1), []);
  assert.deepEqual(filterEventsByLeague(undefined, 1), []);
  assert.deepEqual(filterEventsByLeague([], 1), []);
  assert.deepEqual(filterEventsByLeague('not-array', 1), []);
});

test('剔除缺 leagueId 的赛事；leagueId 为空的项不参与匹配', () => {
  const events = [
    { id: 1, startDate: '2026-01-01' },
    { id: 2, leagueId: '', startDate: '2026-01-02' },
    { id: 3, leagueId: 2, startDate: '2026-01-03' }
  ];
  assert.deepEqual(filterEventsByLeague(events, 2).map((e) => e.id), [3]);
});
