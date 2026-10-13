// 前端单测：业务错误判定（Issue #310 批 3）
// 运行：在 bsball-frontend 目录执行  node --test "tests/*.test.mjs"
import test from 'node:test';
import assert from 'node:assert/strict';
import { isBusinessError } from '../src/utils/apiError.js';

test('isBusinessError：4xx 业务码为真（400/403/404/409）', () => {
  for (const code of [400, 403, 404, 409]) {
    const err = new Error('x');
    err.apiCode = code;
    assert.equal(isBusinessError(err), true, `apiCode=${code}`);
  }
});

test('isBusinessError：5xx/无码/网络错误为假', () => {
  const server = new Error('服务器繁忙');
  server.apiCode = 500;
  assert.equal(isBusinessError(server), false);
  assert.equal(isBusinessError(new TypeError('Failed to fetch')), false);
  assert.equal(isBusinessError(new Error('x')), false);
  assert.equal(isBusinessError(null), false);
  assert.equal(isBusinessError(undefined), false);
});
