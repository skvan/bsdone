// 静态断言：结果录入守备表逐位置展开编辑、增删守位与 i18n 词条（Task 4）
// 运行：node --test tests/gameFieldingTablePositions.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));

const CATCHER_KEYS = ['pb', 'catcherSb', 'catcherCs'];

// 从 <script setup> 抽取纯函数源码，注入依赖后做行为断言（组件不便整体渲染）
function extractFunction(table, name) {
  const script = table.match(/<script setup>([\s\S]*?)<\/script>/)?.[1] ?? '';
  const source = script.match(new RegExp(`function ${name}\\([^)]*\\) \\{[\\s\\S]*?\\n\\}`))?.[0];
  assert.ok(source, `缺少 ${name}`);
  return source;
}

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

test('切换守位离开捕手位时清零 PB/SB/CS 并通知页面', () => {
  const table = read('../src/components/admin/GameFieldingTable.vue');
  assert.ok(
    table.includes("const CATCHER_ONLY_KEYS = ['pb', 'catcherSb', 'catcherCs'];"),
    '捕手字段清单不完整'
  );
  const events = [];
  const changePosition = new Function(
    'CATCHER_ONLY_KEYS',
    'emit',
    `${extractFunction(table, 'changePosition')}\nreturn changePosition;`
  )(CATCHER_KEYS, (...args) => events.push(args));

  const parent = { id: 9 };
  const source = { position: 'C', pb: 3, catcherSb: 2, catcherCs: 1 };
  const row = { ...source, _positionRow: true, _parentRow: parent, _source: source };
  changePosition(row, '1b');
  assert.equal(source.position, '1B', '守位未规范化写回');
  for (const key of CATCHER_KEYS) {
    assert.equal(source[key], 0, `离开捕手位后 ${key} 未在源对象清零`);
    assert.equal(row[key], 0, `离开捕手位后 ${key} 未在展示副本清零`);
  }
  assert.deepEqual(events.at(-1), ['positions-changed', parent], '清零后未通知页面');

  // 换到捕手位不应清空
  const catcherSource = { position: '1B', pb: 4, catcherSb: 1, catcherCs: 2 };
  const catcherRow = { ...catcherSource, _parentRow: parent, _source: catcherSource };
  changePosition(catcherRow, 'C');
  assert.equal(catcherSource.pb, 4, '换到捕手位不应清零 PB');
  assert.equal(catcherSource.catcherSb, 1);
  assert.equal(catcherSource.catcherCs, 2);
});

test('子行守位下拉包含当前守位且去重', () => {
  const table = read('../src/components/admin/GameFieldingTable.vue');
  assert.ok(
    table.includes('positionOptionsWithCurrent(row, positionOptionsFor(row._parentRow))'),
    '下拉未合并当前守位'
  );
  const options = new Function(
    `${extractFunction(table, 'positionOptionsWithCurrent')}\nreturn positionOptionsWithCurrent;`
  )();
  assert.deepEqual(options({ position: 'C' }, ['1b', 'C', '2B']), ['C', '1B', '2B']);
  assert.deepEqual(options({ position: 'P' }, ['C']), ['P', 'C']);
  assert.deepEqual(options({ position: '' }, ['C']), ['C']);
});

test('守备表编辑门禁、GS 可编辑与 final 提示', () => {
  const table = read('../src/components/admin/GameFieldingTable.vue');
  assert.ok(table.includes('v-if="editable"'), '缺少增删按钮 editable 门禁');
  assert.ok(table.includes('v-else-if="editable"'), '缺少添加守位 editable 门禁');
  assert.ok(table.includes(':disabled="!editable"'), 'POS 下拉缺少 editable 门禁');
  assert.ok(
    table.includes(':disabled="!editable || !positionFieldEnabled(row, col.key)"'),
    '子行统计缺少 editable 门禁'
  );
  assert.ok(table.includes("v-else>{{ t('gameResultFielding.finalOnly') }}"), '缺少 final-only 提示分支');
  // GS 是球员级字段：有逐位置明细时也保持可编辑
  const gsStart = table.indexOf("col.key === 'fieldingGs'");
  const gsEnd = table.indexOf("setValue(row, 'fieldingGs', value)");
  assert.ok(gsStart > -1 && gsEnd > gsStart, '缺少 GS 输入');
  const gsBlock = table.slice(gsStart, gsEnd);
  assert.ok(!gsBlock.includes(':disabled'), 'GS 输入不应禁用（球员级保持可编辑）');
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
