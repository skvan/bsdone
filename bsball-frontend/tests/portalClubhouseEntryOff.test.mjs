// 生产下线个人主页入口，但保留页面/路由/功能（已登录用户直链 /<tenantCode>/me 仍可直达）
// 运行：cd bsball-frontend && node --test tests/portalClubhouseEntryOff.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

function read(rel) {
  return fs.readFileSync(new URL('../src/' + rel, import.meta.url), 'utf8').replace(/\r\n/g, '\n');
}

test('门户入口已下线：顶部导航与用户下拉不再渲染 PortalClubhouse', () => {
  const src = read('components/portal/PortalLayout.js');
  assert.ok(!src.includes('PortalClubhouse'), 'PortalLayout 仍存在个人主页入口');
  assert.ok(!src.includes('nav.clubhouse'), 'PortalLayout 仍渲染“我的主场”文案');
});

test('登录分流已下线：默认跳门户首页，显式 redirect 仍保留', () => {
  const src = read('views/auth/AccountLogin.js');
  const start = src.indexOf('async function goAfterLogin(){');
  assert.ok(start >= 0, '找不到 goAfterLogin');
  const end = src.indexOf('\n    }', start);
  assert.ok(end > start, 'goAfterLogin 代码段不完整');
  const body = src.slice(start, end);
  assert.ok(!body.includes('PortalClubhouse'), '登录仍跳转 PortalClubhouse');
  assert.ok(!body.includes('getPlayerProfile'), '登录仍探测球员档案');
  assert.ok(body.includes('ne.value') && body.includes('push(ne.value)'), '显式 redirect 回跳被破坏');
  assert.ok(body.includes('`/${v.value}/`'), '默认未跳门户首页');
});

test('页面/路由/功能保留：已登录用户直链 /<tenantCode>/me 仍可直达', () => {
  const router = read('router/index.js');
  assert.ok(router.includes("import PortalClubhouse from '../views/portal/PortalClubhouse.vue'"), 'PortalClubhouse 组件被误删');
  assert.ok(/path:\s*'me'[^\n]*name:\s*'PortalClubhouse'/.test(router), '/<tenantCode>/me 路由被误删');
  assert.ok(fs.existsSync(new URL('../src/views/portal/PortalClubhouse.vue', import.meta.url)), 'PortalClubhouse.vue 被误删');
  assert.ok(fs.existsSync(new URL('../src/api/clubhouse.js', import.meta.url)), 'clubhouse API 被误删');
});
