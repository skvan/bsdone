import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import { buildGameShareUrl, buildGameShareMeta, gameDateTime } from '../src/composables/useGameShare.js';

function fakeRouter(base = '/bs-ball/') {
  return {
    resolve({ params, query }) {
      const q = query && query.from ? `?from=${query.from}` : '';
      return { href: `${base}${params.tenantCode}/events/${params.eventId}/games/${params.gameId}${q}` };
    }
  };
}

beforeEach(() => {
  globalThis.location = { origin: 'https://bsdone.com' };
});

test('buildGameShareUrl：含 base、赛事/比赛 ID 与 from 来源', () => {
  const router = fakeRouter();
  assert.equal(
    buildGameShareUrl(router, 268, 18, 'share', 'bs-ball'),
    'https://bsdone.com/bs-ball/bs-ball/events/18/games/268?from=share'
  );
  assert.equal(
    buildGameShareUrl(router, 268, 18, 'poster', 'bs-ball'),
    'https://bsdone.com/bs-ball/bs-ball/events/18/games/268?from=poster'
  );
});

test('buildGameShareMeta：已完赛标题为客队比分主队，描述含赛事/场次/日期/平台', () => {
  const meta = buildGameShareMeta(
    {
      status: 'final',
      awayScore: 10,
      homeScore: 8,
      gameNumber: 4,
      gameday: '2026-11-01',
      gameTime: '2026-11-01 15:29:00'
    },
    { name: '广州不死鸟', shortName: '不死鸟', logo: '/files/home.png' },
    { name: '暴力羊', shortName: '暴力羊' },
    { name: '2026花城联盟冬季赛' }
  );
  assert.equal(meta.title, '暴力羊 10 : 8 广州不死鸟');
  assert.equal(meta.desc, '2026花城联盟冬季赛 · Game 4 · 2026-11-01 15:29 · BSD棒垒球数据平台');
  assert.equal(meta.imgUrl, 'https://bsdone.com/files/home.png');
});

test('buildGameShareMeta：未开赛显示 vs，且无球队 logo 回退平台 logo', () => {
  const meta = buildGameShareMeta(
    { status: 'scheduled', awayScore: 0, homeScore: 0, gameday: '2026-11-08' },
    { name: '广州不死鸟' },
    { name: '暴力羊' },
    { name: '2026花城联盟冬季赛' }
  );
  assert.equal(meta.title, '暴力羊 vs 广州不死鸟');
  assert.equal(meta.imgUrl, 'https://bsdone.com/static/logo/logo2.png');
});

test('buildGameShareMeta：缺少比赛返回 null', () => {
  assert.equal(buildGameShareMeta(null, null, null, null), null);
});

test('buildGameShareMeta：支持自定义 origin/resolveAsset/platformName 与绝对图片', () => {
  const meta = buildGameShareMeta(
    { status: 'final', awayScore: 1, homeScore: 2, gameday: '2026-11-01' },
    { name: '主队', logo: '/files/home.png' },
    { name: '客队', logo: 'https://cdn.test/away.png' },
    null,
    { origin: 'https://x.test', resolveAsset: (url) => `/bsball-server${url}`, platformName: '平台X' }
  );
  assert.equal(meta.title, '客队 1 : 2 主队');
  assert.equal(meta.desc, '2026-11-01 · 平台X');
  assert.equal(meta.imgUrl, 'https://x.test/bsball-server/files/home.png');
});

test('buildGameShareMeta：可覆盖主/客队兜底名称', () => {
  const meta = buildGameShareMeta(
    { status: 'scheduled' },
    null,
    null,
    null,
    { origin: 'https://x.test', fallbackHome: '主隊', fallbackAway: '客隊' }
  );
  assert.equal(meta.title, '客隊 vs 主隊');
});

test('gameDateTime：支持带日期/仅时间/个位小时，缺值返回空串', () => {
  assert.equal(gameDateTime({ gameday: '2026-11-01', gameTime: '2026-11-01 15:29:00' }), '2026-11-01 15:29');
  assert.equal(gameDateTime({ gameday: '2026-11-01', gameTime: '9:05:00' }), '2026-11-01 09:05');
  assert.equal(gameDateTime({ gameday: '', gameTime: '' }), '');
  assert.equal(gameDateTime(null), '');
});
