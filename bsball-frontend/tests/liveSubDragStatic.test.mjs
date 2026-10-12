// 静态断言：替补席拖拽换人（#293）接线门禁
// 运行：node --test tests/liveSubDragStatic.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const LIVE = '../src/views/admin/LiveGame.js';
const UTIL = '../src/utils/liveSubDrag.js';
const CSS = '../src/styles/legacy/live-game.css';

test('纯函数层不依赖 DOM/框架（可被 node --test 直接加载）', () => {
  const src = read(UTIL);
  const imports = [...src.matchAll(/from\s+'([^']+)'/g)].map((m) => m[1]);
  assert.deepEqual(imports, [], `liveSubDrag.js 不应引入任何依赖：${imports.join(',')}`);
  assert.ok(!/\bdocument\b|\bwindow\b|appendChild|createElement/.test(src), '纯函数层不得触碰 DOM');
});

test('三个组件 vnode 必须带 patchFlag 16（无 flag 会被 block 优化跳过更新）', () => {
  const src = read(LIVE);
  for (const need of [
    'v(LiveBenchPanel,liveBenchPanelProps("offense"),null,16)',
    'v(LiveBenchPanel,liveBenchPanelProps("defense"),null,16)',
    'v(LiveBenchGhost,{ui:liveBenchDragUi.value},null,16)',
    'v(LiveBenchRings,{targets:liveBenchRingTargets.value},null,16)',
  ]) {
    assert.ok(src.includes(need), `缺少组件接线（或 patchFlag 被去掉）：${need}`);
  }
});

test('四个既有交互入口均已接线（守备员/跑者/打者点击 + 守备拖拽互换）', () => {
  const src = read(LIVE);
  assert.ok(src.includes('liveBenchConsumePendingClicks({\n        kind:"fielder",code:e'), 'Ro 未接待放置消费');
  assert.ok(src.includes('liveBenchConsumePendingClicks({\n        kind:"base",base:e'), 'vs 未接待放置消费');
  assert.ok(src.includes('liveBenchConsumePendingClicks({\n        kind:"batter"'), 'Ao 未接待放置消费');
  assert.ok(src.includes('if(liveBenchStartFieldSwapDrag(e,t))return;'), 'Qf 未接守备互换拖拽');
});

test('执行唯一来源：liveBenchApplyDrop 走 cc()（与换人浮层同一记账路径）', () => {
  const src = read(LIVE);
  const body = src.slice(src.indexOf('function liveBenchApplyDrop'), src.indexOf('function liveBenchConsumePendingClicks'));
  assert.ok(body.includes('cc({'), 'applyDrop 必须调用 cc()');
  for (const banned of ['l.stats', 'fielding.po', 'stats.batting']) {
    assert.ok(!body.includes(banned), `不得在拖拽层复制记账逻辑：${banned}`);
  }
});

test('样式门禁：双面板/幽灵/高亮环 + 小屏与打线同规则隐藏', () => {
  const css = read(CSS);
  for (const need of ['.live-bench-left-col', '.live-bench-panel--defense', '.live-bench-ghost', '.live-sub-drop-ring']) {
    assert.ok(css.includes(need), `缺少样式：${need}`);
  }
  assert.match(css, /@media \(width<=1279px\)\{\.live-bench-left-col,\.live-bench-panel--defense,\.live-bench-ghost\{display:none\}\}/, '小屏隐藏规则缺失或与打线口径不一致');
});
