// #449：手动进垒得分在保存/换局前补成 playEvent，避免 ER 引擎漏分
// 运行：在 bsball-frontend 目录执行 node --test tests/liveGameManualRun.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { nextPlaySequence, pendingRunsPlayEvent, toScoringRunner } from '../src/utils/liveGameManualRun.js';
import { toEarnedRunPlay } from '../src/domain/earnedRun/livePlayEvent.js';

const liveGame = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('nextPlaySequence：取已有事件最大序 +1，兼容字符串/缺失/空列表', () => {
  assert.equal(nextPlaySequence([]), 1);
  assert.equal(nextPlaySequence(undefined), 1);
  assert.equal(nextPlaySequence([{ sequence: 3 }, { sequence: '7' }, { sequence: null }, {}]), 8);
  assert.equal(nextPlaySequence([{ sequence: 1 }, { sequence: 2 }]), 3);
});

test('pendingRunsPlayEvent：保留来源与责任投手，0 出局、独立成 playEvent', () => {
  const event = pendingRunsPlayEvent([
    {
      runs: 1,
      runner: { id: 88, name: '跑者甲' },
      pitcher: { id: 26, name: '投手甲' },
      responsiblePitcherId: 26,
      origin: 'WILD_PITCH',
      runCause: 'WILD_PITCH',
      rbiCredit: false
    },
    {
      runs: 1,
      runner: { id: 99, name: '跑者乙' },
      pitcher: { id: 26, name: '投手甲' },
      responsiblePitcherId: 30,
      origin: 'ERROR',
      runCause: 'ERROR',
      rbiCredit: false
    }
  ], {
    playEvents: [{ sequence: 1 }, { sequence: 2 }, { sequence: 5 }],
    inning: 4,
    half: 'bottom',
    batter: { id: 7, name: '打者' },
    pitcher: { id: 26, name: '投手甲' },
    outs: 2,
    runners: { 1: { id: 11 }, 2: null, 3: null }
  });

  assert.equal(event.sequence, 6);
  assert.equal(event.inning, 4);
  assert.equal(event.half, 'bottom');
  assert.equal(event.actualOutsAdded, 0);
  assert.equal(event.outsBefore, 2);
  assert.equal(event.outsAfter, 2);
  assert.equal(event.resultCode, 'MANUAL_RUN');
  assert.equal(event.manual, true);
  assert.deepEqual(event.scoringRunnerIds, [88, 99]);
  assert.deepEqual(event.scoreDelta, { away: 0, home: 2 });
  assert.equal(event.scoringRunners[0].responsiblePitcherId, 26);
  assert.equal(event.scoringRunners[0].origin, 'WILD_PITCH');
  assert.equal(event.scoringRunners[1].responsiblePitcherId, 30);
  assert.equal(event.scoringRunners[1].origin, 'ERROR');
});

test('pendingRunsPlayEvent：连续补记时序列递增，不撞后续 Qr 的序', () => {
  const playEvents = [{ sequence: 1 }, { sequence: 2 }];
  const first = pendingRunsPlayEvent([{ runs: 1, runner: { id: 10 }, responsiblePitcherId: 5, origin: 'NORMAL' }], {
    playEvents,
    inning: 1,
    half: 'top'
  });
  playEvents.push(first);

  const second = pendingRunsPlayEvent([{ runs: 1, runner: { id: 11 }, responsiblePitcherId: 5, origin: 'NORMAL' }], {
    playEvents,
    inning: 1,
    half: 'top'
  });
  playEvents.push(second);

  const qr = { inning: 1, half: 'top', sequence: nextPlaySequence(playEvents), actualOutsAdded: 1 };

  assert.deepEqual(playEvents.map((event) => event.sequence), [1, 2, 3, 4]);
  assert.equal(qr.sequence, 5, '后续 Qr 取补记后的最大序 +1，不撞序');
  const sequences = [...playEvents, qr].map((event) => toEarnedRunPlay(event).sequence);
  assert.deepEqual(sequences, [1, 2, 3, 4, 5], '同一半局序列严格递增');
});

test('pendingRunsPlayEvent：无可入账得分时返回 null（调用方保留台账）', () => {
  assert.equal(pendingRunsPlayEvent([], {}), null);
  assert.equal(pendingRunsPlayEvent([{ runs: 0, runner: { id: 1 } }], {}), null);
  assert.equal(pendingRunsPlayEvent([{ runs: 1 }], {}), null, '缺跑者 id 不入引擎');
});

test('pendingRunsPlayEvent：快照恢复后 runner 未绑定仍可用 runnerId 入账', () => {
  const event = pendingRunsPlayEvent([{
    runs: 1,
    runner: null,
    runnerId: 77,
    responsiblePitcherId: 26,
    origin: 'ERROR'
  }], {
    playEvents: [],
    inning: 3,
    half: 'top'
  });

  assert.equal(event.scoringRunners.length, 1);
  assert.equal(event.scoringRunners[0].runnerId, 77);
  assert.equal(event.scoringRunners[0].responsiblePitcherId, 26);
});

test('补记事件可被 ER 载荷识别，责任投手与来源不丢', () => {
  const event = pendingRunsPlayEvent([{
    runs: 1,
    runner: { id: 66 },
    responsiblePitcherId: 26,
    origin: 'PASSED_BALL',
    wouldScoreWithoutError: true
  }], {
    playEvents: [{ sequence: 9 }],
    inning: 2,
    half: 'top'
  });
  const play = toEarnedRunPlay(event);

  assert.equal(play.sequence, 10);
  assert.ok(Number.isInteger(play.playId) && play.playId > 0);
  assert.equal(play.actualOutsAdded, 0);
  assert.equal(play.scoringRunners.length, 1);
  assert.equal(play.scoringRunners[0].runnerId, 66);
  assert.equal(play.scoringRunners[0].responsiblePitcherId, 26);
  assert.equal(play.scoringRunners[0].origin, 'PASSED_BALL');
});

test('toScoringRunner：打席落定与补记共用映射，责任投手回退当前投手', () => {
  assert.equal(toScoringRunner({ runnerId: 0 }), null);
  assert.equal(toScoringRunner({ runs: 1 }), null, '无跑者 id 不入引擎');
  assert.deepEqual(
    toScoringRunner({ runnerId: '88', origin: 'ERROR' }, 26),
    {
      runnerId: 88,
      responsiblePitcherId: 26,
      origin: 'ERROR',
      overrideStatus: null,
      overrideReason: null,
      wouldScoreWithoutMisplay: null,
      runCause: null,
      wouldScoreWithoutError: null,
      rbiCredit: null,
      rbiReason: null
    }
  );
  const bound = toScoringRunner({
    runner: { id: 99 },
    pitcher: { id: 30 },
    runCause: 'WILD_PITCH',
    rbiCredit: true
  });
  assert.equal(bound.runnerId, 99);
  assert.equal(bound.responsiblePitcherId, 30);
  assert.equal(bound.runCause, 'WILD_PITCH');
  assert.equal(bound.rbiCredit, true);
});

test('LiveGame 静态接线：Qr 改用统一序列；保存与强制换局前补记台账', () => {
  assert.match(liveGame, /import\s*\{\s*nextPlaySequence,\s*pendingRunsPlayEvent,\s*toScoringRunner\s*\}\s*from\s*'\.\.\/\.\.\/utils\/liveGameManualRun'/,
    '必须引入补记与统一跑者映射工具');
  assert.match(liveGame, /sequence:nextPlaySequence\(f\.playEvents\)/, 'Qr 事件序列取全局最大序 +1');
  assert.doesNotMatch(liveGame, /sequence:Me\.value\.length\+1/, '旧序列口径不得残留');

  const flush = liveGame.indexOf('function flushPendingRunsToPlayEvent(){');
  assert.ok(flush >= 0, '补记函数存在');
  const flushBody = liveGame.slice(flush, liveGame.indexOf('const es=new WeakMap', flush));
  assert.match(flushBody, /pendingRunsPlayEvent\(runLedger\.entries/, '使用台账组装事件');
  assert.match(flushBody, /takeAllRuns\(runLedger\),f\.playEvents\.push\(event\),syncEarnedRunHalfInning\(event\)/,
    '清台账、入 playEvents、同步重建');

  const save = liveGame.slice(liveGame.indexOf('async function hsRun(e){'), liveGame.indexOf('async function gc(){'));
  assert.match(save, /flushPendingRunsToPlayEvent\(\)\|\|resyncEarnedRunHalfInningForSave\(\),await earnedRunSyncPending/,
    '保存前补记台账；无补记时也按当前半局对账（覆盖撤销后的后端判定）');

  const force = liveGame.slice(liveGame.indexOf('async function forceHalf(){'), liveGame.indexOf('function Qa(e){'));
  assert.match(force, /flushPendingRunsToPlayEvent\(\),clearRunLedger\(\)/, '强制换局先补记再清台账');

  assert.match(liveGame, /function syncEarnedRunHalfInning\(event,options=\{\}\)/, '同步函数支持 allowEmpty');
  assert.match(liveGame, /if\(!plays\.length&&!options\.allowEmpty\)return/, '仅显式对账时允许空半局');
  assert.match(liveGame, /if\(!earnedRunPlayEventsTrusted\|\|!F\.value\|\|!R\.value\)return null/,
    '只有 playEvents 完整可信时才做保存对账，Do 重建路径跳过');
});

test('LiveGame 静态接线：现场快照保存并恢复 playEvents 与未落定台账', () => {
  assert.match(liveGame, /playEvents:JSON\.parse\(JSON\.stringify\(f\.playEvents\)\),playEventsComplete:earnedRunPlayEventsTrusted,runLedger:snapshotRunLedger\(\)/,
    '现场快照必须带上 playEvents、完整性标记与 runLedger');
  assert.match(liveGame, /playEventsComplete:earnedRunPlayEventsTrusted,runLedger:snapshotRunLedger\(\)/,
    '撤销/重做快照同样携带完整性标记');
  assert.match(liveGame, /f\.playEvents=Array\.isArray\(e\.playEvents\)\?JSON\.parse\(JSON\.stringify\(e\.playEvents\)\):\[\]/,
    '恢复现场必须还原 playEvents，否则首打席重建会抹掉本半局既有判定');
  assert.match(liveGame, /restoreRunLedger\(e\.runLedger\)/, '恢复现场必须还原台账');
  assert.match(liveGame, /typeof e\.playEventsComplete==="boolean"&&\(earnedRunPlayEventsTrusted=e\.playEventsComplete\)/,
    '完整性只认快照显式标记，不得按 playEvents 是否数组推断');
  assert.match(liveGame, /earnedRunPlayEventsTrusted=e\.playEventsComplete===true/,
    '撤销/重做恢复完整性标记');
  assert.match(liveGame, /earnedRunPlayEventsTrusted=a\.engineManaged!==true/, '按比赛引擎托管标记可信度');
  assert.match(liveGame, /function Do\(e,t,a,s,r,p\)\{[\s\S]*?f\.playEvents=\[\],clearRunLedger\(\)/,
    'Do 仅按统计重建时必须清空不完整的 playEvents 与台账');
  assert.match(liveGame, /runnerId:Number\(t\.runner\?\.id\?\?t\.runnerId\)/,
    '台账快照保留未绑定跑者的 runnerId');
});
