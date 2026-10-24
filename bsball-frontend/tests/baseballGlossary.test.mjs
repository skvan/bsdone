// 前端单测：投手触身球术语键统一（#387）
// 运行：在 bsball-frontend 目录执行  node --test tests/*.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { normalizeGlossaryKey, getGlossaryEntry } from '../src/utils/baseballGlossary.js';

const LOCALES_DIR = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src/locales');

test('投手域 hbp 归一为 pitchHbp', () => {
  assert.equal(normalizeGlossaryKey('hbp', { domain: 'pitching', subject: 'pitcher' }), 'pitchHbp');
  assert.equal(getGlossaryEntry('hbp', { domain: 'pitching', subject: 'pitcher' }).key, 'pitchHbp');
});

test('打者域 hbp 保持原有 hbp 键', () => {
  assert.equal(normalizeGlossaryKey('hbp', { domain: 'batting', subject: 'batter' }), 'hbp');
  assert.equal(normalizeGlossaryKey('hbp'), 'hbp');
});

test('pitchHbp 键可直接识别', () => {
  assert.equal(normalizeGlossaryKey('pitchHbp'), 'pitchHbp');
  assert.equal(getGlossaryEntry('pitchHbp').key, 'pitchHbp');
});

test('旧的 hb 键不再被识别', () => {
  assert.equal(normalizeGlossaryKey('hb'), null);
});

test('五语种 glossary.term.pitchHbp 均存在且缩写为 HBP', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en', 'ja', 'ko']) {
    const data = JSON.parse(fs.readFileSync(path.join(LOCALES_DIR, `${locale}.json`), 'utf8'));
    assert.equal(data.glossary.term.pitchHbp?.shortLabel, 'HBP', locale);
    assert.equal(data.glossary.term.hb, undefined, locale);
  }
});
