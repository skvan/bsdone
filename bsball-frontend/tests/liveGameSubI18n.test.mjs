// 实时页换人模式英文（B3b-2b）：门禁提示、DH 取消/换位/换投确认、守备轮转、净变化摘要
// 运行：node --test tests/liveGameSubI18n.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { canCancelDh, summarizeTeamDiff } from '../src/utils/liveSubstitutionMode.js';
import { parseFieldRotation, buildFieldRotationMoves, formatFieldRotation } from '../src/utils/liveSubRotation.js';

const read = (rel) => fs.readFileSync(new URL(rel, import.meta.url), 'utf8');
const readJson = (rel) => JSON.parse(read(rel));
const liveGame = read('../src/views/admin/LiveGame.js');
const subMode = read('../src/utils/liveSubstitutionMode.js');
const rotation = read('../src/utils/liveSubRotation.js');

function flatten(obj, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, path, out);
    else out[path] = value;
  }
  return out;
}
const flat = Object.fromEntries(
  ['zh-CN', 'zh-TW', 'en'].map((locale) => [locale, flatten(readJson(`../src/locales/${locale}.json`))])
);

test('换人模式引用词条在简繁英三语齐全', () => {
  const keys = new Set();
  for (const source of [liveGame, subMode, rotation]) {
    for (const match of source.matchAll(/['"]?(liveGame\.sub\.[a-zA-Z0-9]+)['"]?/g)) keys.add(match[1]);
  }
  assert.ok(keys.size >= 45, `引用词条数量不足：${keys.size}`);
  for (const locale of Object.keys(flat)) {
    for (const key of keys) assert.ok(flat[locale][key], `${locale} 缺少 ${key}`);
  }
});

test('英文换人词条不残留中文', () => {
  const offenders = Object.entries(flat.en)
    .filter(([key, value]) => key.startsWith('liveGame.sub.') && typeof value === 'string' && /[\u4e00-\u9fff]/.test(value))
    .map(([key, value]) => `${key}=${value}`);
  assert.deepEqual(offenders, []);
});

test('显示文案可传翻译函数：DH 规则与净变化摘要', () => {
  const text = (key, params) => `EN:${key}${params ? ':' + JSON.stringify(params) : ''}`;
  const noDh = canCancelDh({ team: { lineup: [], starterIds: [] }, text });
  assert.equal(noDh.ok, false);
  assert.match(noDh.warning, /^EN:liveGame\.sub\.dhRuleNoDh/);

  const diff = {
    substitutions: [{ slot: 2, outPlayerId: 1, outName: 'A', outPosition: 'C', inPlayerId: 2, inName: 'B', inPosition: 'LF' }],
    positionChanges: [{ name: 'C', outPosition: 'SS', inPosition: '2B' }],
    pitchingChange: { outPlayerId: 3, outName: 'P1', inPlayerId: 4, inName: 'P2' }
  };
  const lines = summarizeTeamDiff(diff, 'Home', text);
  assert.ok(lines.every((line) => line.startsWith('EN:liveGame.sub.summary')));
  assert.ok(lines[0].includes('EN:liveGame.sub.summaryTeamPrefix'));
  assert.ok(lines[0].includes('EN:liveGame.sub.summarySlot'));
});

test('守备轮转错误可英文化', () => {
  const text = (key, params) => `EN:${key}${params ? ':' + JSON.stringify(params) : ''}`;
  const parsed = parseFieldRotation('', text);
  assert.equal(parsed.ok, false);
  assert.match(parsed.error, /^EN:liveGame\.sub\.rotationInputRequired/);

  const moves = buildFieldRotationMoves(['1', '2'], [{ id: 1, position: 'P' }], text);
  assert.equal(moves.ok, false);
  assert.match(moves.error, /^EN:liveGame\.sub\.rotationMissing/);
  assert.ok(moves.error.includes('liveGame.lineup.position'));

  const path = formatFieldRotation(['P', '1B'], (key) => flat.en[key] ?? key);
  assert.equal(path, 'Pitcher → First Base → Pitcher');
  assert.ok(!/[\u4e00-\u9fff]/.test(path), '英文轮转路径残留中文');
  assert.equal(formatFieldRotation(['P', '1B']), '投手 → 一垒手 → 投手');
});

test('轮转位置格式在简繁英均为本地写法', () => {
  assert.equal(flat['zh-CN']['liveGame.common.positionFormat'], '{digit}号位（{label}）');
  assert.equal(flat['zh-TW']['liveGame.common.positionFormat'], '{digit}號位（{label}）');
  assert.equal(flat.en['liveGame.common.positionFormat'], '{digit} ({label})');
});

test('中文默认行为保持（不传翻译函数）', () => {
  const parsed = parseFieldRotation('');
  assert.equal(parsed.error, '请输入守备轮转，如 1341（首尾相同形成闭环）');
  const noDh = canCancelDh({ team: { lineup: [], starterIds: [] } });
  assert.equal(noDh.warning, '当前打线没有 DH。');
});
