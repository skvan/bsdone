// P2c-2：基座快照与“基座 vs 有效值”对比的静态契约。
// 运行：node --test tests/gameStatBaseDiff.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

const games = read('../src/views/admin/Games.js');
const panel = read('../src/components/admin/GameStatBaseDiffPanel.vue');
const business = read('../src/api/business.js');
const api = read('../../bsball-backend/src/main/java/com/bsball/api/GameApi.java');
const migration = read('../../bsball-backend/src/main/resources/db/migration/postgresql/V20261105170000__game_stat_base.sql');

test('结果页新增基座对比页签并接入面板', () => {
  assert.ok(games.includes('GameStatBaseDiffPanel'), '未导入基座对比面板');
  assert.ok(games.includes(`label:gText('liveGame.revision.tabStatBase'),name:"statBase"`), '缺少基座对比页签');
});

test('基座对比面板覆盖差异、一致与 LEGACY', () => {
  assert.ok(panel.includes('原始基座 · 当前有效值'), '缺少标题');
  assert.ok(panel.includes('当前有效值与基座一致'), '缺少无差异状态');
  assert.ok(panel.includes('历史场次没有基座快照'), '缺少 LEGACY 提示');
  assert.ok(panel.includes('field.baseValue'), '缺少基座值展示');
  assert.ok(panel.includes('field.effectiveValue'), '缺少有效值展示');
});

test('前端 API 与后端接口接线', () => {
  assert.ok(
    business.includes('compareStatBase: (id) => fetchResult(`/api/game/${id}/stat-base/compare`)'),
    '缺少基座对比 API'
  );
  assert.ok(api.includes('/{id}/stat-base/compare'), '后端缺少基座对比接口');
});

test('基座表为一场一球员一身份唯一快照', () => {
  assert.ok(migration.includes('bs_game_stat_base'), '缺少基座表');
  assert.ok(migration.includes('ux_bs_game_stat_base_player'), '缺少唯一索引');
  assert.ok(migration.includes('stat_json'), '缺少统计 JSON');
  assert.ok(migration.includes('fielding_pos_json'), '缺少守备 JSON');
});
