import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  createRunLedger,
  beginPlay,
  addRun,
  voidCurrentPlayRuns,
  takeAllRuns,
  clearRuns,
  advanceOrigin,
  runOrigin
} from '../src/utils/liveGameRunLedger.js';

const liveGame = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('新的打席动作开始时不清理同打席内已得的分数', () => {
  const ledger = createRunLedger();
  addRun(ledger, { runs: 1, runner: { id: 11 } });
  beginPlay(ledger);
  beginPlay(ledger);

  assert.equal(ledger.entries.length, 1);
  assert.equal(ledger.entries[0].runs, 1);
});

test('第三出局只撤销当前动作内新增的得分，保留先前动作的得分', () => {
  const ledger = createRunLedger();
  addRun(ledger, { runs: 1, runner: { id: 11 } });
  beginPlay(ledger);
  addRun(ledger, { runs: 2, runner: { id: 12 } });
  addRun(ledger, { runs: 1, runner: { id: 13 } });

  const voided = voidCurrentPlayRuns(ledger);

  assert.deepEqual(voided.map((entry) => entry.runs), [2, 1]);
  assert.deepEqual(ledger.entries.map((entry) => entry.runner.id), [11]);
  assert.equal(ledger.boundary, 1);
});

test('撤销后再次标记边界，后续得分仍可单独撤销', () => {
  const ledger = createRunLedger();
  beginPlay(ledger);
  addRun(ledger, { runs: 1 });
  assert.equal(voidCurrentPlayRuns(ledger).length, 1);

  beginPlay(ledger);
  addRun(ledger, { runs: 1 });
  assert.equal(voidCurrentPlayRuns(ledger).length, 1);
  assert.equal(ledger.entries.length, 0);
});

test('打席落定时取走全部得分并清空台账', () => {
  const ledger = createRunLedger();
  addRun(ledger, { runs: 1 });
  beginPlay(ledger);
  addRun(ledger, { runs: 1 });

  assert.deepEqual(takeAllRuns(ledger).map((entry) => entry.runs), [1, 1]);
  assert.equal(ledger.entries.length, 0);
  assert.equal(ledger.boundary, 0);
});

test('清空台账后边界归零，不会误伤新的打席', () => {
  const ledger = createRunLedger();
  beginPlay(ledger);
  addRun(ledger, { runs: 1 });
  clearRuns(ledger);
  beginPlay(ledger);
  addRun(ledger, { runs: 1 });

  assert.equal(voidCurrentPlayRuns(ledger).length, 1);
  assert.equal(ledger.entries.length, 0);
});

test('推进原因映射为引擎可识别的得分来源', () => {
  assert.equal(advanceOrigin('safe_wp'), 'WILD_PITCH');
  assert.equal(advanceOrigin('safe_pb'), 'PASSED_BALL');
  assert.equal(advanceOrigin('safe_pk_err'), 'ERROR');
  assert.equal(advanceOrigin('safe_diff_err'), 'ERROR');
  assert.equal(advanceOrigin('safe_sb'), 'NORMAL');
  assert.equal(advanceOrigin('safe_di'), 'NORMAL');
  assert.equal(advanceOrigin('safe_last_play'), 'NORMAL');
});

test('上垒失误的跑者得分按失误来源记为自责分判定来源', () => {
  assert.equal(runOrigin({ origin: 'ERROR' }, 'NORMAL'), 'ERROR');
  assert.equal(runOrigin({ origin: 'ERROR' }, 'PASSED_BALL'), 'ERROR');
  assert.equal(runOrigin({ origin: 'TIE_BREAK' }, 'NORMAL'), 'TIE_BREAK');
  assert.equal(runOrigin({ origin: 'INTERFERENCE' }, 'NORMAL'), 'INTERFERENCE');
  assert.equal(runOrigin({ origin: 'NORMAL' }, 'PASSED_BALL'), 'PASSED_BALL');
  assert.equal(runOrigin(null, 'ERROR'), 'ERROR');
  assert.equal(runOrigin(null, undefined), 'NORMAL');
});

test('LiveGame 通过统一台账记录得分，不再有旁路加分', () => {
  assert.match(liveGame, /from '\.\.\/\.\.\/utils\/liveGameRunLedger'/);
  assert.ok(!liveGame.includes('Ct(1)'), '所有得分必须走 ls 台账，禁止 Ct(1) 旁路');
  assert.ok(!liveGame.includes('addLivePitcherEarnedRuns'), '投手失分应随台账统一记账并支持撤销');
  assert.ok(!liveGame.includes('Jl()'), '旧的整表清空入口已由边界台账取代');
});

test('LiveGame 的得分与撤销接线到边界台账', () => {
  assert.match(liveGame, /function beginPlayLedger\(\)\{beginPlay\(runLedger\)\}/);
  assert.match(liveGame, /function clearRunLedger\(\)\{clearRuns\(runLedger\)\}/);
  assert.match(liveGame, /addRun\(runLedger,run\)/);
  assert.match(liveGame, /voidCurrentPlayRuns\(runLedger\)/);
  assert.match(liveGame, /takeAllRuns\(runLedger\)/);
  assert.match(liveGame, /const scoringRunners=scoringEntries\.map\(\(entry\)=>toScoringRunner\(entry,w\.pitcherPlayerId\)\)\.filter\(Boolean\)/);
  assert.match(liveGame, /scoringRunnerIds:scoringRunners\.map\(\(runner\)=>runner\.runnerId\),scoringRunners/);
  assert.match(liveGame, /runOrigin\(/);
  assert.match(liveGame, /advanceOrigin\(/);
  assert.ok((liveGame.match(/beginPlayLedger\(\)/g) ?? []).length >= 5,
    '至少每个击球/出局处理器入口都要标记新的动作边界');
});

test('代跑与上垒路径继承或记录责任投手', () => {
  assert.match(liveGame, /origin:prevRunner\?\.origin,responsiblePitcherId:prevRunner\?\.responsiblePitcherId/);
  assert.match(liveGame, /o\.origin=l\.runCause==="ERROR"\?"ERROR":"NORMAL",o\.responsiblePitcherId=Number\(l\.pitcher\?\.id\)/);
  assert.match(liveGame, /l\.responsiblePitcherId=Number\(f\.pitcher\?\.id\)/);
  assert.match(liveGame, /responsiblePitcherId=Number\(Se\.value\?\.id\)>0/);
  assert.match(liveGame, /toScoringRunner\(entry,w\.pitcherPlayerId\)/,
    '打席落定与保存补记共用同一跑者映射，责任投手回退当前投手');
  assert.match(liveGame, /isBatterRun\?t\.pitcher\?\?null:runnerPitcher/,
    '打者本人得分必须取当前投手，不得沿用其上次上垒的陈旧责任投手');
  assert.match(liveGame, /const metaId=isBatterRun\?NaN/);
  const rebound = liveGame.match(/if\(p\)return p\.origin=a\.origin,p\.responsiblePitcherId=a\.responsiblePitcherId,p/g) ?? [];
  assert.equal(rebound.length, 2, '撤销/恢复重绑跑者时必须保留元数据（qn/ns 各一处）');
});

test('捕手妨碍上垒及其被迫进垒得分记为 INTERFERENCE', () => {
  const body = liveGame.slice(liveGame.indexOf('function Zl('), liveGame.indexOf('function ei('));
  assert.match(body, /const reachedOrigin=t\?\.tagFirstBaseRunnerFromCatcherInterference\?"INTERFERENCE":"NORMAL"/,
    'extra:CI 必须区别于普通保送');
  assert.match(body, /a\.origin=reachedOrigin/, '妨碍上垒的打者自身来源必须为 INTERFERENCE');
  assert.match(body, /ti\(\{\s*origin:reachedOrigin\s*\}\s*\)/, '被迫进垒的得分同样继承 INTERFERENCE');
});

test('撤销/重做快照携带击球台账并在恢复时按 id 重绑球员', () => {
  const snapshots = liveGame.match(/runLedger:snapshotRunLedger\(\)/g) ?? [];
  assert.equal(snapshots.length, 5, 'de/as/Ff/ms 四处撤销快照 + 现场快照都要带台账（#449）');
  assert.match(liveGame, /function restoreRunLedger\(snapshot\)\{/);
  assert.match(liveGame, /restoreRunLedger\(e\.runLedger\)/);
  assert.ok(!liveGame.includes('Aa.value=null,clearRunLedger()'), '恢复快照不得无条件清空台账');
  assert.match(liveGame,
    /const pitcher=players\.get\(Number\(t\.pitcherId\)\)\?\?players\.get\(Number\(t\.responsiblePitcherId\)\)\?\?null/,
    '恢复时按 id 重新绑定投手，找不到再退回责任投手');
});

test('强制换局先补记得分再清空台账，避免得分串到下一半局（#449）', () => {
  assert.match(liveGame, /D\(LgSys\(\{kind:"force_half"\}\)\),flushPendingRunsToPlayEvent\(\),clearRunLedger\(\),Qa\(!0\)/);
});

