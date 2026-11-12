// P2c-3：统一修订台账（结果页字段级留痕）静态契约。
// 运行：node --test tests/gameAdjustmentLedger.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

const games = read('../src/views/admin/Games.js');
const panel = read('../src/components/admin/GameAdjustmentLedgerPanel.vue');
const business = read('../src/api/business.js');
const api = read('../../bsball-backend/src/main/java/com/bsball/api/GameApi.java');
const migration = read('../../bsball-backend/src/main/resources/db/migration/postgresql/V20261105180000__game_stat_adjustment.sql');
const service = read('../../bsball-backend/src/main/java/com/bsball/service/GameRevisionAuditService.java');

test('结果页新增修订台账页签并接入面板', () => {
  assert.ok(games.includes('GameAdjustmentLedgerPanel'), '未导入修订台账面板');
  assert.ok(games.includes('label:"修订台账",name:"adjustmentLedger"'), '缺少修订台账页签');
});

test('台账面板展示字段级 before → after', () => {
  assert.ok(panel.includes('字段级留痕'), '缺少字段级说明');
  assert.ok(panel.includes('row.beforeValue'), '缺少修改前值');
  assert.ok(panel.includes('row.afterValue'), '缺少修改后值');
  assert.ok(panel.includes('暂无字段级修订记录'), '缺少空状态');
});

test('前端 API 与后端接口接线', () => {
  assert.ok(
    business.includes('listStatAdjustments: (id) => fetchResult(`/api/game/${id}/adjustments`)'),
    '缺少修订台账 API'
  );
  assert.ok(api.includes('/{id}/adjustments'), '后端缺少修订台账接口');
});

test('台账表结构与写入范围', () => {
  assert.ok(migration.includes('bs_game_stat_adjustment'), '缺少台账表');
  assert.ok(migration.includes('field_key'), '缺少字段 key');
  assert.ok(migration.includes('before_value'), '缺少修改前值');
  assert.ok(migration.includes('after_value'), '缺少修改后值');
  assert.ok(service.includes('PLAYER_STAT'), '缺少 PLAYER_STAT 作用域');
  assert.ok(service.includes('RESULT_SAVE'), '缺少 RESULT_SAVE 来源');
  assert.ok(service.includes('recordStatChanges'), '缺少台账写入方法');
});
