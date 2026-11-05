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
    drawer.includes('<el-tab-pane label="投手 R/ER" name="pitchers" lazy>'),
    '缺少投手 R/ER 页签'
  );
  assert.ok(drawer.includes('手工覆写投手 R/ER'), '缺少赛后手写口径提示');
  assert.ok(drawer.includes('openPitcherEdit(row)'), '缺少覆写入口');
  assert.ok(drawer.includes('confirmRevokePitcher(row)'), '缺少撤销入口');
  assert.ok(drawer.includes('人工覆写'), '缺少人工覆写状态标识');
  assert.ok(drawer.includes('非自责自动按 R − ER 计算'), '缺少非自责计算口径说明');
});

test('投手 R/ER 覆写遵循 final 门禁与数值校验', () => {
  assert.ok(drawer.includes('if (!props.gameId || !isFinal.value || pitcherLoading.value) return;'),
    '加载投手合计缺少 final 门禁');
  assert.ok(drawer.includes('ER 不能大于 R'), '缺少 ER ≤ R 校验');
  assert.ok(drawer.includes('R/ER 需为 0-99 的整数'), '缺少数值范围校验');
  assert.ok(drawer.includes('若之后重建半局或修改判定，该投手覆写会失效'),
    '缺少覆写失效提示');
});

test('有生效人工覆写时禁止再改该投手的逐分判定', () => {
  assert.ok(drawer.includes('isPitcherOverridden(row.responsiblePitcherId)'),
    '判定覆写按钮未按人工覆写状态禁用');
  assert.ok(drawer.includes('该投手已人工覆写 R/ER，请先撤销'), '缺少冲突提示');
});

test('前端 API 已接线投手 R/ER 查询、覆写与撤销', () => {
  assert.ok(api.includes('listPitcherRunTotals: (id) => fetchResult(`/api/game/${id}/earned-runs/pitchers`)'),
    '缺少投手合计查询 API');
  assert.ok(api.includes('overridePitcherRuns: (id, playerId, data) =>'),
    '缺少投手覆写 API');
  assert.ok(api.includes('revokePitcherRunOverride: (id, overrideId) =>'),
    '缺少投手覆写撤销 API');
});
