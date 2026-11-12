// P2c-1：删除前结构化诊断（剩余字段/ER 判定/守备补记）静态契约。
// 运行：node --test tests/statRowDeleteDiagnostic.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

const games = read('../src/views/admin/Games.js');
const business = read('../src/api/business.js');
const api = read('../../bsball-backend/src/main/java/com/bsball/api/GameApi.java');
const diagnostics = read('../../bsball-backend/src/main/java/com/bsball/utils/GameStatDeleteDiagnostics.java');
const zeros = read('../../bsball-backend/src/main/java/com/bsball/utils/GamePlayerStatZeros.java');

test('前端 API 接线删除诊断接口', () => {
  assert.ok(
    business.includes('/stat-rows/${statId}/delete-check'),
    '缺少删除诊断 API 封装'
  );
  assert.ok(games.includes('backendDeleteCheck'), '删除入口未接入后端诊断');
  assert.ok(games.includes('action==="ER_REVIEW"'), '缺少 ER 复核推荐分支');
  assert.ok(games.includes('action==="FIELDING_REVIEW"'), '缺少守备补记推荐分支');
});

test('后端诊断接口与四类阻断', () => {
  assert.ok(api.includes('/stat-rows/{statId}/delete-check'), '缺少后端诊断接口');
  for (const type of ['DATA_FIELD', 'ER_DECISION', 'FIELDING_ADJUSTMENT', 'FIELDING_POSITION']) {
    assert.ok(diagnostics.includes(type), `缺少阻断类型：${type}`);
  }
  assert.ok(diagnostics.includes('ER_REVIEW'), '缺少 ER 推荐动作');
  assert.ok(diagnostics.includes('FIELDING_REVIEW'), '缺少守备推荐动作');
  assert.ok(zeros.includes('nonZeroFields'), '缺少剩余字段诊断');
});
