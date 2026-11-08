// 赛后复核“投手 R/ER”页签与覆写接线（前端静态契约）。
// 运行：node --test tests/liveGamePitcherRunOverride.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const drawer = fs.readFileSync(
  new URL('../src/components/admin/GameEarnedRunReviewPanel.vue', import.meta.url),
  'utf8'
);
const api = fs.readFileSync(
  new URL('../src/api/business.js', import.meta.url),
  'utf8'
);

test('比赛复核抽屉包含投手 R/ER 页签与覆写/撤销入口', () => {
  assert.ok(
    drawer.includes("lt('liveGame.review.tabPitchers')"),
    '缺少投手 R/ER 页签'
  );
  assert.ok(drawer.includes('liveGame.review.pitcherHintLive'), '缺少赛后手写口径提示');
  assert.ok(drawer.includes('openPitcherEdit(row)'), '缺少覆写入口');
  assert.ok(drawer.includes('confirmRevokePitcher(row)'), '缺少撤销入口');
  assert.ok(drawer.includes('liveGame.review.manualOverride'), '缺少人工覆写状态标识');
  assert.ok(drawer.includes('liveGame.review.pitcherFootnote'), '缺少非自责计算口径说明');
});

test('投手 R/ER 覆写遵循 final 门禁与数值校验', () => {
  assert.ok(drawer.includes('if (!props.gameId || !isFinal.value || pitcherLoading.value) return;'),
    '加载投手合计缺少 final 门禁');
  assert.ok(drawer.includes('liveGame.review.erNotGreater'), '缺少 ER ≤ R 校验');
  assert.ok(drawer.includes('liveGame.review.rErInteger'), '缺少数值范围校验');
  assert.ok(drawer.includes('liveGame.review.pitcherSaveHint'),
    '缺少覆写失效提示');
});

test('有生效人工覆写时禁止再改该投手的逐分判定', () => {
  assert.ok(
    drawer.includes('isPitcherOverridden(row.effectivePitcherId ?? row.responsiblePitcherId)'),
    '判定覆写按钮未按有效责任投手的人工覆写状态禁用'
  );
  assert.ok(drawer.includes('liveGame.review.pitcherOverriddenHint'), '缺少冲突提示');
});

test('前端 API 已接线投手 R/ER 查询、覆写与撤销', () => {
  assert.ok(api.includes('listPitcherRunTotals: (id) => fetchResult(`/api/game/${id}/earned-runs/pitchers`)'),
    '缺少投手合计查询 API');
  assert.ok(api.includes('overridePitcherRuns: (id, playerId, data) =>'),
    '缺少投手覆写 API');
  assert.ok(api.includes('revokePitcherRunOverride: (id, overrideId) =>'),
    '缺少投手覆写撤销 API');
});
