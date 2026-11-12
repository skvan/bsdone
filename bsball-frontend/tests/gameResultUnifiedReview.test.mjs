// P2a 统一补录/复核入口：结果页整合 ER 复核与守备补记 A，列表不再提供赛后复核入口。
// 运行：node --test tests/gameResultUnifiedReview.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const games = fs.readFileSync(
  new URL('../src/views/admin/Games.js', import.meta.url),
  'utf8'
);
const drawer = fs.readFileSync(
  new URL('../src/components/admin/LiveGameEarnedRunDrawer.vue', import.meta.url),
  'utf8'
);
const panel = fs.readFileSync(
  new URL('../src/components/admin/GameEarnedRunReviewPanel.vue', import.meta.url),
  'utf8'
);
const fieldingPanel = fs.readFileSync(
  new URL('../src/components/admin/LiveGameFieldingAdjustmentPanel.vue', import.meta.url),
  'utf8'
);
const business = fs.readFileSync(
  new URL('../src/api/business.js', import.meta.url),
  'utf8'
);

test('结果页新增 ER 复核与守备补记 A 页签', () => {
  assert.ok(
    games.includes('GameEarnedRunReviewPanel') && games.includes('LiveGameFieldingAdjustmentPanel'),
    '结果页未导入复核面板'
  );
  assert.ok(games.includes('label:"ER 复核",name:"erReview"'), '缺少 ER 复核页签');
  assert.ok(games.includes('label:"守备补记 A",name:"fieldingAdjustment"'), '缺少守备补记 A 页签');
  assert.ok(games.includes('"remote-candidates":!0'), '守备补记 A 未使用服务端候选');
});

test('复核变更后切回统计页签有刷新确认', () => {
  assert.ok(games.includes('markReviewChanged'), '未标记复核变更');
  assert.ok(games.includes('beforeLeave:onResultBeforeLeave'), '页签未接入刷新确认');
  assert.ok(games.includes('复核已修改服务器统计'), '缺少刷新确认文案');
  assert.ok(games.includes('保存前需重新加载最新统计'), '保存前缺少复核刷新守卫');
  assert.ok(games.includes('reviewDirty.value=!1,z.value=!1,await Na()'), '确认后未强制重载统计');
});

test('比赛列表 final 场次收敛为数据补录/复核入口', () => {
  assert.ok(
    games.includes('K.status==="final"?"数据补录/复核":"编辑"'),
    'final 场次编辑入口未改名'
  );
  assert.ok(!games.includes('R("赛后复核",-1)'), '仍保留旧的赛后复核按钮');
});

test('ER 抽屉复用统一面板，面板通知变更', () => {
  assert.ok(drawer.includes('GameEarnedRunReviewPanel'), '抽屉未复用 ER 复核面板');
  assert.ok(!drawer.includes('UNEARNED_REASONS'), '抽屉仍保留旧 ER 逻辑');
  assert.ok(panel.includes("defineEmits(['changed'])"), '面板未通知复核变更');
  assert.ok(panel.includes('onMounted'), '面板未在挂载时加载');
});

test('守备补记面板支持服务端候选接口', () => {
  assert.ok(fieldingPanel.includes('remoteCandidates'), '未支持服务端候选模式');
  assert.ok(
    fieldingPanel.includes('listFieldingAdjustmentCandidates'),
    '未调用候选接口'
  );
  assert.ok(
    business.includes('/fielding-adjustments/candidates'),
    'API 封装缺少候选接口'
  );
});
