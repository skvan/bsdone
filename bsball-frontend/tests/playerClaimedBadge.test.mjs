// 静态断言：球员管理页“已认领”头像徽标（Issue #481）
// 运行：node --test tests/playerClaimedBadge.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

const badge = read('../src/components/admin/PlayerClaimedBadge.vue');
const playerList = read('../src/views/admin/PlayerList.js');
const playerListCss = read('../src/styles/legacy/player-list.css');

test('徽标为蓝色齿轮 + 白色对勾，并带无障碍说明', () => {
  assert.match(badge, /role="img"/);
  assert.match(badge, /aria-label="已认领"/);
  assert.match(badge, /title="该球员档案已被本人认领"/);
  assert.match(badge, /color:\s*#2f6bff/);
  assert.match(badge, /fill="currentColor"/);
  assert.match(badge, /stroke="#fff"/);
  // 8 齿：1 个未旋转 + 7 个 rotate
  assert.equal((badge.match(/<rect /g) || []).length, 8);
  assert.equal((badge.match(/transform="rotate\(/g) || []).length, 7);
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
