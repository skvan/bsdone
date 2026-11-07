// 实时录入主界面英文模式（B3b-1）：顶栏/比分板/操作提示/纠偏弹窗
// 运行：node --test tests/liveGameUiI18n.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));
const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en'].map((l) => [l, readJson(`../src/locales/${l}.json`)])
);

const liveGame = read('../src/views/admin/LiveGame.js');
const liveGameText = read('../src/composables/useLiveGameText.js');
const scoreboard = read('../src/components/admin/LiveGameScoreboard.vue');
const pbp = read('../src/components/admin/LiveGamePlayByPlay.vue');
const css = read('../src/styles/legacy/live-game.css');

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, path, out);
    else out[path] = value;
  }
  return out;
}

const flat = Object.fromEntries(Object.entries(locales).map(([l, data]) => [l, flatten(data)]));

function referencedKeys() {
  const keys = new Set();
  for (const source of [liveGame, scoreboard, pbp]) {
    for (const match of source.matchAll(/(?:lgText|lt)\(\s*["']([^"']+)["']/g)) {
      if (match[1].startsWith('liveGame.')) keys.add(match[1]);
    }
  }
  for (const base of [1, 2, 3]) keys.add(`liveGame.scoreboard.base${base}`);
  return keys;
}

test('实时录入界面接入英文模式开关与显式 en 语言回退', () => {
  assert.ok(liveGame.includes('useLiveGameText'), 'LiveGame 未接入英文回退 helper');
  assert.ok(liveGame.includes('isEnglishUi'), 'LiveGame 缺少英文模式开关');
  assert.ok(liveGame.includes('liveGame.top.title'), '顶栏标题未走 i18n');
  assert.ok(liveGame.includes('lgText("liveGame.top.title")'), '顶栏标题未使用 lgText');
  assert.ok(liveGameText.includes("locale: isEnglish.value ? 'en' : locale.value"), '非中文语言未回退到英文');
  for (const source of [scoreboard, pbp]) {
    assert.ok(source.includes('useLiveGameText'), '实时记分牌/实时动态未接入英文回退 helper');
  }
});

test('主界面引用的 liveGame 词条在简繁英三语齐全', () => {
  const keys = referencedKeys();
  assert.ok(keys.size >= 80, `引用词条数量不足：${keys.size}`);
  for (const locale of Object.keys(locales)) {
    for (const key of keys) {
      assert.ok(flat[locale][key], `${locale} 缺少 ${key}`);
    }
  }
});

test('英文包 liveGame 命名空间不残留中文', () => {
  const offenders = Object.entries(flat.en)
    .filter(([key, value]) => key.startsWith('liveGame.') && typeof value === 'string' && /[\u4e00-\u9fff]/.test(value))
    .map(([key, value]) => `${key}=${value}`);
  assert.deepEqual(offenders, []);
});

test('主渲染树不再硬编码顶栏/纠偏/换人模式中文', () => {
  const forbidden = [
    'Q("↩ 撤销"',
    'Q("↪ 重做"',
    'Q("编辑阵容"',
    'Q("保存比赛"',
    'Q("ER 复核"',
    'Q("确认半局结束 / 攻守交换"',
    'n("span",null,"实时比赛录入 · "',
    'title:"垒况纠偏、得分纠偏"',
    'title:"追加板凳球员"'
  ];
  for (const text of forbidden) {
    assert.ok(!liveGame.includes(text), `LiveGame 仍硬编码：${text}`);
  }
  assert.ok(liveGame.includes('lgText("liveGame.top.save")'), '保存比赛未走 i18n');
  assert.ok(liveGame.includes('lgText("liveGame.fixRunners.title")'), '垒况纠偏弹窗标题未走 i18n');
  assert.ok(liveGame.includes('lgText("liveGame.fixScore.title")'), '得分纠偏弹窗标题未走 i18n');
});

test('英文模式下的跑者进垒菜单只显示英文条目，标题与提问同步英文', () => {
  assert.ok(liveGame.includes('"rdm--en":isEnglishUi.value'), '跑者进垒菜单未标记英文模式');
  assert.ok(liveGame.includes('class:re(["rdm",{"rdm--en":isEnglishUi.value}])'), '出局原因菜单未标记英文模式');
  assert.ok(css.includes('.rdm--en .rdm__cn{display:none}'), '英文模式未隐藏中文条目');
  for (const key of ['rdm.routeStay', 'rdm.safeAdvance', 'rdm.outAdvance']) {
    assert.ok(liveGame.includes(`liveGame.${key}`), `跑者进垒菜单未接入 ${key}`);
  }
  assert.ok(liveGame.includes('H(rdmSafePrompt.value)'), '安全进垒提问未走英文模板');
  assert.ok(liveGame.includes('H(rdmOutPrompt.value)'), '出局提问未走英文模板');
  for (const locale of Object.keys(locales)) {
    for (const key of ['safeStay', 'safeToHome', 'safeBack', 'safeAdvance', 'outStay', 'outToHome', 'outBack', 'outAdvance']) {
      assert.ok(locales[locale].liveGame.rdm[key], `${locale} 缺少 rdm.${key}`);
    }
  }
});

test('换局弹窗标题/引导语走英文模板', () => {
  assert.ok(liveGame.includes('lgText("liveGame.inning.endTop"'), '上半局结束标题未走 i18n');
  assert.ok(liveGame.includes('lgText("liveGame.inning.nextInningAway"'), '下一局引导语未走 i18n');
  assert.ok(!liveGame.includes('title:"实时观赛"'), '观赛浮窗 iframe 标题仍硬编码');
  assert.ok(liveGame.includes('title:lgText("liveGame.watchFloat.title")'), '观赛浮窗 iframe 标题未走 i18n');
});

test('快捷记球标签不再被静态缓存冻结', () => {
  for (const index of [135, 136, 137, 138, 139]) {
    assert.ok(!liveGame.includes(`t[${index}]||(t[${index}]=n("span"`), `快捷记球标签仍被 t[${index}] 静态缓存`);
  }
});

test('记分牌与实时动态外壳文案走 i18n', () => {
  for (const key of [
    'liveGame.scoreboard.aria',
    'liveGame.scoreboard.batter',
    'liveGame.scoreboard.pitcher',
    'liveGame.scoreboard.inningAria',
    'liveGame.scoreboard.basesEmpty'
  ]) {
    assert.ok(scoreboard.includes(`lt('${key}'`) || scoreboard.includes(`lt("${key}"`), `记分牌缺少 ${key}`);
  }
  for (const key of ['liveGame.pbp.aria', 'liveGame.pbp.hint', 'liveGame.pbp.count', 'liveGame.pbp.empty']) {
    assert.ok(pbp.includes(`lt('${key}'`) || pbp.includes(`lt("${key}"`), `实时动态缺少 ${key}`);
  }
  assert.ok(!scoreboard.includes('aria-label="实时记分牌"'), '记分牌仍硬编码 aria 文案');
  assert.ok(!pbp.includes('实时动态 · 最新在上'), '实时动态仍硬编码提示');
});
