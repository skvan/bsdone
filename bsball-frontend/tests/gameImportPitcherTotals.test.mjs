// 结果页 JSON 导入/导出：投手失分分解必须带 UER/PEND，配合后端四栏自洽校验。
// 运行：node --test tests/gameImportPitcherTotals.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

test('投手 JSON 导入映射（主客队两份）都带 unearnedR/pendingR', () => {
  const games = read('../src/views/admin/Games.js');
  const matches = games.match(
    /pitchR:h\.pitchR\?\?0,er:h\.er\?\?0,unearnedR:h\.unearnedR,pendingR:h\.pendingR/g
  ) ?? [];
  assert.equal(matches.length, 2, '主队/客队投手导入映射都需要带上 UER/PEND');
});

test('投手 JSON 导出映射带 unearnedR/pendingR', () => {
  const games = read('../src/views/admin/Games.js');
  assert.ok(
    games.includes('pitchR:w.pitchR,er:w.er,unearnedR:w.unearnedR,pendingR:w.pendingR'),
    '导出映射需要带上 UER/PEND，保证导入导出可往返'
  );
});
