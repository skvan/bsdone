// 静态断言：结果录入守备表逐位置展开编辑、增删守位与 i18n 词条（Task 4）
// 运行：node --test tests/gameFieldingTablePositions.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));

test('守备表接入逐位置展开与增删', () => {
  const table = read('../src/components/admin/GameFieldingTable.vue');
  for (const needle of [
    'positionRowsFor',
    'positionOptionsFor',
    'editable',
    'add-position',
    'remove-position',
    'positions-changed',
    'displayRows',
    'expandedKeys',
    '_positionRow'
  ]) {
    assert.ok(table.includes(needle), `缺少 ${needle}`);
  }
  assert.ok(table.includes('hasPlayerPositions'), '缺少球员级只读合计判定');
});

test('守备表保留球员级合计与 GS 单列', () => {
  const table = read('../src/components/admin/GameFieldingTable.vue');
  // GS 仍在既有 columns 循环内特殊处理，不额外新增第二列
  assert.equal(table.match(/key: 'fieldingGs'/g)?.length, 1, 'fieldingGs 列定义重复');
  assert.ok(table.includes("col.key === 'fieldingGs'"), 'GS 未按列 key 特殊处理');
  // 汇总行按列 prop 映射，首列保留 playerName 才能显示 Totals
  assert.ok(table.includes('prop="playerName"'), '首列缺少 playerName prop');
  assert.ok(table.includes(':prop="col.key"'), '动态列缺少 prop，汇总行会错位');
  assert.ok(table.includes('buildFieldingSummaryCells'), '缺少合计行构建');
});

test('守备表子行修改回写原对象并通知页面', () => {
  const table = read('../src/components/admin/GameFieldingTable.vue');
  assert.ok(table.includes('row._source[key] = Number(value) || 0'), '子行数值未回写原对象');
  assert.ok(table.includes("emit('positions-changed', row._parentRow)"), '子行修改未通知页面');
  assert.ok(table.includes("emit('remove-position', row._parentRow, row.position)"), '删除守位未带父行与守位');
  assert.ok(table.includes("emit('add-position', row)"), '添加守位事件未接入');
  assert.ok(table.includes("row.position === 'C'"), 'PB/CS/SB 未按捕手守位启用');
  // 逐位置行以 outs 存储，INN 列需换算而不是直接写 defInn
  assert.ok(table.includes('inningsToOuts') && table.includes('outsToInnings'), 'INN 未按 outs 换算');
  assert.ok(table.includes("key === 'defInn'"), 'INN 列未单独处理');
});

test('五语种 gameResultFielding 词条键一致', () => {
  const locales = ['zh-CN', 'zh-TW', 'en', 'ja', 'ko'];
  const keys = [
    'positions',
    'addPosition',
    'removePosition',
    'autoTotal',
    'positionsCount',
    'autoTotalHint',
    'finalOnly',
    'noPositions'
  ];
  const dicts = Object.fromEntries(locales.map((name) => [name, readJson(`../src/locales/${name}.json`)]));
  for (const name of locales) {
    const block = dicts[name].gameResultFielding;
    assert.ok(block, `${name} 缺少 gameResultFielding`);
    assert.deepEqual(Object.keys(block).sort(), [...keys].sort(), `${name} 词条键不一致`);
    for (const key of keys) {
      assert.equal(typeof block[key], 'string', `${name}.${key} 不是字符串`);
      assert.ok(block[key].trim(), `${name}.${key} 为空`);
    }
  }
  const table = read('../src/components/admin/GameFieldingTable.vue');
  for (const key of keys) {
    if (key === 'positions' || key === 'autoTotal' || key === 'noPositions') continue;
    assert.ok(table.includes(`gameResultFielding.${key}`), `组件未使用 gameResultFielding.${key}`);
  }
});
