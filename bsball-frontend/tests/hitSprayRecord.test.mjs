// 击球落点记录载荷构建单测（纯函数，不触碰 DOM）
// 运行：node --test tests/hitSprayRecord.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolveSprayZone, buildHitSprayRecord, newSprayRid, toHitSprayPayload, pruneSentSprayRecords, restoreFailedSprayBatch } from '../src/utils/hitSprayRecord.js';

const batter = { id: '42', name: '张三' };
const baseState = {
  gameId: 226,
  batter,
  teamId: 7,
  inning: 3,
  isTop: true,
  batterOrder: 2,
  outs: 1,
  runners: { 1: null, 2: { id: 9 }, 3: { id: 10 } },
};
const baseSpray = {
  bipCode: 'bip:ld:1b',
  sprayZone: 'LCF',
  sprayFieldPos: 'LF',
  sprayX: 150,
  sprayY: 260,
  sprayDepth: '中',
};

test('resolveSprayZone：守备位优先，区域回退，无效返回 undefined', () => {
  assert.equal(resolveSprayZone('LCF', 'LF'), 'LF');
  assert.equal(resolveSprayZone('LCF', ''), 'LCF');
  assert.equal(resolveSprayZone('lcf', ' p '), 'P');
  assert.equal(resolveSprayZone('XX', 'YY'), undefined);
  assert.equal(resolveSprayZone(undefined, undefined), undefined);
});

test('buildHitSprayRecord：完整字段映射（含垒况二/三垒有人与上下半局）', () => {
  const rec = buildHitSprayRecord(baseState, 'H1', 0, baseSpray);
  assert.deepEqual(rec, {
    gameId: 226,
    playerId: 42,
    teamId: 7,
    inning: 3,
    half: 'top',
    batterOrder: 2,
    outsBefore: 1,
    runnersBefore: '2,3',
    resultCode: 'H1',
    bipCode: 'bip:ld:1b',
    rbi: 0,
    sprayX: 150,
    sprayY: 260,
    sprayZone: 'LF',
    sprayDepth: '中',
  });
});

test('buildHitSprayRecord：无人上垒 runnersBefore 为 undefined；下半局 half=bottom', () => {
  const rec = buildHitSprayRecord(
    { ...baseState, isTop: false, runners: {} },
    'HR',
    3,
    baseSpray,
  );
  assert.equal(rec.runnersBefore, undefined);
  assert.equal(rec.half, 'bottom');
  assert.equal(rec.rbi, 3);
});

test('buildHitSprayRecord：无坐标 / 非有限坐标 / 区域无效 / 无击球员 均返回 null', () => {
  assert.equal(buildHitSprayRecord(baseState, 'H1', 0, { ...baseSpray, sprayX: null }), null);
  assert.equal(buildHitSprayRecord(baseState, 'H1', 0, { ...baseSpray, sprayY: undefined }), null);
  assert.equal(buildHitSprayRecord(baseState, 'H1', 0, { ...baseSpray, sprayX: NaN }), null);
  assert.equal(buildHitSprayRecord(baseState, 'H1', 0, { ...baseSpray, sprayY: Infinity }), null);
  assert.equal(buildHitSprayRecord(baseState, 'H1', 0, { ...baseSpray, sprayX: '' }), null);
  assert.equal(buildHitSprayRecord(baseState, 'H1', 0, { ...baseSpray, sprayZone: 'x', sprayFieldPos: 'y' }), null);
  assert.equal(buildHitSprayRecord({ ...baseState, batter: null }, 'H1', 0, baseSpray), null);
});

test('buildHitSprayRecord：坐标为 0 是合法落点，不按缺失丢弃', () => {
  const onLine = buildHitSprayRecord(baseState, 'H1', 0, { ...baseSpray, sprayX: 0, sprayY: 120 });
  assert.ok(onLine, 'x=0 的边线落点应生成记录');
  assert.equal(onLine.sprayX, 0);
  assert.equal(onLine.sprayY, 120);

  const onWallTop = buildHitSprayRecord(baseState, 'OUT_F', 0, { ...baseSpray, sprayX: 240, sprayY: 0 });
  assert.ok(onWallTop, 'y=0 的墙顶落点应生成记录');
  assert.equal(onWallTop.sprayY, 0);
});

test('buildHitSprayRecord：gameId 缺省回落 0（沿用原 Lt/R 兜底语义）', () => {
  const rec = buildHitSprayRecord({ ...baseState, gameId: 0 }, 'OUT_F', 0, baseSpray);
  assert.equal(rec.gameId, 0);
  assert.equal(rec.resultCode, 'OUT_F');
});

// 2026-11-09：已送出的落点不能再被「撤销/重进现场」放回队列重复上传
test('newSprayRid：每次生成不同的非空字符串（快照深拷贝后仍能对上同一笔）', () => {
  const a = newSprayRid();
  const b = newSprayRid();
  assert.equal(typeof a, 'string');
  assert.ok(a.length > 0);
  assert.notEqual(a, b);
  // 记录经 structuredClone（撤销/重做快照）后 rid 必须保持不变
  const rec = { ...buildHitSprayRecord(baseState, 'H1', 0, baseSpray), rid: a };
  assert.equal(structuredClone(rec).rid, a);
});

test('toHitSprayPayload：送出前剥掉 rid，其余字段原样、不改动入参', () => {
  const rec = { ...buildHitSprayRecord(baseState, 'E', 0, baseSpray), rid: 'rid-1' };
  const payload = toHitSprayPayload(rec);
  assert.equal('rid' in payload, false);
  assert.equal(payload.resultCode, 'E');
  assert.equal(payload.sprayZone, 'LF');
  assert.equal(rec.rid, 'rid-1', '入参对象不应被改写');
  assert.equal(toHitSprayPayload(null), null);
});

test('pruneSentSprayRecords：剔除已送出 rid，保留无 rid 的历史记录', () => {
  const sent = new Set(['rid-1', 'rid-2']);
  const queue = [
    { rid: 'rid-1', resultCode: 'E' },
    { rid: 'rid-3', resultCode: 'H1' },
    { resultCode: 'OUT_G' },
    null,
  ];
  assert.deepEqual(pruneSentSprayRecords(queue, sent), [
    { rid: 'rid-3', resultCode: 'H1' },
    { resultCode: 'OUT_G' },
    null,
  ]);
  assert.equal(pruneSentSprayRecords(queue, new Set()).length, 4, '无已送出 rid 时原样返回');
  assert.deepEqual(pruneSentSprayRecords(null, sent), []);
});

test('restoreFailedSprayBatch：发送期间撤销已放回同一批时不重复回灌', () => {
  const batch = [{ rid: 'a' }, { rid: 'b' }, { rid: 'b' }, { resultCode: 'E' }];
  const queue = [{ rid: 'a' }, { rid: 'c' }];
  assert.deepEqual(restoreFailedSprayBatch(batch, queue), [
    { rid: 'b' },
    { resultCode: 'E' },
    { rid: 'a' },
    { rid: 'c' },
  ]);
  assert.equal(restoreFailedSprayBatch(batch, null).length, 3, 'batch 内同 rid 应去重');
  assert.equal(restoreFailedSprayBatch([], queue), queue, '空 batch 原样返回当前队列');
});
