// 守备补记接线：抽屉页签、API、LiveGame 传参。
// 运行：node --test tests/liveGameFieldingAdjustmentWiring.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8').replace(/\r\n/g, '\n');

test('ER 抽屉新增守备补记页签并接入独立面板', () => {
  const source = read('../src/components/admin/LiveGameEarnedRunDrawer.vue');
  const panel = read('../src/components/admin/GameEarnedRunReviewPanel.vue');
  for (const needle of [
    "lt('liveGame.review.drawerTitle')",
    "lt('liveGame.review.tabEr')",
    "lt('liveGame.review.tabFielding')",
    'LiveGameFieldingAdjustmentPanel',
    'resolvedGameStatus',
    ':game-status="resolvedGameStatus"',
    ':play-events="playEvents"'
  ]) {
    assert.ok(source.includes(needle), `缺少接线：${needle}`);
  }
  assert.ok(panel.includes("lt('liveGame.review.tabDecisions')"), 'ER 面板缺少自责分判定页签');
});

test('业务 API 暴露补记列表、新增与撤销端点', () => {
  const source = read('../src/api/business.js');
  for (const needle of [
    'listFieldingAdjustments',
    'createFieldingAdjustment',
    'revokeFieldingAdjustment',
    '/fielding-adjustments',
    '/revoke'
  ]) {
    assert.ok(source.includes(needle), `缺少 API：${needle}`);
  }
});

test('LiveGame 向复核抽屉传入比赛状态、Play 与两队名单', () => {
  const source = read('../src/views/admin/LiveGame.js');
  for (const needle of [
    'LiveGameEarnedRunDrawer,{key:reviewDrawerSession.value',
    'playEvents:c(f.playEvents)',
    'homeLineup:c(f.homeLineup)',
    'awayLineup:c(f.awayLineup)',
    'homeFieldingPitcherRow:c(f.homeFieldingPitcherRow)',
    'awayFieldingPitcherRow:c(f.awayFieldingPitcherRow)',
    'playerStatsIncludeFieldingAdjustments=(r?.list??[]).some(row=>Number(row.fieldingAdjustmentA)>0)',
    'playerStatsIncludeFieldingAdjustments=e.playerStatsIncludeFieldingAdjustments===true',
    'statsIncludeFieldingAdjustments:playerStatsIncludeFieldingAdjustments'
  ]) {
    assert.ok(source.includes(needle), `缺少传参：${needle}`);
  }
});

test('补记面板包含 final 门禁、补记预览与撤销', () => {
  const source = read('../src/components/admin/LiveGameFieldingAdjustmentPanel.vue');
  for (const needle of [
    'liveGame.review.fieldingBlockedTitle',
    'isFinal',
    'gameApi.createFieldingAdjustment',
    'gameApi.revokeFieldingAdjustment',
    'liveGame.review.effectLine',
    'liveGame.review.fieldingFinalDesc'
  ]) {
    assert.ok(source.includes(needle), `缺少面板逻辑：${needle}`);
  }
});
