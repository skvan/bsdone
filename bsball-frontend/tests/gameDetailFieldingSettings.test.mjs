// 数据项调整：守备分组、SH/SF 拆分与最新字段接线
// 运行：node --test tests/gameDetailFieldingSettings.test.mjs
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {
  DEFAULT_BATTER_FIELDS,
  DEFAULT_FIELDING_FIELDS,
  DEFAULT_PITCHER_FIELDS,
  readFieldSettings
} from '../src/utils/gameDetailFieldSettingsStorage.js';
import { defaultDisplaySettings, readDisplaySettings, saveDisplaySettings } from '../src/utils/statsDisplayStorage.js';

test('默认打者字段拆分 SH/SF 并包含 GDP/IBB/HB/IO', () => {
  const keys = DEFAULT_BATTER_FIELDS.map((field) => field.key);
  for (const key of ['sh', 'sf', 'gdp', 'ibb', 'hbp', 'io', 'bb']) {
    assert.ok(keys.includes(key), `缺少打者字段 ${key}`);
  }
  assert.ok(!keys.includes('shSf'), '不应再使用 SH+SF 合并字段');
});

test('默认守备字段包含 GS/PO/A/E/DP/INN/PB/SB/CS', () => {
  assert.deepEqual(DEFAULT_FIELDING_FIELDS.map((field) => field.key), [
    'fieldingGs', 'po', 'a', 'e', 'dp', 'defInn', 'pb', 'catcherSb', 'catcherCs'
  ]);
});

test('字段设置读取：无存储时返回完整守备字段', () => {
  const settings = readFieldSettings({
    defaultBatterFields: DEFAULT_BATTER_FIELDS,
    defaultPitcherFields: DEFAULT_PITCHER_FIELDS,
    defaultFieldingFields: DEFAULT_FIELDING_FIELDS,
    defaultDecimalPlaces: 2,
    defaultRateDisplayStyle: 'leadingZero',
    defaultShowTrailingZeros: true
  });
  assert.equal(settings.fieldingFields.length, DEFAULT_FIELDING_FIELDS.length);
  assert.ok(settings.fieldingFields.every((field) => field.visible));
  assert.equal(settings.fieldingFields.filter((field) => field.visible).length >= 3, true);
});

test('数据项调整抽屉：守备分组与保存载荷', () => {
  const src = fs.readFileSync(new URL('../src/components/admin/GameDetailFieldSettingsDrawer.js', import.meta.url), 'utf8');
  assert.match(src, /fieldingItems/, '接收守备字段');
  assert.match(src, /defaultFieldingItems/, '接收守备默认字段');
  assert.match(src, /fieldSettings\.fielding/, '守备标签');
  assert.match(src, /fieldingItems:A\(pf\.value\)/, '保存守备字段');
});

test('门户 display settings 持久化不丢 fieldingFields', () => {
  const store = new Map();
  globalThis.window = {
    localStorage: {
      getItem: (key) => store.get(key) ?? null,
      setItem: (key, value) => store.set(key, value),
      removeItem: (key) => store.delete(key)
    }
  };
  try {
    const settings = defaultDisplaySettings();
    settings.detailPage.fieldingFields = [{ key: 'po', label: 'PO', visible: false }];
    saveDisplaySettings(settings);
    assert.deepEqual(readDisplaySettings().detailPage.fieldingFields, [
      { key: 'po', label: 'PO', visible: false }
    ]);
  } finally {
    delete globalThis.window;
  }
});

test('五语种：守备标签与新字段说明齐全', () => {
  for (const locale of ['zh-CN', 'zh-TW', 'en', 'ja', 'ko']) {
    const data = JSON.parse(fs.readFileSync(new URL(`../src/locales/${locale}.json`, import.meta.url), 'utf8'));
    const fieldSettings = data.fieldSettings;
    assert.equal(typeof fieldSettings.fielding, 'string', `${locale} 缺少守备标签`);
    const desc = fieldSettings.gameDetailColDesc;
    for (const key of ['gdp', 'sh', 'sf', 'bb', 'ibb', 'hbp', 'io']) {
      assert.equal(typeof desc.batter[key], 'string', `${locale} 缺少打者说明 ${key}`);
    }
    for (const key of DEFAULT_FIELDING_FIELDS.map((field) => field.key)) {
      assert.equal(typeof desc.fielding[key], 'string', `${locale} 缺少守备说明 ${key}`);
    }
  }
});

test('比赛详情与观赛页接线守备字段', () => {
  const detail = fs.readFileSync(new URL('../src/views/admin/GameDetail.js', import.meta.url), 'utf8');
  const watch = fs.readFileSync(new URL('../src/views/admin/LiveGameWatch.js', import.meta.url), 'utf8');
  const portal = fs.readFileSync(new URL('../src/views/portal/PortalGameDetail.js', import.meta.url), 'utf8');
  for (const [name, src] of [['GameDetail', detail], ['LiveGameWatch', watch]]) {
    assert.match(src, /"fielding-fields"/, `${name} 未传守备字段到比赛详情`);
    assert.match(src, /"fielding-items"/, `${name} 未传守备字段到数据项抽屉`);
  }
  assert.match(portal, /"fielding-fields"/, 'PortalGameDetail 未传守备字段到比赛详情');
  const content = fs.readFileSync(new URL('../src/components/admin/GameDetailContent.js', import.meta.url), 'utf8');
  assert.match(content, /fieldingFields/, 'GameDetailContent 缺少 fieldingFields prop');
  assert.match(content, /fields:c\.fieldingFields/, '守备表未接收字段设置');
});
