// 静态断言：管理台顶栏品牌标题上方显示构建版本号
// 运行：node --test tests/adminHeaderVersion.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const LAYOUT = '../src/components/admin/AdminLayout.js';
const CSS = '../src/styles/legacy/admin-layout.css';

test('AdminLayout 渲染版本号并包裹标题（四处品牌标题入口）', () => {
  const src = read(LAYOUT);
  assert.match(src, /VITE_APP_VERSION/);
  assert.match(src, /header-logo-text-wrap/);
  assert.match(src, /header-logo-version/);
  assert.equal((src.match(/renderBrandTitle\(r\(y\)\)/g) || []).length, 4, '四处品牌标题都应走同一渲染函数');
  assert.equal((src.match(/getAdminTitle\(\)/g) || []).length, 1, '标题只应在 renderBrandTitle 内读取');
});

test('版本号为小号灰字（11px），颜色取顶栏 muted 变量', () => {
  const css = read(CSS);
  const rule = css.match(/\.header-logo-version\[data-v-c4075566\]\{([^}]*)\}/);
  assert.ok(rule, '缺少 .header-logo-version 样式');
  assert.match(rule[1], /font-size:11px/);
  assert.match(rule[1], /admin-header-fg-muted/);
});
