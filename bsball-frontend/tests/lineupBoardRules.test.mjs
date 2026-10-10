// 阵容规则层单测（useLineupBoard 动作；校验用真实 validateRosterPositions，不 mock）
// 运行：node --test tests/lineupBoardRules.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ref } from 'vue';
import { useLineupBoard } from '../src/composables/useLineupBoard.js';
import { isPlaceholder, posOf } from '../src/utils/lineupBoardRules.js';

const P = (id) => ({ id, name: `球员${id}`, number: id, teamId: 7 });
const FIELD8 = ['C', '1B', '2B', '3B', 'SS', 'LF', 'CF', 'RF'];

function board(mode = 'BASEBALL') {
  const b = useLineupBoard(ref(mode), ref(7));
  b.setRoster([...Array(12)].map((_, i) => P(i + 1)));
  return b;
}
function fill8(b, from = 20) {
  // 8 名守备员用不冲突的 id（20-27）：避开投手 P(3)、DH P(12) 等测试数据撞号
  FIELD8.forEach((code, i) => b.placeAtSlot(P(from + i), code));
}

test('初始为 9 行占位空行，且 ke 特例把「未指定守备位置」提示置空', () => {
  const b = board();
  assert.equal(b.rows.value.length, 9);
  assert.ok(b.rows.value.every(isPlaceholder));
  assert.equal(b.validation.value.ok, false);
  assert.equal(b.validation.value.msg, undefined);
});

test('首次落位定棒次：落位顺序即打线顺序（下标=棒次）', () => {
  const b = board();
  b.placeAtSlot(P(5), 'CF');
  b.placeAtSlot(P(3), 'P');
  assert.equal(b.rows.value[0].id, 5);
  assert.equal(b.rows.value[1].id, 3);
  assert.equal(b.rows.value.length, 9);
});

test('无 DH 时拖人上投手丘 → 进打线且 fieldingPitcherId 同步', () => {
  const b = board();
  b.placeAtSlot(P(3), 'P');
  assert.equal(posOf(b.rows.value[0]), 'P');
  assert.equal(b.fieldingPitcherId.value, 3);
});

test('满 9 人（投手制）校验通过、lineupComplete 为真', () => {
  const b = board();
  b.placeAtSlot(P(3), 'P');
  fill8(b);
  assert.equal(b.rows.value.length, 9);
  assert.equal(b.validation.value.ok, true);
  assert.equal(b.lineupComplete.value, true);
});

test('池球员拖到已占用卡槽 → 顶替，原占用者回池（不在替补）', () => {
  const b = board();
  b.placeAtSlot(P(2), 'C');
  b.placeAtSlot(P(9), 'C');
  assert.equal(b.slotOfRow('C').id, 9);
  assert.equal(b.rows.value.filter((r) => !isPlaceholder(r)).length, 1);
  assert.equal(b.bench.value.length, 0);
  assert.ok(b.pool.value.some((p) => p.id === 2));
});

test('打线内两行拖到占用槽 → 整体对调（棒次与位置一起换）', () => {
  const b = board();
  b.placeAtSlot(P(2), 'C');
  b.placeAtSlot(P(3), '1B');
  b.placeAtSlot(P(2), '1B');
  assert.equal(b.rows.value[0].id, 3);
  assert.equal(b.rows.value[0].position, 'C');
  assert.equal(b.rows.value[1].id, 2);
  assert.equal(b.rows.value[1].position, '1B');
});

test('打线内拖到空槽 → 只改守备位，棒次不变', () => {
  const b = board();
  b.placeAtSlot(P(2), 'C');
  b.placeAtSlot(P(3), '1B');
  b.placeAtSlot(P(2), 'SS');
  assert.equal(b.rows.value[0].id, 2);
  assert.equal(b.rows.value[0].position, 'SS');
});

test('移出 → 回替补、行数不变、空行归位尾部且校验报未指定守备位置', () => {
  const b = board();
  b.placeAtSlot(P(2), 'C');
  b.placeAtSlot(P(3), '1B');
  b.sendBackToBench(2);
  assert.equal(b.rows.value.length, 9);
  assert.ok(isPlaceholder(b.rows.value[1]));
  assert.equal(b.bench.value.length, 1);
  assert.equal(b.bench.value[0].id, 2);
  assert.equal(b.validation.value.ok, false);
  assert.match(b.validation.value.msg ?? '', /未指定守备位置/);
});

test('放 DH：投手自动移出打线、DH 制 9 行校验通过、有 notice', () => {
  const b = board();
  b.placeAtSlot(P(3), 'P');
  fill8(b);
  b.placeAtSlot(P(12), 'DH');
  assert.equal(b.rows.value.filter((r) => !isPlaceholder(r)).length, 9);
  assert.equal(b.slotOfRow('P'), null);
  assert.equal(b.fieldingPitcherId.value, 3);
  assert.equal(b.validation.value.ok, true);
  assert.equal(b.fieldingPitcherRequired.value, false);
  assert.match(b.notice.value, /已按 DH 制把投手移出打线/);
  assert.equal(b.lastDhAddedFromPoolId.value, 12);
});

test('仅守备投手拖进打线列表 → 10 人打击（P+8+DH）校验通过', () => {
  const b = board();
  b.placeAtSlot(P(3), 'P');
  fill8(b);
  b.placeAtSlot(P(12), 'DH');
  b.addToBatting(P(3));
  assert.equal(b.rows.value.length, 10);
  assert.equal(posOf(b.rows.value[9]), 'P');
  assert.equal(b.validation.value.ok, true);
});

test('8 守位 + DH 且投手未指定 → fieldingPitcherRequired 为真，指定后 complete', () => {
  const b = board();
  fill8(b);
  b.placeAtSlot(P(12), 'DH');
  assert.equal(b.rows.value.filter((r) => !isPlaceholder(r)).length, 9);
  assert.equal(b.validation.value.ok, true);
  assert.equal(b.fieldingPitcherRequired.value, true);
  assert.equal(b.lineupComplete.value, false);
  b.setFieldingPitcher(3);
  assert.equal(b.lineupComplete.value, true);
});

test('移除 DH → 投手自动回打线（9 行投手制）', () => {
  const b = board();
  b.placeAtSlot(P(3), 'P');
  fill8(b);
  b.placeAtSlot(P(12), 'DH');
  b.removeDh();
  assert.equal(b.rows.value.length, 9);
  assert.equal(b.slotOfRow('DH'), null);
  assert.equal(b.slotOfRow('P').id, 3);
  assert.equal(b.validation.value.ok, true);
});

test('已有 DH 时拖人上投手丘 → 仅守备不进打线', () => {
  const b = board();
  fill8(b);
  b.placeAtSlot(P(12), 'DH');
  b.placeAtSlot(P(3), 'P');
  assert.equal(b.slotOfRow('P'), null);
  assert.equal(b.fieldingPitcherId.value, 3);
  assert.equal(b.rows.value.filter((r) => !isPlaceholder(r)).length, 9);
});

test('打线满 10 人时 addToBatting 拒绝并给 notice', () => {
  const b = board();
  b.placeAtSlot(P(3), 'P');
  fill8(b);
  b.placeAtSlot(P(12), 'DH'); // 决策 6b：投手移出打线 → 9 人打击
  b.addToBatting(P(3)); // 投手拖回打线 → 10 人打击
  assert.equal(b.rows.value.length, 10);
  b.addToBatting(P(1)); // 第 11 人 → 满员拒绝
  assert.equal(b.rows.value.filter((r) => !isPlaceholder(r)).length, 10);
  assert.match(b.notice.value, /最多 10 人/);
});

test('reorderBatting 改棒次且空行恒留尾部', () => {
  const b = board();
  b.placeAtSlot(P(2), 'C');
  b.placeAtSlot(P(3), '1B');
  b.placeAtSlot(P(4), '2B');
  b.reorderBatting(2, 0);
  assert.deepEqual(b.rows.value.slice(0, 3).map((r) => r.id), [4, 2, 3]);
  assert.ok(b.rows.value.slice(3).every(isPlaceholder));
});

test('applyTemplate：按 slots 落位、bench 覆盖、lastDh 清空', () => {
  const b = board();
  b.lastDhAddedFromPoolId.value = 9;
  b.applyTemplate({
    slots: [
      { playerId: 3, position: 'P' },
      { playerId: 2, position: 'C' },
      { playerId: 12, position: 'DH' },
    ],
    benchPlayerIds: [5, 6],
    pitcherId: 3,
  });
  assert.equal(b.slotOfRow('P').id, 3);
  assert.equal(b.slotOfRow('C').id, 2);
  assert.equal(b.slotOfRow('DH').id, 12);
  assert.deepEqual(b.bench.value.map((p) => p.id), [5, 6]);
  assert.equal(b.lastDhAddedFromPoolId.value, null);
});

test('垒球模式：baseCount 10 / maxCount 11', () => {
  const b = board('SOFTBALL');
  assert.equal(b.baseCount.value, 10);
  assert.equal(b.maxCount.value, 11);
  assert.equal(b.rows.value.length, 10);
});

test('loadDraft：快照恢复（含占位行、替补、投手）', () => {
  const b = board();
  b.loadDraft({
    rows: [
      { id: 3, name: '球员3', number: 3, teamId: 7, position: 'P', stats: {} },
      { id: -10000, name: '', number: '', teamId: 7, position: '', stats: {} },
      { id: 2, name: '球员2', number: 2, teamId: 7, position: 'C', stats: {} },
    ],
    bench: [P(5)],
    fieldingPitcherId: 3,
    lastDhAddedFromPoolId: null,
  });
  assert.equal(b.rows.value.length, 9);
  assert.equal(b.rows.value[0].id, 3);
  assert.equal(b.rows.value[1].id, 2);
  assert.ok(b.rows.value.slice(2).every(isPlaceholder));
  assert.equal(b.bench.value.length, 1);
  assert.equal(b.fieldingPitcherId.value, 3);
});
