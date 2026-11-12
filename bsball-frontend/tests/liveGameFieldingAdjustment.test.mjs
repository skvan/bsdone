// 赛后守备补记：从 playEvents 中筛出带失误的 Play，并整理守备方可选球员。
// 运行：node --test tests/liveGameFieldingAdjustment.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildFieldingAdjustmentCandidates,
  defensiveSide,
  describeBaseSituation,
  isFieldingErrorPlay,
  playerPositionOptions
} from '../src/utils/liveGameFieldingAdjustment.js';

const player = (id, name, position, extra = {}) => ({
  id,
  name,
  position,
  positions: [position],
  teamId: extra.teamId,
  ...extra
});

test('失误 Play 识别：E / 失误推进 / 漏接三振失误 / 文本含失误', () => {
  assert.equal(isFieldingErrorPlay({ resultCode: 'E' }), true);
  assert.equal(isFieldingErrorPlay({ resultCode: 'ADVANCE_FE' }), true);
  assert.equal(isFieldingErrorPlay({ resultCode: 'DK3_REACH_E' }), true);
  assert.equal(isFieldingErrorPlay({ resultCode: 'ERROR_ADVANCE' }), true);
  assert.equal(isFieldingErrorPlay({ resultCode: 'FC', resultText: '5下 失误上垒' }), true);
  assert.equal(isFieldingErrorPlay({ resultCode: 'K', resultText: '三振' }), false);
  assert.equal(isFieldingErrorPlay({ resultCode: 'HR', resultText: '全垒打' }), false);
  assert.equal(isFieldingErrorPlay(null), false);
});

test('守备方：上半局主队守备，下半局客队守备', () => {
  assert.equal(defensiveSide({ half: 'top' }), 'home');
  assert.equal(defensiveSide({ half: 'bottom' }), 'away');
  assert.equal(defensiveSide({}), 'away');
});

test('垒况文案：无人 / 单垒 / 双垒 / 满垒', () => {
  assert.equal(describeBaseSituation({}), '垒上无人');
  assert.equal(describeBaseSituation({ 1: 11 }), '一垒有人');
  assert.equal(describeBaseSituation({ 2: 22, 3: 33 }), '二、三垒有人');
  assert.equal(describeBaseSituation({ 1: 11, 2: 22, 3: 33 }), '满垒');
});

test('候选只包含带失误的 Play，并带守备方球员与已补记记录', () => {
  const playEvents = [
    {
      id: 'play-1', inning: 5, half: 'bottom', sequence: 167, outsBefore: 1,
      resultCode: 'E', resultText: '5下 失误：CF 主中外 (#23) 记 E',
      basesBefore: { 1: null, 2: 31, 3: null }
    },
    {
      id: 'play-2', inning: 5, half: 'bottom', sequence: 168, outsBefore: 2,
      resultCode: 'K', resultText: '三振'
    },
    {
      id: 'play-3', inning: 6, half: 'top', sequence: 169, outsBefore: 0,
      resultCode: 'ADVANCE_FE', resultText: '暴传失误推进',
      basesBefore: { 1: 45, 2: null, 3: null }
    }
  ];
  const data = {
    playEvents,
    homeLineup: [player(101, '主投手', 'P'), player(108, '主中外', 'CF')],
    awayLineup: [
      player(23, '客中外', 'CF'),
      player(24, '客捕手', 'C'),
      player(25, '客一垒', '1B')
    ],
    homeBench: [player(110, '主替补', '')],
    awayBench: [player(26, '客替补', 'LF')],
    homeFieldingPitcherRow: null,
    awayFieldingPitcherRow: null,
    homeRoster: [],
    awayRoster: [player(23, '客中外', 'CF')],
    adjustments: [
      { adjustmentId: 77, sourceEventId: 'play-1', playerId: 23, position: 'CF', status: 'APPROVED' }
    ]
  };

  const candidates = buildFieldingAdjustmentCandidates(data);

  assert.equal(candidates.length, 2);
  assert.equal(candidates[0].sourceEventId, 'play-1');
  assert.equal(candidates[0].side, 'away');
  assert.equal(candidates[0].inningLabel, '5下 #167');
  assert.equal(candidates[0].outsBefore, 1);
  assert.equal(candidates[0].baseLabel, '二垒有人');
  assert.equal(candidates[0].resultText, '5下 失误：CF 主中外 (#23) 记 E');
  assert.deepEqual(candidates[0].players.map((item) => item.id), [23, 24, 25, 26]);
  assert.equal(candidates[0].adjustments.length, 1);
  assert.equal(candidates[1].side, 'home');
  assert.deepEqual(candidates[1].players.map((item) => item.id), [101, 108, 110]);
});

test('守位选项：合并 position / positions，去重并排除 DH/PH/EP', () => {
  assert.deepEqual(
    playerPositionOptions({ position: 'CF', positions: ['CF', 'LF', 'DH', 'CF'] }),
    ['CF', 'LF']
  );
  assert.deepEqual(playerPositionOptions({ position: 'P', positions: [] }), ['P']);
  assert.deepEqual(playerPositionOptions({ position: 'DH', positions: ['DH', 'PH'] }), []);
  assert.deepEqual(playerPositionOptions(null), []);
});
