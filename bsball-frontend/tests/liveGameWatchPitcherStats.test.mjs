// Issue #450：实时观赛 BOX SCORE 投手行需完整映射被 2B/3B/BB/HBP 等字段
// 运行：node --test tests/liveGameWatchPitcherStats.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { derivePitcherStatsFromSummary, mergePitcherStatsWithSummary } from '../src/utils/liveGameWatchPitcherStats.js';

test('旧快照回算：2B/3B/HR/IPHR/BB/IBB/HBP/SH/SF/SO 全部可推导', () => {
  const summaries = [
    { pitcherPlayerId: 7, outsBefore: 0, outsAfter: 1, resultCode: 'H2' },
    { pitcherPlayerId: 7, outsBefore: 1, outsAfter: 1, resultCode: 'H3' },
    { pitcherPlayerId: 7, outsBefore: 1, outsAfter: 1, resultCode: 'BB' },
    { pitcherPlayerId: 7, outsBefore: 1, outsAfter: 1, resultCode: 'IBB' },
    { pitcherPlayerId: 7, outsBefore: 1, outsAfter: 1, resultCode: 'HBP' },
    { pitcherPlayerId: 7, outsBefore: 1, outsAfter: 1, resultCode: 'SH' },
    { pitcherPlayerId: 7, outsBefore: 1, outsAfter: 1, resultCode: 'SF' },
    { pitcherPlayerId: 7, outsBefore: 1, outsAfter: 2, resultCode: 'K_SWING' },
    { pitcherPlayerId: 7, outsBefore: 2, outsAfter: 2, resultCode: 'HR' },
    { pitcherPlayerId: 7, outsBefore: 2, outsAfter: 3, resultCode: 'IPHR' },
    { pitcherPlayerId: 7, outsBefore: 3, outsAfter: 3, resultCode: 'H1' },
    { pitcherPlayerId: 7, outsBefore: 3, outsAfter: 3, resultCode: 'DK3_OUT_1ST' },
    { pitcherPlayerId: 8, outsBefore: 0, outsAfter: 1, resultCode: 'H2' }
  ];
  assert.deepEqual(derivePitcherStatsFromSummary(7, summaries), {
    ip: 1,
    pitchH: 5,
    pitchDoubles: 1,
    pitchTriples: 1,
    pitchHr: 1,
    pitchInsideParkHr: 1,
    pitchBb: 2,
    pitchIbb: 1,
    pitchHbp: 1,
    pitchBbHp: 3,
    pitchSh: 1,
    pitchSf: 1,
    pitchSo: 2,
    pitchPa: 12,
    pitchBf: 12
  });
  assert.equal(derivePitcherStatsFromSummary(9, summaries), null);
});

test('LiveGameWatch 投手行映射包含被 2B/3B/BB/HBP 等字段', () => {
  const src = fs
    .readFileSync(new URL('../src/views/admin/LiveGameWatch.js', import.meta.url), 'utf8')
    .replace(/\r\n/g, '\n');
  for (const key of [
    'pitchDoubles:k.pitchDoubles',
    'pitchTriples:k.pitchTriples',
    'pitchBb:k.pitchBb',
    'pitchIbb:k.pitchIbb',
    'pitchHbp:k.pitchHbp',
    'pitchSh:k.pitchSh',
    'pitchSf:k.pitchSf',
    'pitchIo:k.pitchIo'
  ]) {
    assert.ok(src.includes(key), `投手行缺少 ${key}`);
  }
  assert.ok(src.includes('pitchBbHp:k.pitchBbHp??((k.pitchBb??0)+(k.pitchHbp??0))'), 'BB+HBP 合计缺少兜底');
  assert.ok(src.includes('derivePitcherStatsFromSummary as LgDerivePitcherStats'), '未接入回算工具');
  assert.ok(src.includes('mergePitcherStatsWithSummary as LgMergePitcherStats'), '未接入缺失字段合并工具');
  assert.ok(src.includes('LgMergePitcherStats({'), '未合并旧快照回算字段');
  assert.ok(!src.includes('pitAb:ab'), 'AB 不应由死字段映射');
  assert.ok(!src.includes('pitE:Number(y.e)'), 'E 不应由死字段映射');
  assert.ok(src.includes('return LgDerivePitcherStats(e,t)'), '旧快照回算未走工具');
});

test('实时投手统计用回算补齐缺失/为 0 的 2B/3B/BB/HBP，同时保留 R/ER/WP', () => {
  const live = {
    ip: 3.2, pitchH: 6, pitchDoubles: 0, pitchBb: 0, pitchHbp: 0, pitchBbHp: 5,
    pitchR: 13, er: 0, wp: 2, pitchSo: 3, pitchPa: 29, pitchBf: 29
  };
  const derived = {
    ip: 3.2, pitchH: 6, pitchDoubles: 1, pitchTriples: 1, pitchHr: 0, pitchInsideParkHr: 0,
    pitchBb: 5, pitchIbb: 0, pitchHbp: 2, pitchBbHp: 7, pitchSh: 0, pitchSf: 0,
    pitchSo: 3, pitchPa: 29, pitchBf: 29
  };
  const merged = mergePitcherStatsWithSummary(live, derived);
  assert.equal(merged.pitchDoubles, 1);
  assert.equal(merged.pitchTriples, 1);
  assert.equal(merged.pitchBb, 5);
  assert.equal(merged.pitchHbp, 2);
  assert.equal(merged.pitchBbHp, 7);
  assert.equal(merged.pitchR, 13);
  assert.equal(merged.er, 0);
  assert.equal(merged.wp, 2);
});

test('旧加载回填形态（BB=BB+HBP 合计、HBP=0）按 PBP 同时重算 BB/HBP', () => {
  const live = {
    ip: 3.2, pitchH: 6, pitchDoubles: 0, pitchBb: 7, pitchIbb: 0, pitchHbp: 0, pitchBbHp: 7,
    pitchR: 13, er: 0, wp: 2
  };
  const derived = {
    ip: 3.2, pitchH: 6, pitchDoubles: 1, pitchTriples: 1, pitchHr: 0, pitchInsideParkHr: 0,
    pitchBb: 5, pitchIbb: 0, pitchHbp: 2, pitchBbHp: 7, pitchSh: 0, pitchSf: 0,
    pitchSo: 3, pitchPa: 29, pitchBf: 29
  };
  const merged = mergePitcherStatsWithSummary(live, derived);
  assert.equal(merged.pitchBb, 5);
  assert.equal(merged.pitchHbp, 2);
  assert.equal(merged.pitchBbHp, 7);
  assert.equal(merged.pitchR, 13);
  assert.equal(merged.er, 0);
});
