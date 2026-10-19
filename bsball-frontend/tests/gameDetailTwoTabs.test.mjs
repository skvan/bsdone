// 回归：盒子比分顶部只保留打击/守备两项，投手表独立常显在下方
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const detail = fs.readFileSync(
  new URL('../src/components/admin/GameDetailContent.js', import.meta.url),
  'utf8'
);

test('盒子比分顶部只有打击/守备两个 Tab', () => {
  const start = detail.indexOf('gd-box-view-tabs');
  const end = detail.indexOf('c.game?.isSpecialResult||boxView.value!=="batting"', start);
  const tabs = detail.slice(start, end);
  assert.match(tabs, /gameDetail\.batting/);
  assert.match(tabs, /gameDetail\.fielding/);
  assert.equal((tabs.match(/tabLabel\("gameDetail\./g) || []).length, 2);
  assert.ok(!tabs.includes('gameDetail.pitching'), '投球 Tab 仍然存在');
});

test('投手块常显，且排在守备/打击内容之后', () => {
  assert.ok(!detail.includes('boxView.value==="pitching"'), '仍存在投球 Tab 状态');
  assert.ok(!detail.includes('boxView.value!=="pitching"'), '投手块仍受投球 Tab 控制');
  assert.ok(detail.includes('gd-pitchers-section'), '缺少投手常显区标记');
  assert.ok(
    detail.indexOf('gd-fielding-block') < detail.indexOf('gd-pitchers-section'),
    '投手块未排在守备块之后'
  );
});
