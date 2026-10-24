// 阵容板布局静态断言：球场按可用宽度填满、卡槽随场地缩放、底部冗余提示已移除
// 运行：node --test tests/lineupCanvasFit.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { SLOT_SIZE } from '../src/utils/lineupFieldPositions.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');

test('球场列宽按容器宽度填满，右侧两列上限与间距保持', () => {
  const css = read('../src/styles/lineup-board.css');
  assert.ok(
    css.includes('grid-template-columns: minmax(300px, calc(100% - 588px)) minmax(170px, 240px) minmax(240px, 320px)'),
    '球场列宽未按容器宽度填满',
  );
  assert.ok(css.includes('gap: 14px'), '列间距与设计不一致');
  assert.ok(!css.includes('--lineup-field-cap'), '不应再依赖 JS 列宽变量');
});

test('卡槽尺寸与 SLOT_SIZE 同步（随场地等比缩放）', () => {
  const css = read('../src/styles/lineup-board.css');
  const ratio = +(SLOT_SIZE.ratio * 100).toFixed(1);
  const guardRatio = +(SLOT_SIZE.guardRatio * 100).toFixed(1);
  assert.ok(
    css.includes(`min(clamp(${SLOT_SIZE.min}px, ${ratio}%, ${SLOT_SIZE.max}px), ${guardRatio}%)`),
    '卡槽尺寸与 SLOT_SIZE 不一致',
  );
});

test('底部仅保留 EP 未开放提示，空位/投手提示已移除', () => {
  const vue = read('../src/views/admin/LiveGameLineupBoard.vue');
  assert.ok(vue.includes('visibleLineupError'), '未接入提示过滤器');
  assert.ok(vue.includes('lineupErrorMsg'), '提示渲染缺失');
  assert.ok(!vue.includes('须指定先发投手后才可开始录入'), '投手提示未移除');
  const css = read('../src/styles/lineup-board.css');
  assert.ok(css.includes('.lineup-warn'), 'EP 提示样式缺失');
});

test('移动端保持单列，球场撑满', () => {
  const css = read('../src/styles/lineup-board.css');
  assert.ok(css.includes('@media (max-width: 768px)'), '缺少移动端断点');
  assert.ok(css.includes('grid-template-columns: 1fr'), '移动端未回落单列');
});
