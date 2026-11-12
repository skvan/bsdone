// 赛前阵容提交（球队端）工具：构建/解析/数据判断/快照侧数据/提交时间
// 运行：node --test tests/teamLineupDraft.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildTeamLineupDraft,
  parseTeamLineupDraft,
  hasTeamLineupData,
  toBoardDraft,
  snapshotSideHasData,
  submissionIsNewer,
  formatLineupDateTime
} from '../src/utils/teamLineupDraft.js';

test('buildTeamLineupDraft：v:1 单队结构，字段透传并补默认值', () => {
  const draft = buildTeamLineupDraft({
    gameId: 900,
    teamId: 100,
    gameMode: 'BASEBALL',
    complete: true,
    lineup: [{ id: 5 }],
    bench: [{ id: 6 }],
    fieldingPitcherId: 7
  });
  assert.equal(draft.v, 1);
  assert.equal(draft.gameId, 900);
  assert.equal(draft.teamId, 100);
  assert.equal(draft.gameMode, 'BASEBALL');
  assert.equal(draft.complete, true);
  assert.deepEqual(draft.lineup, [{ id: 5 }]);
  assert.deepEqual(draft.bench, [{ id: 6 }]);
  assert.equal(draft.fieldingPitcherId, 7);
  assert.equal(draft.lastDhAddedFromPoolId, null);
});

test('parseTeamLineupDraft：非 v:1 / 非法 JSON / 空串一律 null', () => {
  assert.equal(parseTeamLineupDraft(''), null);
  assert.equal(parseTeamLineupDraft('   '), null);
  assert.equal(parseTeamLineupDraft(null), null);
  assert.equal(parseTeamLineupDraft('{bad json'), null);
  assert.equal(parseTeamLineupDraft('{"v":2}'), null);
  assert.equal(parseTeamLineupDraft('[1,2]'), null);
  const round = parseTeamLineupDraft(JSON.stringify(buildTeamLineupDraft({ gameId: 1, teamId: 2 })));
  assert.equal(round.v, 1);
  assert.equal(round.gameId, 1);
});

test('hasTeamLineupData：占位行/空替补/无投手不算数据', () => {
  assert.equal(hasTeamLineupData(null), false);
  assert.equal(hasTeamLineupData({ lineup: [{ id: -1 }], bench: [], fieldingPitcherId: null }), false);
  assert.equal(hasTeamLineupData({ lineup: [{ id: 5 }] }), true);
  assert.equal(hasTeamLineupData({ lineup: [], bench: [{ id: 6 }] }), true);
  assert.equal(hasTeamLineupData({ lineup: [], bench: [], fieldingPitcherId: 7 }), true);
});

test('toBoardDraft：只取阵容板需要的四个字段', () => {
  const source = {
    v: 1,
    gameId: 900,
    lineup: [{ id: 5 }],
    bench: [{ id: 6 }],
    fieldingPitcherId: 7,
    lastDhAddedFromPoolId: 8,
    extra: 'ignored'
  };
  assert.deepEqual(toBoardDraft(source), {
    rows: [{ id: 5 }],
    bench: [{ id: 6 }],
    fieldingPitcherId: 7,
    lastDhAddedFromPoolId: 8
  });
  assert.equal(toBoardDraft(null), null);
});

test('snapshotSideHasData：按侧识别实际阵容，占位行不算', () => {
  const snapshot = {
    homeLineup: [{ id: -1 }, { id: 5 }],
    homeBench: [],
    homeFieldingPitcherId: null,
    awayLineup: [{ id: -1 }],
    awayBench: [],
    awayFieldingPitcherId: null
  };
  assert.equal(snapshotSideHasData(snapshot, 'home'), true);
  assert.equal(snapshotSideHasData(snapshot, 'away'), false);
  assert.equal(snapshotSideHasData(null, 'home'), false);
  assert.equal(snapshotSideHasData(snapshot, 'other'), false);
});

test('submissionIsNewer：无提交时间不提示；无已载入时间视为更新；按 ISO 时间比较', () => {
  assert.equal(submissionIsNewer(null, null), false);
  assert.equal(submissionIsNewer('2026-11-11T18:20:00', null), true);
  assert.equal(submissionIsNewer('2026-11-11T18:20:00', '2026-11-11T18:20:00'), false);
  assert.equal(submissionIsNewer('2026-11-11T18:20:01', '2026-11-11T18:20:00'), true);
  assert.equal(submissionIsNewer('bad-date', '2026-11-11T18:20:00'), true);
});

test('formatLineupDateTime：截到分钟，空值用占位文案', () => {
  assert.equal(formatLineupDateTime('2026-11-11T18:20:00'), '2026-11-11 18:20');
  assert.equal(formatLineupDateTime('2026-11-11 18:20:00'), '2026-11-11 18:20');
  assert.equal(formatLineupDateTime(null), '时间待定');
  assert.equal(formatLineupDateTime('   '), '时间待定');
  assert.equal(formatLineupDateTime(null, '时间未知'), '时间未知');
});
