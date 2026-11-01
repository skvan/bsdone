// 比赛详情守备表：按首次上场守位归组、替补缩进、投手必在、同球员去重、Totals 汇总
// 运行：node --test tests/gameFieldingRows.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  buildFieldingDisplayRows,
  buildFieldingSummaryCells,
  buildFieldingTotals
} from '../src/utils/gameFieldingRows.js';
import { formatInningsPitched } from '../src/utils/inningsFormat.js';
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

test('守备行：组按先发打线顺序排序，投手与改投者都在表内', () => {
  const rows = buildFieldingDisplayRows(stats, 1);
  // 3B=第1棒、LF=第2棒（含替补）、1B=第3棒、C=第4棒、P=第9棒
  assert.deepEqual(rows.map((r) => r.playerId), [4, 5, 6, 3, 2, 1]);
  assert.deepEqual(rows.map((r) => r.fieldPosition), ['3B', 'LF', 'LF', '1B', 'C', 'P']);
});

test('守备行：无棒次的纯投手组排在所有打线组之后', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 3, playerId: 30, position: 'CF', firstPosition: 'CF', battingOrder: 1, listOrder: 0, defInn: 3, po: 1 },
    { teamId: 3, playerId: 31, position: 'P', firstPosition: 'P', isPitcher: 1, listOrder: 10, defInn: 3, np: 30 },
    { teamId: 3, playerId: 32, position: 'C', firstPosition: 'C', battingOrder: 2, listOrder: 1, defInn: 3, po: 6 }
  ], 3);
  assert.deepEqual(rows.map((r) => r.playerId), [30, 32, 31]);
  assert.equal(rows[2].subRow, false);
});

test('守备行：先发 DH 后上场不抢先发守位者当组首', () => {
  const rows = buildFieldingDisplayRows([
    // LF 先发（先发守位 LF，棒次 6）与一名棒次更靠前的先发 DH 后来守 LF（棒次 3）
    { teamId: 5, playerId: 70, position: 'LF', firstPosition: 'LF', startingPosition: 'LF', battingOrder: 6, listOrder: 5, defInn: 3, po: 2 },
    { teamId: 5, playerId: 71, position: 'LF', firstPosition: 'LF', startingPosition: '', battingOrder: 3, listOrder: 2, defInn: 1, po: 1 }
  ], 5);
  assert.deepEqual(rows.map((r) => r.playerId), [70, 71]);
  assert.equal(rows[0].subRow, false, 'LF 先发守位者是组首');
  assert.equal(rows[1].subRow, true, '先发 DH 后上场守 LF 缩进');
});

test('守备行：先发三垒手改投后仍是 3B 组首，DH 补位缩进', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 4, playerId: 40, position: 'P', firstPosition: '3B', startingPosition: '3B', battingOrder: 5, listOrder: 4, defInn: 4, po: 1 },
    { teamId: 4, playerId: 41, position: '3B', firstPosition: '3B', startingPosition: '', battingOrder: 3, listOrder: 2, defInn: 2, po: 0 }
  ], 4);
  assert.deepEqual(rows.map((r) => r.playerId), [40, 41]);
  assert.equal(rows[0].subRow, false);
  assert.equal(rows[1].subRow, true);
});

test('守备行：先发投手组首、救援缩进，P 组无棒次排最后', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 6, playerId: 60, position: 'CF', firstPosition: 'CF', startingPosition: 'CF', battingOrder: 1, listOrder: 0, defInn: 3, po: 1 },
    { teamId: 6, playerId: 61, position: 'P', firstPosition: 'P', startingPosition: 'P', isPitcher: 1, pitcherOrder: 1, listOrder: 20, defInn: 3 },
    { teamId: 6, playerId: 62, position: 'P', firstPosition: 'P', isPitcher: 1, pitcherOrder: 2, listOrder: 21, defInn: 1 },
    { teamId: 6, playerId: 63, position: 'C', firstPosition: 'C', startingPosition: 'C', battingOrder: 2, listOrder: 1, defInn: 3, po: 6 }
  ], 6);
  assert.deepEqual(rows.map((r) => r.playerId), [60, 63, 61, 62]);
  assert.equal(rows[2].subRow, false);
  assert.equal(rows[3].subRow, true);
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

test('守备 Totals：计数列求和，INN 取全队最大守备局数', () => {
  const rows = [
    { po: 3, a: 1, e: 0, dp: 0, defInn: 3, pb: 1, catcherSb: 1, catcherCs: 0, fieldingGs: 1 },
    { po: 2, a: 4, e: 1, dp: 1, defInn: 2.1, pb: 0, catcherSb: 2, catcherCs: 1, fieldingGs: 0 }
  ];
  const fields = [
    { key: 'fieldingGs' }, { key: 'po' }, { key: 'a' }, { key: 'e' },
    { key: 'dp' }, { key: 'defInn' }, { key: 'pb' }, { key: 'catcherSb' }, { key: 'catcherCs' }
  ];
  assert.deepEqual(buildFieldingTotals(rows, fields), {
    fieldingGs: 1, po: 5, a: 5, e: 1, dp: 1, defInn: 3,
    pb: 1, catcherSb: 3, catcherCs: 1
  });
});

test('守备 Totals：空表全 0，且不修改入参', () => {
  const fields = [{ key: 'po' }, { key: 'a' }, { key: 'defInn' }];
  assert.deepEqual(buildFieldingTotals([], fields), { po: 0, a: 0, defInn: 0 });
  const rows = [{ po: 1, a: 2, defInn: 3 }];
  const snapshot = JSON.stringify(rows);
  buildFieldingTotals(rows, fields);
  assert.equal(JSON.stringify(rows), snapshot);
});

test('守备 Totals：只汇总传入字段，隐藏列不参与', () => {
  assert.deepEqual(buildFieldingTotals([{ po: 2, a: 1 }], [{ key: 'po' }]), { po: 2 });
});

test('守备 Totals：二刀流去重后按单行求和，不重复计入', () => {
  const rows = buildFieldingDisplayRows([
    { teamId: 1, playerId: 11, position: 'P', firstPosition: '3B', po: 2, a: 1, dp: 1, defInn: 3, listOrder: 0 },
    { teamId: 1, playerId: 11, position: 'P', firstPosition: '3B', po: 2, a: 1, dp: 1, defInn: 3, listOrder: 20, isPitcher: 1 }
  ], 1);
  assert.equal(rows.length, 1, '去重后只剩一行');
  assert.deepEqual(
    buildFieldingTotals(rows, [{ key: 'po' }, { key: 'a' }, { key: 'dp' }, { key: 'defInn' }]),
    { po: 2, a: 1, dp: 1, defInn: 3 }
  );
});

test('守备汇总行：按列 prop 映射，字段隐藏/重排后仍对齐', () => {
  const totals = { po: 20, defInn: 3, catcherSb: 2 };
  assert.deepEqual(buildFieldingSummaryCells([
    { property: 'playerName' },
    { property: 'fieldPosition' },
    { property: 'po' },
    { property: 'defInn' },
    { property: 'catcherSb' }
  ], totals), ['Totals', '', '20', '3', '2']);
  // 隐藏 PO、SB 列后重排：每列仍取自己的合计，不按位置串列
  assert.deepEqual(buildFieldingSummaryCells([
    { property: 'playerName' },
    { property: 'fieldPosition' },
    { property: 'catcherSb' },
    { property: 'defInn' }
  ], totals), ['Totals', '', '2', '3']);
  assert.deepEqual(buildFieldingSummaryCells([
    { property: 'playerName' },
    { property: 'fieldPosition' }
  ], totals), ['Totals', '']);
  assert.deepEqual(buildFieldingSummaryCells([], totals), []);
});

test('守备局数显示复用投球局数格式化（.1/.2 记法）', () => {
  assert.equal(formatInningsPitched(3), '3');
  assert.equal(formatInningsPitched(2.2), '2.2');
  assert.equal(formatInningsPitched(2.1), '2.1');
  assert.equal(formatInningsPitched(0), '0');
  assert.equal(formatInningsPitched(null), '0');
});

test('比赛详情守备表接线：首个守位、替补缩进与 Totals 汇总', () => {
  const detail = fs.readFileSync(new URL('../src/components/admin/GameDetailFieldingTable.vue', import.meta.url), 'utf8');
  assert.match(detail, /buildFieldingDisplayRows/, '详情守备表复用行构建');
  assert.match(detail, /prop="playerName"/, '首列缺少 playerName prop，Totals 标签会丢失');
  assert.match(detail, /posLabel\(row\)/, 'POS 列通过 posLabel 显示首守位/多守位留空');
  assert.match(detail, /row\??\.fieldPosition/, 'POS 列显示首次上场守位');
  assert.match(detail, /row\.subRow/, '替补行缩进标记');
  assert.match(detail, /visibleFields/, '列由字段设置控制');
  assert.match(detail, /DEFAULT_FIELDING_FIELDS/, '缺省字段包含守备/捕手列');
  assert.match(detail, /buildFieldingTotals/, '详情守备表未接入 Totals 汇总');
  assert.match(detail, /buildFieldingSummaryCells\(columns, totals\.value\)/, '汇总行未按列 prop 取合计');
  assert.match(detail, /:prop="col\.key"/, '动态列未绑定 prop，汇总行无法稳定映射');
  assert.match(detail, /:show-summary="rows\.length > 0"/, '详情守备表未开启合计行');
  assert.match(detail, /summary-method="summaryMethod"/, '详情守备表缺少合计行方法');
  assert.match(detail, /el-table__footer-wrapper/, '合计行缺少样式钩子');
  const entry = fs.readFileSync(new URL('../src/components/admin/GameFieldingTable.vue', import.meta.url), 'utf8');
  for (const key of ['pb', 'catcherSb', 'catcherCs']) {
    assert.ok(entry.includes(`key: '${key}'`), `录入页守备表缺少 ${key} 列`);
  }
});
