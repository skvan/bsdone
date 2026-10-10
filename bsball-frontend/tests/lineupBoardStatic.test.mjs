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
