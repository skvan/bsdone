// 门户「位置守备榜」接线（#478 P2）：API 客户端、PortalStats Tab、组件与 5 语种文案
// 运行：node --test tests/portalFieldingPositions.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const business = read('../src/api/business.js');
const portalStats = read('../src/views/portal/PortalStats.js');
const board = read('../src/views/portal/PortalFieldingPositions.vue');

const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en', 'ja', 'ko'].map((name) => [name, JSON.parse(read(`../src/locales/${name}.json`))])
);
const pick = (obj, path) => path.split('.').reduce((o, key) => (o == null ? undefined : o[key]), obj);

test('API 客户端：按位置守备榜走独立端点且透传 minOuts', () => {
  assert.ok(
    business.includes("getFieldingPositions: (params) => getData(`/api/stats/leaders/fielding-positions${statsQuery(params)}`)"),
    'business.js 缺少 getFieldingPositions 端点'
  );
  assert.match(business, /params\?\.minOuts != null\).*minOuts=\$\{params\.minOuts\}/, 'statsQuery 未透传 minOuts');
});

test('PortalStats：注册 Tab、挂载组件并按筛选条件透传', () => {
  assert.ok(portalStats.includes("import PortalFieldingPositions from './PortalFieldingPositions.vue'"), '未导入组件');
  assert.ok(portalStats.includes('name:"fieldingPositions"'), '缺少 fieldingPositions Tab');
  assert.ok(portalStats.includes('"stats.tabFieldingPositions"'), 'Tab 未使用文案键');
  assert.ok(
    portalStats.includes('t==="fielding"||t==="fieldingPositions"?'),
    'URL tab 白名单未包含 fieldingPositions（刷新会回落到投球榜）'
  );
  assert.ok(portalStats.includes('x(PortalFieldingPositions,{key:5,'), '内容区未挂载组件');
  for (const prop of ['years:X.value', 'eventIds:V.value', 'teamIds:$.value', 'gameMode:te.value', 'scope:T.value']) {
    assert.ok(portalStats.includes(prop), `组件缺少筛选属性 ${prop}`);
  }
  // 球队视角下位置/球员筛选被禁用，不得把隐藏值带给榜单；主客场筛选需透传
  assert.ok(portalStats.includes('position:T.value==="team"?"":ee.value'), '球队视角未清空位置筛选');
  assert.ok(portalStats.includes('homeAway:T.value==="team"?"":ue.value'), '未透传主客场筛选');
  assert.ok(portalStats.includes('playerName:T.value==="team"?"":Ae.value'), '球队视角未清空球员筛选');
});

test('位置守备榜不占用旧榜加载器（组件自行加载）', () => {
  assert.ok(!portalStats.includes('e==="fielding"||e==="fieldingPositions"?M()'), 'al() 不应把 fieldingPositions 交给 M()');
  assert.ok(portalStats.includes('e==="standings"&&qt()'), 'al() 只在积分榜切换时加载积分榜');
});

test('榜单组件：端点、资格线、列与筛选监听齐全', () => {
  assert.ok(board.includes('statsApi.getFieldingPositions'), '组件未调用 getFieldingPositions');
  assert.ok(board.includes('const minOuts = 9'), '资格线应为 9 出局');
  assert.ok(board.match(/minOuts,\s*\n\s*page:/), '请求未携带 minOuts');
  assert.ok(board.includes("prop=\"gp\"") && board.includes("prop=\"gs\"") && board.includes("prop=\"inn\""), '缺少 GP/GS/INN 列');
  assert.ok(board.includes('formatPositionDisplay(row)'), '守位列应按整行格式化（传字符串会恒显示 -）');
  assert.ok(!board.includes('formatPositionDisplay(row.position)'), '不得把字符串传给 formatPositionDisplay');
  assert.ok(board.includes('props.position') && board.includes('props.homeAway') && board.includes('props.playerName'), '未按位置/主客场/球员筛选请求');
  assert.ok(board.includes('watch('), '缺少筛选变化重新加载');
});

test('5 语种文案齐全（Tab/标题/资格线/列名）', () => {
  const keys = [
    'stats.tabFieldingPositions',
    'stats.fieldingPositionsTitle',
    'stats.fieldingPositionsHint',
    'stats.positionBoardPlayer',
    'stats.positionBoardTeam',
    'stats.positionBoardPosition',
    'stats.positionBoardGp',
    'stats.positionBoardGs',
    'stats.positionBoardInn'
  ];
  for (const [name, dict] of Object.entries(locales)) {
    for (const key of keys) {
      const value = pick(dict, key);
      assert.ok(typeof value === 'string' && value.length > 0, `${name} 缺少键 ${key}`);
    }
    assert.ok(pick(dict, 'stats.fieldingPositionsHint').includes('{n}'), `${name} 资格线文案缺少 {n} 占位`);
  }
});
