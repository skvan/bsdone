// 结果录入页：引擎场次 final 允许直接改投手 R/ER，保存时带人工覆写标记；投手行可转记。
// 运行：node --test tests/gameResultPitcherRunEdit.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const games = fs.readFileSync(
  new URL('../src/views/admin/Games.js', import.meta.url),
  'utf8'
);
const dialog = fs.readFileSync(
  new URL('../src/components/admin/PitcherTransferDialog.vue', import.meta.url),
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

test('投手行转记入口：主客队工具栏 + 选择弹窗 + 转记后单独删除确认', () => {
  assert.ok(
    games.includes('openPitcherTransfer(!0)') && games.includes('openPitcherTransfer(!1)'),
    '主客队投手工具栏缺少转记入口'
  );
  assert.ok(
    games.includes('PitcherTransferDialog') && games.includes('confirmPitcherTransfer'),
    '未接线转记选择弹窗'
  );
  assert.ok(games.includes('是否删除源行'), '转记成功后未单独询问删除源行');
  assert.ok(
    games.includes('nonZeroPitcherFields') && games.includes('去转记'),
    '删除诊断缺少剩余字段/推荐转记'
  );
});

test('转记弹窗含目标选择、预览和“不自动删除”提示', () => {
  assert.ok(dialog.includes('选择同队投手'), '缺少目标投手选择');
  assert.ok(dialog.includes('转记后'), '缺少转记预览');
  assert.ok(dialog.includes('确认转记只执行转记'), '缺少不自动删除提示');
});

test('PA 列与 BF 双向同步', () => {
  assert.ok(games.includes('key:"pitchPa",label:"PA"'), '投手表缺少 PA 列');
  assert.ok(
    games.includes('p.pitchPa=g,p.pitchBf=g') && games.includes('p.pitchBf=g,p.pitchPa=g'),
    'PA/BF 未双向同步'
  );
});

test('删除诊断直接定位待转记行，不依赖当前高亮行', () => {
  assert.ok(
    games.includes('Ha(t?"away-pitcher":"home-pitcher",dn(t,blocker.row))'),
    '去转记未按主客队更新当前行键'
  );
  assert.ok(
    games.includes('openPitcherTransfer(t,blocker.row)'),
    '去转记未直接传入待转记行'
  );
  assert.ok(
    games.includes('sourceKey=e?Pe(e):key'),
    '转记源行仍只依赖当前高亮行'
  );
});

test('老用户列设置迁移补齐 PA 列', () => {
  assert.ok(games.includes('columnFields583'), '缺少 PA 列迁移标记');
  assert.ok(
    games.includes('a(uo,["pitchPa"]),a(io,["pitchPa"])'),
    '迁移未补齐投手可见列/排序'
  );
});
