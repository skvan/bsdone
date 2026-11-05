// 已结束比赛的列表入口：统一走“数据补录/复核”，不再提供独立“赛后复核”按钮。
// 运行：node --test tests/gamesFinalReviewEntry.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const games = fs.readFileSync(
  new URL('../src/views/admin/Games.js', import.meta.url),
  'utf8'
);

test('final 列表不再显示“赛后复核”独立入口', () => {
  assert.ok(
    !games.includes('R("赛后复核",-1)'),
    '仍保留旧的“赛后复核”按钮'
  );
  assert.ok(
    !games.includes('K.status==="final"&&K.hasLiveSnapshot&&Y(ie)("business:game:saveResult")'),
    '仍保留 final + 快照进入实时复核的条件'
  );
});

test('final 的编辑入口改名为“数据补录/复核”', () => {
  assert.ok(
    games.includes('K.status==="final"?"数据补录/复核":"编辑"'),
    'final 场次未显示“数据补录/复核”'
  );
  assert.ok(
    games.includes('$.$emit("edit",K)'),
    '数据补录/复核未复用结果录入页'
  );
});

test('保留“查看比赛实况记录”入口', () => {
  assert.ok(
    games.includes('$.$emit("viewLiveRecord",K)'),
    'final 的实况查看入口被误删'
  );
});
