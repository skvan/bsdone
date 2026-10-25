// 阵容页底部提示过滤（纯函数真值表）
// 运行：node --test tests/lineupValidationMessage.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { visibleLineupError } from '../src/utils/lineupValidationMessage.js';

test('未开放 EP 制提示保留', () => {
  const ep = '该联赛未开放 EP 制（额外打击者），请联系管理员（上限 9 人，当前 11 人）';
  assert.equal(visibleLineupError(ep), ep);
});

test('空值 / 空位 / 重复 / 无效位置 / 人数提示均过滤', () => {
  const filtered = [
    undefined,
    '',
    '第5、6、7棒未指定守备位置',
    '守备位置 DH 重复（第3、4棒）',
    '无效守备位置: X（请选 LF/CF/RF 等具体位置）',
    '阵容需 9-10 人（DH 可选）',
  ];
  for (const msg of filtered) {
    assert.equal(visibleLineupError(msg), '', `应过滤：${msg}`);
  }
});
