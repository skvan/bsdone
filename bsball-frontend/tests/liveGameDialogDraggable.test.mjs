// Issue #529：实时录入互动弹窗可移动（Element Plus MessageBox draggable）
// 运行：node --test tests/liveGameDialogDraggable.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const liveGame = fs.readFileSync(new URL('../src/views/admin/LiveGame.js', import.meta.url), 'utf8');

test('实时录入全部 MessageBox 入口均启用原生 draggable', () => {
  const pattern = /\bal\(\{|al\.(?:confirm|alert|prompt)\(/g;
  const starts = [...liveGame.matchAll(pattern)].map((match) => match.index);
  assert.equal(starts.length, 9, '应覆盖 LgPick、离开确认及全部 al.confirm 弹窗');

  starts.forEach((start, index) => {
    const block = liveGame.slice(start, starts[index + 1] ?? liveGame.length);
    assert.match(
      block,
      /draggable:\s*(?:!0|true)/,
      `第 ${index + 1} 个 MessageBox 入口未启用 draggable`
    );
  });
});
