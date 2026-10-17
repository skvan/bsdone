import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (p) => readFileSync(path.join(root, p), 'utf8');

test('request.js：埋点来源提取接线', () => {
  const src = read('src/api/request.js');
  assert.match(src, /from '\.\.\/utils\/shareSource'/);
  assert.match(src, /extractShareSource\(typeof window/);
  assert.match(src, /source \? \{ path, source \} : \{ path \}/);
});
