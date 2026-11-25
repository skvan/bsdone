// 半局定案那三条警告的本地化：真跑 vue-i18n 取词，确认插值真的发生（#713 文案本地化）
// 运行：node --test tests/liveGameSettleNoticeI18n.test.mjs
//
// 为什么不是静态正则：本项目的词典用单花括号 {name}，而 vue-i18n 的一般写法是 {{name}}。
// 实测（vue-i18n 9.14.5）：单括号在这里正常插值，写 {{x}} 反而报
// "Not allowed nest placeholder" 并原样吐出。也就是说「照着 vue-i18n 通用惯例写」会静默坏掉，
// 只能真的解一遍。缺参数时它也不报错，直接把 {name} 位置留成空串——更得靠这条兜住。
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createI18n } from 'vue-i18n';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (p) => fs.readFileSync(path.join(root, p), 'utf8');

const messages = {};
for (const locale of ['zh-CN', 'zh-TW', 'en']) {
  messages[locale] = JSON.parse(read(`src/locales/${locale}.json`));
}
// 与 src/i18n/index.js 同构（legacy:false + fallbackLocale zh-CN）
const i18n = createI18n({ legacy: false, locale: 'zh-CN', fallbackLocale: 'zh-CN', messages });
const t = (key, params, locale) => String(i18n.global.t(key, params ?? {}, { locale }));

const PARAMS = { inning: 3, count: 2, msg: '后端给的原因' };
const KEYS = [
  'liveGame.settle.overrideVoidTop',
  'liveGame.settle.overrideVoidBottom',
  'liveGame.settle.erIssueTop',
  'liveGame.settle.erIssueBottom',
  'liveGame.settle.untrustedSkipped'
];

test('五个键在三语词典都存在，且都真能解出文案', () => {
  for (const locale of Object.keys(messages)) {
    for (const key of KEYS) {
      const out = t(key, PARAMS, locale);
      assert.notEqual(out, key, `${locale} 缺 ${key}（vue-i18n 缺词会把 key 原样返回）`);
      assert.ok(out.trim().length > 8, `${locale} / ${key} 解出来的文案太短：${out}`);
    }
  }
});

test('插值真的发生：数字进了正文，且没有留下花括号', () => {
  for (const locale of Object.keys(messages)) {
    for (const key of KEYS) {
      const out = t(key, PARAMS, locale);
      assert.ok(!/[{}]/.test(out), `${locale} / ${key} 还有没被替换的占位符：${out}`);
      if (key.endsWith('Skipped')) continue; // 那一例没有 inning/count 之外的要求
      if (key.includes('overrideVoid') || key.includes('erIssue')) {
        assert.ok(out.includes('3'), `${locale} / ${key} 没插进局数：${out}`);
      }
      if (key.includes('overrideVoid') || key.includes('untrustedSkipped')) {
        assert.ok(out.includes('2'), `${locale} / ${key} 没插进条数/人数：${out}`);
      }
      if (key.includes('erIssue')) {
        assert.ok(out.includes('后端给的原因'), `${locale} / ${key} 没插进原因正文：${out}`);
      }
    }
  }
});

test('上/下半局是两条不同文案，不是同一句换字', () => {
  for (const locale of Object.keys(messages)) {
    const top = t('liveGame.settle.overrideVoidTop', PARAMS, locale);
    const bottom = t('liveGame.settle.overrideVoidBottom', PARAMS, locale);
    assert.notEqual(top, bottom, `${locale} 上/下半局文案相同，half 被丢了`);
  }
});

test('英文包不出现中文，繁中包不用简体「拒绝/记录」', () => {
  // 参数值本身可能是中文（erIssue 那两键插的是后端 msg），所以要拿纯 ASCII 的参数渲染，
  // 断的是模板而不是插进去的内容——第一版直接把中文 msg 插进英文句子里，自己把自己测红了。
  const ASCII_PARAMS = { inning: 3, count: 2, msg: 'SERVER_REASON' };
  for (const key of KEYS) {
    const en = t(key, ASCII_PARAMS, 'en');
    assert.ok(!/[\u4e00-\u9fff]/.test(en), `en / ${key} 中文残留：${en}`);
    const tw = t(key, ASCII_PARAMS, 'zh-TW');
    assert.ok(!/拒绝|记录|覆写|复核/.test(tw), `zh-TW / ${key} 简体残留：${tw}`);
    const cn = t(key, ASCII_PARAMS, 'zh-CN');
    assert.ok(!/拒絕|記錄|覆寫|複核/.test(cn), `zh-CN / ${key} 繁体重现：${cn}`);
  }
});

test('LiveGame.js 三句不再硬编码中文，且接的是这几个键', () => {
  const src = read('src/views/admin/LiveGame.js');
  assert.ok(!src.includes('位投手的人工 R/ER 覆写已失效，需重新复核'), '作废覆写那句仍硬编码');
  assert.ok(!src.includes('自责分判定未更新（第'), '判定未更新那句仍硬编码');
  assert.ok(!src.includes('条逐棒记录都不会送去重建）：记分与投手自责分可能对不上'), '定案没执行那句仍硬编码');
  for (const key of ['liveGame.settle.overrideVoidTop', 'liveGame.settle.overrideVoidBottom',
    'liveGame.settle.erIssueTop', 'liveGame.settle.erIssueBottom', 'liveGame.settle.untrustedSkipped']) {
    assert.ok(src.includes(`"${key}"`), `LiveGame.js 没引用 ${key}`);
  }
});

test('局数文案语序正确：说「第 3 局上」，不是改造前的「第 3上局」', () => {
  // 改造前模板是 `第 ${inning}${上|下}局定案`，实际吐「第 3上局定案」。这条钉住语序修好且不倒退。
  assert.match(t('liveGame.settle.overrideVoidTop', PARAMS, 'zh-CN'), /^第 3 局上定案：/);
  assert.match(t('liveGame.settle.overrideVoidBottom', PARAMS, 'zh-CN'), /^第 3 局下定案：/);
});
