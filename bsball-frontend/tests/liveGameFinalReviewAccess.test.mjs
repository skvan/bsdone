// final 比赛允许进入实时录入页进行赛后复核（ER / 守备补记）。
// 运行：node --test tests/liveGameFinalReviewAccess.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const live = fs.readFileSync(
  new URL('../src/views/admin/LiveGame.js', import.meta.url),
  'utf8'
);

test('final 比赛不再被 _c 直接挡回赛程，并强制进入结束态', () => {
  assert.ok(
    live.includes('if(!a||(a.status!=="live"&&a.status!=="final"))'),
    'final 比赛应允许进入实时录入页'
  );
  assert.ok(
    live.includes('a.status==="final"&&(l.isGameEnd=!0),loaded'),
    'final 比赛加载后应标记结束态，开放赛后复核'
  );
  assert.ok(
    live.includes('比赛已结束，可进行赛后复核'),
    'final 恢复提示应引导赛后复核'
  );
});
