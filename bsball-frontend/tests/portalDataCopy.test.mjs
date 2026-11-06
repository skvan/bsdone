// 门户数据文案（B1b）：明星球员奖项、驾驶舱数据标签、赛程提示、路由标题
// 运行：node --test tests/portalDataCopy.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));
const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en', 'ja', 'ko'].map((l) => [l, readJson(`../src/locales/${l}.json`)])
);

const starPlayers = read('../src/views/portal/StarPlayers.js');
const clubhouse = read('../src/views/portal/PortalClubhouse.vue');
const teamDetail = read('../src/views/portal/PortalTeamDetail.js');
const events = read('../src/views/portal/PortalEvents.js');
const filterBar = read('../src/components/portal/PortalScheduleFilterBar.js');
const scheduleTable = read('../src/components/admin/EventScheduleListTable.js');
const legacyMeta = read('../src/router/legacy-meta.js');

test('明星球员奖项使用 i18n 而不直接渲染中文 award', () => {
  assert.ok(starPlayers.includes('AWARD_KEY_BY_CN'), '缺少奖项中文名到 key 的映射');
  assert.ok(starPlayers.includes('function awardText(row)'), '缺少 awardText');
  assert.ok(starPlayers.includes('function winnerText(row)'), '缺少 winnerText');
  assert.ok(starPlayers.includes('awardText(a)'), '奖项列未接入 awardText');
  assert.ok(starPlayers.includes('winnerText(a)'), '得主列未接入 winnerText');
  assert.ok(starPlayers.includes("o('starPlayers.awards.pending')"), '待公布未走 i18n');
  const awardKeys = Object.keys(locales['zh-CN'].starPlayers.award);
  assert.equal(awardKeys.length, 23, '奖项词条数量应为 23');
  const mapBody = starPlayers.match(/AWARD_KEY_BY_CN=\{([\s\S]*?)\};/);
  assert.ok(mapBody, '无法解析 AWARD_KEY_BY_CN');
  const mappedKeys = [...mapBody[1].matchAll(/:\s*'([a-z0-9]+)'/g)].map((match) => match[1]);
  assert.equal(mappedKeys.length, 23, '奖项映射键数量应为 23');
  for (const locale of Object.keys(locales)) {
    const award = locales[locale].starPlayers.award;
    assert.ok(award, `${locale} 缺少 starPlayers.award`);
    for (const key of new Set([...awardKeys, ...mappedKeys])) {
      assert.ok(award[key], `${locale} 缺少 starPlayers.award.${key}`);
    }
    assert.ok(locales[locale].starPlayers.awards.pending, `${locale} 缺少 awards.pending`);
  }
});

test('驾驶舱数据标签走 stats.col/stats.colEn，术语不再硬编码', () => {
  assert.ok(clubhouse.includes('useI18n'), '驾驶舱未接入 i18n');
  assert.ok(clubhouse.includes('statMatrixLabel'), '缺少 statMatrixLabel');
  const matrixKeys = ['obp', 'slg', 'ops', 'h', 'sb', 'bb', 'era', 'whip', 'pitchSo', 'w', 'sv', 'ip', 'tcPct', 'tc', 'po', 'a', 'e', 'dp'];
  for (const key of matrixKeys) {
    assert.ok(clubhouse.includes(`statMatrixLabel('${key}')`), `驾驶舱数据标签未接入 ${key}`);
  }
  for (const locale of Object.keys(locales)) {
    for (const key of matrixKeys) {
      assert.ok(locales[locale].stats.col[key], `${locale} 缺少 stats.col.${key}`);
      assert.ok(locales[locale].stats.colEn[key], `${locale} 缺少 stats.colEn.${key}`);
    }
  }
  assert.ok(!clubhouse.includes("'上垒率 OBP'"), '驾驶舱仍硬编码上垒率');
  assert.ok(!clubhouse.includes("'防御率 ERA'"), '驾驶舱仍使用防御率旧词');
  assert.ok(!clubhouse.includes("'刺杀 PO'"), '驾驶舱仍使用刺杀旧词');
  assert.ok(!clubhouse.includes("'夺三振 SO'"), '驾驶舱仍使用夺三振旧词');
  assert.ok(!clubhouse.includes("'救援 SV'"), '驾驶舱仍使用救援旧词');
  assert.ok(!clubhouse.includes("'保送 BB'"), '驾驶舱仍使用保送旧词');
});

test('球队详情/赛事页的缺日期提示与展开 aria 走 i18n', () => {
  for (const source of [teamDetail, events]) {
    assert.ok(source.includes('events.missingDateHint'), '缺日期提示未走 i18n');
    assert.ok(source.includes('events.collapseScheduleAria'), 'aria 收起未走 i18n');
    assert.ok(source.includes('events.expandScheduleAria'), 'aria 展开未走 i18n');
    assert.ok(source.includes('events.pdfUndated'), '缺日期分组名未走 i18n');
  }
  assert.ok(!teamDetail.includes('场比赛未填写比赛日或开始时间'), '球队详情仍有硬编码缺日期提示');
  assert.ok(!events.includes('场比赛未填写比赛日或开始时间'), '赛事页仍有硬编码缺日期提示');
  assert.ok(scheduleTable.includes('events.pdfUndated'), '列表分组表头未走 i18n');
  assert.ok(!scheduleTable.includes('未指定日期'), '列表分组表头仍有硬编码');
  for (const locale of Object.keys(locales)) {
    const hint = locales[locale].events.missingDateHint;
    assert.ok(hint, `${locale} 缺少 events.missingDateHint`);
    assert.ok(hint.includes('{n}') && hint.includes('{group}'), `${locale} 缺 {n}/{group}`);
    assert.ok(locales[locale].events.pdfUndated, `${locale} 缺少 pdfUndated`);
    assert.ok(locales[locale].events.expandScheduleAria.includes('{name}'), `${locale} 展开 aria 缺 {name}`);
    assert.ok(locales[locale].events.collapseScheduleAria.includes('{name}'), `${locale} 收起 aria 缺 {name}`);
  }
});

test('赛程特殊情况标记走 i18n', () => {
  assert.ok(filterBar.includes('events.specialResult'), '特殊情况未走 i18n');
  assert.ok(!filterBar.includes('"特殊情况"'), '赛程栏仍有硬编码特殊情况');
  for (const locale of Object.keys(locales)) {
    assert.ok(locales[locale].events.specialResult, `${locale} 缺少 events.specialResult`);
  }
});

test('门户路由标题键覆盖联盟/我的主场/介绍/欢迎页', () => {
  for (const [name, key] of [
    ['PortalLeagues', 'route.leagues'],
    ['PortalClubhouse', 'route.clubhouse'],
    ['PortalIntro', 'route.intro'],
    ['PortalWelcome', 'route.welcome']
  ]) {
    assert.ok(legacyMeta.includes(`${name}: "${key}"`), `${name} 未映射 ${key}`);
    for (const locale of Object.keys(locales)) {
      assert.ok(locales[locale].route[key.split('.')[1]], `${locale} 缺少 ${key}`);
    }
  }
});
