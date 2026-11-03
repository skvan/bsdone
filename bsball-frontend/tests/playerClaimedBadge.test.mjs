// 静态断言：球员管理页“已认领”头像徽标（Issue #481）
// 运行：node --test tests/playerClaimedBadge.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const badge = read('../src/components/admin/PlayerClaimedBadge.vue');
const playerList = read('../src/views/admin/PlayerList.js');
const playerListCss = read('../src/styles/legacy/player-list.css');
const playerDetailCss = read('../src/styles/legacy/player-detail-content.css');

test('徽标为蓝色圆形 + 白色对勾，并带无障碍说明', () => {
  assert.match(badge, /role="img"/);
  assert.match(badge, /aria-label="已认领"/);
  assert.match(badge, /title="该球员档案已被本人认领"/);
  assert.match(badge, /color:\s*#2f6bff/);
  assert.match(badge, /<circle cx="8" cy="8" r="8" fill="currentColor" \/>/);
  assert.match(badge, /stroke="#fff"/);
  assert.equal((badge.match(/<circle /g) || []).length, 1);
  assert.equal((badge.match(/<rect /g) || []).length, 0);
});

test('球员详情已认证徽标使用同一蓝色背景', () => {
  assert.match(
    playerDetailCss,
    /\.player-verify-badge--claimed\[data-v-abd97122\]\{background:#2f6bff\}/
  );
});

test('球员列表仅在 userId 存在时渲染徽标，并包在头像容器内', () => {
  assert.match(playerList, /import PlayerClaimedBadge from '\.\.\/\.\.\/components\/admin\/PlayerClaimedBadge\.vue'/);
  assert.match(playerList, /class:"cell-avatar-wrap"/);
  assert.match(playerList, /h\.userId!=null\?\(m\(\),R\(PlayerClaimedBadge/);
});

test('头像容器提供相对定位上下文', () => {
  assert.match(
    playerListCss,
    /\.cell-avatar-wrap\[data-v-8cbc228d\]\{[^}]*position:relative/
  );
});
