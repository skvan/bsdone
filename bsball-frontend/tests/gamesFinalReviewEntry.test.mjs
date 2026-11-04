// 已结束比赛的列表入口：有快照且有录入权限时显示“赛后复核”，复用 AdminGameLiveResume。
// 运行：node --test tests/gamesFinalReviewEntry.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const games = fs.readFileSync(
  new URL('../src/views/admin/Games.js', import.meta.url),
  'utf8'
);

test('final 且有快照的比赛显示“赛后复核”入口', () => {
  assert.ok(
    games.includes('K.status==="final"&&K.hasLiveSnapshot&&Y(ie)("business:game:saveResult")'),
    '缺少 final + 快照 + 录入权限的显示条件'
  );
  assert.ok(
    games.includes('R("赛后复核",-1)'),
    '缺少“赛后复核”按钮文案'
  );
});

test('“赛后复核”复用 resumeLive 导航到实时录入页', () => {
  assert.ok(
    games.includes('$.$emit("resumeLive",K)'),
    '未复用 resumeLive 事件'
  );
  assert.ok(
    games.includes('name:"AdminGameLiveResume"'),
    'AdminGameLiveResume 路由缺失'
  );
});

test('保留“查看比赛实况记录”入口', () => {
  assert.ok(
    games.includes('$.$emit("viewLiveRecord",K)'),
    'final 的实况查看入口被误删'
  );
});
