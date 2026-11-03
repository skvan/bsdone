// DH 制仅守备投手：PO/A/E 记账目标必须解析到活体投手行，不能落在 Hn 深拷贝
import { test } from 'node:test';
import assert from 'node:assert/strict';
import * as rules from '../src/utils/liveGameFieldingByPos.js';

test('P 位记账目标解析到活体投手；其他守位保持原对象', () => {
  const { resolveFieldingCreditTarget } = rules;
  assert.equal(typeof resolveFieldingCreditTarget, 'function', '缺少 resolveFieldingCreditTarget');
  const copy = { id: 101, position: 'P', stats: { fielding: {} } };
  const live = { id: 101, stats: { fielding: {} } };
  assert.equal(resolveFieldingCreditTarget('P', copy, live), live);
  assert.equal(resolveFieldingCreditTarget(' p ', copy, live), live);
  assert.equal(resolveFieldingCreditTarget('SS', copy, live), copy);
  assert.equal(resolveFieldingCreditTarget('P', copy, null), copy);
  assert.equal(resolveFieldingCreditTarget('P', null, live), live);
  assert.equal(resolveFieldingCreditTarget('', copy, live), copy);
});
