import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const games = fs.readFileSync(new URL('../src/views/admin/Games.js', import.meta.url), 'utf8');
const compact = games.replace(/\s+/g, '');

test('结果页接入逐位置状态与保存 payload', () => {
  for (const needle of [
    'groupFieldingPositionRows',
    'resultPositionRows',
    'dirtyFieldingPlayers',
    'buildFieldingPlayerParts',
    'applyFieldingPositionTotals',
    'fieldingPlayerParts',
    'positionRowsFor',
    'addFieldingPosition',
    'removeFieldingPosition',
    'markFieldingPositionsChanged'
  ]) {
    assert.ok(games.includes(needle), `缺少 ${needle}`);
  }
});

test('逐位置工具与局数换算从既有工具导入', () => {
  assert.ok(
    compact.includes("groupFieldingPositionRows,applyFieldingPositionTotals,buildFieldingPlayerParts,positionOptions}from'../../utils/gameResultFieldingPositions'"),
    '缺少逐位置工具导入'
  );
  assert.ok(
    compact.includes("import{inningsToOutsasresultInningsToOuts}from'../../utils/inningsFormat'"),
    '缺少 inningsToOuts 导入'
  );
});

test('编辑后替换 resultPositionRows 引用以触发组件重算', () => {
  assert.ok(
    compact.includes('resultPositionRows.value={...resultPositionRows.value,[key]:[...rows]}'),
    'markFieldingPositionsChanged 未替换 resultPositionRows 引用'
  );
  assert.ok(
    compact.includes('resultPositionRows.value={...resultPositionRows.value,[key]:rows}'),
    '增删守位未替换 resultPositionRows 引用'
  );
});

test('装载即汇总：分组、清理 dirty 并回写球员级合计与首守位', () => {
  assert.ok(
    compact.includes('resultPositionRows.value=groupFieldingPositionRows(fieldingPositionRows)'),
    'Na() 未装载逐位置分组'
  );
  assert.ok(
    compact.includes('dirtyFieldingPlayers.clear()'),
    'Na() 未清理 dirty 球员'
  );
  assert.match(
    compact,
    /for\(const\[key,rows\]ofObject\.entries\(resultPositionRows\.value\)\)\{for\(constrowof\[\.\.\.te\.value,\.\.\.le\.value\]\)\{if\(`\$\{row\.teamId\}_\$\{row\.id\}`!==key\)continue;conststats=D\[X\(row,row\.teamId\?\?m\.homeTeamId\)\];if\(stats\)applyFieldingPositionTotals\(stats,rows\);if\(rows\.length\)row\._fieldPosition=rows\[0\]\.position;break/,
    'Na() 未按球员回写逐位置合计'
  );
});

test('保存 payload 携带脏球员逐位置明细并在成功后清空', () => {
  const payload = /fieldingPlayerParts:buildFieldingPlayerParts\(dirtyFieldingPlayers,resultPositionRows\.value\)/g;
  assert.equal((compact.match(payload) ?? []).length, 2, '修改/新增保存未同时携带 fieldingPlayerParts');
  assert.ok(
    compact.includes('}),dirtyFieldingPlayers.clear(),removedStatRows.clear(),manualPitcherRunIds.clear(),J.success("修改成功")'),
    '修改保存成功后未清空 dirty 球员'
  );
  assert.ok(
    compact.includes('}),dirtyFieldingPlayers.clear(),removedStatRows.clear(),manualPitcherRunIds.clear(),J.success("新增成功")'),
    '新增保存成功后未清空 dirty 球员'
  );
});

test('两处守备表传入逐位置 props 与事件，且仅 final 可编辑', () => {
  assert.equal(
    (compact.match(/"position-rows-for":positionRowsFor/g) ?? []).length,
    2,
    '两处守备表未同时传入 positionRowsFor'
  );
  assert.equal(
    (compact.match(/"position-options-for":positionOptionsFor/g) ?? []).length,
    2,
    '两处守备表未同时传入 positionOptionsFor'
  );
  assert.equal(
    (compact.match(/editable:m\.status==="final"/g) ?? []).length,
    2,
    '两处守备表未按 final 控制可编辑'
  );
  for (const needle of [
    'onAddPosition:addFieldingPosition',
    'onRemovePosition:removeFieldingPosition',
    'onPositionsChanged:markFieldingPositionsChanged'
  ]) {
    assert.ok(compact.includes(needle), `缺少 ${needle}`);
  }
});

test('旧球员首个守位用现有合计 seed，其余新行从零开始', () => {
  assert.ok(compact.includes('constseed=rows.length===0'), '缺少首个守位 seed 判定');
  assert.ok(
    compact.includes('outs:seed?resultInningsToOuts(Number(stats?.defInn)||0):0'),
    '首个守位未用现有 INN seed'
  );
  assert.ok(
    compact.includes('po:seed?Number(stats?.po)||0:0'),
    '首个守位未用现有合计 seed'
  );
  assert.ok(
    compact.includes('fieldingGs:seed?Number(stats?.fieldingGs)===1:false'),
    '首个守位未沿用球员级 GS'
  );
});
