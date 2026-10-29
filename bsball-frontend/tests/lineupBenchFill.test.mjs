// 赛前替补席补齐：全队可上场且未先发的球员进替补席（buildFullBenchIds）
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { buildFullBenchIds } from '../src/utils/lineupDraft.js';
import { readFileSync } from 'node:fs';

const rows = [101, 102, 103, 104, 105, 106, 107, 108, 109].map((id) => ({ id }));
const roster = [...rows.map((r) => ({ id: r.id })), { id: 110 }, { id: 111 }];

test('替补席 = 名册 - 先发 - 不可上', () => {
  assert.deepEqual(buildFullBenchIds(roster, rows, [110]), ['111']);
});

test('打序占位行（id<0）不影响补齐', () => {
  assert.deepEqual(buildFullBenchIds(roster, [...rows, { id: -1 }, { id: -2 }], []), ['110', '111']);
});

test('不可上 id 字符串/数字不敏感', () => {
  assert.deepEqual(buildFullBenchIds(roster, rows, ['110']), ['111']);
});


test('仅守备投手不算替补（DH 制无 P 行）', () => {
  const dhRows = [...rows.slice(0, 8), { id: 112, position: 'DH' }];
  assert.deepEqual(buildFullBenchIds(roster, dhRows, [], [110]), ['109', '111']);
});

test('缺省/空输入安全', () => {
  assert.deepEqual(buildFullBenchIds(null, null, null), []);
  assert.deepEqual(buildFullBenchIds(roster, [], null), ['101', '102', '103', '104', '105', '106', '107', '108', '109', '110', '111']);
});

const liveSrc = readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
const boardSrc = readFileSync(new URL('../src/views/admin/LiveGameLineupBoard.vue', import.meta.url), 'utf8');

test('追加新球员入口已移除', () => {
  assert.ok(!liveSrc.includes('"show-append-button":!0'));
  assert.ok(!liveSrc.includes(',onAppend:'));
});

test('保存阵容与开始录入前都补齐替补席', () => {
  assert.ok(boardSrc.includes('buildFullBenchIds'));
  assert.ok((boardSrc.match(/fillBenchesWithEligible()/g) || []).length >= 3);
});
