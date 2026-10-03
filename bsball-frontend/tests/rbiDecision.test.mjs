import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

import {
  decideRbi,
  rbiContextForAdvance,
  rbiContextForBattedBall,
  sacrificeStatForError,
} from '../src/domain/rbi/rbiDecision.js';

const credit = (input) => decideRbi(input).creditRbi;

test('dropped sacrifice fly credits an RBI when ordinary defense would score the runner', () => {
  const context = rbiContextForBattedBall({ playType: 'E', bipCode: 'bip:fly:sac:e' });
  assert.equal(credit(context), true);
});

test('hit followed by a throwing error keeps the RBI caused by the hit', () => {
  const context = rbiContextForAdvance({ resultCode: 'H1', advanceReasonId: 'safe_same_err_rbi' });
  assert.equal(credit(context), true);
  assert.equal(decideRbi(context).reason, 'RUN_PRECEDED_ERROR');
});

test('hit or fielders choice is separated from a later same-play error', () => {
  assert.equal(credit(rbiContextForAdvance({ resultCode: 'FC', advanceReasonId: 'safe_same_err_rbi' })), true);
  assert.equal(credit(rbiContextForAdvance({ resultCode: 'H1', advanceReasonId: 'safe_same_err' })), false);
  assert.equal(credit(rbiContextForAdvance({ resultCode: 'H1', advanceReasonId: 'safe_diff_err' })), false);
});

test('ordinary fielding error that creates the run does not credit an RBI', () => {
  const context = rbiContextForBattedBall({ playType: 'E', bipCode: 'bip:g:e' });
  assert.equal(credit(context), false);
});

test('failed double-play opportunity does not credit an RBI', () => {
  const context = rbiContextForBattedBall({
    playType: 'E',
    bipCode: 'bip:g:dp:e',
    basesBefore: { 1: { id: 10 }, 2: { id: 11 }, 3: { id: 12 } },
    outsBefore: 0,
  });
  assert.equal(context.doublePlayContext, 'FAILED_DOUBLE_PLAY_OPPORTUNITY');
  assert.equal(credit(context), false);
});

test('bases-loaded walk and hit by pitch credit an RBI', () => {
  assert.equal(credit({ resultCode: 'BB' }), true);
  assert.equal(credit({ resultCode: 'IBB' }), true);
  assert.equal(credit({ resultCode: 'HBP' }), true);
});

test('ordinary sacrifice fly credits an RBI', () => {
  assert.equal(credit({ resultCode: 'SF' }), true);
});

test('wild pitch passed ball and balk never credit the batter', () => {
  assert.equal(credit({ resultCode: 'OTHER', scoreCause: 'WILD_PITCH' }), false);
  assert.equal(credit({ resultCode: 'OTHER', scoreCause: 'PASSED_BALL' }), false);
  assert.equal(credit({ resultCode: 'OTHER', scoreCause: 'BALK' }), false);
});

test('sacrifice bunt with a throwing error credits an RBI when the run was expected', () => {
  const context = rbiContextForBattedBall({ playType: 'E', bipCode: 'bip:b:sac:e' });
  assert.equal(credit(context), true);
});

test('sacrifice error excludes an at-bat only when the required advance occurred', () => {
  assert.equal(sacrificeStatForError({
    bipCode: 'bip:b:sac:e',
    scoredRuns: 0,
    runnerAdvanced: true,
  }), 'SH');
  assert.equal(sacrificeStatForError({
    bipCode: 'bip:b:sac:e',
    scoredRuns: 0,
    runnerAdvanced: false,
  }), null);
  assert.equal(sacrificeStatForError({
    bipCode: 'bip:fly:sac:e',
    scoredRuns: 0,
    runnerAdvanced: true,
  }), null);
  assert.equal(sacrificeStatForError({
    bipCode: 'bip:fly:sac:e',
    scoredRuns: 1,
    runnerAdvanced: true,
  }), 'SF');
});

test('force and reverse-force double plays do not credit an RBI', () => {
  assert.equal(credit({ resultCode: 'DP', doublePlayContext: 'FORCE_DOUBLE_PLAY' }), false);
  assert.equal(credit({ resultCode: 'DP', doublePlayContext: 'REVERSE_FORCE_DOUBLE_PLAY' }), false);
});

test('LiveGame persists structured RBI decisions instead of checking playType only', async () => {
  const source = await readFile(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');
  assert.match(source, /options:m\?\.rbiDecision\?\{rbiDecision:m\.rbiDecision\}:null/);
  assert.match(source, /decideRbi\(rbiContextForBattedBall/);
  assert.match(source, /basesBefore:r,outsBefore:l\.outs/);
  assert.match(source, /rbiCredited:Boolean\(t\.batter&&t\.rbiDelta\)/);
  assert.match(source, /rbiDecision:t\.rbiDecision\?\?w\.options\?\.rbiDecision\?\?null/);
  assert.match(source, /sacrificeStatForError\(\{bipCode:e\.opts\?\.bipCode,scoredRuns,runnerAdvanced\}\)/);
  assert.match(source, /I\("bip:g:reverse:dp"\)/);
  assert.match(source, /I\("bip:g:dp:e"\)/);
  assert.match(source, /case"bip:g:reverse:dp"/);
  assert.match(source, /case"bip:g:dp:e"/);
  assert.match(source, /a\.stats\.batting\[w\]=\(a\.stats\.batting\[w\]\?\?0\)\+1/);
  assert.doesNotMatch(source, /creditRBI:e\.playType!=="E"/);
});
