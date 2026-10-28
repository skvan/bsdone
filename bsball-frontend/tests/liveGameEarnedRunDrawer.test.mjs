// ER 覆核抽屉：列表接口契约与文案回归
// 运行：在 bsball-frontend 目录执行 node --test tests/liveGameEarnedRunDrawer.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const component = fs.readFileSync(
  new URL('../src/components/admin/LiveGameEarnedRunDrawer.vue', import.meta.url),
  'utf8'
);
const business = fs.readFileSync(new URL('../src/api/business.js', import.meta.url), 'utf8');
const liveGame = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('判定列表使用 fetchResult 解包数组，不再按 PageResult 解构', () => {
  assert.match(
    business,
    /listEarnedRunDecisions: \(id\) => fetchResult\(`\/api\/game\/\$\{id\}\/earned-runs\/decisions`\)/,
    '后端返回 List，不是 {list,total}'
  );
  assert.match(component, /const result = await gameApi\.listEarnedRunDecisions/);
  assert.match(component, /Array\.isArray\(result\?\.data\) \? result\.data : \[\]/);
  assert.doesNotMatch(component, /const \{ data \} = await gameApi\.listEarnedRunDecisions/);
});

test('抽屉文案为简体中文，且入口按钮可打开抽屉', () => {
  assert.match(component, /自责分复核/);
  assert.match(component, /暂无判定记录/);
  assert.match(component, /确认覆写/);
  assert.match(liveGame, /Q\("ER 复核",-1\)/);
});
