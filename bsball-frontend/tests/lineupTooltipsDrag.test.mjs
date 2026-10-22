// 静态断言：指标术语提示 + 三区拖拽互通
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const glossary = read('../src/utils/baseballGlossary.js');
const board = read('../src/views/admin/LiveGameLineupBoard.vue');
const boardState = read('../src/composables/useLineupBoard.js');

const locales = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en', 'ja', 'ko'].map((name) => [name, JSON.parse(read(`../src/locales/${name}.json`))])
);

const newTerms = ['io', 'pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo', 'unearnedR', 'pendingR', 'catcherSb'];
const newBattingTips = ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io'];
const newPitchingTips = ['pitchDoubles', 'pitchTriples', 'pitchSh', 'pitchSf', 'pitchIo'];

test('术语表登记新增指标并带公式/定义', () => {
  for (const key of newTerms) assert.ok(glossary.includes(`'${key}'`), `GLOSSARY_KEYS 缺少 ${key}`);
  assert.ok(glossary.includes("unearnedR: 'R-ER-PENDING'"), 'UER 公式缺失');
  assert.ok(glossary.includes("pitchibb: 'ibb'") && glossary.includes("pitchhbp: 'hbp'"), '球队列表头别名缺失');
});

test('五语种：术语与比赛详情新列提示齐全', () => {
  for (const [name, dict] of Object.entries(locales)) {
    for (const key of newTerms) {
      const term = dict.glossary?.term?.[key];
      assert.ok(term?.fullName && term?.definition, `${name} 缺少术语 ${key}`);
    }
    for (const key of newBattingTips) {
      assert.ok(dict.gameDetail?.fieldTips?.batter?.[key], `${name} 比赛详情打者提示缺少 ${key}`);
    }
    for (const key of newPitchingTips) {
      assert.ok(dict.gameDetail?.fieldTips?.pitcher?.[key], `${name} 比赛详情投手提示缺少 ${key}`);
    }
    assert.ok(String(dict.player?.seasonColTip?.pitchAb || '').includes('BB'), `${name} pitchAb 提示未说明 BB 含 IBB`);
  }
});

test('场→池拖拽：场上球员与仅守备投手直接回池', () => {
  assert.ok(boardState.includes('function sendToPool'), 'useLineupBoard 缺少 sendToPool');
  assert.ok(boardState.includes('sendToPool,'), 'sendToPool 未导出');
  assert.ok(board.includes('b.sendToPool(player.id)'), 'handleDrop pool 分支未调用 sendToPool');
  assert.ok(board.includes('inRows || fieldingPitcher'), 'pool 分支未覆盖场上球员/仅守备投手');
});
