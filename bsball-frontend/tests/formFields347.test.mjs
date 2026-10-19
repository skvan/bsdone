// 静态断言：Issue #347 纸质表单字段对齐（PR A）
// 运行：node --test tests/formFields347.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { resolveAtBats } from '../src/utils/gamePlayerStat.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const games = read('../src/views/admin/Games.js');
const detail = read('../src/components/admin/GameDetailContent.js');
const player = read('../src/components/admin/PlayerDetailContent.js');
const statsCols = read('../src/stores/statsColumns.js');
const storage = read('../src/utils/gameDetailFieldSettingsStorage.js');
const defaults = read('../src/utils/gamePlayerStat.js');
const tableMeta = read('../src/composables/useStatsTableMeta.js');

const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en', 'ja', 'ko'].map((name) => [name, JSON.parse(read(`../src/locales/${name}.json`))])
);

test('管理端打者列拆分：GDP/SH/SF/BB/IBB/HB/IO', () => {
  for (const key of ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io']) {
    assert.ok(games.includes(`key:"${key}"`), `打者列缺少 ${key}`);
  }
  assert.ok(games.includes('key:"decision"'), '投手列缺少 W/L/SV 三选一');
  assert.ok(games.includes('function decisionValue'), '缺少 decisionValue');
  assert.ok(games.includes('function applyDecision'), '缺少 applyDecision');
});

test('管理端守备页签与字段组件已挂载', () => {
  assert.ok(games.includes("import GameFieldingTable"), '未导入 GameFieldingTable');
  assert.ok(games.includes('label:"守备数据"'), '缺少守备数据页签');
  assert.ok(games.includes('fieldingStatsAway'), '缺少客队守备 statsFor');
  assert.ok(games.includes('fieldingStatsHome'), '缺少主队守备 statsFor');
});

test('管理端保存/导入导出包含新字段', () => {
  for (const needle of [
    'io:S?.io',
    'pitchDoubles:v?.pitchDoubles??0',
    'pitchTriples:v?.pitchTriples??0',
    'pitchSh:v?.pitchSh??0',
    'pitchSf:v?.pitchSf??0',
    'pitchIo:v?.pitchIo??0',
    'pitchBbHp:(Number(v?.pitchBb)||0)+(Number(v?.pitchHbp)||0)',
    'bbHp:(Number(u?.bb)||0)+(Number(u?.hbp)||0)'
  ]) {
    assert.ok(games.includes(needle), `保存/导入导出缺少 ${needle}`);
  }
});

test('门户比赛详情打者/投手列已拆并补新字段', () => {
  for (const key of ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io']) {
    assert.ok(detail.includes(`key:"${key}"`), `详情打者列缺少 ${key}`);
  }
  for (const key of ['pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
    assert.ok(storage.includes(`key: '${key}'`), `详情投手列缺少 ${key}`);
  }
  assert.ok(storage.includes("key: 'pitchPa', label: 'PA', visible: false"), 'PA 应默认隐藏但保留设置项（#211）');
  assert.ok(!storage.includes("key: 'pitchBbHp'"), '投手表仍保留 BB+HP 合并列');
});

test('球员详情/数据排行列为新字段', () => {
  for (const prop of ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
    assert.ok(player.includes(`prop:"${prop}"`), `球员详情缺少 ${prop}`);
  }
  for (const key of ['io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
    assert.ok(statsCols.includes(`key:"${key}"`), `数据排行缺少 ${key}`);
  }
});

test('投手 AB 回退不重复扣 IBB，并兼容旧 BBHP 合计', () => {
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 2, pitchIbb: 1, pitchHbp: 1 }), 7);
  assert.equal(resolveAtBats({ pitchBf: 10, pitchBb: 0, pitchHbp: 0, pitchBbHp: 3 }), 7);
  assert.equal(resolveAtBats({ pitchBf: 10, pitchAb: 8 }), 8);
});

test('统计表列名：pitchIbb/pitchHbp 映射到 IBB/HBP 词典', () => {
  assert.ok(tableMeta.includes("pitchIbb: 'ibb'"));
  assert.ok(tableMeta.includes("pitchHbp: 'hbp'"));
});

test('默认值与五语种词条', () => {
  assert.ok(defaults.includes('gdp:0'));
  assert.ok(defaults.includes('io:0'));
  assert.ok(defaults.includes('pitchDoubles:0'));
  assert.ok(defaults.includes('pitchIo:0'));
  for (const [name, dict] of Object.entries(locales)) {
    for (const key of ['io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo']) {
      assert.ok(dict.stats?.col?.[key], `${name} stats.col 缺少 ${key}`);
    }
    for (const key of ['sh', 'sf', 'ibb', 'hbp', 'io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIbb', 'pitchIo']) {
      assert.ok(dict.player?.seasonColTip?.[key], `${name} seasonColTip 缺少 ${key}`);
    }
  }
});
