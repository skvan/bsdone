// 前端单测：实时动态打席头部垒包解析（#399）
import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveAtBatGroupBases } from '../src/utils/liveGameGroupBases.js';

test('当前打席优先使用快照 activeAbContext.basesBefore，不跟随实时 runners', () => {
  const result = resolveAtBatGroupBases({
    isCurrent: true,
    liveBases: { 1: false, 2: false, 3: true },
    summary: null,
    activeBases: { 1: false, 2: false, 3: false }
  });
  assert.deepEqual(result, { 1: false, 2: false, 3: false });
});

test('当前打席 active 与摘要不一致时以 active 为准', () => {
  const result = resolveAtBatGroupBases({
    isCurrent: true,
    liveBases: { 1: false, 2: false, 3: true },
    summary: { basesBefore: { 1: false, 2: false, 3: true } },
    activeBases: { 1: false, 2: true, 3: false }
  });
  assert.deepEqual(result, { 1: false, 2: true, 3: false });
});

test('当前打席已有摘要时，头部使用摘要 basesBefore，不跟随实时 runners', () => {
  const result = resolveAtBatGroupBases({
    isCurrent: true,
    liveBases: { 1: false, 2: false, 3: true },
    summary: { basesBefore: { 1: false, 2: false, 3: false } }
  });
  assert.deepEqual(result, { 1: false, 2: false, 3: false });
});

test('当前打席尚无摘要时，回退实时垒包', () => {
  const result = resolveAtBatGroupBases({
    isCurrent: true,
    liveBases: { 1: true, 2: false, 3: false },
    summary: null
  });
  assert.deepEqual(result, { 1: true, 2: false, 3: false });
});

test('历史打席使用摘要 basesBefore', () => {
  const result = resolveAtBatGroupBases({
    isCurrent: false,
    liveBases: { 1: true, 2: true, 3: true },
    summary: { basesBefore: { 1: false, 2: true, 3: false } }
  });
  assert.deepEqual(result, { 1: false, 2: true, 3: false });
});

test('历史打席无摘要时回退全空，不借用实时垒包', () => {
  const result = resolveAtBatGroupBases({
    isCurrent: false,
    liveBases: { 1: true, 2: true, 3: true },
    summary: undefined
  });
  assert.deepEqual(result, { 1: false, 2: false, 3: false });
});

test('摘要 basesBefore 缺字段或空值按无人处理', () => {
  const result = resolveAtBatGroupBases({
    isCurrent: false,
    liveBases: { 1: true, 2: true, 3: true },
    summary: { basesBefore: { 1: false } }
  });
  assert.deepEqual(result, { 1: false, 2: false, 3: false });
});
