// 静态断言：GameDetailContent 内不得再声明与 getCurrentInstance 别名 Zt 同名的函数
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const detail = fs.readFileSync(
  new URL('../src/components/admin/GameDetailContent.js', import.meta.url),
  'utf8'
);

test('GameDetailContent 不存在本地 function Zt（会提升遮蔽 getCurrentInstance 导入）', () => {
  assert.ok(detail.includes('getCurrentInstance as Zt'), '缺少 getCurrentInstance 别名');
  assert.ok(
    !/function\s+Zt\s*\(/.test(detail),
    '本地 function Zt 会遮蔽 getCurrentInstance，导致 GameDetailContent setup 崩溃'
  );
});

test('二垒/三垒/本垒打汇总使用 buildExtraBaseSummary', () => {
  assert.ok(detail.includes('function buildExtraBaseSummary('), '缺少汇总函数');
  assert.ok(detail.includes('buildExtraBaseSummary(ut.value)'), '客队汇总调用缺失');
  assert.ok(detail.includes('buildExtraBaseSummary(dt.value)'), '主队汇总调用缺失');
});
