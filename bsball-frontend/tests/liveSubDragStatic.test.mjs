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
// 切片必须两端都命中：任一 indexOf 为 -1 会导致 slice(start,-1) 几乎整文件、断言失效
const sliceBetween = (src, start, end) => {
  const i = src.indexOf(start);
  assert.ok(i >= 0, `缺少起始标记：${start}`);
  const j = src.indexOf(end, i + start.length);
  assert.ok(j > i, `缺少结束标记：${end}`);
  return src.slice(i, j);
};

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
  const body = sliceBetween(src, 'function liveBenchApplyDrop', 'function liveBenchConsumePendingClicks');
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
  const pointerBody = sliceBetween(src, 'function liveBenchCardPointerDown', 'function liveBenchStartDhDrag');
  assert.ok(pointerBody.includes('!substitutionMode.value') && pointerBody.includes('player?.disabled') && pointerBody.includes('event.button!==0') && pointerBody.includes('liveBenchDragSession'), '替补卡 pointerdown 未加模式门禁/已换下禁用');
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

test('#535：换人模式内日志静音，提交后只保留换人前日志 + 净变化结果', () => {
  const src = read(LIVE);
  assert.ok(src.includes('if(substitutionLogMuted)return;'), 'addLog 未接入换人模式日志静音');
  assert.ok(src.includes('setSubstitutionLogMuted'), '缺少日志静音开关导出');
  // 静音开关必须分别落在进入/退出函数体内（整文件断言会被别处调用掩盖）
  assert.ok(sliceBetween(src, 'function enterSubstitutionMode(){', 'function toggleSubstitutionMode(){').includes('subLogMuted(!0)'), '进入换人模式未开启日志静音');
  assert.ok(sliceBetween(src, 'function exitSubstitutionMode(){', 'function cancelSubstitutionMode(){').includes('subLogMuted(!1)'), '退出换人模式未解除日志静音');
  // 提交：先回滚日志并在静音窗口内写入净变化（reverse 抵消 unshift，与确认弹窗同序），再由 hs 内部唯一一次 Pf 推送
  const finishBody = sliceBetween(src, 'async function finishSubstitutionMode(){', 'function substitutionModePlayers(');
  assert.ok(finishBody.includes('logBase&&(ke.value=logBase),subLogMuted(!1),lines.slice().reverse().forEach(_line=>D(_line)),subLogMuted(!0),await hs({'), '提交成功后未回滚中间日志并写入净变化结果');
  assert.ok(!finishBody.includes('await Pf('), '提交路径不应额外补推快照（统一由 hs 内 Pf 携带最终日志）');
  assert.ok(finishBody.includes('logBase&&(ke.value=logBase),subLogMuted(!0)'), '提交失败未回滚日志/恢复静音');
});

test('换人模式外禁止一切换人执行路径（含换守备位置）', () => {
  const src = read(LIVE);
  const applyBody = sliceBetween(src, 'function liveBenchApplyDrop', 'function liveBenchConsumePendingClicks');
  assert.ok(applyBody.includes('if(!substitutionMode.value){'), '拖拽执行未做模式外硬拦截');
  const ccBody = sliceBetween(src, 'function cc(e){', '/* ---------------- 换人事务模式');
  assert.ok(ccBody.includes('if(!substitutionMode.value&&mt.value!=="setRunner")'), '换人执行器 cc() 未做模式外硬拦截');
  assert.ok(ccBody.includes('if(substitutionModeBusy.value)return;'), 'cc() 提交进行中未拒绝执行');
  assert.ok(ccBody.includes('换人模式进行中，请先结束或取消换人模式后再设置跑者'), '模式内 setRunner 纠偏未拦截（会被净变化 diff 忽略）');
  const zaBody = sliceBetween(src, 'function Za(e,t,a,s){', 'async function oc(');
  assert.ok(zaBody.includes('if(!substitutionMode.value&&e!=="setRunner")'), '换人浮层入口未做模式外拦截');
  assert.ok(zaBody.includes('if(substitutionModeBusy.value)return;'), 'Za() 提交进行中未拒绝打开浮层');
  // 空垒不是换人目标：模式内不画落点环/不参与落点命中/点选空垒保留已选卡（纠偏走模式外空垒点击）
  const layoutBody = sliceBetween(src, 'function liveBenchCollectFieldLayout', 'function liveBenchInvalidateDragLayout');
  assert.ok(layoutBody.includes('.filter(item=>!!l.runners[item.base])'), '模式内涵盖空垒的落点矩形未过滤');
  assert.ok(src.includes('if(kinds.includes("base"))for(const b of[1,2,3])l.runners[b]&&out.push({'), '空垒仍显示替补落点环');
  const consumeBody = sliceBetween(src, 'function liveBenchConsumePendingClicks', 'function liveBenchCollectFieldLayout');
  assert.ok(consumeBody.includes('if(hit.kind==="base"&&!l.runners[hit.base])return!1'), '点选空垒未保留已选卡（会触发无效 setRunner）');
});

test('进攻方选中特效：替补→打者/跑者也走红绿反馈', () => {
  const src = read(LIVE);
  assert.ok(src.includes('outKind:"batter"'), '缺少打者下场反馈状态');
  assert.ok(src.includes('outKind:out?"runner":"empty"'), '缺少跑者/空垒反馈状态');
  assert.ok(read(CSS).includes('.batter-at-plate-group.live-sub-swap-out'), '缺少打者红色放大样式');
});
