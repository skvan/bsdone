// 阵容板画布尺寸计算单测（纯函数）
// 运行：node --test tests/lineupCanvasFit.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  FIELD_MIN_WIDTH, FIELD_BOTTOM_MARGIN, COMPACT_FORM_THRESHOLD, COMPACT_FORM_HYSTERESIS,
  computeFieldCap, shouldCompactForm,
} from '../src/utils/lineupCanvasFit.js';
import { FIELD_VIEWBOX, SLOT_SIZE } from '../src/utils/lineupFieldPositions.js';

// 基准取线上实测（1280×792 视口）：球场顶部 423、网格容器宽 1015
const BASE = { viewportHeight: 792, boardTop: 423, boardWidth: 1015 };

test('高度受限：按视口高度反推列宽（1280×792 → 303px）', () => {
  assert.equal(computeFieldCap(BASE), 303);
});

test('设置区压缩后顶部上移（423→353），球场随之变大到 363px', () => {
  assert.equal(computeFieldCap({ ...BASE, boardTop: 353 }), 363);
});

test('下限 300px：矮屏/负可用高度都不低于 300', () => {
  assert.equal(computeFieldCap({ viewportHeight: 400, boardTop: 350, boardWidth: 1015 }), 300);
  assert.equal(computeFieldCap({ viewportHeight: 300, boardTop: 350, boardWidth: 1015 }), 300);
});

test('宽度受限：容器窄时按宽度上限，且不低于 300', () => {
  // 宽度侧 = 900 − 560 − 28 = 312 < 高度侧 821
  assert.equal(computeFieldCap({ viewportHeight: 1400, boardTop: 423, boardWidth: 900 }), 312);
  // 宽度侧 = 700 − 588 = 112 → 下限 300
  assert.equal(computeFieldCap({ viewportHeight: 1400, boardTop: 423, boardWidth: 700 }), 300);
});

test('大屏（1920×1080）：球场 548px ≥ 阈值，设置区维持三行', () => {
  const m = { viewportHeight: 1080, boardTop: 423, boardWidth: 1655 };
  assert.equal(computeFieldCap(m), 548);
  assert.equal(shouldCompactForm({ ...m, boardTopRoomy: 423 }), false);
});

test('矮屏（1280×792）高度受限：压缩设置区让位', () => {
  assert.equal(shouldCompactForm({ ...BASE, boardTopRoomy: 423, currentlyCompact: false }), true);
});

test('宽度受限（竖屏窄窗）：压缩不会增大画布，维持现状', () => {
  assert.equal(shouldCompactForm({ viewportHeight: 1400, boardTopRoomy: 423, boardWidth: 700 }), false);
});

test('回差：已压缩时阈值 480~490 之间保持压缩，到 490 以上才展开', () => {
  const boardWidth = 1655;
  const viewportFor = (cap) => Math.ceil((cap * FIELD_VIEWBOX.height) / FIELD_VIEWBOX.width + 423 + FIELD_BOTTOM_MARGIN);
  const at485 = viewportFor(485);
  assert.equal(shouldCompactForm({ viewportHeight: at485, boardTopRoomy: 423, boardWidth, currentlyCompact: true }), true);
  assert.equal(shouldCompactForm({ viewportHeight: at485, boardTopRoomy: 423, boardWidth, currentlyCompact: false }), false);
  const at492 = viewportFor(COMPACT_FORM_THRESHOLD + COMPACT_FORM_HYSTERESIS + 2);
  assert.equal(shouldCompactForm({ viewportHeight: at492, boardTopRoomy: 423, boardWidth, currentlyCompact: true }), false);
});

test('CSS 与 JS 常量同步：卡槽尺寸、右侧两列上限、间距、CSS 变量', () => {
  const css = fs.readFileSync(new URL('../src/styles/lineup-board.css', import.meta.url), 'utf8');
  const ratio = +(SLOT_SIZE.ratio * 100).toFixed(1);
  const guardRatio = +(SLOT_SIZE.guardRatio * 100).toFixed(1);
  assert.ok(
    css.includes(`min(clamp(${SLOT_SIZE.min}px, ${ratio}%, ${SLOT_SIZE.max}px), ${guardRatio}%)`),
    '卡槽尺寸与 SLOT_SIZE 不一致',
  );
  assert.ok(css.includes('minmax(170px, 240px) minmax(240px, 320px)'), '右侧两列上限与 FIELD_SIDE_COLUMNS 不一致');
  assert.ok(css.includes('gap: 14px'), '列间距与 FIELD_GAP_TOTAL 不一致');
  assert.ok(css.includes('--lineup-field-cap'), '缺少列宽上限 CSS 变量');
  assert.ok(css.includes(`minmax(${FIELD_MIN_WIDTH}px,`), '列宽下限与 FIELD_MIN_WIDTH 不一致');
});
