// 比赛复核英文（B3b-3a）：复核抽屉、自责分判定、投手 R/ER 覆写
// 运行：node --test tests/liveGameReviewI18n.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));
const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en'].map((l) => [l, readJson(`../src/locales/${l}.json`)])
);
const drawer = read('../src/components/admin/LiveGameEarnedRunDrawer.vue');
const panel = read('../src/components/admin/GameEarnedRunReviewPanel.vue');
const fielding = read('../src/components/admin/LiveGameFieldingAdjustmentPanel.vue');
const sources = [drawer, panel, fielding];

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, path, out);
    else out[path] = value;
  }
  return out;
}
const flat = Object.fromEntries(Object.entries(locales).map(([l, d]) => [l, flatten(d)]));

test('复核面板引用词条在简繁英三语齐全', () => {
  const keys = new Set();
  for (const source of sources) {
    for (const match of source.matchAll(/['"](liveGame\.review\.[a-zA-Z0-9]+)['"]/g)) keys.add(match[1]);
  }
  assert.ok(keys.size >= 85, `引用词条数量不足：${keys.size}`);
  for (const locale of Object.keys(locales)) {
    for (const key of keys) assert.ok(flat[locale][key], `${locale} 缺少 ${key}`);
  }
});

test('英文复核词条不残留中文', () => {
  const offenders = Object.entries(flat.en)
    .filter(([key, value]) => key.startsWith('liveGame.review.') && typeof value === 'string' && /[\u4e00-\u9fff]/.test(value))
    .map(([key, value]) => `${key}=${value}`);
  assert.deepEqual(offenders, []);
});

test('复核抽屉与面板不再硬编码主要中文', () => {
  for (const text of ['比赛复核', '比赛进行中', 'ER 复核', '守备补记', '自责分判定', '投手 R/ER', '暂无判定记录', '确认覆写', '人工覆写']) {
    assert.ok(!drawer.includes(`>${text}<`) && !drawer.includes(`"${text}"`), `抽屉仍硬编码：${text}`);
    assert.ok(!panel.includes(`>${text}<`) && !panel.includes(`"${text}"`), `面板仍硬编码：${text}`);
  }
  assert.ok(drawer.includes("lt('liveGame.review.drawerTitle')"), '抽屉标题未走 i18n');
  assert.ok(panel.includes("lt('liveGame.review.tabDecisions')"), '判定页签未走 i18n');
  assert.ok(panel.includes("lt('liveGame.review.tabPitchers')"), '投手 R/ER 页签未走 i18n');
  assert.ok(panel.includes("lt('liveGame.review.pitcherOverrideTitle')"), '投手覆写弹窗未走 i18n');
});

test('状态、原因与队伍标记走 i18n', () => {
  for (const key of ['decisionEarned', 'decisionUnearned', 'decisionPending']) {
    assert.ok(panel.includes(`liveGame.review.${key}`), `状态未接入 ${key}`);
  }
  for (const key of ['reasonErrorReached', 'reasonPassedBall', 'reasonTieBreak', 'reasonManual']) {
    assert.ok(panel.includes(`liveGame.review.${key}`), `非自责原因未接入 ${key}`);
  }
  assert.ok(panel.includes("lt('liveGame.team.homeShort')"), '主队标记未走 i18n');
  assert.ok(panel.includes("lt('liveGame.team.awayShort')"), '客队标记未走 i18n');
});

test('守备补记面板英文词条与状态', () => {
  assert.ok(fielding.includes("lt('liveGame.review.fieldingBlockedTitle')"), '未开赛提示未走 i18n');
  assert.ok(fielding.includes("lt('liveGame.review.creditAssist')"), '补记助杀按钮未走 i18n');
  assert.ok(fielding.includes("lt('liveGame.review.noCredits')"), '空记录文案未走 i18n');
  for (const key of ['statusApproved', 'statusRevoked', 'statusInvalidated']) {
    assert.ok(fielding.includes(`liveGame.review.${key}`), `状态未接入 ${key}`);
  }
  assert.ok(!fielding.includes('加载守备补记失败'), '守备补记仍硬编码错误提示');
  assert.ok(fielding.includes('candidateInningLabel(candidate)'), '候选局次未本地化');
  assert.ok(fielding.includes('candidateBaseLabel(candidate)'), '候选垒况未本地化');
  for (const key of ['basesEmpty', 'basesFull', 'baseOccupied', 'basesOccupied']) {
    assert.ok(flat.en[`liveGame.review.${key}`], `英文缺少 ${key}`);
  }
});
