// 静态门禁：实时录入全屏 Play-by-Play（#359 基础 + 四向吸附）
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const component = read('../src/components/admin/LiveGamePlayByPlay.vue');
const splitter = read('../src/components/admin/LiveGamePbpSplitter.vue');
const liveGame = read('../src/views/admin/LiveGame.js');
const css = read('../src/styles/legacy/live-game.css');

test('PBP 面板提供四向吸附按钮与日志列表', () => {
  assert.match(component, /class="lg-pbp"/);
  assert.match(component, /aria-live="polite"/);
  assert.match(component, /aria-atomic="true"/);
  assert.match(component, /role="group" aria-label="日志吸附位置"/);
  assert.match(component, /v-for="item in visibleDocks"/);
  assert.match(component, /@click="\$emit\('dock-change', item.key\)"/);
  assert.match(component, /v-for="\(entry, index\) in entries"/);
  assert.match(component, /:key="entry\.key"/);
  assert.match(component, /lg-pbp__sr/);
  assert.match(component, /entry\.inning/);
  assert.match(component, /entry\.time/);
  assert.match(component, /entry\.tail/);
  assert.match(component, /entry\.lines/);
  assert.match(component, /buildPbpEntries/);
  assert.match(component, /props\.allowHorizontal \? DOCK_ITEMS : DOCK_ITEMS\.filter/);
});

test('分界线组件支持拖动、双击复位与键盘调整', () => {
  assert.match(splitter, /role="separator"/);
  assert.match(splitter, /:aria-orientation="sideDock \? 'vertical' : 'horizontal'"/);
  assert.match(splitter, /@pointerdown="startDrag"/);
  assert.match(splitter, /@dblclick="resetSize"/);
  assert.match(splitter, /@keydown="onKeydown"/);
  assert.match(splitter, /parent\.dataset\.dock = props\.dock/);
  assert.match(splitter, /parent\.style\.setProperty\('--pbp-size'/);
  assert.match(splitter, /persistPbpSize\(props\.dock, size\.value\)/);
});

test('PBP 面板只在全屏时挂载，并透传 eventLog 与吸附方向', () => {
  assert.match(
    liveGame,
    /import LiveGamePlayByPlay from '\.\.\/\.\.\/components\/admin\/LiveGamePlayByPlay\.vue'/
  );
  assert.match(
    liveGame,
    /import LiveGamePbpSplitter from '\.\.\/\.\.\/components\/admin\/LiveGamePbpSplitter\.vue'/
  );
  assert.match(liveGame, /c\(oe\)\?v\(LiveGamePbpSplitter,\{dock:pbpEffectiveDock\.value\}/);
  assert.match(
    liveGame,
    /c\(oe\)\?v\(LiveGamePlayByPlay,\{logs:ke\.value,dock:pbpEffectiveDock\.value,allowHorizontal:!pbpNarrow\.value,onDockChange:setPbpDock\}/
  );
});

test('全屏四向吸附样式约束网格轨道、分界线与球场缩放', () => {
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
    /\.card-body-fullscreen \.field-svg-wrap>\.ballpark-svg\[data-v-78b7b6a8\]\{flex:1 1 auto;min-height:200px;width:auto;height:100%;max-width:100%!important;margin:0 auto\}/
  );
  assert.match(css, /\.live-pbp-shell\[data-v-78b7b6a8\]\{--pbp-size:220px;display:grid/);
  for (const dock of ['top', 'bottom', 'left', 'right']) {
    assert.match(css, new RegExp(`\\.live-pbp-shell\\[data-dock=${dock}\\]\\[data-v-78b7b6a8\\]\\{grid-template-areas:`));
  }
  assert.match(css, /\.live-pbp-shell\[data-v-78b7b6a8\]:has\(>\.main-content:only-child\)\{display:flex/);
  assert.match(css, /\.live-pbp-shell\[data-dock=left\]>\.live-pbp-splitter,\.live-pbp-shell\[data-dock=right\]>\.live-pbp-splitter\{cursor:col-resize\}/);
  assert.match(css, /\.live-pbp-shell\[data-dock=top\]>\.lg-pbp \.lg-pbp__head\{padding-right:96px\}/);
  assert.match(css, /body\.lg-pbp-dock-resizing\{user-select:none!important;cursor:row-resize!important\}/);
  assert.match(css, /body\.lg-pbp-dock-resizing--col\{user-select:none!important;cursor:col-resize!important\}/);
});
