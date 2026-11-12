// #511 端口归属防呆：必须跑在 Playwright webServer 之前，且 Windows 不能依赖 lsof。
// 运行：node --test tests/e2eDevServerGuard.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

test('端口检查走 npm pretest，不再挂在 Playwright globalSetup', () => {
  const pkg = JSON.parse(read('../package.json'));
  assert.equal(pkg.scripts['pretest:e2e'], 'node e2e/assert-dev-server-owner.mjs');
  assert.equal(pkg.scripts['pretest:e2e:local'], 'node e2e/assert-dev-server-owner.mjs');

  const config = read('../playwright.config.js');
  assert.ok(!/^\s*globalSetup\s*:/m.test(config), 'globalSetup 会晚于 webServer 启动，不能用于端口检查');
  assert.equal(fs.existsSync(new URL('../e2e/global-setup.mjs', import.meta.url)), false);
});

test('检查脚本覆盖 Windows fail-closed 与 E2E_PORT/E2E_BASE_URL 逃生口', () => {
  const guard = read('../e2e/assert-dev-server-owner.mjs');
  assert.ok(guard.includes("process.platform === 'win32'"), 'Windows 需要单独分支');
  assert.ok(guard.includes("execFileSync('lsof'"), 'Unix 用 lsof 取 cwd');
  assert.ok(guard.includes('E2E_PORT'), '端口占用时应提示 E2E_PORT');
  assert.ok(guard.includes('E2E_BASE_URL'), '外部目标应跳过本地检查');
});
