// #712：重进信任校验的纯函数（playId ↔ 云端 sourceEventId 覆盖关系）
// 运行：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"
import test from 'node:test';
import assert from 'node:assert/strict';
import { localPlayKey, findMissingPlayKeys } from '../src/domain/earnedRun/snapshotKeyCoverage.js';

test('localPlayKey：与后端 sourceEventId 使用同一套 inning/half/sequence 编号', () => {
  assert.equal(localPlayKey({ inning: 3, half: 'top', sequence: 2 }), '3000002');
  assert.equal(localPlayKey({ inning: 3, half: 'bottom', sequence: 1 }), '3500001');
});

test('localPlayKey：缺字段时按 toEarnedRunPlay 的默认值编号，null 事件返回 null', () => {
  assert.equal(localPlayKey({}), '1000001');
  assert.equal(localPlayKey(null), null);
});

test('findMissingPlayKeys：本地覆盖全部云端 key 时没有缺口', () => {
  const serverKeys = [
    { inning: 3, half: 'top', sequence: 2, sourceEventId: '3000002' },
    { inning: 3, half: 'bottom', sequence: 1, sourceEventId: '3500001' }
  ];
  const localEvents = [
    { inning: 3, half: 'top', sequence: 2 },
    { inning: 3, half: 'bottom', sequence: 1 }
  ];

  const { missing, byHalf } = findMissingPlayKeys(serverKeys, localEvents);

  assert.deepEqual(missing, []);
  assert.deepEqual(byHalf, []);
});

test('findMissingPlayKeys：列出缺口并按半局汇总条数', () => {
  const serverKeys = [
    { inning: 3, half: 'top', sequence: 2, sourceEventId: '3000002' },
    { inning: 3, half: 'top', sequence: 1, sourceEventId: '3000001' },
    { inning: 3, half: 'bottom', sequence: 1, sourceEventId: '3500001' }
  ];
  const localEvents = [{ inning: 3, half: 'top', sequence: 2 }];

  const { missing, byHalf } = findMissingPlayKeys(serverKeys, localEvents);

  assert.deepEqual(missing.map((key) => key.sourceEventId), ['3000001', '3500001']);
  assert.deepEqual(byHalf, [
    { inning: 3, half: 'top', count: 1 },
    { inning: 3, half: 'bottom', count: 1 }
  ]);
});

test('findMissingPlayKeys：空 sourceEventId 的云端行不参与校验', () => {
  const serverKeys = [
    { inning: 3, half: 'top', sequence: 1, sourceEventId: null },
    { inning: 3, half: 'top', sequence: 2, sourceEventId: '' },
    { inning: 3, half: 'top', sequence: 3, sourceEventId: '3000003' }
  ];

  const { missing } = findMissingPlayKeys(serverKeys, []);

  assert.deepEqual(missing.map((key) => key.sourceEventId), ['3000003']);
});

test('findMissingPlayKeys：非法输入按空集合处理', () => {
  assert.deepEqual(findMissingPlayKeys(null, null), { missing: [], byHalf: [] });
  assert.deepEqual(findMissingPlayKeys(undefined, [{ inning: 1, half: 'top', sequence: 1 }]),
    { missing: [], byHalf: [] });
});
