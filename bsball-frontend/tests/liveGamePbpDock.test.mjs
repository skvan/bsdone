// 实时录入·球场与日志分栏纯函数单测（2026-11-12）
// 运行：node --test tests/liveGamePbpDock.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  PBP_DOCK_KEY,
  PBP_HORIZONTAL_KEY,
  PBP_LEGACY_RATIO_KEY,
  PBP_VERTICAL_KEY,
  clampPbpSize,
  defaultPbpSize,
  effectivePbpDock,
  isHorizontalDock,
  normalizePbpDock,
  persistPbpDock,
  persistPbpSize,
  readLegacyPbpHeight,
  readPbpDock,
  readPbpSize,
} from '../src/utils/liveGamePbpDock.js';

const mockStorage = (initial = {}) => {
  const data = new Map(Object.entries(initial));
  return {
    getItem: (key) => (data.has(key) ? data.get(key) : null),
    setItem: (key, value) => data.set(key, String(value)),
    data,
  };
};

test('吸附边归一化：四向合法，非法回落下方', () => {
  for (const dock of ['top', 'bottom', 'left', 'right']) assert.equal(normalizePbpDock(dock), dock);
  assert.equal(normalizePbpDock(' LEFT '), 'left');
  assert.equal(normalizePbpDock('side'), 'bottom');
  assert.equal(normalizePbpDock(null), 'bottom');
  assert.equal(isHorizontalDock('left'), true);
  assert.equal(isHorizontalDock('right'), true);
  assert.equal(isHorizontalDock('top'), false);
});

test('手机宽度：左右吸附回落下方，上下保持', () => {
  assert.equal(effectivePbpDock('left', 600), 'bottom');
  assert.equal(effectivePbpDock('right', 767), 'bottom');
  assert.equal(effectivePbpDock('left', 768), 'left');
  assert.equal(effectivePbpDock('top', 600), 'top');
  assert.equal(effectivePbpDock('bottom', 1400), 'bottom');
});

test('上下吸附尺寸夹取：最小 120，最大 55% 且球场至少 250', () => {
  assert.equal(clampPbpSize('bottom', 80, { height: 740 }), 120);
  assert.equal(clampPbpSize('bottom', 500, { height: 740 }), 407);
  assert.equal(clampPbpSize('top', 300, { height: 740 }), 300);
  assert.equal(clampPbpSize('bottom', 300, { height: 0 }), 120);
});

test('左右吸附尺寸夹取：最小 260，最大 42% 且球场至少 430', () => {
  assert.equal(clampPbpSize('left', 100, { width: 1400 }), 260);
  assert.equal(clampPbpSize('left', 800, { width: 1400 }), 588);
  assert.equal(clampPbpSize('right', 320, { width: 1400 }), 320);
  assert.equal(clampPbpSize('left', 320, { width: 0 }), 260);
});

test('默认尺寸：左右 320；上下迁移旧比例或 30%', () => {
  assert.equal(defaultPbpSize('left', { width: 1400, height: 740 }), 320);
  assert.equal(defaultPbpSize('bottom', { width: 1400, height: 740 }), 222);
  assert.equal(defaultPbpSize('bottom', { width: 1400, height: 740, legacyHeight: 270 }), 270);
  assert.equal(defaultPbpSize('top', { width: 1400, height: 740, legacyHeight: 900 }), 407);
});

test('持久化：读写吸附边与两种尺寸键', () => {
  const storage = mockStorage();
  persistPbpDock('left', storage);
  persistPbpSize('left', 356, storage);
  persistPbpSize('top', 248, storage);
  assert.equal(storage.data.get(PBP_DOCK_KEY), 'left');
  assert.equal(storage.data.get(PBP_HORIZONTAL_KEY), '356');
  assert.equal(storage.data.get(PBP_VERTICAL_KEY), '248');
  assert.equal(readPbpDock(storage), 'left');
  assert.equal(readPbpSize('left', storage), 356);
  assert.equal(readPbpSize('bottom', storage), 248);
  assert.equal(readPbpDock(mockStorage()), 'bottom');
});

test('旧高度比例迁移：viewport × ratio；无效值返回 0', () => {
  const storage = mockStorage({ [PBP_LEGACY_RATIO_KEY]: '0.3' });
  assert.equal(readLegacyPbpHeight(storage, 900), 270);
  assert.equal(readLegacyPbpHeight(storage, 0), 0);
  assert.equal(readLegacyPbpHeight(mockStorage({ [PBP_LEGACY_RATIO_KEY]: 'x' }), 900), 0);
});
