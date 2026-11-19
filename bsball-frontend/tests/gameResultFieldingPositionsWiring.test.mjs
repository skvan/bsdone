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
    compact.includes("groupFieldingPositionRows,applyFieldingPositionTotals,buildFieldingPlayerParts,positionOptions,validateFieldingPositionRows}from'../../utils/gameResultFieldingPositions'"),
    '缺少逐位置工具导入'
  );
  assert.ok(
    compact.includes("import{inningsToOutsasresultInningsToOuts}from'../../utils/inningsFormat'"),
    '缺少 inningsToOuts 导入'
  );
});

test('逐位置编辑统一由 markFieldingPositionsChanged 替换引用', () => {
  assert.match(compact, /functionmarkFieldingPositionsChanged\(row,nextRows\)\{/, 'mark 未接收本次 rows');
  assert.ok(compact.includes('constrows=nextRows??positionRowsFor(row)'), 'mark 未复用调用方 rows');
  assert.equal(
    (compact.match(/resultPositionRows\.value=\{\.\.\.resultPositionRows\.value,\[key\]:\[\.\.\.rows\]\}/g) ?? []).length,
    1,
    '引用替换应只在 mark 中发生一次'
  );
  assert.ok(
    compact.includes('row._fieldPosition=rows.length>0?rows[0].position:""'),
    '删光守位后未清空 _fieldPosition'
  );
});

test('新建/切换比赛时清理逐位置状态，避免脏数据跨比赛', () => {
  assert.ok(
    compact.includes('resultPositionRows.value={},dirtyFieldingPlayers.clear(),te.value=[]'),
    'Mn() 未清空 resultPositionRows 与 dirty 球员'
  );
});

test('添加守位优先显式位置，旧 emit 回退当前守位并校验白名单/占用', () => {
  assert.ok(compact.includes('constnormalize=value=>String(value??"").trim().toUpperCase()'), '缺少守位归一');
  assert.ok(compact.includes('lettarget=normalize(position)'), '未优先使用显式传入的守位');
  assert.ok(
    compact.includes('constcurrent=normalize(row?._fieldPosition||row?.position)'),
    '旧 emit 未优先当前守位'
  );
  assert.ok(
    compact.includes('target=allowed.includes(current)&&!used.has(current)?current:""'),
    '当前守位未校验模式白名单与占用'
  );
  assert.ok(
    compact.includes('target=allowed.find(item=>!used.has(item))??""'),
    '缺少首个可用守位回落'
  );
  assert.ok(compact.includes('if(!target){J.warning("守位已满");return}'), '无可用守位未提示');
  assert.ok(
    compact.includes('if(!allowed.includes(target)||used.has(target))return'),
    '显式守位未校验白名单/重复'
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

test('添加守位 seed：捕手字段只给 C 行，其他守位从零开始', () => {
  assert.ok(
    compact.includes('constseedCatcher=seed&&target==="C"'),
    '缺少捕手 seed 判定'
  );
  for (const needle of [
    'pb:seedCatcher?Number(stats?.pb)||0:0',
    'catcherSb:seedCatcher?Number(stats?.catcherSb)||0:0',
    'catcherCs:seedCatcher?Number(stats?.catcherCs)||0:0'
  ]) {
    assert.ok(compact.includes(needle), `捕手 seed 未限定 C 行：${needle}`);
  }
});

test('删除守位先二次确认，取消不改 rows', () => {
  assert.ok(
    compact.includes(
      'asyncfunctionremoveFieldingPosition(row,position){try{awaitdt.confirm("确定删除该守位行？","删除确认",{confirmButtonText:"确定",cancelButtonText:"取消",type:"warning"})}catch{return}'
    ),
    '删除守位未先确认并在取消时直接返回'
  );
  assert.ok(
    compact.includes(
      'constrows=positionRowsFor(row).filter(item=>item.position!==position);markFieldingPositionsChanged(row,rows)'
    ),
    '确认后未再执行 splice/mark'
  );
});

test('保存前校验所有 dirty 球员逐位置行，错误带球员名', () => {
  assert.ok(
    compact.includes(
      'functionvalidateDirtyFieldingPositions(){for(constkeyofdirtyFieldingPlayers){consterror=validateFieldingPositionRows(resultPositionRows.value[key]??[],allowedFieldingPositions());if(error)thrownewError(`${fieldingPlayerName(key)}逐位置守备：${error}`)}}'
    ),
    'dirty 球员未逐位置校验或错误未带球员名'
  );
  assert.ok(
    compact.includes('returnu(playerId)?.name??playerId'),
    '球员名未按 u(playerId)?.name 回退 playerId'
  );
  assert.ok(
    compact.includes('validateDirtyFieldingPositions();be.value=!0'),
    '保存流程未在提交前调用逐位置校验'
  );
});

test('删除统计行后清理对应逐位置状态，避免同时提交 removedStatIds 与 parts', () => {
  assert.ok(
    compact.includes('constactivePlayers=newSet(rows.map(S=>`${S.teamId}_${S.playerId}`))'),
    '未按 payload 行统计仍保留的球员'
  );
  assert.ok(
    compact.includes('if(activePlayers.has(key))continue'),
    '仍有统计行的球员不应被清理'
  );
  assert.ok(
    compact.includes('dirtyFieldingPlayers.delete(key)'),
    '无统计行球员未从 dirty 中移除'
  );
  assert.ok(
    compact.includes('deletenext[key];resultPositionRows.value=next'),
    '无统计行球员未清理逐位置 rows'
  );
});
