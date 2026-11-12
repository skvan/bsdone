import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  diffTeamState,
  summarizeTeamDiff,
  buildSubstitutionEvents,
  applyNetTeamState,
  battingAppearances,
  countActivePitchers,
  canCancelDh,
  originalLineupSlot,
  validateReentrySlot,
  effectiveUnavailableIds,
  findReentryInTransaction,
} from '../src/utils/liveSubstitutionMode.js';

const player = (id, name, position) => ({ id, name, position, number: String(id) });
const team = (lineup, bench = [], extra = {}) => ({
  teamId: 7,
  lineup,
  bench,
  fpId: 0,
  fpRow: null,
  unavailable: [],
  starterIds: Array.from({ length: 12 }, (_, i) => Number(lineup[i]?.id) || null),
  slotHistory: Array.from({ length: 12 }, () => []),
  ...extra,
});

test('无变化：不产生换人/守位/换投', () => {
  const before = team([player(1, 'A', 'SS'), player(2, 'B', 'C')]);
  const after = team([player(1, 'A', 'SS'), player(2, 'B', 'C')]);
  const diff = diffTeamState(before, after);
  assert.equal(diff.hasChanges, false);
  assert.deepEqual(diff.substitutions, []);
  assert.deepEqual(diff.positionChanges, []);
});

test('一笔换人：按槽位得到 out/in 与守位', () => {
  const before = team([player(1, '甲', 'SS'), player(2, '乙', 'C')]);
  const after = team([player(1, '甲', 'SS'), player(9, '丙', 'LF')]);
  const diff = diffTeamState(before, after);
  assert.equal(diff.hasChanges, true);
  assert.deepEqual(diff.substitutions, [{
    type: 'SUBSTITUTION',
    slot: 2,
    outPlayerId: 2,
    inPlayerId: 9,
    outName: '乙',
    inName: '丙',
    outPosition: 'C',
    inPosition: 'LF',
  }]);
  assert.deepEqual(summarizeTeamDiff(diff, '主队'), ['【主队】第2棒 乙(C) → 丙(LF)']);
});

test('守位互换：同一批球员只产生守位调整，不产生换人', () => {
  const before = team([player(1, '甲', 'SS'), player(2, '乙', '2B')]);
  const after = team([player(1, '甲', '2B'), player(2, '乙', 'SS')]);
  const diff = diffTeamState(before, after);
  assert.equal(diff.substitutions.length, 0);
  assert.deepEqual(diff.positionChanges.map((p) => [p.slot, p.playerId, p.outPosition, p.inPosition]), [
    [1, 1, 'SS', '2B'],
    [2, 2, '2B', 'SS'],
  ]);
});

test('同槽两次换人：只按 before/after 净变化记一笔', () => {
  const before = team([player(1, '甲', 'SS'), player(2, '乙', 'C')]);
  const after = team([player(1, '甲', 'SS'), player(10, '丁', 'C')]);
  const diff = diffTeamState(before, after);
  assert.equal(diff.substitutions.length, 1);
  assert.equal(diff.substitutions[0].outPlayerId, 2);
  assert.equal(diff.substitutions[0].inPlayerId, 10);
});

test('DH 取消：DH 位换成投手，标记 DH_CANCEL 并生成对应事件', () => {
  const before = team([player(1, '甲', 'SS'), player(5, 'DH', 'DH')], [], { fpId: 20 });
  const after = team([player(1, '甲', 'SS'), player(20, '投手', 'P')], [], { fpId: 20 });
  const diff = diffTeamState(before, after);
  assert.ok(diff.dhCancel);
  assert.equal(diff.dhCancel.outPlayerId, 5);
  assert.equal(diff.dhCancel.inPlayerId, 20);
  assert.match(summarizeTeamDiff(diff, '主队').join('\n'), /取消DH/);
  const events = buildSubstitutionEvents([diff], { transactionId: 'tx', inning: 3, topBottom: 'top' });
  assert.equal(events[0].changeType, 'DH_CANCEL');
  assert.equal(events[0].slot, 2);
});

test('DH 制换投：只变 fpId，记 PITCHING_CHANGE，不产生打序换人', () => {
  const before = team([player(1, '甲', 'SS')], [player(20, '新投', 'P')], { fpId: 19, fpRow: player(19, '旧投', 'P') });
  const after = team([player(1, '甲', 'SS')], [player(19, '旧投', 'P')], { fpId: 20, fpRow: player(20, '新投', 'P') });
  const diff = diffTeamState(before, after);
  assert.equal(diff.substitutions.length, 0);
  assert.deepEqual(diff.pitchingChange, {
    outPlayerId: 19, inPlayerId: 20, outName: '旧投', inName: '新投',
  });
  const events = buildSubstitutionEvents([diff], { transactionId: 'tx', inning: 4, topBottom: 'bottom' });
  assert.equal(events.length, 1);
  assert.equal(events[0].changeType, 'PITCHING_CHANGE');
});

test('只动板凳：不记换人', () => {
  const before = team([player(1, '甲', 'SS')], [player(9, '乙', '')]);
  const after = team([player(1, '甲', 'SS')], [player(10, '丙', '')]);
  const diff = diffTeamState(before, after);
  assert.equal(diff.hasChanges, false);
  assert.deepEqual(summarizeTeamDiff(diff), []);
});

test('净状态：先发被换下不进 slotHistory、进 unavailable；替补被换下进 slotHistory', () => {
  const before = team(
    [player(1, '先发', 'SS'), player(2, '替补B', 'C')],
    [player(9, '替补C', '')],
    {
      starterIds: [1, 99, null, null, null, null, null, null, null, null, null, null],
      slotHistory: [[], [8], [], [], [], [], [], [], [], [], [], []],
      unavailable: [8],
    },
  );
  const after = team([player(1, '先发', 'SS'), player(9, '替补C', 'C')]);
  const diff = diffTeamState(before, after);
  const net = applyNetTeamState(before, after, diff);
  assert.deepEqual(net.starterIds.slice(0, 2), [1, 99]);
  assert.deepEqual(net.slotHistory[1], [8, 2]);
  assert.ok(net.unavailable.includes(2), '被换下的替补要进不可再上名单');
  assert.ok(net.unavailable.includes(8), '原有不可再上保留');
  assert.ok(!net.unavailable.includes(9), '换上的人要从不可再上移除');
  assert.equal(net.outPositions.get(2), 'C');
});

test('净状态：先发换成替补时只进 unavailable，不把先发写进 slotHistory', () => {
  const before = team([player(1, '先发', 'SS')], [], { starterIds: [1] });
  const after = team([player(9, '替补', 'SS')]);
  const diff = diffTeamState(before, after);
  const net = applyNetTeamState(before, after, diff);
  assert.deepEqual(net.slotHistory[0], []);
  assert.deepEqual(net.unavailable, [1]);
  assert.equal(net.outPositions.get(1), 'SS');
});

test('事件：守位调整落 POSITION_CHANGE；P 位换人已覆盖时不重复落 PITCHING_CHANGE', () => {
  const before = team([player(1, '甲', 'P')], [], { fpId: 1 });
  const after = team([player(2, '乙', 'P')], [], { fpId: 2 });
  const diff = diffTeamState(before, after);
  const events = buildSubstitutionEvents([diff], { transactionId: 'tx', inning: 1, topBottom: 'top' });
  assert.deepEqual(events.map((e) => e.changeType), ['SUBSTITUTION']);

  const movedBefore = team([player(1, '甲', 'P')], [], { fpId: 1 });
  const movedAfter = team([player(1, '甲', 'SS')], [], { fpId: 1 });
  const movedEvents = buildSubstitutionEvents([diffTeamState(movedBefore, movedAfter)], {
    transactionId: 'tx2', inning: 1, topBottom: 'top',
  });
  assert.deepEqual(movedEvents.map((e) => e.changeType), ['POSITION_CHANGE']);
  assert.equal(movedEvents[0].outPlayerId, 1);
  assert.equal(movedEvents[0].inPlayerId, 1);
});

test('DH 取消后完成 P↔野手互换：仍识别 DH_CANCEL，并记录守位/换投', () => {
  const before = team(
    [player(1, 'SS甲', 'SS'), player(3, '三垒乙', '3B'), player(5, 'DH丙', 'DH')],
    [],
    { fpId: 20, starterIds: [1, 3, 5], fpRow: player(20, '投手丁', 'P') },
  );
  const after = team(
    [player(1, 'SS甲', 'SS'), player(3, '三垒乙', 'P'), player(20, '投手丁', '3B')],
    [],
    { fpId: 3, starterIds: [1, 3, 5] },
  );
  const diff = diffTeamState(before, after);
  assert.ok(diff.dhCancel, '互换后仍应识别取消 DH');
  assert.equal(diff.dhCancel.outPlayerId, 5);
  assert.equal(diff.dhCancel.inPlayerId, 20);
  assert.deepEqual(diff.positionChanges.map((p) => [p.playerId, p.outPosition, p.inPosition]), [
    [3, '3B', 'P'],
  ]);
  const events = buildSubstitutionEvents([diff], { transactionId: 'tx', inning: 3, topBottom: 'top' });
  const types = events.map((e) => e.changeType);
  assert.ok(types.includes('DH_CANCEL'));
  assert.ok(types.includes('POSITION_CHANGE'));
  assert.ok(types.includes('PITCHING_CHANGE'));
  assert.match(summarizeTeamDiff(diff, '主队').join('\n'), /取消DH/);
});

test('DH 规则：先发 DH 未打击且对方未换投时提醒；已打击或对方换投后放行', () => {
  const dh = { ...player(5, 'DH丙', 'DH'), stats: { batting: { ab: 0, bb: 0, hbp: 0, sf: 0, sh: 0, io: 0 } } };
  const before = team([dh], [], { starterIds: [5], roster: [dh] });
  const blocked = canCancelDh({ team: before, opponentPlayers: [] });
  assert.equal(blocked.ok, false);
  assert.match(blocked.warning, /先发 DH/);

  const batted = { ...dh, stats: { batting: { ab: 1, so: 1 } } };
  assert.equal(canCancelDh({
    team: team([batted], [], { starterIds: [5], roster: [batted] }),
    opponentPlayers: [],
  }).ok, true);

  const twoOpponentPitchers = [
    { id: 31, stats: { pitching: { np: 10 } } },
    { id: 32, stats: { pitching: { pitchBf: 1 } } },
  ];
  assert.equal(canCancelDh({ team: before, opponentPlayers: twoOpponentPitchers }).ok, true);
});

test('打席数/活跃投手统计口径', () => {
  assert.equal(battingAppearances({ stats: { batting: { ab: 2, bb: 1, hbp: 0, sf: 0, sh: 0, io: 0 } } }), 3);
  assert.equal(battingAppearances({ stats: { batting: { pa: 4 } } }), 4);
  assert.equal(countActivePitchers([
    { id: 1, stats: { pitching: { np: 1 } } },
    { id: 1, stats: { pitching: { np: 1 } } },
    { id: 2, stats: { pitching: { np: 0 } } },
  ]), 1);
});

test('净状态：仅换投（无打序投手）把旧投手列入不可再上并记录 P 位', () => {
  const before = team([player(1, '甲', 'DH')], [player(9, '乙', '')], { fpId: 8, fpRow: player(8, '旧投', 'P') });
  const after = team([player(1, '甲', 'DH')], [player(10, '丙', '')], { fpId: 10, fpRow: player(10, '新投', 'P') });
  const diff = diffTeamState(before, after);
  assert.deepEqual(diff.substitutions, []);
  assert.ok(diff.pitchingChange);
  const net = applyNetTeamState(before, after, diff);
  assert.ok(net.unavailable.includes(8), '旧投手要进不可再上');
  assert.ok(!net.unavailable.includes(10), '新投手不能留在不可再上');
  assert.equal(net.outPositions.get(8), 'P');
});

test('净状态：垒球仅换投不把旧投手列入不可再上', () => {
  const before = team([player(1, '甲', 'DH')], [], { fpId: 8 });
  const after = team([player(1, '甲', 'DH')], [], { fpId: 10 });
  const net = applyNetTeamState(before, after, diffTeamState(before, after), { sport: 'SOFTBALL' });
  assert.ok(!net.unavailable.includes(8));
});

test('净状态：P↔野手互换（旧投手仍在打序）不把旧投手列入不可再上', () => {
  const before = team([player(1, '投', 'P'), player(2, '野', 'SS')], [], { fpId: 1 });
  const after = team([player(1, '投', 'SS'), player(2, '野', 'P')], [], { fpId: 2 });
  const diff = diffTeamState(before, after);
  assert.ok(diff.pitchingChange);
  const net = applyNetTeamState(before, after, diff);
  assert.ok(!net.unavailable.includes(1), '仍在打序的旧投手不能进不可再上');
  assert.ok(!net.outPositions.has(1));
});

test('净状态：DH被代打但仍是守备投手，旧DH不进不可再上', () => {
  const before = team([player(5, '大谷', 'DH')], [], { fpId: 5, fpRow: player(5, '大谷', 'P') });
  const after = team([player(9, '代打', 'DH')], [], { fpId: 5, fpRow: player(5, '大谷', 'P') });
  const diff = diffTeamState(before, after);
  const net = applyNetTeamState(before, after, diff);
  assert.ok(net.unavailable.includes(9) === false, '新代打不能直接进不可再上');
  assert.ok(!net.unavailable.includes(5), '仍在投球的旧DH不能进不可再上');
});

test('净状态：DH被代打且不再投球，旧DH进不可再上', () => {
  const before = team([player(5, '大谷', 'DH')], [], { fpId: 5, fpRow: player(5, '大谷', 'P') });
  const after = team([player(9, '代打', 'DH')], [], { fpId: 10, fpRow: player(10, '新投', 'P') });
  const diff = diffTeamState(before, after);
  const net = applyNetTeamState(before, after, diff);
  assert.ok(net.unavailable.includes(5), '既不投球也不打DH时才进不可再上');
});

test('垒球再上场：原槽位先发优先、换人史兜底、纯板凳 null（#479）', () => {
  const state = { starterIds: [null, 5, null], slotHistory: [[], [], [7]] };
  assert.equal(originalLineupSlot(state, 5), 1);
  assert.equal(originalLineupSlot(state, 7), 2);
  assert.equal(originalLineupSlot(state, 99), null);
  assert.equal(originalLineupSlot(state, null), null);
});

test('垒球再上场：同槽通过，错槽/无法定位阻止，纯板凳放行（#479）', () => {
  const state = { starterIds: [5, 6], slotHistory: [[], [], [7]] };
  assert.deepEqual(validateReentrySlot({ team: state, playerId: 5, targetSlot: 0 }), { ok: true, error: '' });
  const wrong = validateReentrySlot({ team: state, playerId: 5, targetSlot: 2 });
  assert.equal(wrong.ok, false);
  assert.match(wrong.error, /第1棒/);
  const unknown = validateReentrySlot({ team: state, playerId: 7, targetSlot: -1 });
  assert.equal(unknown.ok, false);
  assert.match(unknown.error, /无法确认/);
  assert.deepEqual(validateReentrySlot({ team: state, playerId: 99, targetSlot: 2 }), { ok: true, error: '' });
});

test('垒球再上场开关关闭：换下球员进不可再上；开启则不进制（#479）', () => {
  const before = team([player(1, '甲', 'SS'), player(2, '乙', 'C')], [player(9, '替补', '')]);
  const after = team([player(9, '替补', 'SS'), player(2, '乙', 'C')]);
  const diff = diffTeamState(before, after);
  const closed = applyNetTeamState(before, after, diff, { sport: 'SOFTBALL', reentryAllowed: false });
  assert.ok(closed.unavailable.includes(1));
  const open = applyNetTeamState(before, after, diff, { sport: 'SOFTBALL', reentryAllowed: true });
  assert.ok(!open.unavailable.includes(1));
});

test('垒球再上场：targetSlot 为空/NaN 视为无法定位（#479）', () => {
  const state = { starterIds: [5] };
  assert.equal(validateReentrySlot({ team: state, playerId: 5, targetSlot: null }).ok, false);
  assert.equal(validateReentrySlot({ team: state, playerId: 5, targetSlot: undefined }).ok, false);
  assert.equal(validateReentrySlot({ team: state, playerId: 5, targetSlot: '' }).ok, false);
});

test('有效不可再上：关闭态回填板凳前打序球员，开启态只认显式名单（#479）', () => {
  const state = { unavailable: [9], bench: [{ id: 1 }, { id: 9 }, { id: 5 }], starterIds: [1, null], slotHistory: [[], [2]] };
  assert.deepEqual(effectiveUnavailableIds({ ...state, reentryAllowed: false }), [9, 1]);
  assert.deepEqual(effectiveUnavailableIds({ ...state, reentryAllowed: true }), [9]);
});

test('同事务换下再上：关闭态拦截，开启态放行（#479）', () => {
  const diff = {
    teamId: 7,
    substitutions: [
      { slot: 3, outPlayerId: 1, inPlayerId: 2 },
      { slot: 5, outPlayerId: 3, inPlayerId: 1 },
    ],
    pitchingChange: null,
  };
  assert.deepEqual(findReentryInTransaction([diff], { reentryAllowed: false }), { teamId: 7, inPlayerId: 1, slot: 5 });
  assert.equal(findReentryInTransaction([diff], { reentryAllowed: true }), null);
  const pitchDiff = {
    teamId: 7,
    substitutions: [{ slot: 2, outPlayerId: 4, inPlayerId: 5 }],
    pitchingChange: { outPlayerId: 6, inPlayerId: 4 },
  };
  assert.deepEqual(findReentryInTransaction([pitchDiff], { reentryAllowed: false }), { teamId: 7, inPlayerId: 4, slot: null });
});

test('净状态：关闭态回填历史遗漏的板凳前打序球员（#479）', () => {
  const starterIds = [1, null, null, null, null, null, null, null, null, null, null, null];
  const before = team([player(2, '乙', 'SS')], [player(1, '甲', 'SS')], { starterIds });
  const after = team([player(9, '替补', 'SS')], [player(1, '甲', 'SS')], { starterIds });
  const diff = diffTeamState(before, after);
  const closed = applyNetTeamState(before, after, diff, { sport: 'SOFTBALL', reentryAllowed: false });
  assert.ok(closed.unavailable.includes(1), '前先发在板凳上应回填不可再上');
  const open = applyNetTeamState(before, after, diff, { sport: 'SOFTBALL', reentryAllowed: true });
  assert.ok(!open.unavailable.includes(1));
});
