// 比赛详情打者列：新增 1B/IPHR、旧配置迁移与安打派生
// 运行：在 bsball-frontend 目录执行  node --test
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const store = new Map();
globalThis.window = {
  localStorage: {
    getItem: (key) => (store.has(key) ? store.get(key) : null),
    setItem: (key, value) => { store.set(key, String(value)); },
    removeItem: (key) => { store.delete(key); }
  }
};

const {
  DEFAULT_BATTER_FIELDS,
  writeFieldSettings,
  writeWatchFieldSettings,
  readFieldSettings,
  readWatchFieldSettings,
  buildInitialFieldSettings
} = await import('../src/utils/gameDetailFieldSettingsStorage.js');

const { defaultDisplaySettings, saveDisplaySettings } = await import('../src/utils/statsDisplayStorage.js');

const { resolveSingles, sumHitTypes } = await import('../src/utils/battingHits.js');

const PITCHER_FIELDS = [
  { key: 'ip', label: 'IP', visible: true },
  { key: 'pitchR', label: 'R', visible: true },
  { key: 'er', label: 'ER', visible: true },
  { key: 'pitchH', label: 'H', visible: true },
  { key: 'pitchSo', label: 'SO', visible: true },
  { key: 'era', label: 'ERA', visible: true }
];

const FIELDING_FIELDS = [
  { key: 'fieldingGs', label: 'GS', visible: true },
  { key: 'po', label: 'PO', visible: true },
  { key: 'a', label: 'A', visible: true }
];

const DEFAULTS = {
  defaultBatterFields: DEFAULT_BATTER_FIELDS,
  defaultPitcherFields: PITCHER_FIELDS,
  defaultFieldingFields: FIELDING_FIELDS,
  defaultDecimalPlaces: 2,
  defaultRateDisplayStyle: 'leadingZero',
  defaultShowTrailingZeros: true
};

function oldBatterFields() {
  return [
    { key: 'pa', label: 'PA', visible: true },
    { key: 'ab', label: 'AB', visible: true },
    { key: 'r', label: 'R', visible: true },
    { key: 'h', label: 'H', visible: true },
    { key: 'doubles', label: '2B', visible: true },
    { key: 'triples', label: '3B', visible: true },
    { key: 'hr', label: 'HR', visible: true },
    { key: 'rbi', label: 'RBI', visible: true },
    { key: 'bb', label: 'BB', visible: false },
    { key: 'so', label: 'SO', visible: true },
    { key: 'sb', label: 'SB', visible: true },
    { key: 'cs', label: 'CS', visible: true }
  ];
}

function makeState(batterFields) {
  return {
    batterFields,
    pitcherFields: PITCHER_FIELDS.map((item) => ({ ...item })),
    fieldingFields: FIELDING_FIELDS.map((item) => ({ ...item })),
    decimalPlaces: 2,
    rateDisplayStyle: 'leadingZero',
    showTrailingZeros: true
  };
}

test('默认打者列在 H 后提供 1B、在 HR 后提供 IPHR', () => {
  const keys = DEFAULT_BATTER_FIELDS.map((item) => item.key);
  assert.equal(keys[keys.indexOf('h') + 1], 'singles');
  assert.equal(keys[keys.indexOf('hr') + 1], 'insideParkHr');
  for (const [key, label] of [['singles', '1B'], ['insideParkHr', 'IPHR']]) {
    const field = DEFAULT_BATTER_FIELDS.find((item) => item.key === key);
    assert.equal(field?.label, label);
    assert.equal(field?.visible, true);
  }
});

test('旧配置迁移：新列插到 H/HR 后，已有列顺序与可见性不变', () => {
  store.clear();
  writeFieldSettings(makeState(oldBatterFields()));

  const result = readFieldSettings(DEFAULTS);
  const keys = result.batterFields.map((item) => item.key);
  assert.equal(keys[keys.indexOf('h') + 1], 'singles');
  assert.equal(keys[keys.indexOf('hr') + 1], 'insideParkHr');
  assert.ok(keys.indexOf('rbi') < keys.indexOf('bb'));
  assert.ok(keys.indexOf('bb') < keys.indexOf('so'));
  assert.equal(result.batterFields.find((item) => item.key === 'bb')?.visible, false);
});

test('已保存过 1B/IPHR 时迁移不重复也不移动用户位置', () => {
  store.clear();
  const saved = DEFAULT_BATTER_FIELDS.map((item) => ({ ...item }));
  const singles = saved.splice(saved.findIndex((item) => item.key === 'singles'), 1)[0];
  const insideParkHr = saved.splice(saved.findIndex((item) => item.key === 'insideParkHr'), 1)[0];
  saved.push(singles, insideParkHr);
  writeFieldSettings(makeState(saved));

  const keys = readFieldSettings(DEFAULTS).batterFields.map((item) => item.key);
  assert.equal(keys.filter((key) => key === 'singles').length, 1);
  assert.equal(keys.filter((key) => key === 'insideParkHr').length, 1);
  assert.equal(keys[keys.length - 2], 'singles');
  assert.equal(keys[keys.length - 1], 'insideParkHr');
});

test('旧配置连锚点列（H/HR）都缺时：补列仍各出现一次且紧随 H/HR', () => {
  store.clear();
  const saved = oldBatterFields().filter((item) => item.key !== 'h' && item.key !== 'hr');
  writeFieldSettings(makeState(saved));

  const keys = readFieldSettings(DEFAULTS).batterFields.map((item) => item.key);
  assert.equal(keys.filter((key) => key === 'singles').length, 1);
  assert.equal(keys.filter((key) => key === 'insideParkHr').length, 1);
  assert.equal(keys[keys.indexOf('h') + 1], 'singles');
  assert.equal(keys[keys.indexOf('hr') + 1], 'insideParkHr');
});

test('观赛页独立配置同样迁移出新列', () => {
  store.clear();
  writeWatchFieldSettings(makeState(oldBatterFields()));

  const keys = readWatchFieldSettings(DEFAULTS).batterFields.map((item) => item.key);
  assert.equal(keys[keys.indexOf('h') + 1], 'singles');
  assert.equal(keys[keys.indexOf('hr') + 1], 'insideParkHr');
});

test('新列隐藏后保存与回读仍保持隐藏（管理端/观赛页）', () => {
  const hidden = DEFAULT_BATTER_FIELDS.map((item) =>
    item.key === 'singles' || item.key === 'insideParkHr' ? { ...item, visible: false } : { ...item }
  );

  store.clear();
  writeFieldSettings(makeState(hidden));
  const admin = readFieldSettings(DEFAULTS).batterFields;
  assert.equal(admin.find((item) => item.key === 'singles')?.visible, false);
  assert.equal(admin.find((item) => item.key === 'insideParkHr')?.visible, false);
  assert.equal(admin.filter((item) => item.key === 'singles').length, 1);

  store.clear();
  writeWatchFieldSettings(makeState(hidden));
  const watch = readWatchFieldSettings(DEFAULTS).batterFields;
  assert.equal(watch.find((item) => item.key === 'singles')?.visible, false);
  assert.equal(watch.find((item) => item.key === 'insideParkHr')?.visible, false);
  assert.equal(watch.filter((item) => item.key === 'insideParkHr').length, 1);
});

test('旧版分散键迁移：同样补列并清理旧键', () => {
  store.clear();
  store.set('bsball.admin.gameDetail.batterFields', JSON.stringify(oldBatterFields()));

  const keys = readFieldSettings(DEFAULTS).batterFields.map((item) => item.key);
  assert.equal(keys[keys.indexOf('h') + 1], 'singles');
  assert.equal(keys[keys.indexOf('hr') + 1], 'insideParkHr');
  assert.equal(store.has('bsball.admin.gameDetail.batterFields'), false);
});

test('门户缓存（display settings）里的旧打者列同样补列', () => {
  store.clear();
  const display = defaultDisplaySettings();
  display.detailPage.batterFields = oldBatterFields();
  saveDisplaySettings(display);

  const keys = buildInitialFieldSettings({ ...DEFAULTS, tenantCode: '' }).batterFields.map((item) => item.key);
  assert.equal(keys[keys.indexOf('h') + 1], 'singles');
  assert.equal(keys[keys.indexOf('hr') + 1], 'insideParkHr');
});

test('1B 由安打减去 2B/3B/HR/IPHR 后逐行派生，且不小于 0', () => {
  assert.equal(resolveSingles({ h: 5, doubles: 1, triples: 0, hr: 1, insideParkHr: 1 }), 2);
  assert.equal(resolveSingles({ h: 2 }), 2);
  assert.equal(resolveSingles({ h: 1, doubles: 1, hr: 1 }), 0);
  assert.equal(resolveSingles(null), 0);
  assert.equal(resolveSingles({ h: '5', doubles: '1', triples: '1', hr: '1', insideParkHr: '1' }), 1);
});

test('合计口径：HR 不含 IPHR，1B/2B/3B/HR/IPHR 各自独立汇总', () => {
  const rows = [
    { h: 5, doubles: 1, triples: 0, hr: 1, insideParkHr: 1 },
    { h: 2, doubles: 0, triples: 0, hr: 0, insideParkHr: 0 }
  ];
  assert.deepEqual(sumHitTypes(rows), {
    singles: 4,
    doubles: 1,
    triples: 0,
    hr: 1,
    insideParkHr: 1
  });
});

test('合计口径：1B 按逐行派生值累加（等于各行单元格之和）', () => {
  const rows = [
    { h: 1, doubles: 1, hr: 1 },
    { h: 5, doubles: 1 }
  ];
  // 第 1 行 1B 夹到 0，合计为 0+4；不是 H 合计 6 减长打合计 3 的 3
  assert.deepEqual(sumHitTypes(rows), {
    singles: 4,
    doubles: 2,
    triples: 0,
    hr: 1,
    insideParkHr: 0
  });
});

test('五语种：表头提示包含新增 1B/IPHR 列', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en', 'ja', 'ko']) {
    const dict = JSON.parse(fs.readFileSync(new URL(`../src/locales/${locale}.json`, import.meta.url), 'utf8'));
    const tips = dict.gameDetail?.fieldTips?.batter;
    for (const key of ['singles', 'insideParkHr']) {
      assert.ok(tips?.[key], `${locale} 比赛详情打者列提示缺少 ${key}`);
    }
  }
});
