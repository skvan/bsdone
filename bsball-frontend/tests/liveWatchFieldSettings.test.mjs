// 前端单测：观赛页独立列设置存储（#211）
// 运行：在 bsball-frontend 目录执行  node --test
import test from 'node:test';
import assert from 'node:assert/strict';

// 导入被测模块前注入最小 localStorage 桩（模块内仅在调用时访问 window）
const store = new Map();
globalThis.window = {
  localStorage: {
    getItem: (key) => (store.has(key) ? store.get(key) : null),
    setItem: (key, value) => { store.set(key, String(value)); },
    removeItem: (key) => { store.delete(key); }
  }
};

const {
  LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY,
  FIELD_SETTINGS_STORAGE_KEY,
  readWatchFieldSettings,
  writeWatchFieldSettings,
  readFieldSettings,
  writeFieldSettings
} = await import('../src/utils/gameDetailFieldSettingsStorage.js');

const DEFAULTS = {
  defaultBatterFields: [
    { key: 'pa', label: 'PA', visible: true },
    { key: 'ab', label: 'AB', visible: true },
    { key: 'r', label: 'R', visible: true },
    { key: 'h', label: 'H', visible: true },
    { key: 'doubles', label: '2B', visible: true },
    { key: 'triples', label: '3B', visible: true },
    { key: 'hr', label: 'HR', visible: true },
    { key: 'shSf', label: 'SH+SF', visible: true },
    { key: 'rbi', label: 'RBI', visible: true },
    { key: 'bbHp', label: 'BB+HP', visible: true },
    { key: 'so', label: 'SO', visible: false },
    { key: 'sb', label: 'SB', visible: false }
  ],
  defaultPitcherFields: [
    { key: 'ip', label: 'IP', visible: true },
    { key: 'pitchR', label: 'R', visible: true },
    { key: 'er', label: 'ER', visible: true },
    { key: 'pitchH', label: 'H', visible: true },
    { key: 'pitchBbHp', label: 'BB+HP', visible: true },
    { key: 'pitchSo', label: 'SO', visible: true },
    { key: 'wp', label: 'WP', visible: false },
    { key: 'np', label: 'NP', visible: false }
  ],
  defaultDecimalPlaces: 2,
  defaultRateDisplayStyle: 'leadingZero',
  defaultShowTrailingZeros: false
};

function makeState(overrides = {}) {
  return {
    batterFields: DEFAULTS.defaultBatterFields.map((item) => ({ ...item })),
    pitcherFields: DEFAULTS.defaultPitcherFields.map((item) => ({ ...item })),
    decimalPlaces: 2,
    rateDisplayStyle: 'leadingZero',
    showTrailingZeros: false,
    ...overrides
  };
}

test('未设置任何存储时返回默认值，且不创建观赛键（现状链的迁移落盘行为保留）', () => {
  store.clear();
  const result = readWatchFieldSettings(DEFAULTS);
  assert.equal(result.batterFields.length, DEFAULTS.defaultBatterFields.length);
  assert.equal(result.pitcherFields.length, DEFAULTS.defaultPitcherFields.length);
  assert.equal(result.decimalPlaces, 2);
  assert.equal(store.has(LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY), false);
  assert.equal(store.has(FIELD_SETTINGS_STORAGE_KEY), true);
});

test('无观赛键时跟随管理端设置（回退现状链，不创建观赛键）', () => {
  store.clear();
  writeFieldSettings(makeState({ decimalPlaces: 3, rateDisplayStyle: 'dot', showTrailingZeros: true }));
  const result = readWatchFieldSettings(DEFAULTS);
  assert.equal(result.decimalPlaces, 3);
  assert.equal(result.rateDisplayStyle, 'dot');
  assert.equal(result.showTrailingZeros, true);
  assert.equal(store.has(LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY), false);
});

test('观赛键优先于管理端键', () => {
  store.clear();
  writeFieldSettings(makeState({ decimalPlaces: 3, rateDisplayStyle: 'dot', showTrailingZeros: true }));
  writeWatchFieldSettings(makeState({ decimalPlaces: 2, rateDisplayStyle: 'leadingZero', showTrailingZeros: false }));
  const result = readWatchFieldSettings(DEFAULTS);
  assert.equal(result.decimalPlaces, 2);
  assert.equal(result.rateDisplayStyle, 'leadingZero');
  assert.equal(result.showTrailingZeros, false);
});

test('写观赛键不修改管理端键，管理端读取不受影响', () => {
  store.clear();
  writeFieldSettings(makeState({ decimalPlaces: 3 }));
  const adminBefore = store.get(FIELD_SETTINGS_STORAGE_KEY);
  writeWatchFieldSettings(makeState({ decimalPlaces: 2 }));
  assert.equal(store.get(FIELD_SETTINGS_STORAGE_KEY), adminBefore);
  assert.equal(readFieldSettings(DEFAULTS).decimalPlaces, 3);
});

test('观赛键损坏或结构非法时回退到管理端值', () => {
  store.clear();
  writeFieldSettings(makeState({ decimalPlaces: 3 }));
  store.set(LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY, '{broken-json');
  assert.equal(readWatchFieldSettings(DEFAULTS).decimalPlaces, 3);
  store.set(LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY, JSON.stringify({ v: 1, batterFields: 'x', pitcherFields: [] }));
  assert.equal(readWatchFieldSettings(DEFAULTS).decimalPlaces, 3);
});

test('观赛键可见列少于 6 时按序补足保底', () => {
  store.clear();
  writeWatchFieldSettings(makeState({
    batterFields: DEFAULTS.defaultBatterFields.map((item) => ({ ...item, visible: false })),
    pitcherFields: DEFAULTS.defaultPitcherFields.map((item) => ({ ...item, visible: false }))
  }));
  const result = readWatchFieldSettings(DEFAULTS);
  assert.equal(result.batterFields.filter((item) => item.visible).length, 6);
  assert.equal(result.pitcherFields.filter((item) => item.visible).length, 6);
});
