// P2b-1：ER 判定改派/作废/恢复的静态契约。
// 运行：node --test tests/erDecisionAdjustment.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

const panel = read('../src/components/admin/GameEarnedRunReviewPanel.vue');
const business = read('../src/api/business.js');
const service = read('../../bsball-backend/src/main/java/com/bsball/service/EarnedRunReconstructionService.java');
const api = read('../../bsball-backend/src/main/java/com/bsball/api/GameApi.java');
const migration = read('../../bsball-backend/src/main/resources/db/migration/postgresql/V20261105160000__er_decision_adjustment.sql');

test('前端 API 接线判定改派、作废与撤销', () => {
  assert.ok(
    business.includes('/earned-runs/decisions/${decisionId}/reassign'),
    '缺少改派 API'
  );
  assert.ok(
    business.includes('/earned-runs/decisions/${decisionId}/void'),
    '缺少作废 API'
  );
  assert.ok(
    business.includes('/earned-runs/decision-adjustments/${adjustmentId}/revoke'),
    '缺少撤销调整 API'
  );
});

test('ER 面板展示有效投手与人工改派/作废徽标', () => {
  assert.ok(panel.includes('effectivePitcherName'), '缺少有效投手展示');
  assert.ok(panel.includes("adjustmentAction === 'REASSIGN'"), '缺少人工改派徽标');
  assert.ok(panel.includes('row.voided'), '缺少已作废状态');
});

test('ER 面板提供改派/作废/恢复入口', () => {
  assert.ok(panel.includes('openAdjust(row, \'REASSIGN\')'), '缺少改派入口');
  assert.ok(panel.includes('openAdjust(row, \'VOID\')'), '缺少作废入口');
  assert.ok(panel.includes('restoreAdjustment(row)'), '缺少恢复入口');
  assert.ok(panel.includes('adjustTargets'), '缺少同队目标投手过滤');
  assert.ok(panel.includes('emit(\'changed\')'), '操作后未通知复核变更');
});

test('后端稳定键审计表与接口存在', () => {
  assert.ok(migration.includes('bs_er_decision_adjustment'), '缺少判定调整表');
  assert.ok(migration.includes('source_event_id'), '缺少稳定源 Play 键');
  assert.ok(migration.includes('runner_id'), '缺少跑者稳定键');
  assert.ok(
    migration.includes('ux_bs_er_decision_adjustment_active'),
    '缺少 ACTIVE 唯一索引'
  );
  assert.ok(
    api.includes('/earned-runs/decisions/{decisionId}/reassign'),
    '缺少改派接口'
  );
  assert.ok(
    api.includes('/earned-runs/decisions/{decisionId}/void'),
    '缺少作废接口'
  );
  assert.ok(
    api.includes('/earned-runs/decision-adjustments/{adjustmentId}/revoke'),
    '缺少撤销调整接口'
  );
});

test('后端有效责任投手参与重算与删除守卫', () => {
  assert.ok(service.includes('enginePitcherTotals'), '缺少有效合计重算');
  assert.ok(service.includes('effectivePitcherId(decision'), '未按有效投手聚合');
  assert.ok(service.includes('hasResponsiblePitcherDecisions'), '缺少删除守卫');
  assert.ok(service.includes('ADJUSTMENT_SUPERSEDED'), '缺少调整替代状态');
  assert.ok(service.includes('findDecisionByKey'), '缺少稳定键回查');
  assert.ok(service.includes('saveAndFlush(current)'), '替代旧 ACTIVE 前未先 flush');
});
