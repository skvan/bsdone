// 前端单测：ER 重建载荷映射（#149 第一阶段契约桥接，#203）
// 运行：在 bsball-frontend 目录执行  node --test tests/
import test from 'node:test';
import assert from 'node:assert/strict';
import {
  toEarnedRunPlay,
  createLivePlayEvent
} from '../src/domain/earnedRun/livePlayEvent.js';

test('裸 playEvent 转换为后端契约载荷：缺省字段推导且不出现 null 原始类型', () => {
  const raw = {
    id: 'ab-4-bottom-7-1790990281362-1yj2fv5',
    inning: 4,
    half: 'bottom',
    sequence: 52,
    batterPlayerId: 115,
    pitcherPlayerId: 26,
    resultCode: 'E',
    outsBefore: 2,
    outsAfter: 2,
    scoringRunners: [
      { runnerId: 115, responsiblePitcherId: 26, origin: null, overrideStatus: null, overrideReason: null }
    ]
  };

  const play = toEarnedRunPlay(raw);
  const wire = JSON.parse(JSON.stringify(play));

  assert.equal(wire.actualOutsAdded, 0);
  assert.equal(wire.reconstructedOutsAdded, 0);
  assert.equal(wire.rulingPending, false);
  assert.ok(!Number.isNaN(wire.actualOutsAdded) && wire.actualOutsAdded !== null);
  assert.ok(!Number.isNaN(wire.reconstructedOutsAdded) && wire.reconstructedOutsAdded !== null);
  assert.equal(typeof wire.rulingPending, 'boolean');
  assert.equal(wire.sequence, 52);
  assert.ok(Number.isInteger(wire.playId) && wire.playId > 0);
  assert.equal(wire.scoringRunners.length, 1);
  assert.equal(wire.scoringRunners[0].origin, 'ERROR');
});

test('出局增量由 outsAfter-outsBefore 推导并钳制在 0~3（含比赛 226 的负差样本）', () => {
  assert.equal(toEarnedRunPlay({ inning: 1, half: 'top', sequence: 1, outsBefore: 0, outsAfter: 3 }).actualOutsAdded, 3);
  assert.equal(toEarnedRunPlay({ inning: 1, half: 'top', sequence: 2, outsBefore: 1, outsAfter: 1 }).actualOutsAdded, 0);
  assert.equal(toEarnedRunPlay({ inning: 1, half: 'bottom', sequence: 1, outsBefore: 2, outsAfter: 1 }).actualOutsAdded, 0);
  assert.equal(toEarnedRunPlay({ inning: 1, half: 'top', sequence: 3 }).actualOutsAdded, 0);
});

test('playId 稳定且同半局重放一致；不同局次/攻守不撞号', () => {
  const a = toEarnedRunPlay({ inning: 4, half: 'bottom', sequence: 52 });
  const b = toEarnedRunPlay({ inning: 4, half: 'bottom', sequence: 52 });
  const c = toEarnedRunPlay({ inning: 4, half: 'top', sequence: 52 });
  const d = toEarnedRunPlay({ inning: 5, half: 'bottom', sequence: 52 });

  assert.equal(a.playId, b.playId);
  assert.notEqual(a.playId, c.playId);
  assert.notEqual(a.playId, d.playId);
  assert.notEqual(c.playId, d.playId);
});

test('显式正数 playId 原样保留（兼容富事件）', () => {
  assert.equal(toEarnedRunPlay({ playId: 123, inning: 1, half: 'top', sequence: 1 }).playId, 123);
});

test('scoringRunners 归一化：责任投手回退、无效条目过滤、来源推断与覆核态归一', () => {
  const play = toEarnedRunPlay({
    inning: 1,
    half: 'top',
    sequence: 3,
    pitcherPlayerId: 77,
    resultCode: 'H1',
    scoringRunners: [
      { runnerId: 55, responsiblePitcherId: null, origin: null },
      { runnerId: null, responsiblePitcherId: 77 },
      { runnerId: 56, responsiblePitcherId: 88, origin: 'PASSED_BALL' },
      { runnerId: 57, responsiblePitcherId: 88, overrideStatus: 'UNKNOWN' }
    ]
  });

  assert.equal(play.scoringRunners.length, 3);
  assert.deepEqual(play.scoringRunners[0], {
    runnerId: 55,
    responsiblePitcherId: 77,
    origin: 'NORMAL',
    overrideStatus: null,
    overrideReason: null
  });
  assert.equal(play.scoringRunners[1].origin, 'PASSED_BALL');
  assert.equal(play.scoringRunners[2].overrideStatus, 'PENDING');
});

test('与 createLivePlayEvent 输出兼容（富事件路径保留既有值）', () => {
  const rich = createLivePlayEvent({
    playId: 'play-x',
    inning: 2,
    half: 'top',
    sequence: 7,
    pitcherId: 77,
    actualOutsAdded: 1,
    scoringRunners: [{ runnerId: 55, responsiblePitcherId: 77, origin: 'ERROR' }]
  });

  const play = toEarnedRunPlay(rich);
  assert.equal(play.actualOutsAdded, 1);
  assert.equal(play.reconstructedOutsAdded, 1);
  assert.ok(Number.isInteger(play.playId) && play.playId > 0);
  assert.equal(play.scoringRunners[0].origin, 'ERROR');
});

test('空/缺字段事件不抛错并给出安全缺省', () => {
  const play = toEarnedRunPlay({});
  assert.equal(play.sequence, 1);
  assert.equal(play.actualOutsAdded, 0);
  assert.equal(play.reconstructedOutsAdded, 0);
  assert.equal(play.rulingPending, false);
  assert.deepEqual(play.scoringRunners, []);
  assert.ok(play.playId > 0);
});
