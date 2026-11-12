// #524 守备表逐位置明细：多守位判定、主行汇总一致性、GS 口径、顺序与 Totals 排除
// 运行：node --test tests/gameFieldingBreakdown.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  attachFieldingPositions,
  buildFieldingDisplayRows,
  buildFieldingTotals
} from '../src/utils/gameFieldingRows.js';

function baseRow(overrides = {}) {
  return {
    teamId: 1,
    playerId: 50,
    position: 'P',
    firstPosition: '3B',
    startingPosition: '3B',
    battingOrder: 5,
    listOrder: 4,
    fieldingGs: 1,
    po: 1,
    a: 3,
    e: 1,
    dp: 1,
    defInn: 9,
    pb: 0,
    catcherSb: 0,
    catcherCs: 0,
    fieldingPositions: [
      { teamId: 1, playerId: 50, position: '3B', outs: 15, po: 1, a: 2, e: 1, dp: 1, fieldingGs: true },
      { teamId: 1, playerId: 50, position: 'P', outs: 12, po: 0, a: 1, e: 0, dp: 0, fieldingGs: false }
    ],
    ...overrides
  };
}

test('#524 多守位：生成逐位置明细，顺序为起始守位优先，主行标记多守位', () => {
  const [row] = buildFieldingDisplayRows([baseRow()], 1);
  assert.equal(row.multiPosition, true);
  assert.equal(row.kind, 'player');
  assert.deepEqual(row.breakdown.map((b) => b.position), ['3B', 'P']);
  assert.deepEqual(row.breakdown.map((b) => b.defInn), [5, 4]);
  assert.deepEqual(row.breakdown.map((b) => b.fieldingGs), [1, 0]);
  assert.equal(row.breakdown[0].kind, 'breakdown');
  assert.equal(row.breakdown[0].playerId, 50);
  assert.equal(row.fieldPosition, '3B', '主行仍按首次守位归组');
  assert.equal(row.position, 'P', '汇总行的当前位置保留');
});

test('#524 多守位：主行计数与明细之和一致（PO/A/E/DP/INN）', () => {
  const [row] = buildFieldingDisplayRows([baseRow()], 1);
  const sum = (key) => row.breakdown.reduce((n, b) => n + b[key], 0);
  for (const key of ['po', 'a', 'e', 'dp', 'pb', 'catcherSb', 'catcherCs']) {
    assert.equal(row[key], sum(key), `${key} 主行应等于明细之和`);
  }
  assert.equal(row.defInn, row.breakdown[0].defInn + row.breakdown[1].defInn);
});

test('#524 单守位或无逐位置数据：不出现展开标记', () => {
  const single = baseRow({
    po: 1,
    a: 2,
    e: 0,
    dp: 0,
    defInn: 5,
    fieldingPositions: [
      { position: '3B', outs: 15, po: 1, a: 2, e: 0, dp: 0 }
    ]
  });
  const [singleRow] = buildFieldingDisplayRows([single], 1);
  assert.equal(singleRow.multiPosition, false);
  assert.deepEqual(singleRow.breakdown, []);

  const legacy = baseRow({ fieldingPositions: null });
  const [legacyRow] = buildFieldingDisplayRows([legacy], 1);
  assert.equal(legacyRow.multiPosition, false);
  assert.deepEqual(legacyRow.breakdown, []);
});

test('#524 明细与球员汇总不一致：不展开，保留现有汇总行', () => {
  const [row] = buildFieldingDisplayRows([baseRow({ po: 2 })], 1);
  assert.equal(row.multiPosition, false);
  assert.deepEqual(row.breakdown, []);
  assert.equal(row.po, 2);
});

test('#524 INN 一致性按出局数比较（5.1 = 16 outs）', () => {
  const positions = [
    { position: 'LF', outs: 9, po: 1, a: 1 },
    { position: 'RF', outs: 7, po: 1, a: 1 }
  ];
  const row = baseRow({
    firstPosition: 'LF',
    startingPosition: 'LF',
    position: 'RF',
    po: 2,
    a: 2,
    e: 0,
    dp: 0,
    defInn: 5.1,
    fieldingPositions: positions
  });
  assert.equal(buildFieldingDisplayRows([row], 1)[0].multiPosition, true);

  const mismatch = { ...row, defInn: 5.2 };
  assert.equal(buildFieldingDisplayRows([mismatch], 1)[0].multiPosition, false);
});

test('#524 GS：有 startingPosition 时按先发守位判定，DH 后上场全为 0', () => {
  const row = baseRow({
    firstPosition: 'LF',
    startingPosition: '',
    position: 'RF',
    po: 2,
    a: 2,
    e: 0,
    dp: 0,
    defInn: 9,
    fieldingPositions: [
      { position: 'LF', outs: 18, po: 2, a: 1, fieldingGs: true },
      { position: 'RF', outs: 9, po: 0, a: 1, fieldingGs: false }
    ]
  });
  const [out] = buildFieldingDisplayRows([row], 1);
  assert.deepEqual(out.breakdown.map((b) => b.fieldingGs), [0, 0]);
});

test('#524 GS：旧数据无 startingPosition 时回退逐位置 fieldingGs', () => {
  const row = baseRow({ startingPosition: undefined });
  const [out] = buildFieldingDisplayRows([row], 1);
  assert.deepEqual(out.breakdown.map((b) => b.fieldingGs), [1, 0]);
});

test('#524 实时快照 byPos：旧数字值按 outs 处理，字段与接口一致', () => {
  const row = baseRow({
    a: 2,
    fieldingPositions: null,
    fieldingByPos: {
      '3B': { outs: 15, po: 1, a: 2, e: 1, dp: 1 },
      'P': 12
    }
  });
  const [out] = buildFieldingDisplayRows([row], 1);
  assert.equal(out.multiPosition, true);
  assert.deepEqual(out.breakdown.map((b) => b.position), ['3B', 'P']);
  assert.equal(out.breakdown[1].outs, 12);
  assert.equal(out.breakdown[1].defInn, 4);
  assert.deepEqual(
    out.breakdown.map((b) => [b.po, b.a, b.e, b.dp]),
    [[1, 2, 1, 1], [0, 0, 0, 0]]
  );
});

test('#524 明细排序：起始守位在前，其余保持输入（首次触达）顺序', () => {
  const row = baseRow({
    firstPosition: 'P',
    startingPosition: 'P',
    position: 'P',
    fieldingPositions: [
      { position: 'P', outs: 12, po: 0, a: 1 },
      { position: '3B', outs: 15, po: 1, a: 2, e: 1, dp: 1 }
    ]
  });
  const [out] = buildFieldingDisplayRows([row], 1);
  assert.deepEqual(out.breakdown.map((b) => b.position), ['P', '3B']);
});

test('#524 首次守位缺失时用明细首项兜底归组', () => {
  const row = baseRow({ firstPosition: '', startingPosition: '3B' });
  const [out] = buildFieldingDisplayRows([row], 1);
  assert.equal(out.fieldPosition, '3B');
  assert.equal(out.multiPosition, true);
});

test('#524 其它球员/非守备位的明细被忽略', () => {
  const row = baseRow({
    fieldingPositions: [
      { teamId: 1, playerId: 50, position: '3B', outs: 15, po: 1, a: 2, e: 1, dp: 1 },
      { teamId: 1, playerId: 50, position: 'P', outs: 12, po: 0, a: 1 },
      { teamId: 1, playerId: 99, position: 'LF', outs: 27, po: 9 },
      { teamId: 1, playerId: 50, position: 'DH', outs: 0, po: 99 }
    ]
  });
  const [out] = buildFieldingDisplayRows([row], 1);
  assert.deepEqual(out.breakdown.map((b) => b.position), ['3B', 'P']);
});

test('#524 赛后接口：按 teamId+playerId 挂载逐位置数据，不串队', () => {
  const stats = [
    { teamId: 1, playerId: 50 },
    { teamId: 1, playerId: 60 },
    { teamId: 2, playerId: 50 }
  ];
  const positions = [
    { teamId: 1, playerId: 50, position: '3B', outs: 15 },
    { teamId: 1, playerId: 50, position: 'P', outs: 12 },
    { teamId: 2, playerId: 50, position: 'CF', outs: 27 }
  ];
  const out = attachFieldingPositions(stats, positions);
  assert.equal(out[0].fieldingPositions.length, 2);
  assert.equal(out[1].fieldingPositions, undefined);
  assert.equal(out[2].fieldingPositions.length, 1);
  assert.equal(out[2].fieldingPositions[0].position, 'CF');

  const untouched = [{ teamId: 1, playerId: 70 }];
  attachFieldingPositions(untouched, []);
  assert.equal(untouched[0].fieldingPositions, undefined);
  attachFieldingPositions(untouched, null);
  assert.equal(untouched[0].fieldingPositions, undefined);
});

test('#524 Totals：展开行不重复计入', () => {
  const [main] = buildFieldingDisplayRows([baseRow()], 1);
  const breakdown = { ...main.breakdown[0], kind: 'breakdown', po: 99 };
  const totals = buildFieldingTotals([main, breakdown], [
    { key: 'po' },
    { key: 'defInn' }
  ]);
  assert.equal(totals.po, 1);
  assert.equal(totals.defInn, 9);
});
