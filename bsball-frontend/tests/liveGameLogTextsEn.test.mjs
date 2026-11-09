// 实时日志英文模式（B4a）：只影响新产生日志；历史中文日志不迁移
// 运行：node --test tests/liveGameLogTextsEn.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  setLogTextLocale,
  nameWithNumber,
  runnerLabel,
  fielderLabel,
  advanceText,
  outText,
  caughtStealingText,
  pickoffGroup,
  fcGroup,
  pitchText,
  coachTimeoutText,
  subText,
  setRunnerText,
  pickoffNoneText,
  fpMissText,
  foulErrorText,
  sysText,
  errorAdvanceText,
  stealSingleText,
  runCauseLabel,
  staysOnBaseText,
  errorNoteText,
  fieldingIpNoteText,
  hitResultText,
  noAdvanceRunText,
  noAdvanceThirdText,
  runOutNoScoreText,
  dpMainText,
  dpRunnerOutText
} from '../src/utils/liveGameLogTexts.js';

const player = { name: 'Alex', number: 7 };
const runner = { name: 'Sam', number: 21 };

test('英文位置/垒位/出局累计', () => {
  setLogTextLocale('en');
  assert.equal(nameWithNumber(player), 'Alex (#7)');
  assert.equal(runnerLabel(1, runner), '1B runner Sam (#21)');
  assert.equal(fielderLabel('SS', player), 'SS Shortstop Alex (#7)');
  assert.equal(runCauseLabel('ERROR', 'score'), 'Scored on error');
  assert.equal(advanceText({ reasonCn: '盗垒成功 (SB)', runner, from: 1, to: 2 }), 'Stole base (SB): 1B runner Sam (#21) advances to 2B — safe');
  assert.equal(
    advanceText({ reasonCn: '安打得分', runner, from: 3, to: 4, rbiNote: 'Alex：3打点' }),
    'Scored on hit: 3B runner Sam (#21) scores (Alex: 3 RBI)'
  );
  assert.equal(
    advanceText({ reasonCn: 'Scored on error', runner, from: 3, to: 4 }),
    'Scored on error: 3B runner Sam (#21) scores',
    'runCauseLabel 已英译时不应再次映射'
  );
  assert.equal(
    advanceText({ reasonCn: '盗垒（连续进垒）', runner, from: 1, to: 3 }),
    'Stolen base (continuous): 1B runner Sam (#21) advances to 3B — safe'
  );
  assert.equal(
    outText({ reasonCn: '封杀出局 (FO)', runner, from: 1, to: 2, chain: ['6', '4', '3'], outsCum: 1, total: 2 }),
    'Force out (FO): 1B runner Sam (#21) out at 2B (6-4-3); 1 out this play, 2 total'
  );
  assert.equal(
    caughtStealingText({ fielderPos: '捕手', runner, from: 2, to: 3 }),
    'Caught stealing (CS Catcher): 2B runner Sam (#21) from 2B to 3B'
  );
  assert.match(outText({ reasonCn: '封杀出局', runner, from: 1, to: 2, chain: ['1', '3'] }), /^Force out: 1B runner Sam/);
  assert.match(outText({ reasonCn: '触杀出局', runner, from: 1, to: 2, fielderCode: '2B', fielder: player }), /^Tag out: 1B runner Sam/);
});

test('英文投球/换人/系统/纠偏行', () => {
  setLogTextLocale('en');
  assert.equal(pitchText({ who: 'Alex', kind: 'ball', count: '2-1' }), 'Alex: Ball 2-1');
  assert.equal(pitchText({ who: 'Alex', kind: 'balk_ball', count: '1-0' }), 'Alex: balk (ball 1-0)');
  assert.equal(coachTimeoutText('Home'), 'Coach timeout: Home');
  assert.equal(subText({ kind: 'hit', inName: 'B', outName: 'A' }), 'Substitution: pinch hitter B replaces A');
  assert.equal(setRunnerText({ name: 'Sam', baseCn: '二垒' }), 'Set runner: Sam -> 2B');
  assert.equal(pickoffNoneText({ pitcher: player, base: 1 }), 'Pitcher Alex (#7): pickoff at 1B (no advance/out)');
  assert.equal(foulErrorText({ chainText: '6-3' }), 'Foul ball + error: 6-3');
  assert.equal(sysText({ kind: 'game_start' }), 'Game started');
  assert.equal(sysText({ kind: 'inning_header', inning: 3, isTop: false }), '--- Inning 3 Bottom ---');
  assert.equal(errorAdvanceText({ reasonCn: '传球失误', runnerName: 'Sam', to: '本垒得分' }), 'Throwing error, Sam advances to Home');
  assert.equal(stealSingleText({ name: 'Sam', home: true }), 'Sam: stole home');
  assert.equal(fpMissText({ runner, verdict: '判进' }), "Fielder's choice advance · Sam (#21) called strike — no run");
  assert.equal(fpMissText({ runner, verdict: '封杀出局 (FO)' }), "Fielder's choice advance · Sam (#21) Force out (FO) — no run");
  assert.equal(advanceText({ reasonCn: '守备失误', runner, from: 1, to: 2 }), 'Fielding error: 1B runner Sam (#21) advances to 2B — safe');
});

test('英文多行组：野选与牵制', () => {
  setLogTextLocale('en');
  const fc = fcGroup({
    batter: player, ballDesc: '击出滚地球到游击（6）', chain: ['6', '4', '3'], chainPlayers: [player, player, player],
    outRunner: runner, outFrom: 1, outBase: 2, reasonCn: '野选推进', batterTo: 1, outsCum: 1, total: 1
  });
  assert.equal(fc.lines[0], 'Alex: Hit ground ball to Shortstop (6)');
  const fcSide = fcGroup({
    batter: player, ballDesc: '击出滚地球到游击左侧（6）', chain: ['6'], chainPlayers: [player],
    outRunner: runner, outFrom: 1, outBase: 2, reasonCn: '野选推进', outsCum: 1, total: 1
  });
  assert.equal(fcSide.lines[0], 'Alex: Hit ground ball to Shortstop left side (6)');
  const fcLower = fcGroup({
    batter: player, ballDesc: '击出滚地球到游击右下侧（6）', chain: ['6'], chainPlayers: [player],
    outRunner: runner, outFrom: 1, outBase: 2, reasonCn: '野选推进', outsCum: 1, total: 1
  });
  assert.equal(fcLower.lines[0], 'Alex: Hit ground ball to Shortstop lower-right (6)');
  assert.ok(fc.lines.at(-2).includes('forced out at 2B'));
  assert.ok(fc.lines.at(-1).includes('reaches 1B'));

  const pickoff = pickoffGroup({ pitcher: player, runner, from: 1, fielderCode: '1B', fielder: player, outsCum: 1, total: 1 });
  assert.ok(pickoff.lines[0].includes('pickoff at 1B'));
  assert.ok(pickoff.lines[1].includes('catch and tag (PO)'));
});

test('英文注入注记：留垒/失误/守备局数', () => {
  setLogTextLocale('en');
  assert.equal(staysOnBaseText('Sam (#21)'), 'Runner Sam (#21) stays on base');
  assert.equal(errorNoteText('机 (6)', false), ' (机 (6) credited E)');
  assert.equal(errorNoteText('A, B', true), ' (A, B each credited E)');
  assert.equal(fieldingIpNoteText('Sam (#21)', 3), ' (Sam (#21): 3 fielding innings settled for this game)');
});

test('英文安打与本垒跑垒确认日志', () => {
  setLogTextLocale('en');
  assert.equal(hitResultText('H2'), 'Double');
  assert.equal(hitResultText('HR', 3), 'Home run 3 runs');
  assert.equal(noAdvanceRunText('Sam (#21)'), 'No advance · Sam (#21) — returns to 3B');
  assert.equal(noAdvanceThirdText('Sam (#21)'), 'No advance · Sam (#21) returns to 3B');
  assert.equal(runOutNoScoreText('安打', 'Sam (#21)', '封杀出局 (FO)'), 'Hit · Sam (#21) Force out (FO) — no run');
});

test('英文双杀/三杀主行与跑者行', () => {
  setLogTextLocale('en');
  assert.equal(
    dpMainText({ batterName: 'Alex', type: 'DP', chainText: '6-4-3', batterOut: true, batterBase: 1, batterBaseText: '一垒', fielderLabel: 'Sam', gdp: true }),
    'Alex: Double play (6-4-3) (out at 1B on force by Sam, GDP +1)'
  );
  assert.equal(
    dpRunnerOutText({ runner, base: 2, baseText: '二垒', fielderLabel: 'Sam ', action: 'force', chainText: '6-4-3' }),
    'Sam (#21) out at 2B by Sam (force) (6-4-3)'
  );
});

test('切回中文后输出恢复', () => {
  setLogTextLocale('zh-CN');
  assert.equal(runnerLabel(1, runner), '一垒跑者 Sam (#21)');
  assert.equal(pitchText({ who: 'Alex', kind: 'ball' }), 'Alex: 坏球');
  assert.equal(hitResultText('HR', 3), '全垒打 3分');
  assert.equal(hitResultText('IPHR'), '场内全垒打');
  assert.equal(noAdvanceRunText('Sam (#21)'), '不进垒 · Sam (#21) — 退回三垒');
  assert.equal(noAdvanceThirdText('Sam (#21)'), '不进垒 · Sam (#21) 退回三垒');
  assert.equal(runOutNoScoreText('安打', 'Sam (#21)', '封杀出局 (FO)'), '安打 · Sam (#21) 封杀出局 (FO) — 未得分');
});
