// 观赛页英文模式（B4b）：数据源/同步/空态/日志与实时动态页签/事件统计/返回入口
// 运行：node --test tests/liveGameWatchI18n.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));
const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en'].map((l) => [l, readJson(`../src/locales/${l}.json`)])
);
const watch = read('../src/views/admin/LiveGameWatch.js');

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, path, out);
    else out[path] = value;
  }
  return out;
}
const flat = Object.fromEntries(Object.entries(locales).map(([l, d]) => [l, flatten(d)]));

test('观赛页引用词条在简繁英三语齐全', () => {
  const keys = new Set();
  for (const match of watch.matchAll(/wText\(\s*["'](liveGame\.[^"']+)["']/g)) keys.add(match[1]);
  assert.ok(keys.size >= 30, `引用词条数量不足：${keys.size}`);
  for (const locale of Object.keys(locales)) {
    for (const key of keys) assert.ok(flat[locale][key], `${locale} 缺少 ${key}`);
  }
});

test('英文观赛词条不残留中文', () => {
  const offenders = Object.entries(flat.en)
    .filter(([key, value]) => key.startsWith('liveGame.watch.') && typeof value === 'string' && /[\u4e00-\u9fff]/.test(value))
    .map(([key, value]) => `${key}=${value}`);
  assert.deepEqual(offenders, []);
});

test('观赛页界面文案不再硬编码主要中文', () => {
  for (const text of ['数据源：本地实时', '数据源：服务器快照', '日志', '实时动态', '暂无事件', '得分记录', '暂无得分', '未找到比赛']) {
    assert.ok(!watch.includes(`>${text}<`) && !watch.includes(`"${text}"`), `观赛页仍硬编码：${text}`);
  }
  assert.ok(watch.includes('wText("liveGame.watch.dataSourceLocal")'), '数据源未走 i18n');
  assert.ok(watch.includes('wText("liveGame.watch.tabLogs")'), '日志页签未走 i18n');
  assert.ok(watch.includes('wText("liveGame.watch.tabPbp")'), '实时动态页签未走 i18n');
});

test('观赛页半局/局次/统计文案走 i18n', () => {
  for (const key of ['liveInning', 'halfTop', 'halfBottom', 'inningTitle', 'eventCount', 'pitcherLine', 'countUnit']) {
    assert.ok(watch.includes(`liveGame.watch.${key}`), `未接入 ${key}`);
  }
});
