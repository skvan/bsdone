// 比赛详情守备表：按首次上场守位归组、替补缩进、投手必在、同球员去重
// 运行：node --test tests/gameFieldingRows.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { buildFieldingDisplayRows } from '../src/utils/gameFieldingRows.js';
import { normalizeFieldingPosition, resolveFirstFieldPosition } from '../src/utils/gamePlayerStat.js';

const stats = [
  // 先发投手：零守备数据也必须出现
  { teamId: 1, playerId: 1, name: '投手', position: 'P', po: 0, battingOrder: 9, listOrder: 8, isPitcher: 1 },
  { teamId: 1, playerId: 2, name: '捕手', position: 'C', po: 3, a: 1, e: 0, dp: 0, defInn: 3, pb: 1, catcherSb: 7, catcherCs: 0, battingOrder: 4, listOrder: 3, firstPosition: 'C' },
  { teamId: 1, playerId: 3, name: '一垒手', position: '1B', battingOrder: 3, listOrder: 2, firstPosition: '1B' },
  // 先发三垒手，后来改投：仍应按首个守位 3B 归组
  { teamId: 1, playerId: 4, name: '三垒手后援投', position: 'P', battingOrder: 1, listOrder: 0, firstPosition: '3B', po: 2, a: 1 },
  { teamId: 1, playerId: 5, name: '左外野', position: 'LF', battingOrder: 2, listOrder: 1, firstPosition: 'LF', po: 1 },
  // 左外野替补：首次上场守位 LF，缩进在 LF 首发下一行
  { teamId: 1, playerId: 6, name: '左外野替补', position: 'LF', battingOrder: null, listOrder: 9, firstPosition: 'LF', po: 1 },
  { teamId: 1, playerId: 7, name: '指定打击', position: 'DH', battingOrder: 5, listOrder: 4 },
  { teamId: 2, playerId: 8, name: '他队游击', position: 'SS', battingOrder: 1, firstPosition: 'SS' }
];

test('守备行：按首个守位归组（P→C→1B→3B→LF），投手与改投者都在表内', () => {
  const rows = buildFieldingDisplayRows(stats, 1);
  assert.deepEqual(rows.map((r) => r.playerId), [1, 2, 3, 4, 5, 6]);
  assert.deepEqual(rows.map((r) => r.fieldPosition), ['P', 'C', '1B', '3B', 'LF', 'LF']);
});

test('守备行：首个守位优先于当前守位，后续换位不改显示', () => {
  const rows = buildFieldingDisplayRows(stats, 1);
  const moved = rows.find((r) => r.playerId === 4);
  assert.equal(moved.position, 'P', '原始当前位置保留在数据里');
  assert.equal(moved.fieldPosition, '3B', '守备表只认首次上场守位');
});

test('守备行：同守位组首位不缩进，替补缩进', () => {
  const rows = buildFieldingDisplayRows(stats, 1);
  assert.equal(rows.find((r) => r.playerId === 5).subRow, false);
  assert.equal(rows.find((r) => r.playerId === 6).subRow, true);
  assert.ok(rows.filter((r) => r.fieldPosition === '3B').every((r) => r.subRow === false), '单人行不缩进');
});

test('守备行：DH/PH/EP 与未上场者不进入，位置缺失但有守备动作的脏数据保留在末尾', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 1, playerId: 1, position: 'P', isPitcher: 1 },
    { teamId: 1, playerId: 2, position: 'DH', po: 2 },
    { teamId: 1, playerId: 3, position: 'PH' },
    { teamId: 1, playerId: 4, position: 'EP' },
    { teamId: 1, playerId: 5, position: '', po: 2, a: 1 }
  ], 1);
  assert.deepEqual(rows.map((r) => r.playerId), [1, 5]);
  assert.equal(rows[1].fieldPosition, '');
  assert.equal(rows[1].subRow, false);
});

test('守备行：旧数据无 firstPosition 时回退当前 position', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 1, playerId: 9, position: 'cf', battingOrder: 1, defInn: 3 }
  ], 1);
  assert.equal(rows[0].fieldPosition, 'CF');
});

test('守备行：代打/代跑继承守位但从未防守时不得进入守备表', () => {
  const rows = buildFieldingDisplayRows([
    // 代打：占第 3 棒（battingOrder=null），position 继承被换下者的 LF，但零守备证据
    { teamId: 1, playerId: 31, position: 'LF', battingOrder: null, listOrder: 2, pa: 1, ab: 1 },
    // 代跑：以代跑身份进打线，position 继承 CF，零守备证据
    { teamId: 1, playerId: 32, position: 'CF', battingOrder: null, listOrder: 4 },
    // 同一个代打后来真的上场防守：有守备局数，旧数据回退 position 纳入
    { teamId: 1, playerId: 33, position: 'RF', battingOrder: null, listOrder: 6, defInn: 2 }
  ], 1);
  assert.deepEqual(rows.map((r) => r.playerId), [33]);
  assert.equal(rows[0].fieldPosition, 'RF');
});

test('守备行：真正进入投手角色的零守备数据投手必在；仅板凳首选 P 的不显示', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 1, playerId: 41, position: 'P', isPitcher: 1, battingOrder: 9 },
    { teamId: 1, playerId: 42, position: 'P' }
  ], 1);
  assert.deepEqual(rows.map((r) => r.playerId), [41]);
});

test('守备行：二刀流同一球员的打者行+投手行只保留一行', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 1, playerId: 11, position: 'P', firstPosition: '3B', po: 2, a: 1, listOrder: 0 },
    { teamId: 1, playerId: 11, position: 'P', firstPosition: '3B', po: 2, a: 1, listOrder: 20, isPitcher: 1 }
  ], 1);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].fieldPosition, '3B');
});

test('守备行：缺失的守备/捕手字段补 0，已有值保留', () => {
  const rows = buildFieldingDisplayRows(stats, 1);
  const first = rows.find((r) => r.playerId === 3);
  assert.equal(first.po, 0);
  assert.equal(first.a, 0);
  assert.equal(first.e, 0);
  assert.equal(first.dp, 0);
  assert.equal(first.defInn, 0);
  assert.equal(first.pb, 0);
  assert.equal(first.catcherSb, 0);
  assert.equal(first.catcherCs, 0);
  const catcher = rows.find((r) => r.playerId === 2);
  assert.equal(catcher.po, 3);
  assert.equal(catcher.defInn, 3);
  assert.equal(catcher.pb, 1);
  assert.equal(catcher.catcherSb, 7);
});

test('守备行：不修改入参', () => {
  const input = [
    { teamId: 1, playerId: 8, position: 'RF' },
    { teamId: 1, playerId: 9, position: 'CF', battingOrder: 1 }
  ];
  const snapshot = JSON.stringify(input);
  buildFieldingDisplayRows(input, 1);
  assert.equal(JSON.stringify(input), snapshot);
});

test('首个守位工具：大小写归一，DH/PH/EP 视为未守备', () => {
  assert.equal(normalizeFieldingPosition('lf'), 'LF');
  assert.equal(normalizeFieldingPosition(' DH '), '');
  assert.equal(normalizeFieldingPosition('ph'), '');
  assert.equal(normalizeFieldingPosition('ep'), '');
  assert.equal(normalizeFieldingPosition(null), '');
  assert.equal(resolveFirstFieldPosition({ position: 'P', firstPosition: '3b' }), '3B');
  assert.equal(resolveFirstFieldPosition({ position: 'cf' }), 'CF');
  assert.equal(resolveFirstFieldPosition({ position: 'DH', firstPosition: 'DH' }), '');
});

test('比赛详情守备表接线：使用首个守位与替补缩进标记', () => {
  const detail = fs.readFileSync(new URL('../src/components/admin/GameDetailFieldingTable.vue', import.meta.url), 'utf8');
  assert.match(detail, /buildFieldingDisplayRows/, '详情守备表复用行构建');
  assert.match(detail, /row\.fieldPosition/, 'POS 列显示首次上场守位');
  assert.match(detail, /row\.subRow/, '替补行缩进标记');
  assert.match(detail, /visibleFields/, '列由字段设置控制');
  assert.match(detail, /DEFAULT_FIELDING_FIELDS/, '缺省字段包含守备/捕手列');
  const entry = fs.readFileSync(new URL('../src/components/admin/GameFieldingTable.vue', import.meta.url), 'utf8');
  for (const key of ['pb', 'catcherSb', 'catcherCs']) {
    assert.ok(entry.includes(`key: '${key}'`), `录入页守备表缺少 ${key} 列`);
  }
});
