import { test, beforeEach } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildClubhouseShareUrl,
  buildClubhouseShareMeta
} from '../src/composables/useWechatShare.js';

function fakeRouter(base = '/bs-ball/') {
  return {
    resolve({ name, params, query }) {
      const search = new URLSearchParams();
      Object.entries(query || {}).forEach(([key, value]) => {
        if (value != null && value !== '') search.set(key, String(value));
      });
      const qs = search.toString();
      return {
        href: `${base}${params.tenantCode}/players/${params.id}/clubhouse${qs ? `?${qs}` : ''}`,
        name
      };
    }
  };
}

beforeEach(() => {
  globalThis.location = { origin: 'https://www.bsdone.com' };
});

test('buildClubhouseShareUrl：固定主页地址 + 分享范围 + from', () => {
  const router = fakeRouter();
  const url = buildClubhouseShareUrl(router, 9, 'share', 'team1', {
    teamId: 12,
    leagueId: 3,
    eventId: 0,
    gameMode: 'BASEBALL'
  });

  assert.equal(
    url,
    'https://www.bsdone.com/bs-ball/team1/players/9/clubhouse?from=share&teamId=12&leagueId=3&gameMode=BASEBALL'
  );
});

test('buildClubhouseShareUrl：无范围时只带 from', () => {
  const router = fakeRouter();
  assert.equal(
    buildClubhouseShareUrl(router, '9', 'share', 'team1', {}),
    'https://www.bsdone.com/bs-ball/team1/players/9/clubhouse?from=share'
  );
});

test('buildClubhouseShareMeta：标题/摘要/缩略图', () => {
  const meta = buildClubhouseShareMeta(
    { name: '张三', avatar: '/files/a.png' },
    { name: '雄鹰队' },
    { name: '春季联赛' },
    { batting: { ab: 100, avg: '.350', hr: 12, rbi: 34 } },
    { title: '张三 | 我的主场' }
  );

  assert.equal(meta.title, '张三 | 我的主场');
  assert.equal(meta.desc, '打击率 .350 · 12HR · 34打点 · 雄鹰队 · 春季联赛 · BSD棒垒球数据平台');
  assert.equal(meta.imgUrl, 'https://www.bsdone.com/files/a.png');
});

test('buildClubhouseShareMeta：无头像回退平台 logo，无球员返回 null', () => {
  const meta = buildClubhouseShareMeta(
    { name: '李明', avatar: '' },
    null,
    null,
    { pitching: { gp: 8, ip: 40, era: '2.10', pitchSo: 30, w: 3 } }
  );

  assert.equal(meta.title, '李明 | 我的主场');
  assert.equal(meta.desc, '防御率 2.10 · 30K · 3胜 · BSD棒垒球数据平台');
  assert.equal(meta.imgUrl, 'https://www.bsdone.com/static/logo/logo2.png');
  assert.equal(buildClubhouseShareMeta(null, null, null, null), null);
  assert.equal(buildClubhouseShareMeta({ name: '' }, null, null, null), null);
});
