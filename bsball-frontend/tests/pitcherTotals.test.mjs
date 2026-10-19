// 前端单测：重建接口绝对总量回显（#149/#204）
// 运行：在 bsball-frontend 目录执行  node --test tests/pitcherTotals.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { applyAbsolutePitcherTotals } from '../src/domain/earnedRun/pitcherTotals.js';

function player(id, pitching = {}) {
  return { id, stats: { pitching } };
}

test('绝对总量直接覆盖四个字段，不依赖旧值', () => {
  const p = player(1324, { pitchR: 99, er: 99, unearnedR: 99, pendingR: 99 });
  const applied = applyAbsolutePitcherTotals([p], {
    1324: { runs: 6, earnedRuns: 4, unearnedRuns: 1, pendingRuns: 1 }
  });

  assert.equal(applied, 1);
  assert.equal(p.stats.pitching.pitchR, 6);
  assert.equal(p.stats.pitching.er, 4);
  assert.equal(p.stats.pitching.unearnedR, 1);
  assert.equal(p.stats.pitching.pendingR, 1);
});

test('重复应用同一份绝对总量保持幂等', () => {
  const p = player(1324, { er: 4 });
  const totals = { 1324: { runs: 6, earnedRuns: 4, unearnedRuns: 0, pendingRuns: 2 } };

  applyAbsolutePitcherTotals([p], totals);
  applyAbsolutePitcherTotals([p], totals);

  assert.equal(p.stats.pitching.pitchR, 6);
  assert.equal(p.stats.pitching.er, 4);
  assert.equal(p.stats.pitching.pendingR, 2);
});

test('旧后端没有绝对总量时不做任何修改', () => {
  const p = player(1324, { er: 7 });
  const applied = applyAbsolutePitcherTotals([p], undefined);

  assert.equal(applied, 0);
  assert.equal(p.stats.pitching.er, 7);
});

test('未知投手与非投手行被忽略', () => {
  const p = player(1, { er: 1 });
  const applied = applyAbsolutePitcherTotals([p], {
    999: { runs: 9, earnedRuns: 9, unearnedRuns: 0, pendingRuns: 0 }
  });

  assert.equal(applied, 0);
  assert.equal(p.stats.pitching.er, 1);
});

test('负值与非法值钳制为 0', () => {
  const p = player(7, {});
  applyAbsolutePitcherTotals([p], {
    7: { runs: -1, earnedRuns: 'x', unearnedRuns: null, pendingRuns: 2 }
  });

  assert.equal(p.stats.pitching.pitchR, 0);
  assert.equal(p.stats.pitching.er, 0);
  assert.equal(p.stats.pitching.unearnedR, 0);
  assert.equal(p.stats.pitching.pendingR, 2);
});

test('LiveGame 保存映射不再发送客户端 er，且不再保留半局增量状态', () => {
  const source = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

  assert.ok(!source.includes('er:r.er??0'), 'Eo() 仍发送 er');
  assert.ok(!source.includes('earnedRunHalfTotals'), '仍保留 earnedRunHalfTotals 增量状态');
  assert.ok(source.includes('applyAbsolutePitcherTotals(players,absolute)'), '未改用绝对总量回显');
});

