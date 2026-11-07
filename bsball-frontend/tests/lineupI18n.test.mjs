// 阵容确认页英文模式（B3b-2a）：页面标题/表单/提示/模板弹窗与三个子组件
// 运行：node --test tests/lineupI18n.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { ref } from 'vue';
import { useLineupBoard } from '../src/composables/useLineupBoard.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));
const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en'].map((l) => [l, readJson(`../src/locales/${l}.json`)])
);

const board = read('../src/views/admin/LiveGameLineupBoard.vue');
const batting = read('../src/components/admin/LineupBattingOrder.vue');
const pool = read('../src/components/admin/LineupPoolPanel.vue');
const state = read('../src/composables/useLineupBoard.js');
const sources = [board, batting, pool];

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, path, out);
    else out[path] = value;
  }
  return out;
}
const flat = Object.fromEntries(Object.entries(locales).map(([l, d]) => [l, flatten(d)]));
const POSITION_CODES = ['C', 'P', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF', 'DH', 'XF'];

test('阵容页与子组件引用词条在简繁英三语齐全', () => {
  const keys = new Set();
  for (const source of sources) {
    for (const match of source.matchAll(/lt\(\s*['"](liveGame\.[^'"]+)['"]/g)) keys.add(match[1]);
  }
  assert.ok(keys.size >= 40, `引用词条数量不足：${keys.size}`);
  for (const [locale, messages] of Object.entries(flat)) {
    for (const key of keys) assert.ok(messages[key], `${locale} 缺少 ${key}`);
    for (const code of POSITION_CODES) {
      assert.ok(messages[`liveGame.lineup.position.${code}`], `${locale} 缺少 position.${code}`);
    }
  }
});

test('阵容状态通知词条在简繁英三语齐全', () => {
  const keys = new Set([...state.matchAll(/['"](liveGame\.lineup\.notice\.[a-zA-Z]+)['"]/g)].map((m) => m[1]));
  assert.ok(keys.size >= 12, `通知词条数量不足：${keys.size}`);
  for (const locale of Object.keys(locales)) {
    for (const key of keys) assert.ok(flat[locale][key], `${locale} 缺少 ${key}`);
  }
});

test('英文阵容词条不残留中文', () => {
  const offenders = Object.entries(flat.en)
    .filter(([key, value]) => key.startsWith('liveGame.lineup.') && typeof value === 'string' && /[\u4e00-\u9fff]/.test(value))
    .map(([key, value]) => `${key}=${value}`);
  assert.deepEqual(offenders, []);
});

test('阵容页模板不再硬编码主要中文', () => {
  for (const text of ['确认先发阵容 ·', '比赛模式', '选择球场', '开始录入', '打线顺序', '可用球员', '替补名单', '批量添加替补']) {
    assert.ok(!board.includes(`>${text}<`) && !board.includes(`"${text}"`), `阵容页仍硬编码：${text}`);
  }
  assert.ok(board.includes("lt('liveGame.lineup.title')"), '页面标题未走 i18n');
  assert.ok(board.includes("lt('liveGame.lineup.startEntry')"), '开始录入未走 i18n');
  assert.ok(board.includes('liveGame.lineup.position.${code}'), '球场卡槽位置名未走 i18n');
});

test('阵容状态通知接入翻译函数（英文模式不再弹出中文）', () => {
  const board = useLineupBoard(ref('BASEBALL'), ref(1), {
    ohtaniRule: ref(true),
    text: (key) => `EN:${key}`,
  });
  assert.equal(board.toggleDhPitcher(), false);
  assert.equal(board.notice.value, 'EN:liveGame.lineup.notice.putDhFirst');
  assert.ok(locales.en.liveGame.lineup.notice.putDhFirst, '英文缺少 putDhFirst');
});

test('子组件标题与英文棒次序号', () => {
  assert.ok(batting.includes("liveGame.lineup.battingOrder.title"), '打线顺序标题未走 i18n');
  assert.ok(batting.includes('const orderText'), '缺少英文棒次格式化');
  assert.ok(batting.includes("isEnglish.value ? ordinal(n)"), '英文棒次未使用序数');
  assert.ok(pool.includes("liveGame.lineup.pool.title"), '可用球员标题未走 i18n');
  assert.ok(pool.includes("liveGame.lineup.pool.batchTitle"), '批量添加弹窗标题未走 i18n');
});
