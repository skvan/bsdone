// 结果录入页：引擎场次 final 允许直接改投手 R/ER，保存时带人工覆写标记。
// 运行：node --test tests/gameResultPitcherRunEdit.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const games = fs.readFileSync(
  new URL('../src/views/admin/Games.js', import.meta.url),
  'utf8'
);

test('final 引擎场次的 R/ER 输入在结果页可编辑', () => {
  const editable = 'disabled:engineManaged.value&&m.status!=="final"';
  assert.equal(games.split(editable).length - 1, 4, 'R/ER 四个输入应使用 final 解锁条件');
  assert.ok(
    !games.includes('disabled:engineManaged.value,controls'),
    '仍存在未解锁的引擎场次 R/ER 输入'
  );
});

test('修改 R/ER 时记录人工覆写并写入保存载荷', () => {
  assert.ok(
    games.includes('markPitcherRunManualByIndex(!0,r)') && games.includes('markPitcherRunManualByIndex(!1,r)'),
    '主客队 R/ER 修改未标记人工覆写'
  );
  assert.ok(
    games.includes('manualPitcherRunOverride:engineManaged.value'),
    '保存载荷缺少 manualPitcherRunOverride'
  );
  assert.ok(
    games.includes('manualPitcherRunIds.clear()'),
    '加载/保存后未清空人工覆写标记'
  );
});
