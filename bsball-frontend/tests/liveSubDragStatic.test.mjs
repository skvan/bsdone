// 静态断言：替补席拖拽换人（#293）接线门禁
// 运行：node --test tests/liveSubDragStatic.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

// 读文件并归一化行尾（Windows 检出的 CRLF 会让多行字符串匹配失效）
const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const LIVE = '../src/views/admin/LiveGame.js';
const UTIL = '../src/utils/liveSubDrag.js';
const CSS = '../src/styles/legacy/live-game.css';

test('纯函数层不依赖 DOM/框架（可被 node --test 直接加载）', () => {
  const src = read(UTIL);
  const imports = [...src.matchAll(/from\s+'([^']+)'/g)].map((m) => m[1]);
  const external = imports.filter((spec) => !spec.startsWith('./'));
  assert.deepEqual(external, [], `liveSubDrag.js 不得引入框架/第三方依赖：${external.join(',')}`);
  assert.ok(!/\bdocument\b|\bwindow\b|appendChild|createElement/.test(src), '纯函数层不得触碰 DOM');
  // 本地相对依赖同样须保持纯函数，不能借道触碰 DOM/框架
  const jersey = read('../src/utils/sortByJersey.js');
  assert.ok(!/\bdocument\b|\bwindow\b|appendChild|createElement|from\s+'vue'/.test(jersey), 'sortByJersey.js 必须保持纯函数');
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

test('换人模式门禁：拖拽仅在模式内启动，场上→替补席落点已接线', () => {
  const src = read(LIVE);
  assert.ok(src.includes('if(!substitutionMode.value||substitutionModeBusy.value||player?.disabled||event.button!==0||liveBenchDragSession)return;'), '替补卡 pointerdown 未加模式门禁/已换下禁用');
  assert.ok(src.includes('live-bench-card--disabled'), '缺少已换下球员灰显样式接线');
  assert.ok(src.includes(':not([disabled])'), '拖拽落点未排除已换下球员');
  assert.ok(src.includes('ensureSubstitutedOutBench('), '换下球员未回落到板凳');
  assert.ok(src.includes('ensureSubstitutedOutBench(side,before[side],diffs[index])'), '换人提交后未调用换下球员回落');
  assert.ok(src.includes('该球员已换下，不可再上场'), '缺少已换下球员拦截提示');
  assert.ok(!src.includes('"show-append-button":!0'), '追加新球员入口必须移除');
  assert.ok(src.includes('fieldingPositionsBaseline') && src.includes('awayStarterFpId'), '逐位置基线/开局投手未持久化');
  assert.ok(src.includes('hasHistoricalFieldingStats('), 'Do 重建路径未做逐位置基线可信判定');
  assert.ok(src.includes('if(!substitutionMode.value||l.flow||liveBenchDragSession'), '守备员拖拽未加模式门禁');
  assert.ok(src.includes('benchRects'), '拖拽布局未收集替补席矩形');
  assert.ok(src.includes('action==="benchSwap"'), 'applyDrop 未处理场上→替补席');
  const css = read(CSS);
  assert.ok(css.includes('.live-bench-card--swap-in'), '缺少替补席选中绿色放大样式');
  assert.ok(css.includes('.defender-hit.live-sub-swap-out'), '缺少场上球员红色放大样式');
});

test('进攻方选中特效：替补→打者/跑者也走红绿反馈', () => {
  const src = read(LIVE);
  assert.ok(src.includes('outKind:"batter"'), '缺少打者下场反馈状态');
  assert.ok(src.includes('outKind:out?"runner":"empty"'), '缺少跑者/空垒反馈状态');
  assert.ok(read(CSS).includes('.batter-at-plate-group.live-sub-swap-out'), '缺少打者红色放大样式');
});
