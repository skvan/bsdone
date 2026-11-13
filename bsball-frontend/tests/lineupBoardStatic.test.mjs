// 静态断言：批次 1 的依赖边界与「不搬 DOM」约束
// 运行：node --test tests/lineupBoardStatic.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const DRAG_SOURCES = [
  '../src/composables/useLineupDrag.js',
  '../src/composables/useLineupBoard.js',
  '../src/utils/lineupBoardRules.js',
  '../src/utils/lineupFieldPositions.js',
];

test('拖拽/规则层只允许 interactjs 与 sortablejs，不得引入其它拖拽库', () => {
  for (const rel of DRAG_SOURCES) {
    const src = read(rel);
    const imports = [...src.matchAll(/from\s+'([^']+)'/g)].map((m) => m[1]);
    for (const i of imports) {
      const ok = i === 'interactjs' || i === 'sortablejs' || i === 'vue' || i.startsWith('.') || i.startsWith('..');
      assert.ok(ok, `${rel} 引入未授权依赖：${i}`);
    }
  }
});

test('规则层不得出现直接搬 DOM 的调用（appendChild/removeChild/insertBefore）', () => {
  for (const rel of DRAG_SOURCES) {
    const src = read(rel);
    assert.ok(!/appendChild|removeChild|insertBefore|replaceChild/.test(src), `${rel} 出现直接搬 DOM 调用`);
  }
});

test('package.json 依赖白名单：interactjs 已登记、未引入重复拖拽封装库', () => {
  const pkg = JSON.parse(read('../package.json'));
  const deps = { ...pkg.dependencies, ...pkg.devDependencies };
  assert.ok(deps.interactjs, '缺少 interactjs');
  assert.ok(deps.sortablejs, '缺少 sortablejs');
  for (const banned of ['vuedraggable', 'vue-draggable-plus', '@formkit/drag-and-drop', '@atlaskit/pragmatic-drag-and-drop']) {
    assert.ok(!deps[banned], `不应引入 ${banned}`);
  }
});

test('旧件未被改动：LiveGameLineup.js 仍以「勿手改」注释开头', () => {
  const src = read('../src/views/admin/LiveGameLineup.js');
  assert.match(src.split('\n')[0], /勿手改/);
});

test('单击口径（2026-10-12 修订）：操作面板接线已移除，球场/打线行单击走 pick', () => {
  const board = read('../src/views/admin/LiveGameLineupBoard.vue');
  for (const banned of ['openPanel', 'card-open', 'row-open', 'open-panel']) {
    assert.ok(!board.includes(banned), `LiveGameLineupBoard 残留旧接线：${banned}`);
  }
  const field = read('../src/components/admin/LineupFieldBoard.vue');
  assert.ok(field.includes("emit('slot-click', slot.code)"), '卡槽需保留 slot-click');
  assert.ok(field.includes('slot.selfPending'), '球场卡片需带拿起态 selfPending');
  const batting = read('../src/components/admin/LineupBattingOrder.vue');
  assert.ok(batting.includes("emit('pick', row.id)"), '打线行单击应发 pick');
  const card = read('../src/components/admin/LineupPlayerCard.vue');
  assert.ok(!card.includes("emit('open'"), '卡片组件不得再有 open 事件');
});

test('拖拽起步 = 净位移门控（interactjs 无 distance 选项；阈值在 move 内判定、无长按 delay）', () => {
  const src = read('../src/composables/useLineupDrag.js');
  assert.ok(src.includes('export const DRAG_START_DISTANCE'), '缺少起步位移常量');
  assert.ok(src.includes('net <= DRAG_START_DISTANCE'), '缺少 move 内净位移门控');
  assert.ok(!/distance:\s*DRAG_START_DISTANCE/.test(src), '不得使用不存在的 interactjs distance 选项（评审 Critical）');
  assert.ok(!/delay:\s*(DRAG_DELAY|1[0-9]{2})/.test(src), '不得再使用长按 delay');
});

test('打线整卡拖动排序（#340）：桌面按插入位重排、移动端把手 + sortablejs 零回归', () => {
  const drag = read('../src/composables/useLineupDrag.js');
  assert.ok(drag.includes('battingRowRects'), 'resolveDropTarget 需按行 rect 计算插入位 gap');
  assert.ok(drag.includes('getLayout?.(el)'), 'move 取布局需传被拖元素（行 rect 排除自身）');
  assert.ok(drag.includes('getLayout?.(event.target)'), 'end 取布局需传被拖元素');
  assert.ok(drag.includes("ignoreFrom: '.drag-handle'"), 'interactjs 仍不得抢把手势（移动端）');

  const page = read('../src/views/admin/LiveGameLineupBoard.vue');
  assert.ok(page.includes("useMediaQuery('(pointer: fine)')"), '需以 (pointer: fine) 区分桌面整卡拖动');
  assert.ok(page.includes(':drop-line="battingDropLine"'), '需下发布置线状态给打线组件');
  assert.ok(page.includes('reorderBatting(from, target.index)'), 'batting 落点分支需调用 reorderBatting');

  const batting = read('../src/components/admin/LineupBattingOrder.vue');
  assert.ok(batting.includes("from 'sortablejs'"), '移动端行内排序仍需保留 sortablejs');
  assert.ok(batting.includes('.drag-handle'), '把手节点保留（移动端使用）');
  assert.ok(batting.includes('is-drop-before') && batting.includes('is-drop-after'), '需渲染插入线状态类');

  const css = read('../src/styles/lineup-board.css');
  assert.ok(css.includes('@media (pointer: fine)'), '桌面隐藏把手需要媒体查询');
  assert.ok(css.includes('.lineup-batting__handle'), '把手样式保留（移动端使用）');
  assert.ok(css.includes('is-drop-before') && css.includes('is-drop-after'), '插入线样式需登记');
  const fineBlock = css.slice(css.indexOf('@media (pointer: fine)'), css.indexOf('@media (max-width: 768px)'));
  assert.ok(fineBlock.includes('touch-action: none'), '触屏笔记本（主指针 fine）需禁触摸滚动抢占整卡拖动');
});

test('大谷规则：投手位/仅守备投手可点「兼DH」自动填充 DH 槽', () => {
  const page = read('../src/views/admin/LiveGameLineupBoard.vue');
  assert.ok(page.includes('setPitcherAsDh'), '页面需调用 setPitcherAsDh');
  assert.ok(page.includes('liveGame.lineup.pitcherToDh'), '按钮需显示「兼DH」');
  assert.ok(page.includes('ohtaniAction'), '按钮需按投手/DH 状态切换动作');

  const board = read('../src/composables/useLineupBoard.js');
  assert.ok(board.includes('function setPitcherAsDh()'), '规则层需提供 setPitcherAsDh');
  assert.ok(board.includes('setPitcherAsDh, setBench'), '需导出 setPitcherAsDh');
});

test('批量添加替补：行前 checkbox + 表头全选/半选，不再使用多选下拉', () => {
  const panel = read('../src/components/admin/LineupPoolPanel.vue');
  assert.ok(panel.includes('el-checkbox-group'), '批量弹窗需使用 checkbox 组');
  assert.ok(panel.includes('class="lineup-batch__row"'), '球员行需带 checkbox 行样式');
  assert.ok(panel.includes('全选'), '表头需有全选 checkbox');
  assert.ok(panel.includes(':indeterminate="batchSelection.indeterminate"'), '表头需呈半选态');
  assert.ok(panel.includes('@change="toggleAll"'), '表头勾选需一键全选/取消');
  assert.ok(panel.includes('batchSelection.selectedCount'), '需显示已选数量');
  assert.ok(panel.includes('sanitizeBenchIds'), '提交前需剔除候选外旧 id');
  assert.ok(!panel.includes('<el-select'), '批量弹窗不得再使用多选下拉');
  assert.ok(panel.includes('openBatch'), '打开弹窗需重置为当前替补席勾选态');

  const css = read('../src/styles/lineup-board.css');
  assert.ok(css.includes('.lineup-batch__head'), '缺少表头样式');
  assert.ok(css.includes('.lineup-batch__row'), '缺少球员行 checkbox 样式');
  assert.ok(css.includes('.lineup-batch__list'), '缺少可滚动列表样式');
});
