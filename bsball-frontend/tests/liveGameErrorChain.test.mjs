import assert from 'node:assert/strict';
import test from 'node:test';
import { computeErrorChainAssists, computeErrorChainPutout } from '../src/utils/liveGameErrorChain.js';

test('4-3 接球失误：链前传球人记 A，最后一人不记', () => {
  assert.deepEqual(computeErrorChainAssists(['1B'], ['2B', '1B']), ['2B']);
});

test('多传链：除最后一人外全部记 A', () => {
  assert.deepEqual(computeErrorChainAssists(['1B'], ['CF', 'SS', '1B']), ['CF', 'SS']);
});

test('链上失误责任者不计 A', () => {
  assert.deepEqual(computeErrorChainAssists(['2B', '1B'], ['2B', '1B']), []);
  assert.deepEqual(computeErrorChainAssists(['SS'], ['2B', 'SS', '1B']), ['2B']);
});

test('单段链与空链不记 A', () => {
  assert.deepEqual(computeErrorChainAssists(['1B'], ['SS']), []);
  assert.deepEqual(computeErrorChainAssists(['1B'], []), []);
  assert.deepEqual(computeErrorChainAssists([], []), []);
});

test('同一位置重复出现只记一次 A，位置大小写归一', () => {
  assert.deepEqual(computeErrorChainAssists(['1B'], ['2b', 'SS', '2B', '1B']), ['2B', 'SS']);
});

test('空值输入安全', () => {
  assert.deepEqual(computeErrorChainAssists(null, null), []);
  assert.deepEqual(computeErrorChainAssists(undefined, ['2B', '1B']), ['2B']);
});

test('链尾不是失误者时链尾记 PO', () => {
  assert.equal(computeErrorChainPutout(['2B'], ['2B', '1B']), '1B');
  assert.equal(computeErrorChainPutout(['SS'], ['CF', 'SS', '1B']), '1B');
});

test('链尾是失误者、单段链或空链不记 PO', () => {
  assert.equal(computeErrorChainPutout(['1B'], ['2B', '1B']), null);
  assert.equal(computeErrorChainPutout(['2B', '1B'], ['2B', '1B']), null);
  assert.equal(computeErrorChainPutout(['1B'], ['SS']), null);
  assert.equal(computeErrorChainPutout(['1B'], []), null);
  assert.equal(computeErrorChainPutout(null, null), null);
});
