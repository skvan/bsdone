// 前端单测：整册电子秩序册数据装配（#310 延伸）
// 运行：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"
import test from 'node:test';
import assert from 'node:assert/strict';
import { buildOrderBookSections, summarizeOrderBook } from '../src/utils/orderBook.js';

const team = (id, extra = {}) => ({
  teamId: id,
  teamName: `球队${id}`,
  teamLogo: null,
  status: 'confirmed',
  playerCount: 0,
  rosterSubmittedAt: null,
  rosterLocked: false,
  ...extra
});

test('buildOrderBookSections：仅收录已确认球队（待确认/已谢绝被过滤），保持原列表顺序', () => {
  const sections = buildOrderBookSections([
    team(100, { status: 'invited' }),
    team(200, { rosterSubmittedAt: '2026-10-15T10:00:00' }),
    team(300, { status: 'declined' }),
    team(400, { rosterSubmittedAt: '2026-10-15T11:00:00' })
  ]);
  assert.deepEqual(sections.map((s) => s.teamId), [200, 400]);
});

test('buildOrderBookSections：已提交球队带出名单行；未提交标记占位（rows 为空）', () => {
  const sections = buildOrderBookSections(
    [
      team(200, { rosterSubmittedAt: '2026-10-15T10:00:00', playerCount: 2 }),
      team(400, { playerCount: 0 })
    ],
    { 200: { rows: [{ playerId: 1, name: '张三' }, { playerId: 2, name: '李四' }] } }
  );
  const [submitted, notSubmitted] = sections;
  assert.equal(submitted.submitted, true);
  assert.equal(submitted.rows.length, 2);
  assert.equal(submitted.rows[0].name, '张三');
  assert.equal(submitted.loadFailed, false);
  assert.equal(notSubmitted.submitted, false);
  assert.equal(notSubmitted.rows.length, 0);
});

test('buildOrderBookSections：已提交但拉取失败 → loadFailed 占位，不阻断其余球队', () => {
  const sections = buildOrderBookSections(
    [
      team(200, { rosterSubmittedAt: '2026-10-15T10:00:00' }),
      team(400, { rosterSubmittedAt: '2026-10-15T11:00:00', playerCount: 1, rosterLocked: true })
    ],
    { 200: { failed: true }, 400: { rows: [{ playerId: 3, name: '王五' }] } }
  );
  assert.equal(sections[0].loadFailed, true);
  assert.equal(sections[0].rows.length, 0);
  assert.equal(sections[1].loadFailed, false);
  assert.equal(sections[1].rows.length, 1);
});

test('buildOrderBookSections：容错空入参（null/undefined 不抛）', () => {
  assert.deepEqual(buildOrderBookSections(null), []);
  assert.deepEqual(buildOrderBookSections(undefined, null), []);
  assert.deepEqual(buildOrderBookSections([team(200)]), [{
    teamId: 200,
    teamName: '球队200',
    teamLogo: null,
    rosterLocked: false,
    playerCount: 0,
    submitted: false,
    loadFailed: false,
    rows: []
  }]);
});

test('summarizeOrderBook：球队数/已锁定数/报名球员总数（人数以球队行为准）', () => {
  const sections = buildOrderBookSections(
    [
      team(200, { rosterSubmittedAt: '2026-10-15T10:00:00', playerCount: 2, rosterLocked: true }),
      team(400, { rosterSubmittedAt: '2026-10-15T11:00:00', playerCount: 3 })
    ],
    { 200: { rows: [{ playerId: 1 }, { playerId: 2 }] }, 400: { rows: [{ playerId: 3 }, { playerId: 4 }, { playerId: 5 }] } }
  );
  assert.deepEqual(summarizeOrderBook(sections), { teams: 2, locked: 1, players: 5 });
});

test('summarizeOrderBook：空册全零', () => {
  assert.deepEqual(summarizeOrderBook([]), { teams: 0, locked: 0, players: 0 });
  assert.deepEqual(summarizeOrderBook(null), { teams: 0, locked: 0, players: 0 });
});
