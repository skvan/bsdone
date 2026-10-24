// 静态门禁：实时录入全屏下方 Play-by-Play（#359）
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const component = read('../src/components/admin/LiveGamePlayByPlay.vue');
const liveGame = read('../src/views/admin/LiveGame.js');
const css = read('../src/styles/legacy/live-game.css');

test('全屏 PBP 面板包含可拖动分隔条与日志列表', () => {
  assert.match(component, /class="lg-pbp"/);
  assert.match(component, /role="separator"/);
  assert.match(component, /aria-live="polite"/);
  assert.match(component, /aria-atomic="true"/);
  assert.match(component, /tabindex="0"/);
  assert.match(component, /aria-orientation="horizontal"/);
  assert.match(component, /@pointerdown="startResize"/);
  assert.match(component, /@keydown="handleKeydown"/);
  assert.match(component, /v-for="\(entry, index\) in entries"/);
  assert.match(component, /:key="entry\.key"/);
  assert.match(component, /lg-pbp__sr/);
  assert.match(component, /entry\.inning/);
  assert.match(component, /entry\.time/);
  assert.match(component, /entry\.tail/);
  assert.match(component, /entry\.lines/);
  assert.match(component, /PBP_STORAGE_KEY/);
  assert.match(component, /clampPbpHeight/);
  assert.match(component, /buildPbpEntries/);
});

test('PBP 面板只在全屏时挂载，并透传实时 eventLog', () => {
  assert.match(
    liveGame,
    /import LiveGamePlayByPlay from '\.\.\/\.\.\/components\/admin\/LiveGamePlayByPlay\.vue'/
  );
  assert.match(liveGame, /c\(oe\)\?v\(LiveGamePlayByPlay,\{logs:ke\.value\}\):W\("",!0\)/);
});

test('全屏上下分屏样式约束场地缩放与 PBP 高度', () => {
  assert.match(css, /\.card-body-wrap\.card-body-fullscreen\[data-v-78b7b6a8\]\{overflow:hidden\}/);
  assert.match(
    css,
    /\.card-body-fullscreen \.main-content--entry-only\[data-v-78b7b6a8\]\{[^}]*flex-wrap:nowrap[^}]*align-items:stretch;gap:0\}/
  );
  assert.match(
    css,
    /\.card-body-fullscreen \.field-column\[data-v-78b7b6a8\]\{display:flex;flex-direction:column;flex:1 1 auto;min-height:0\}/
  );
  assert.match(
    css,
    /\.card-body-fullscreen \.field-panel\[data-v-78b7b6a8\]\{[^}]*overflow:auto!important\}/
  );
  assert.match(
    css,
    /@media \(min-width:769px\)\{[\s\S]*?\.card-body-fullscreen \.field-panel\[data-v-78b7b6a8\]\{display:flex;flex-direction:column\}/
  );
  assert.match(
    css,
    /\.card-body-fullscreen \.field-svg-wrap>\.ballpark-svg\[data-v-78b7b6a8\]\{flex:1 1 auto;min-height:200px;width:100%;height:auto;max-width:900px;margin:0 auto\}/
  );
  assert.match(css, /body\.lg-pbp-resize-active\{user-select:none!important;cursor:row-resize!important\}/);
});
