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
    live.includes('finalReviewMode=!0,l.isGameEnd=!0'),
    'final 比赛加载后应标记复核态与结束态'
  );
  assert.ok(
    live.includes('比赛已结束，可进行赛后复核'),
    'final 恢复提示应引导赛后复核'
  );
  assert.ok(
    live.includes('name:"AdminGameDetail"'),
    'final 但无阵容/统计时应回到比赛详情，而不是开阵容编辑'
  );
});

test('赛后复核态禁止撤销/重做，保存仍保持 final', () => {
  assert.ok(live.includes('if(finalReviewMode){'), '撤销/重做缺少复核态守卫');
  assert.ok(live.includes('finalReviewMode&&(O.isGameEnd=!0)'), '撤销恢复后未强制结束态');
  assert.ok(
    live.includes('status:(finalReviewMode||l.isGameEnd)?"final":"live"'),
    '保存载荷未在复核态保持 final'
  );
  assert.ok(
    live.includes('canUndo||finalReviewMode') && live.includes('canRedo||finalReviewMode'),
    '撤销/重做按钮未在复核态禁用'
  );
});

test('赛后复核不恢复本地旧现场，也不允许写回录入现场', () => {
  assert.ok(
    live.includes('a.status==="final"?(m?{source:"server",payload:m}:null):id(zo(Re.value,e),m)'),
    'final 复核仍可能优先恢复本地旧现场'
  );
  assert.ok(
    live.includes('赛后复核模式请使用复核抽屉保存，录入现场不会写回'),
    '缺少复核态主保存守卫提示'
  );
  assert.ok(
    live.includes('disabled:finalReviewMode,title:finalReviewMode?'),
    '“保存比赛”按钮未在复核态禁用'
  );
  assert.ok(
    live.includes('enabled:()=>F.value&&!finalReviewMode'),
    '复核态离开确认仍会触发保存现场'
  );
});
