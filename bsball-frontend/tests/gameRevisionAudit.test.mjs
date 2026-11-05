// P2b-2：结果页修订记录审计页签的静态契约。
// 运行：node --test tests/gameRevisionAudit.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

const games = read('../src/views/admin/Games.js');
const panel = read('../src/components/admin/GameRevisionAuditPanel.vue');
const business = read('../src/api/business.js');
const api = read('../../bsball-backend/src/main/java/com/bsball/api/GameApi.java');

test('结果页新增修订记录页签并接入面板', () => {
  assert.ok(games.includes('GameRevisionAuditPanel'), '未导入审计面板');
  assert.ok(games.includes('label:"修订记录",name:"revisionAudit"'), '缺少修订记录页签');
  assert.ok(games.includes('"refresh-key":reviewVersion.value'), '复核变更后未刷新审计页签');
  assert.ok(
    games.replace(/\r\n/g, '\n').includes(
      'const transferId=result?.data?.transferId;\n        markReviewChanged();'
    ),
    '投手转记成功后未标记审计变更'
  );
});

test('审计面板覆盖四类修订与空状态', () => {
  for (const type of ['ER_DECISION', 'PITCHER_RUN_OVERRIDE', 'FIELDING_ADJUSTMENT', 'PITCHER_TRANSFER']) {
    assert.ok(panel.includes(type), `缺少审计类型：${type}`);
  }
  assert.ok(panel.includes('暂无修订记录'), '缺少空状态');
  assert.ok(panel.includes('实时记录为基座 · 人工修订为最终值'), '缺少状态说明');
  assert.ok(panel.includes('刷新'), '缺少刷新入口');
});

test('前端 API 与后端接口接线', () => {
  assert.ok(
    business.includes('listRevisionAudit: (id) => fetchResult(`/api/game/${id}/revision-audit`)'),
    '缺少审计 API 封装'
  );
  assert.ok(
    api.includes('/{id}/revision-audit'),
    '后端缺少 revision-audit 接口'
  );
});
