// 语言包完整性 / 禁用词 / 简繁残留守护
// 运行：node --test tests/i18nLocaleParity.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const readJson = (rel) => JSON.parse(fs.readFileSync(new URL(rel, import.meta.url), 'utf8'));
const locales = ['zh-CN', 'zh-TW', 'en', 'ja', 'ko'];
const data = Object.fromEntries(locales.map((l) => [l, readJson(`../src/locales/${l}.json`)]));

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, path, out);
    else out[path] = value;
  }
  return out;
}

const flat = Object.fromEntries(locales.map((l) => [l, flatten(data[l])]));
const zhKeys = new Set(Object.keys(flat['zh-CN']));

// 英文版不投放微信分享/扫码入口，这些键允许缺失；B2 如需通用分享再补。
const EN_ALLOWED_MISSING = new Set([
  'shareLanding.text',
  'shareLanding.home',
  'shareLanding.learn',
  'shareLanding.close',
  'player.shareCard.wechatTip',
  'player.shareCard.wechatGuideTitle',
  'player.shareCard.wechatGuideBody',
  'player.shareCard.wechatGuideOk',
  'player.shareCard.qrCaption',
  'player.shareCard.qrBrand'
]);

// 英文包中允许出现的中文：反馈内容解析用的系统预留标记。
const EN_HAN_ALLOWED = new Set(['portalFeedback.ruleReservedMarker']);

const SIMPLIFIED_HINT = /[门问题实发让给现机关这没样种数据队垒运动训练纪录影视众后进过将来还么须应与为见观贝页风飞马鸟鱼龙龟齿龄场击纯独牵终结长对时总记满触滚统获较联员职责备术单双败胜负]/;
const ZH_CN_FORBIDDEN = /死球|完美比赛|偷垒|二安|三安|一安|牺短|牺飞|盗失|全垒打/;

test('英文语言包只允许缺微信分享相关键', () => {
  const missing = [...zhKeys].filter((k) => !(k in flat.en));
  const unexpected = missing.filter((k) => !EN_ALLOWED_MISSING.has(k));
  assert.deepEqual(unexpected, [], `英文缺失未登记键：${unexpected.join(', ')}`);
});

test('繁中语言包键完整', () => {
  const missing = [...zhKeys].filter((k) => !(k in flat['zh-TW']));
  assert.deepEqual(missing, [], `繁中缺失键：${missing.join(', ')}`);
});

test('英文语言包不出现中文（预留标记除外）', () => {
  const offenders = Object.entries(flat.en)
    .filter(([key, value]) => typeof value === 'string' && /[\u4e00-\u9fff]/.test(value))
    .filter(([key]) => !EN_HAN_ALLOWED.has(key));
  assert.deepEqual(offenders, [], `英文包中文残留：${offenders.map(([k, v]) => `${k}=${v}`).join('; ')}`);
});

test('繁中统计列与术语标签不含简体残留', () => {
  const offenders = [];
  for (const [key, value] of Object.entries(data['zh-TW'].stats.col)) {
    if (typeof value === 'string' && SIMPLIFIED_HINT.test(value)) offenders.push(`stats.col.${key}=${value}`);
  }
  for (const [key, term] of Object.entries(data['zh-TW'].glossary.term)) {
    if (SIMPLIFIED_HINT.test(term.fullName || '')) offenders.push(`glossary.${key}.fullName=${term.fullName}`);
    if (SIMPLIFIED_HINT.test(term.shortLabel || '')) offenders.push(`glossary.${key}.shortLabel=${term.shortLabel}`);
  }
  assert.deepEqual(offenders, [], `繁中简体残留：${offenders.join('; ')}`);
});

test('繁中术语定义不含简体残留', () => {
  const offenders = [];
  for (const [key, term] of Object.entries(data['zh-TW'].glossary.term)) {
    if (SIMPLIFIED_HINT.test(term.definition || '')) offenders.push(`glossary.${key}.definition=${term.definition}`);
  }
  assert.deepEqual(offenders, [], `繁中术语定义含简体：${offenders.join('; ')}`);
});

test('简中术语定义不含已废弃术语', () => {
  const offenders = [];
  for (const [key, term] of Object.entries(data['zh-CN'].glossary.term)) {
    if (ZH_CN_FORBIDDEN.test(term.definition || '')) offenders.push(`glossary.${key}.definition=${term.definition}`);
  }
  assert.deepEqual(offenders, [], `简中术语定义含废弃术语：${offenders.join('; ')}`);
});

test('简中统计列不含已废弃术语', () => {
  const offenders = [];
  for (const [key, value] of Object.entries(data['zh-CN'].stats.col)) {
    if (typeof value === 'string' && ZH_CN_FORBIDDEN.test(value)) offenders.push(`stats.col.${key}=${value}`);
  }
  for (const [key, term] of Object.entries(data['zh-CN'].glossary.term)) {
    if (ZH_CN_FORBIDDEN.test(term.fullName || '')) offenders.push(`glossary.${key}.fullName=${term.fullName}`);
    if (ZH_CN_FORBIDDEN.test(term.shortLabel || '')) offenders.push(`glossary.${key}.shortLabel=${term.shortLabel}`);
  }
  assert.deepEqual(offenders, [], `简中废弃术语：${offenders.join('; ')}`);
});

test('英文标准缩写按 MLB 口径落库', () => {
  assert.equal(data.en.stats.colEn.pg, 'SHO');
  assert.equal(data.en.stats.colEn.tcPct, 'FPCT');
  assert.equal(data.en.stats.colEn.pendingR, 'PEND');
  assert.equal(data.en.stats.colEn.goFo, 'GO/FO');
  assert.equal(data.en.glossary.term.pg.shortLabel, 'SHO');
  assert.equal(data.en.glossary.term.tcPct.shortLabel, 'FPCT');
  assert.equal(data.en.glossary.term.pitchBf.shortLabel, 'BF');
  assert.equal(data.en.glossary.term.kBb.shortLabel, 'K/BB');
});

test('路由与导航新增键在三语都有值', () => {
  const keys = [
    'nav.portalLoginEntry',
    'nav.accountProfile',
    'route.register',
    'route.accountLogin',
    'route.accountProfile',
    'route.forgotPassword',
    'route.playerClaimInvite',
    'gameDetail.fieldTips.pitcher.whip',
    'fieldSettings.gameDetailColDesc.pitcher.whip',
    'adminLogin.captchaAutoFetchIn'
  ];
  for (const locale of ['zh-CN', 'zh-TW', 'en']) {
    for (const key of keys) {
      assert.ok(flat[locale][key], `${locale} 缺少 ${key}`);
    }
  }
});
