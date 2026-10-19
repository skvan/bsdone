// 静态断言：球员详情统计列补齐（Issue #130 / #350 后续）
// 运行：node --test tests/playerDetailStatsColumns.test.mjs
// 依据：审计报告 6.4/6.5——球员详情缺 DP、SB允许；投手 UER/PENDING 无展示出口
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const detail = read('../src/components/admin/PlayerDetailContent.js');

const locales = {
  'zh-CN': JSON.parse(read('../src/locales/zh-CN.json')),
  'zh-TW': JSON.parse(read('../src/locales/zh-TW.json')),
  en: JSON.parse(read('../src/locales/en.json')),
  ja: JSON.parse(read('../src/locales/ja.json')),
  ko: JSON.parse(read('../src/locales/ko.json'))
};

function pick(obj, path) {
  return path.split('.').reduce((o, key) => (o == null ? undefined : o[key]), obj);
}

test('投手赛季列：R/ER/UER/PENDING 有列定义', () => {
  for (const [prop, abbr, tip] of [
    ['pitchR', 'R', 'pitchR'],
    ['er', 'ER', 'er'],
    ['unearnedR', 'UER', 'unearnedR'],
    ['pendingR', 'PENDING', 'pendingR']
  ]) {
    const needle = `prop:"${prop}",abbr:"${abbr}",tip:"${tip}"`;
    assert.ok(detail.includes(needle), `投手列缺少 ${needle}`);
  }
});

test('守备赛季列：DP 与 SB允许 有列定义', () => {
  assert.ok(detail.includes('prop:"dp",abbr:"DP",tip:"dp"'), '守备列缺少 DP');
  assert.ok(detail.includes('prop:"catcherSb",abbr:"SB",tip:"catcherSb"'), '守备列缺少 SB允许');
});

test('守备区块显示条件包含 DP/SB允许', () => {
  assert.ok(detail.includes('(a.dp??0)>0'), 'Ca() 未把 DP 纳入显示条件');
  assert.ok(detail.includes('(a.catcherSb??0)>0'), 'Ca() 未把 SB允许 纳入显示条件');
});

test('钻取映射：投手 R/ER，守备 DP/SB允许', () => {
  assert.ok(detail.includes('WHIP:"whip",R:"pitchR",ER:"er",BF:"pitchBf"'), '投手钻取映射缺少 R/ER');
  assert.ok(detail.includes('E:"e",DP:"dp","TC%":"tcPct",PB:"pb",SB:"catcherSb"'), '守备钻取映射缺少 DP/SB允许');
});

test('生涯卡片：投手与守备新增项已挂载', () => {
  for (const key of [
    'player.careerPitch.pitchR',
    'player.careerPitch.er',
    'player.careerPitch.unearnedR',
    'player.careerPitch.pendingR',
    'player.careerFld.dp',
    'player.careerFld.catcherSb'
  ]) {
    assert.ok(detail.includes(key), `生涯卡片缺少 ${key}`);
  }
});

test('5 语种词条：seasonColTip / careerPitch / careerFld 新键齐全', () => {
  const need = [
    'player.seasonColTip.unearnedR',
    'player.seasonColTip.pendingR',
    'player.seasonColTip.catcherSb',
    'player.careerPitch.pitchR',
    'player.careerPitch.er',
    'player.careerPitch.unearnedR',
    'player.careerPitch.pendingR',
    'player.careerFld.dp',
    'player.careerFld.catcherSb'
  ];
  for (const [name, dict] of Object.entries(locales)) {
    for (const key of need) {
      const value = pick(dict, key);
      assert.ok(typeof value === 'string' && value.length > 0, `${name} 缺少词条 ${key}`);
    }
  }
});
