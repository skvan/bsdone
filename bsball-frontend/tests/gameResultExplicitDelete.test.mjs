// 结果页保存改为显式删除：只有本页移除的行带 removedStatIds，payload 漏带不再删除真实行
// 运行：cd bsball-frontend && node --test tests/gameResultExplicitDelete.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

function read(rel) {
  return fs.readFileSync(new URL('../src/' + rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
}

test('结果页记录被移除统计行的 statId 及其行身份', () => {
  const src = read('views/admin/Games.js');
  assert.ok(src.includes('const removedStatRows=new Map()'), '缺少 removedStatRows 映射');
  assert.ok(
    src.includes('removedStatRows.set(statId,`${S.teamId??u}_${S.id}_${n?1:0}`)'),
    '批量删除时未记录 statId 与 teamId_playerId_isPitcher 身份'
  );
});

test('保存请求显式携带 removedStatIds，成功后清空', () => {
  const src = read('views/admin/Games.js');
  const occurrences = src.split('removedStatIds:removedStatIdsFor(').length - 1;
  assert.ok(occurrences >= 2, '编辑保存与新增保存都应携带 removedStatIds');
  assert.ok(src.includes('removedStatRows.clear()'), '保存后未清空 removedStatRows');
});

test('加载统计时不保留上一次页面的删除名单', () => {
  const src = read('views/admin/Games.js');
  assert.ok(
    /listByGame\(pe\.value\);.*removedStatRows\.clear\(\)/.test(src),
    'Na() 加载统计后未清空 removedStatRows'
  );
});

test('删除后又补录同一球员：保存时不再提交该 statId', () => {
  const src = read('views/admin/Games.js');
  const start = src.indexOf('function removedStatIdsFor(rows){');
  assert.ok(start >= 0, '缺少 removedStatIdsFor');
  const helper = src.slice(start, src.indexOf('function zn(', start));
  assert.ok(
    helper.includes('rows.map(S=>`${S.teamId}_${S.playerId}_${S.isPitcher?1:0}`)'),
    '补录身份未按 teamId_playerId_isPitcher 与删除名单比对'
  );
  assert.ok(
    helper.includes('.filter(([,key])=>!current.has(key))'),
    '重新补录的行仍会被提交为删除'
  );
});
