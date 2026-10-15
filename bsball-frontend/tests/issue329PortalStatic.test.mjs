// 静态断言：门户页优化（Issue #329）——菜单改名/新路由/5 语种新键/未误改锚点
// 运行：node --test tests/issue329PortalStatic.test.mjs
// 依据：Issue #329 方案（联盟球队→球队；新增 leagueDetail/freeAgents 页面与路由；自由球员口径 teamId=0）
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const router = read('../src/router/index.js');
const legacyMeta = read('../src/router/legacy-meta.js');
const portalTeams = read('../src/views/portal/PortalTeams.js');
const portalLeagues = read('../src/views/portal/PortalLeagues.js');
const portalLeagueDetail = read('../src/views/portal/PortalLeagueDetail.js');
const portalFreeAgents = read('../src/views/portal/PortalFreeAgents.js');

const locales = {
  'zh-CN': JSON.parse(read('../src/locales/zh-CN.json')),
  'zh-TW': JSON.parse(read('../src/locales/zh-TW.json')),
  en: JSON.parse(read('../src/locales/en.json')),
  ko: JSON.parse(read('../src/locales/ko.json')),
  ja: JSON.parse(read('../src/locales/ja.json'))
};

function pick(obj, path) {
  return path.split('.').reduce((o, key) => (o == null ? undefined : o[key]), obj);
}

test('路由：新增 leagues/:id 与 teams/free-agents 两路由且标题键已注册', () => {
  assert.ok(
    router.includes("{ path: 'leagues/:id', name: 'PortalLeagueDetail'"),
    '缺少 leagues/:id 路由（PortalLeagueDetail）'
  );
  assert.ok(
    router.includes("{ path: 'teams/free-agents', name: 'PortalFreeAgents'"),
    '缺少 teams/free-agents 路由（PortalFreeAgents）'
  );
  assert.ok(legacyMeta.includes('PortalLeagueDetail: "route.leagueDetail"'), 'ROUTE_TITLE_KEYS 缺少 PortalLeagueDetail');
  assert.ok(legacyMeta.includes('PortalFreeAgents: "route.freeAgents"'), 'ROUTE_TITLE_KEYS 缺少 PortalFreeAgents');
});

test('菜单改名：zh-CN 4 键=球队，zh-TW 4 键=球隊，en/ko/ja 保持通用词', () => {
  const changedKeys = ['nav.teams', 'route.teams', 'home.teamsTitle', 'teams.title'];
  for (const key of changedKeys) {
    assert.equal(pick(locales['zh-CN'], key), '球队', `zh-CN ${key} 应为「球队」`);
    assert.equal(pick(locales['zh-TW'], key), '球隊', `zh-TW ${key} 应为「球隊」`);
  }
  assert.equal(pick(locales.en, 'nav.teams'), 'Teams');
  assert.equal(pick(locales.ko, 'nav.teams'), '팀');
  assert.equal(pick(locales.ja, 'nav.teams'), 'チーム');
});

test('未误改锚点：orderBook.teamsTitle 保持「参赛球队」，词典无「联盟球队」残留', () => {
  assert.equal(pick(locales['zh-CN'], 'orderBook.teamsTitle'), '参赛球队', 'orderBook.teamsTitle 不应被误改');
  for (const [name, zh] of [['zh-CN', '联盟球队'], ['zh-TW', '聯盟球隊']]) {
    const file = name === 'zh-CN' ? '../src/locales/zh-CN.json' : '../src/locales/zh-TW.json';
    assert.ok(!read(file).includes(zh), `${name} 词典不应再有「${zh}」残留`);
  }
});

test('5 语种新键齐全（联盟详情/自由球员/球队末位卡）', () => {
  const need = [
    'route.leagueDetail',
    'route.freeAgents',
    'leagueDetail.events',
    'leagueDetail.emptyEvents',
    'leagueDetail.notFound',
    'freeAgents.title',
    'freeAgents.empty',
    'teams.freeAgentsCard'
  ];
  for (const [name, dict] of Object.entries(locales)) {
    for (const key of need) {
      const value = pick(dict, key);
      assert.ok(typeof value === 'string' && value.length > 0, `${name} 缺少键 ${key}`);
    }
  }
});

test('球队页末位自由球员卡：链接 teams/free-agents + 文案键', () => {
  assert.ok(portalTeams.includes('teams/free-agents'), 'PortalTeams 缺少 teams/free-agents 跳转');
  assert.ok(portalTeams.includes('teams.freeAgentsCard'), 'PortalTeams 缺少 teams.freeAgentsCard 文案');
});

test('联盟页卡片可点击进入详情；详情页按 leagueId 过滤且挂事件跳转', () => {
  assert.ok(portalLeagues.includes('RouterLink'), 'PortalLeagues 卡片应改为 RouterLink');
  assert.ok(portalLeagues.includes('leagues/${l.id}'), 'PortalLeagues 卡片应链接到 leagues/:id');
  assert.ok(portalLeagueDetail.includes('/api/league/'), 'PortalLeagueDetail 应请求联盟详情');
  assert.ok(portalLeagueDetail.includes('filterEventsByLeague'), 'PortalLeagueDetail 应使用 filterEventsByLeague 过滤下辖赛事');
});

test('自由球员页口径：teamId=0（无现属球队，含退役）', () => {
  assert.ok(portalFreeAgents.includes('teamId: 0'), 'PortalFreeAgents 必须以 teamId=0 查询自由球员');
  assert.ok(portalFreeAgents.includes('players/'), 'PortalFreeAgents 球员卡应链接到球员详情');
});
