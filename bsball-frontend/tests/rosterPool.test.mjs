// 前端单测：电子秩序册阵容池过滤（Issue #310 批 3）
// 运行：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"
import test from 'node:test';
import assert from 'node:assert/strict';
import { isInPool, filterOutOfRoster, filterTemplateByPool } from '../src/utils/rosterPool.js';

const pool = (ids) => ({ restricted: true, players: ids.map((id) => ({ id })) });
const full = { restricted: false, players: [] };

test('isInPool：未受限池/空池（null）一律放行；受限池按 id 判定（字符串归一）', () => {
  assert.equal(isInPool(5, full), true);
  assert.equal(isInPool(5, null), true);
  assert.equal(isInPool('5', pool([5, 6])), true);
  assert.equal(isInPool(7, pool([5, 6])), false);
});

test('filterOutOfRoster：受限池剔除名单外球员并回传 removed', () => {
  const rows = [{ id: 5, name: '张三' }, { id: 7, name: '离队者' }];
  const bench = [{ id: 6, name: '李四' }, { id: 8, name: '外人' }];
  const out = filterOutOfRoster(rows, bench, pool([5, 6]));
  assert.deepEqual(out.keptRows.map((p) => p.id), [5]);
  assert.deepEqual(out.keptBench.map((p) => p.id), [6]);
  assert.deepEqual(out.removed, [
    { id: 7, name: '离队者' },
    { id: 8, name: '外人' }
  ]);
});

test('filterOutOfRoster：受限且池为空时全部剔除；未受限原样放行', () => {
  const rows = [{ id: 5, name: '张三' }];
  const restrictedEmpty = filterOutOfRoster(rows, [], pool([]));
  assert.equal(restrictedEmpty.keptRows.length, 0);
  assert.equal(restrictedEmpty.removed.length, 1);

  const untouched = filterOutOfRoster(rows, [], full);
  assert.equal(untouched.keptRows.length, 1);
  assert.equal(untouched.removed.length, 0);
});

test('filterTemplateByPool：槽位置空留位、替补与投手剔除并计数；未受限原样', () => {
  const template = {
    slots: [
      { position: 'P', playerId: 5 },
      { position: 'C', playerId: 9 }
    ],
    benchPlayerIds: [6, 10],
    startingPitcherPlayerId: 9
  };
  const out = filterTemplateByPool(template, pool([5, 6]));
  assert.equal(out.slots[0].playerId, 5);
  assert.equal(out.slots[1].playerId, null);
  assert.deepEqual(out.benchPlayerIds, [6]);
  assert.equal(out.pitcherId, null);
  assert.equal(out.removedCount, 3);

  const untouched = filterTemplateByPool(template, full);
  assert.equal(untouched.removedCount, 0);
  assert.equal(untouched.slots.length, 2);
  assert.equal(untouched.pitcherId, 9);
});
