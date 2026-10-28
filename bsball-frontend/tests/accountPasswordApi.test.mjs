// 前端单测：改密接口契约（Issue #473）
// 后端唯一改密接口为 PATCH /auth/change-password（AuthApi.java:245 @PatchMapping，
// 文档 docs/tech/API接口参考/认证授权API.md「修改密码」）；门户个人资料与管理台均须走该接口。
// 运行：在 bsball-frontend 目录执行  node --test tests/accountPasswordApi.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(new URL('../src/api/account.js', import.meta.url), 'utf8');
const changePasswordDefs = [...source.matchAll(/changePassword:\s*\(data\)\s*=>\s*([^\n]+)/g)].map((m) => m[1].trim());

test('account.js 定义了门户与管理台两处 changePassword', () => {
  assert.equal(changePasswordDefs.length, 2, `期望 2 处 changePassword 定义，实际 ${changePasswordDefs.length} 处`);
});

test('两处 changePassword 均以 PATCH 调用 /api/auth/change-password', () => {
  for (const def of changePasswordDefs) {
    assert.ok(def.includes("patch('/api/auth/change-password'"), `实际调用：${def}`);
  }
});

test('不得残留后端不存在的 /api/account/password/change', () => {
  assert.ok(!source.includes('/api/account/password/change'), '该地址后端无路由，实测 404「接口不存在」');
});
