// P3-1：整名球员身份转记（统计/守备/ER 判定）静态契约。
// 运行：node --test tests/gameIdentityTransfer.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

const games = read('../src/views/admin/Games.js');
const panel = read('../src/components/admin/GameIdentityTransferPanel.vue');
const business = read('../src/api/business.js');
const api = read('../../bsball-backend/src/main/java/com/bsball/api/GameApi.java');
const migration = read('../../bsball-backend/src/main/resources/db/migration/postgresql/V20261106100000__game_identity_transfer.sql');

test('结果页新增身份转记页签并接入面板', () => {
  assert.ok(games.includes('GameIdentityTransferPanel'), '未导入身份转记面板');
  assert.ok(games.includes('label:"身份转记",name:"identityTransfer"'), '缺少身份转记页签');
});

test('身份转记面板覆盖目标、范围、预览与单独删除', () => {
  assert.ok(panel.includes('整名球员身份转记'), '缺少功能说明');
  assert.ok(panel.includes('目标球员'), '缺少目标球员');
  assert.ok(panel.includes('ER 判定'), '缺少 ER 判定范围');
  assert.ok(panel.includes('预览转记'), '缺少预览');
  assert.ok(panel.includes('是否删除源数据'), '缺少单独删除确认');
  assert.ok(panel.includes('transferIdentity'), '未调用转记接口');
  assert.ok(panel.includes('deleteIdentityTransferSource'), '未调用删除源数据接口');
});

test('前端 API 与后端接口接线', () => {
  assert.ok(
    business.includes('/identity-transfers/preview'),
    '缺少预览 API'
  );
  assert.ok(
    business.includes('/identity-transfers`'),
    '缺少转记 API'
  );
  assert.ok(
    business.includes('/delete-source'),
    '缺少删除源数据 API'
  );
  assert.ok(api.includes('/identity-transfers/preview'), '后端缺少预览接口');
  assert.ok(api.includes('/identity-transfers/{transferId}/delete-source'), '后端缺少删除接口');
});

test('身份转记审计表存在并保存前后快照', () => {
  assert.ok(migration.includes('bs_game_identity_transfer'), '缺少身份转记审计表');
  assert.ok(migration.includes('source_before'), '缺少源快照');
  assert.ok(migration.includes('target_after'), '缺少目标转记后快照');
  assert.ok(migration.includes('TRANSFERRED'), '缺少状态');
});
