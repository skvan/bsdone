// 比赛结果修订面板英文（B3b-3b）：修订台账、修订审计、身份转记
// 运行：node --test tests/gameRevisionI18n.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));
const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en'].map((l) => [l, readJson(`../src/locales/${l}.json`)])
);
const ledger = read('../src/components/admin/GameAdjustmentLedgerPanel.vue');
const audit = read('../src/components/admin/GameRevisionAuditPanel.vue');
const transfer = read('../src/components/admin/GameIdentityTransferPanel.vue');
const games = read('../src/views/admin/Games.js');
const sources = [ledger, audit, transfer, games];

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, path, out);
    else out[path] = value;
  }
  return out;
}
const flat = Object.fromEntries(Object.entries(locales).map(([l, d]) => [l, flatten(d)]));

test('修订面板引用词条在简繁英三语齐全', () => {
  const keys = new Set();
  for (const source of sources) {
    for (const match of source.matchAll(/['"](liveGame\.revision\.[a-zA-Z0-9]+)['"]/g)) keys.add(match[1]);
  }
  assert.ok(keys.size >= 60, `引用词条数量不足：${keys.size}`);
  for (const locale of Object.keys(locales)) {
    for (const key of keys) assert.ok(flat[locale][key], `${locale} 缺少 ${key}`);
  }
});

test('英文修订词条不残留中文', () => {
  const offenders = Object.entries(flat.en)
    .filter(([key, value]) => key.startsWith('liveGame.revision.') && typeof value === 'string' && /[\u4e00-\u9fff]/.test(value))
    .map(([key, value]) => `${key}=${value}`);
  assert.deepEqual(offenders, []);
});

test('修订台账与审计面板不再硬编码主要中文', () => {
  for (const text of ['比赛结束后才能查看修订台账', '人工修订台账', '暂无字段级修订记录', '暂无修订记录', '实时记录为基座', '全部类型']) {
    assert.ok(!ledger.includes(`"${text}"`) && !ledger.includes(`>${text}<`), `台账仍硬编码：${text}`);
    assert.ok(!audit.includes(`"${text}"`) && !audit.includes(`>${text}<`), `审计仍硬编码：${text}`);
  }
  assert.ok(ledger.includes('liveGame.revision.ledgerTitle'), '台账标题未走 i18n');
  assert.ok(audit.includes('liveGame.revision.auditTitle'), '审计标题未走 i18n');
  assert.ok(audit.includes('labelKey'), '审计类型/状态未走 i18n');
});

test('身份转记面板覆盖目标、范围、预览与独立删除文案', () => {
  for (const needle of [
    'liveGame.revision.identityTitle',
    'liveGame.revision.targetPlayer',
    'liveGame.revision.scopeEr',
    'liveGame.revision.previewBtn',
    'liveGame.revision.transferredTo',
    'liveGame.revision.deleteSource',
    'LIVE_RECORDS',
    'transferIdentity',
    'deleteIdentityTransferSource'
  ]) {
    assert.ok(transfer.includes(needle), `身份转记缺少：${needle}`);
  }
  assert.ok(!transfer.includes('整名球员身份转记'), '身份转记仍硬编码标题');
});

test('结果页复核相关页签走 i18n', () => {
  for (const key of ['tabErReview', 'tabFieldingAdjust', 'tabAudit', 'tabStatBase', 'tabLedger', 'tabIdentity']) {
    assert.ok(games.includes(`liveGame.revision.${key}`), `页签未接入 ${key}`);
    assert.ok(flat.en[`liveGame.revision.${key}`], `英文缺少 ${key}`);
  }
});
