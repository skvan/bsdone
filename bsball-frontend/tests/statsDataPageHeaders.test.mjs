// 球员详情 / 比赛详情数据表头与新增术语守护
// 运行：node --test tests/statsDataPageHeaders.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));

const player = read('../src/components/admin/PlayerDetailContent.js');
const game = read('../src/components/admin/GameDetailContent.js');
const fielding = read('../src/components/admin/GameDetailFieldingTable.vue');

test('球员详情三张赛季表接入双行表头与列宽覆盖', () => {
  assert.equal((player.match(/playerHeaderNode\(s\)/g) || []).length, 3, '打击/投球/守备三张表未全部接入');
  assert.ok(player.includes('playerHeaderAbbr(col.prop,col.abbr)'), '表头未使用统一 MLB 缩写');
  assert.ok(player.includes('playerWidth(s.prop,s.w)'), '列宽未使用球员详情覆盖');
});

test('比赛详情四张技术统计表接入双行表头，表名已本地化', () => {
  assert.ok((game.match(/gdHeaderNode\(a\)/g) || []).length >= 8, '主客队打击/投手四张表未全部接入');
  assert.ok(game.includes('gdWidth(a.key,fe(a))'), '列宽未使用比赛详情覆盖');
  for (const key of ['batters', 'pitchers', 'totals']) {
    assert.ok(game.includes(`gameDetail.${key}`), `表名 gameDetail.${key} 未本地化`);
  }
  assert.ok(fielding.includes('gameDetail.fieldingTableTitle'), '守备表标题未本地化');
});

test('新增比赛详情标签在五语种齐全', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en', 'ja', 'ko']) {
    const data = readJson(`../src/locales/${locale}.json`);
    for (const key of ['batters', 'pitchers', 'totals', 'fieldingTableTitle']) {
      assert.ok(data.gameDetail?.[key], `${locale} 缺少 gameDetail.${key}`);
    }
    assert.ok(data.glossary.term.gsFielding?.fullName, `${locale} 缺少 gsFielding`);
  }
});

test('自由打数 / 四坏触身在简中、繁中、英文齐全', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en']) {
    const data = readJson(`../src/locales/${locale}.json`);
    for (const key of ['pitchAb', 'bbHp']) {
      assert.ok(data.glossary.term[key]?.fullName, `${locale} 缺少 glossary.term.${key}.fullName`);
      assert.ok(data.stats.col[key], `${locale} 缺少 stats.col.${key}`);
      assert.ok(data.stats.colEn[key], `${locale} 缺少 stats.colEn.${key}`);
    }
  }
});
