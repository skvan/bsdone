// 前端单测：实时录入阵容「中途保存」草稿工具（#207）
// 运行：在 bsball-frontend 目录执行  node --test tests/
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  toLocalGameTime,
  splitGameTime,
  buildLineupDraft,
  parseLineupDraft,
  resolveLineupResumeInit
} from '../src/utils/lineupDraft.js';

test('toLocalGameTime：按本地时间格式化为 YYYY-MM-DD HH:mm:ss（固定时刻）', () => {
  const d = new Date(2026, 9, 3, 9, 5, 7); // 2026-10-03 09:05:07 本地时间
  assert.equal(toLocalGameTime(d), '2026-10-03 09:05:07');
});

test('splitGameTime：完整时间转 gameday；分钟精度补秒；日期短串补零点；非法值原样返回', () => {
  assert.deepEqual(splitGameTime('2026-10-03 15:30:00'), {
    gameTime: '2026-10-03 15:30:00',
    gameday: '2026-10-03'
  });
  assert.deepEqual(splitGameTime('2026-10-03 15:30'), {
    gameTime: '2026-10-03 15:30:00',
    gameday: '2026-10-03'
  });
  assert.deepEqual(splitGameTime('2026-10-03T15:30:00'), {
    gameTime: '2026-10-03 15:30:00',
    gameday: '2026-10-03'
  });
  assert.deepEqual(splitGameTime('2026-10-03'), {
    gameTime: '2026-10-03 00:00:00',
    gameday: '2026-10-03'
  });
  assert.deepEqual(splitGameTime(''), { gameTime: '', gameday: '' });
  assert.deepEqual(splitGameTime('不是时间'), { gameTime: '不是时间', gameday: '' });
  assert.deepEqual(splitGameTime(null), { gameTime: '', gameday: '' });
});

test('buildLineupDraft：v:1 草稿标记 gameStarted:false 且字段透传', () => {
  const lineups = [{ id: 1, position: 'P' }];
  const draft = buildLineupDraft({
    gameId: 88,
    gameMode: 'BASEBALL',
    homeTeamId: 11,
    awayTeamId: 22,
    venue: '中山熊猫纪念球场',
    gameTime: '2026-10-03 18:30:00',
    gameday: '2026-10-03',
    homeLineup: lineups,
    awayLineup: [],
    homeBench: [],
    awayBench: [],
    homeFieldingPitcherId: 1,
    awayFieldingPitcherId: null,
    homeLastDhAddedFromPoolId: null,
    awayLastDhAddedFromPoolId: null,
    homeUnavailablePlayerIds: [9],
    awayUnavailablePlayerIds: []
  });
  assert.equal(draft.v, 1);
  assert.equal(draft.gameStarted, false);
  assert.equal(draft.gameId, 88);
  assert.ok(Number.isFinite(draft.savedAt) && draft.savedAt > 0);
  assert.deepEqual(draft.setupForm, {
    homeTeamId: 11,
    awayTeamId: 22,
    venue: '中山熊猫纪念球场',
    gameTime: '2026-10-03 18:30:00',
    gameday: '2026-10-03'
  });
  assert.equal(draft.homeLineup, lineups);
  assert.deepEqual(draft.homeUnavailablePlayerIds, [9]);
  assert.equal(draft.awayFieldingPitcherId, null);
});

test('parseLineupDraft：非 v:1 / 非法 JSON / 空串一律 null，合法草稿可往返', () => {
  assert.equal(parseLineupDraft(''), null);
  assert.equal(parseLineupDraft('   '), null);
  assert.equal(parseLineupDraft(null), null);
  assert.equal(parseLineupDraft('{bad json'), null);
  assert.equal(parseLineupDraft('{"v":2}'), null);
  assert.equal(parseLineupDraft('[1,2]'), null);
  const draft = buildLineupDraft({ gameId: 3, gameMode: 'BASEBALL', homeLineup: [], awayLineup: [] });
  const round = parseLineupDraft(JSON.stringify(draft));
  assert.equal(round.v, 1);
  assert.equal(round.gameId, 3);
  assert.equal(round.gameStarted, false);
});

test('resolveLineupResumeInit：快照优先、缺失回落比赛记录；空字符串视为明确清空', () => {
  const game = { homeTeamId: 11, awayTeamId: 22, venue: '老场地', gameTime: '2026-10-01 10:00:00', gameMode: 'SOFTBALL' };
  const snapshot = {
    gameMode: 'BASEBALL',
    setupForm: {
      homeTeamId: 33,
      awayTeamId: null,
      venue: '',
      gameTime: '2026-10-03 18:30:00'
    }
  };
  const init = resolveLineupResumeInit(game, snapshot);
  assert.equal(init.homeTeamId, 33); // 快照优先
  assert.equal(init.awayTeamId, 22); // null 回落比赛记录
  assert.equal(init.venue, ''); // 空字符串 = 快照里明确清空，不回落
  assert.equal(init.gameTime, '2026-10-03 18:30:00');
  assert.equal(init.gameMode, 'BASEBALL'); // 快照模式优先
});

test('resolveLineupResumeInit：无快照时全部回落比赛记录，无比赛时返回空值', () => {
  const game = { homeTeamId: 11, awayTeamId: 22, venue: '球场A', gameTime: '2026-10-01 10:00:00', gameMode: 'BASEBALL' };
  const init = resolveLineupResumeInit(game, null);
  assert.deepEqual(init, {
    homeTeamId: 11,
    awayTeamId: 22,
    venue: '球场A',
    gameTime: '2026-10-01 10:00:00',
    gameMode: 'BASEBALL'
  });
  const empty = resolveLineupResumeInit(null, null);
  assert.deepEqual(empty, {
    homeTeamId: null,
    awayTeamId: null,
    venue: null,
    gameTime: null,
    gameMode: ''
  });
});
