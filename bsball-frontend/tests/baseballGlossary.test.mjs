// 前端单测：触身球术语键与缩写统一（#387）
// 运行：在 bsball-frontend 目录执行  node --test tests/*.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { normalizeGlossaryKey, getGlossaryEntry } from '../src/utils/baseballGlossary.js';

const LOCALES_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src/locales');

test('pitchHbp 复用 hbp 术语（球队列表头别名保留）', () => {
  assert.equal(normalizeGlossaryKey('pitchHbp'), 'hbp');
  assert.equal(getGlossaryEntry('pitchHbp').key, 'hbp');
  assert.equal(normalizeGlossaryKey('hbp', { domain: 'pitching', subject: 'pitcher' }), 'hbp');
  assert.equal(normalizeGlossaryKey('hbp'), 'hbp');
});

test('旧的 hb 键不再被识别', () => {
  assert.equal(normalizeGlossaryKey('hb'), null);
});

test('五语种 glossary.term.hb 已移除', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en', 'ja', 'ko']) {
    const data = JSON.parse(fs.readFileSync(path.join(LOCALES_DIR, locale + '.json'), 'utf8'));
    assert.equal(data.glossary.term.hb, undefined, locale);
    assert.ok(data.glossary.term.hbp, locale);
  }
});

test('PBP 浮层文案与事件映射统一使用 HBP', () => {
  const liveGame = fs.readFileSync(
    path.join(path.dirname(fileURLToPath(import.meta.url)), '../src/views/admin/LiveGame.js'),
    'utf8'
  );
  assert.equal(liveGame.includes('B/K/HP'), false, 'PBP 浮层仍残留 B/K/HP');
  assert.ok(liveGame.includes('B/K/HBP'), 'PBP 浮层未改为 B/K/HBP');
  assert.match(liveGame, /"pitch:HBP":"hbp"/, 'PBP 事件映射未改为 hbp');
});
