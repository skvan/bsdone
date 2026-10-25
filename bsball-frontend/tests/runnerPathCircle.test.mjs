// 静态断言：跑垒路径动画与出局动画均使用实心圆形 token
// 运行：在 bsball-frontend 目录执行  node --test tests/runnerPathCircle.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

// 读文件并归一化行尾（Windows 检出的 CRLF 会让多行字符串匹配失效）
const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
const LIVE = '../src/views/admin/LiveGame.js';
const CSS = '../src/styles/legacy/live-game.css';

test('RunnerPathAnimDot 路径与出局分支都渲染 circle', () => {
  const src = read(LIVE);
  const start = src.indexOf('__name:"RunnerPathAnimDot"');
  const end = src.indexOf('hv=wa(_v', start);
  assert.ok(start >= 0 && end > start, '未找到 RunnerPathAnimDot 组件');
  const body = src.slice(start, end);
  assert.ok(body.includes('runner-path-anim-dot--path-circle'), '路径模式未使用圆形 class');
  assert.ok(body.includes('runner-path-anim-dot--exit-circle'), '出局模式未使用圆形 class');
  assert.ok(!body.includes('runner-path-anim-dot--path-square'), '路径模式仍残留方形 class');
  assert.ok(!body.includes('runner-path-anim-dot--exit-square'), '出局模式仍残留方形 class');
  assert.ok(body.includes('cx:l.value,cy:o.value,r:"18"'), '路径圆形缺少 cx/cy/r=18');
  assert.ok(body.includes('cx:l.value,cy:o.value,r:"8"'), '出局圆形缺少 cx/cy/r=8');
});

test('动态 patch 属性改为 cx/cy', () => {
  const src = read(LIVE);
  assert.ok(src.includes('fv=["cx","cy"]'), '出局圆形的动态属性未改为 cx/cy');
  assert.ok(src.includes('dv=["cx","cy"]'), '路径圆形的动态属性未改为 cx/cy');
});

test('CSS 圆形规则存在且不再保留方形/rx:0', () => {
  const css = read(CSS);
  assert.ok(css.includes('.runner-path-anim-dot--path-circle[data-v-64a5f5be]{'), '缺少路径圆形 CSS');
  assert.ok(css.includes('.runner-path-anim-dot--exit-circle[data-v-64a5f5be]{'), '缺少出局圆形 CSS');
  assert.ok(!css.includes('runner-path-anim-dot--path-square'), 'CSS 仍残留路径方形 class');
  assert.ok(!css.includes('runner-path-anim-dot--exit-square'), 'CSS 仍残留出局方形 class');
  const rule = css.match(/\.runner-path-anim-dot--path-circle\[[^\]]+\]\{[^}]*\}/)?.[0] ?? '';
  assert.ok(rule && !rule.includes('rx:0'), '路径圆形规则仍带 rx:0');
});
