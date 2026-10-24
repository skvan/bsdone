// 回归：比赛详情 Tab 文案不能把 i18n 消息对象渲染成 [object Object]
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

import { pickI18nText, resolveBoxViewLabel } from '../src/utils/i18nText.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');

test('pickI18nText 从常见 i18n 返回形态中提取字符串', () => {
  assert.equal(pickI18nText('进攻'), '进攻');
  assert.equal(pickI18nText({ value: '进攻' }), '进攻');
  assert.equal(pickI18nText({ default: '投球' }), '投球');
  assert.equal(pickI18nText({ message: '防守' }), '防守');
  assert.equal(pickI18nText({ value: { default: '进攻' } }), '进攻');
  assert.equal(pickI18nText('gameDetail.batting', 'gameDetail.batting'), '');
  assert.equal(pickI18nText({ value: { nested: true } }), '');
  assert.equal(pickI18nText(null), '');
  assert.equal(pickI18nText(undefined), '');
  assert.equal(pickI18nText('   '), '');
});

test('resolveBoxViewLabel 本地优先、全局次之、最后视图兜底', () => {
  assert.equal(resolveBoxViewLabel('gameDetail.batting', '打击', '全局'), '打击');
  assert.equal(
    resolveBoxViewLabel('gameDetail.batting', 'gameDetail.batting', { value: { default: '进攻' } }),
    '进攻'
  );
  assert.equal(
    resolveBoxViewLabel('gameDetail.batting', 'gameDetail.batting', 'gameDetail.batting'),
    '进攻'
  );
  assert.equal(resolveBoxViewLabel('gameDetail.foo', '', ''), 'foo');
});

test('GameDetailContent Tab 文案使用 resolveBoxViewLabel，不再 String(i18n 对象)', () => {
  const detail = read('../src/components/admin/GameDetailContent.js');
  assert.ok(detail.includes('resolveBoxViewLabel'), '未使用 resolveBoxViewLabel');
  assert.ok(!/String\(\s*\w+\?\.value/.test(detail), '仍存在 String(i18n 对象) 的旧写法');
});
