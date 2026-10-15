// 前端单测：整册电子秩序册数据装配（#310 延伸）
// 运行：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"
import test from 'node:test';
import assert from 'node:assert/strict';
import { buildOrderBookSections, summarizeOrderBook, buildFlatOrderBookPlayers } from '../src/utils/orderBook.js';

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

test('buildOrderBookSections：已提交球队带出在役名单行；未提交标记占位（rows 为空）', () => {
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

test('buildOrderBookSections：已离队球员不展示（rows 与人数均剔除），仅显示在役', () => {
  const sections = buildOrderBookSections(
    [team(200, { rosterSubmittedAt: '2026-10-15T10:00:00', playerCount: 3 })],
    {
      200: {
        rows: [
          { playerId: 1, name: '张三', currentInTeam: true },
          { playerId: 2, name: '李四', currentInTeam: false },
          { playerId: 3, name: '王五' }
        ]
      }
    }
  );
  const [s] = sections;
  assert.deepEqual(s.rows.map((r) => r.playerId), [1, 3], '显式离队被剔除；缺省视为在役');
  assert.equal(s.playerCount, 2, '人数与展示口径一致（仅在役）');
  assert.deepEqual(summarizeOrderBook(sections), { teams: 1, locked: 0, players: 2 });
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

test('summarizeOrderBook：球队数/已锁定数/在役报名球员总数（与分节人数同口径）', () => {
  const sections = buildOrderBookSections(
    [
      team(200, { rosterSubmittedAt: '2026-10-15T10:00:00', rosterLocked: true }),
      team(400, { rosterSubmittedAt: '2026-10-15T11:00:00' })
    ],
    { 200: { rows: [{ playerId: 1 }, { playerId: 2 }] }, 400: { rows: [{ playerId: 3 }, { playerId: 4 }, { playerId: 5 }] } }
  );
  assert.deepEqual(summarizeOrderBook(sections), { teams: 2, locked: 1, players: 5 });
});

test('summarizeOrderBook：空册全零', () => {
  assert.deepEqual(summarizeOrderBook([]), { teams: 0, locked: 0, players: 0 });
  assert.deepEqual(summarizeOrderBook(null), { teams: 0, locked: 0, players: 0 });
});

test('buildFlatOrderBookPlayers：跨队合并同一球员为一行，球队列完整列出全部队名、背号去重拼接', () => {
  const sections = [
    {
      teamId: 200,
      teamName: '雷霆队',
      rows: [{ playerId: 1, name: '张三', number: '7', positions: ['P'], avatar: '/a.png', currentInTeam: true }, { playerId: 2, name: '李四', number: '', positions: ['C', '1B'] }]
    },
    {
      teamId: 400,
      teamName: '疾风队',
      rows: [{ playerId: 1, name: '张三', number: '11', positions: ['P', 'OF'], avatar: '/a.png' }]
    }
  ];
  const flat = buildFlatOrderBookPlayers(sections);
  assert.equal(flat.length, 2, '张三跨队合并为一行');
  const zhangsan = flat.find((p) => p.playerId === 1);
  assert.equal(zhangsan.teamText, '雷霆队、疾风队');
  assert.equal(zhangsan.numberText, '7 / 11');
  assert.equal(zhangsan.positionText, 'P / OF', '位置去重后拼接');
  assert.equal(zhangsan.avatar, '/a.png');
  const lisi = flat.find((p) => p.playerId === 2);
  assert.equal(lisi.numberText, '', '无背号为空串（渲染层回落 —）');
});

test('buildFlatOrderBookPlayers：按姓名排序（中文拼音序）；空入参容错', () => {
  const sections = [
    { teamId: 200, teamName: 'A队', rows: [{ playerId: 1, name: '王五' }, { playerId: 2, name: '张三' }, { playerId: 3, name: '李四' }] }
  ];
  const names = buildFlatOrderBookPlayers(sections).map((p) => p.name);
  assert.deepEqual(names, ['李四', '王五', '张三'], '中文按拼音序（李 l < 王 w < 张 z）');
  assert.deepEqual(buildFlatOrderBookPlayers(null), []);
  assert.deepEqual(buildFlatOrderBookPlayers([{ teamId: 200, teamName: 'A队' }]), []);
  assert.deepEqual(
    buildFlatOrderBookPlayers([{ teamId: 200, teamName: 'A队', rows: [{ name: '无ID行' }, null] }]),
    [],
    '缺 playerId 的行不参与聚合'
  );
});

test('buildFlatOrderBookPlayers：缺省队名回落 ID 展示', () => {
  const flat = buildFlatOrderBookPlayers([{ teamId: 999, rows: [{ playerId: 5, name: '赵六' }] }]);
  assert.equal(flat[0].teamText, 'ID:999');
});
