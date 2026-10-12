import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  decideRbiForRun,
  isDoublePlayThrowingErrorException
} from '../src/domain/liveGameRbi.js';

const liveGame = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('正常防守仍可得分時，失誤來源的跑者可記 RBI', () => {
  assert.equal(decideRbiForRun({
    runCause: 'ERROR',
    wouldScoreWithoutError: true
  }), true);
});

test('失誤造成得分但正常防守不會得分時不記 RBI', () => {
  assert.equal(decideRbiForRun({
    runCause: 'ERROR',
    wouldScoreWithoutError: false
  }), false);
});

test('失誤得分尚未覆核時預設不記 RBI', () => {
  assert.equal(decideRbiForRun({ runCause: 'ERROR' }), false);
});

test('記分員明確裁量可覆蓋預設判定', () => {
  assert.equal(decideRbiForRun({ runCause: 'ERROR', manualRbi: true }), true);
  assert.equal(decideRbiForRun({ runCause: 'HIT', manualRbi: false }), false);
});

test('非失誤造成的正常打席得分依一般原則給 RBI', () => {
  assert.equal(decideRbiForRun({ runCause: 'HIT' }), true);
  assert.equal(decideRbiForRun({ runCause: 'SACRIFICE' }), true);
});

test('雙殺機會的一出局傳球失誤符合不記 E 特例', () => {
  assert.equal(isDoublePlayThrowingErrorException({
    firstBaseOccupied: true,
    outsBefore: 1,
    outsAdded: 1,
    throwingError: true,
    doublePlayCompleted: false
  }), true);
});

test('雙殺特例不適用無一壘跑者、兩出局、無出局或已完成雙殺', () => {
  const valid = {
    firstBaseOccupied: true,
    outsBefore: 0,
    outsAdded: 1,
    throwingError: true,
    doublePlayCompleted: false
  };
  assert.equal(isDoublePlayThrowingErrorException({ ...valid, firstBaseOccupied: false }), false);
  assert.equal(isDoublePlayThrowingErrorException({ ...valid, outsBefore: 2 }), false);
  assert.equal(isDoublePlayThrowingErrorException({ ...valid, outsAdded: 0 }), false);
  assert.equal(isDoublePlayThrowingErrorException({ ...valid, doublePlayCompleted: true }), false);
});

test('LiveGame 要求記分員判定錯誤得分，並將 RBI 判定隨得分跑者保存', () => {
  assert.match(liveGame, /失誤得分 RBI 判定/);
  assert.match(liveGame, /if\(e==="E"&&l\.runners\[3\]&&rbiOverride==null\)/);
  assert.match(liveGame, /async function fcApply/);
  assert.match(liveGame, /const runDecision=l\.runners\[3\]\?await askErrorRunRbiDecision/);
  assert.match(liveGame, /askErrorRunRbiDecision\(l\.runners\[3\]\)/);
  assert.match(liveGame, /function Ql\(e,r=!0,runDecision=null\)/);
  assert.match(liveGame, /wouldScoreWithoutError:t\.wouldScoreWithoutError\?\?null,rbiCredit:typeof t\.rbiCredit/);
  assert.match(liveGame, /runCause:t\.runCause\?\?null/);
});

test('雙殺傳球失誤特例記一出局、取消 E 並將結果保存為 FC', () => {
  assert.match(liveGame, /isDoublePlayThrowingErrorException\(/);
  assert.match(liveGame, /Bt\(Gt\(r\[1\],"雙殺機會先取得一出局/);
  assert.match(liveGame, /applyChainFieldingStats\("FC",e\.fielders\),m="FC"/);
  assert.match(liveGame, /doublePlayThrowingErrorException:!0/);
});

test('SF/SH 得分依正常防守裁量 RBI，並將裁量附加至得分事件', () => {
  assert.match(liveGame, /牺牲打得分 RBI 判定/);
  assert.match(liveGame, /wouldScoreWithoutError:sacrificeRunDecision\.wouldScoreWithoutError/);
  assert.match(liveGame, /bo\(r,sacrificeRunDecision\)/);
  assert.match(liveGame, /wouldScoreWithoutError:e\?\.wouldScoreWithoutError\?\?null/);
});
