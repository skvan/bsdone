import { test } from 'node:test';
import assert from 'node:assert/strict';
import { extractShareSource, SHARE_SOURCES } from '../src/utils/shareSource.js';

test('白名单内来源返回原值', () => {
  assert.equal(extractShareSource('?from=share'), 'share');
  assert.equal(extractShareSource('?from=poster'), 'poster');
  assert.equal(extractShareSource('?a=1&from=share&b=2'), 'share');
  assert.equal(extractShareSource('from=share'), 'share');
});

test('白名单外/缺失/空 → null', () => {
  assert.equal(extractShareSource('?from=email'), null);
  assert.equal(extractShareSource('?from='), null);
  assert.equal(extractShareSource(''), null);
  assert.equal(extractShareSource(null), null);
  assert.equal(extractShareSource(undefined), null);
});

test('常量导出', () => {
  assert.deepEqual(SHARE_SOURCES, ['share', 'poster']);
});
