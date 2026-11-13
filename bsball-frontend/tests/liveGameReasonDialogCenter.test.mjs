// 原因弹窗定位回归：普通/全屏都必须在视口中央（不能再用 120px 顶部 padding 把面板推下去）
// 运行：node --test tests/liveGameReasonDialogCenter.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const css = fs.readFileSync(new URL('../src/styles/legacy/live-game.css', import.meta.url), 'utf8');
const rule = css.match(/\.rdm\[data-v-78b7b6a8\]\{([^}]*)\}/)?.[1] ?? '';

test('原因弹窗 .rdm 固定全屏且上下 padding 对称，普通/全屏都居中', () => {
  assert.ok(rule, '缺少 .rdm 规则');
  assert.ok(rule.includes('position:fixed'), '原因弹窗容器必须 fixed 到视口');
  assert.ok(rule.includes('inset:0'), '原因弹窗容器必须铺满视口');
  assert.ok(rule.includes('justify-content:center'), '原因弹窗缺少水平居中');
  assert.ok(rule.includes('align-items:center'), '原因弹窗缺少垂直居中');
  assert.ok(
    rule.includes('padding:max(16px, env(safe-area-inset-top))'),
    '顶部 padding 必须改为安全区对称值，不能再用 120px 顶部 padding'
  );
  assert.ok(rule.includes('max(16px, env(safe-area-inset-bottom))'), '底部 padding 缺失');
  assert.ok(!rule.includes('120px'), '不得保留 120px 顶部 padding');
});
