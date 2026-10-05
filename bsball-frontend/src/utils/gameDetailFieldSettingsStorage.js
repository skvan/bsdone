// 比赛详情字段设置存储——行为保真移植自编译产物 chunk gameDetailFieldSettingsStorage-DkSFgXpY.js
// 逐字对应：H=默认投手字段、m=存储键、F=版本、c=旧版键组、u=合并、d=保底可见、g/A/I/E=旧版读取、
//   w=旧版清理、T=解析、v/S=校验、L=构建、R=保存 detailPage、Z=保存 valueControl、B=写入、_=读取+迁移
import { readDisplaySettings, saveDisplaySettings } from './statsDisplayStorage.js';
import { isPortalGuideDismissed } from './portalGuide.js';

export const DEFAULT_PITCHER_FIELDS = [
  { key: 'ip', label: 'IP', visible: true },
  { key: 'pitchR', label: 'R', visible: true },
  { key: 'er', label: 'ER', visible: true },
  { key: 'pitchH', label: 'H', visible: true },
  { key: 'pitchBbHp', label: 'BB+HP', visible: true },
  { key: 'pitchSo', label: 'SO', visible: true },
  { key: 'pitchHr', label: 'HR', visible: true },
  { key: 'pitchInsideParkHr', label: 'IPHR', visible: true },
  { key: 'pitAb', label: 'AB', visible: false },
  { key: 'pitE', label: 'E', visible: false },
  { key: 'np', label: 'NP', visible: false },
  { key: 'pitchPa', label: 'PA', visible: true },
  { key: 'pitchBf', label: 'BF', visible: true },
  { key: 'pitchBb', label: 'BB', visible: false },
  { key: 'pitchIbb', label: 'IBB', visible: false },
  { key: 'pitchHbp', label: 'HP', visible: false },
  { key: 'wp', label: 'WP', visible: false },
  { key: 'bk', label: 'BK', visible: false },
  { key: 'go', label: 'GO', visible: false },
  { key: 'fo', label: 'FO', visible: false },
  { key: 'gs', label: 'GS', visible: false },
  { key: 'w', label: 'W', visible: false },
  { key: 'l', label: 'L', visible: false },
  { key: 'sv', label: 'SV', visible: false },
  { key: 'svo', label: 'SVO', visible: false },
  { key: 'hld', label: 'HLD', visible: false },
  { key: 'cg', label: 'CG', visible: false },
  { key: 'pg', label: 'PG', visible: false },
  { key: 'era', label: 'ERA', visible: true },
  { key: 'whip', label: 'WHIP', visible: true }
];
export const FIELD_SETTINGS_STORAGE_KEY = 'bsball.admin.gameDetail.fieldSettings';
const FIELD_SETTINGS_VERSION = 1;
const LEGACY_KEYS = [
  'bsball.admin.gameDetail.batterFields',
  'bsball.admin.gameDetail.pitcherFields',
  'bsball.admin.gameDetail.decimalPlaces',
  'bsball.admin.gameDetail.rateDisplayStyle',
  'bsball.admin.gameDetail.showTrailingZeros'
];
const MIN_VISIBLE_FIELDS = 6;

function mergeFields(saved, defaults) {
  if (!Array.isArray(saved) || saved.length === 0) return defaults;
  const byKey = new Map(defaults.map((item) => [item.key, item]));
  const seen = new Set();
  const result = [];
  for (const item of saved) {
    if (!item?.key || seen.has(item.key)) continue;
    const known = byKey.get(item.key);
    if (known) {
      // #211 修复：不再强制 pitchPa/pitchBf 恒显（遗留夹取丢弃用户保存的可见性，
      // 导致观赛页/比赛详情页取消勾选 PA/BF 保存后，任何回读都被夹回默认可见）
      result.push({ ...known, visible: item.visible ?? known.visible });
      seen.add(item.key);
    }
  }
  for (const item of defaults) if (!seen.has(item.key)) result.push(item);
  return result;
}

function ensureMinVisible(fields, min) {
  if (min <= 0) return fields;
  let visibleCount = fields.filter((item) => item.visible).length;
  if (visibleCount >= min) return fields;
  const next = fields.map((item) => ({ ...item }));
  for (const item of next) {
    if (visibleCount >= min) break;
    if (!item.visible) {
      item.visible = true;
      visibleCount += 1;
    }
  }
  return next;
}

function readLegacyArray(key, defaults) {
  try {
    const raw = typeof window !== 'undefined' ? window.localStorage.getItem(key) : null;
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? mergeFields(parsed, defaults) : null;
  } catch {
    return null;
  }
}

function readLegacyNumber(key, fallback) {
  try {
    const raw = typeof window !== 'undefined' ? window.localStorage.getItem(key) : null;
    if (raw == null) return null;
    const num = Number(raw);
    return num === 2 || num === 3 ? num : fallback;
  } catch {
    return null;
  }
}

function readLegacyRateStyle(key) {
  try {
    const raw = typeof window !== 'undefined' ? window.localStorage.getItem(key) : null;
    return raw === 'dot' || raw === 'leadingZero' ? raw : null;
  } catch {
    return null;
  }
}

function readLegacyBoolean(key) {
  try {
    const raw = typeof window !== 'undefined' ? window.localStorage.getItem(key) : null;
    return raw === 'true' ? true : raw === 'false' ? false : null;
  } catch {
    return null;
  }
}

function clearLegacyKeys(keys) {
  if (typeof window === 'undefined') return;
  try {
    for (const key of keys) window.localStorage.removeItem(key);
  } catch {}
}

function parseStored(raw) {
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return null;
    const state = parsed;
    if (!Array.isArray(state.batterFields) || !Array.isArray(state.pitcherFields)) return null;
    return state;
  } catch {
    return null;
  }
}

function normalizeDecimalPlaces(value, fallback) {
  return value === 2 || value === 3 ? value : fallback;
}

function normalizeRateDisplayStyle(value, fallback) {
  return value === 'dot' || value === 'leadingZero' ? value : fallback;
}

// 按键读取并归一化一份字段设置状态（无窗/无值/损坏/结构非法 → null）
function readKeyedFieldSettings(key, defaults) {
  try {
    const raw = typeof window !== 'undefined' ? window.localStorage.getItem(key) : null;
    if (!raw) return null;
    const stored = parseStored(raw);
    if (!stored?.batterFields || !stored?.pitcherFields) return null;
    return {
      batterFields: ensureMinVisible(mergeFields(stored.batterFields, defaults.defaultBatterFields), MIN_VISIBLE_FIELDS),
      pitcherFields: ensureMinVisible(mergeFields(stored.pitcherFields, defaults.defaultPitcherFields), MIN_VISIBLE_FIELDS),
      decimalPlaces: normalizeDecimalPlaces(stored.decimalPlaces, defaults.defaultDecimalPlaces),
      rateDisplayStyle: normalizeRateDisplayStyle(stored.rateDisplayStyle, defaults.defaultRateDisplayStyle),
      showTrailingZeros: typeof stored.showTrailingZeros === 'boolean' ? stored.showTrailingZeros : defaults.defaultShowTrailingZeros
    };
  } catch {
    return null;
  }
}

function writeKeyedFieldSettings(key, state) {
  try {
    if (typeof window === 'undefined') return;
    const payload = { v: FIELD_SETTINGS_VERSION, ...state };
    window.localStorage.setItem(key, JSON.stringify(payload));
  } catch {}
}

export function buildInitialFieldSettings(options) {
  const { defaultBatterFields, defaultPitcherFields, defaultDecimalPlaces, defaultRateDisplayStyle, defaultShowTrailingZeros, tenantCode } = options;
  const display = readDisplaySettings();
  const guideDismissed =
    tenantCode != null && String(tenantCode).trim() !== '' ? isPortalGuideDismissed(String(tenantCode)) : !!display.detailPage.guideDismissed;
  return {
    batterFields: ensureMinVisible(mergeFields(display.detailPage.batterFields, defaultBatterFields), MIN_VISIBLE_FIELDS),
    pitcherFields: ensureMinVisible(mergeFields(display.detailPage.pitcherFields, defaultPitcherFields), MIN_VISIBLE_FIELDS),
    decimalPlaces: normalizeDecimalPlaces(display.valueControl.decimalPlaces, defaultDecimalPlaces),
    rateDisplayStyle: normalizeRateDisplayStyle(display.valueControl.rateDisplayStyle, defaultRateDisplayStyle),
    showTrailingZeros: typeof display.valueControl.showTrailingZeros === 'boolean' ? display.valueControl.showTrailingZeros : defaultShowTrailingZeros,
    guideDismissed
  };
}

export function saveDetailPageSettings(settings) {
  try {
    if (typeof window === 'undefined') return;
    const display = readDisplaySettings();
    Object.assign(display.detailPage, settings);
    saveDisplaySettings(display);
  } catch {}
}

export function saveValueControlSettings(settings) {
  try {
    if (typeof window === 'undefined') return;
    const display = readDisplaySettings();
    display.valueControl = { ...display.valueControl, ...settings };
    saveDisplaySettings(display);
  } catch {}
}

export function writeFieldSettings(state) {
  writeKeyedFieldSettings(FIELD_SETTINGS_STORAGE_KEY, state);
}

export function readFieldSettings(options) {
  const { defaultBatterFields, defaultPitcherFields, defaultDecimalPlaces, defaultRateDisplayStyle, defaultShowTrailingZeros } = options;
  const storedSettings = readKeyedFieldSettings(FIELD_SETTINGS_STORAGE_KEY, options);
  if (storedSettings) {
    clearLegacyKeys(LEGACY_KEYS);
    return storedSettings;
  }
  const batterFields = readLegacyArray(LEGACY_KEYS[0], defaultBatterFields) ?? defaultBatterFields;
  const pitcherFields = readLegacyArray(LEGACY_KEYS[1], defaultPitcherFields) ?? defaultPitcherFields;
  const decimalPlaces = readLegacyNumber(LEGACY_KEYS[2], defaultDecimalPlaces) ?? defaultDecimalPlaces;
  const rateDisplayStyle = readLegacyRateStyle(LEGACY_KEYS[3]) ?? defaultRateDisplayStyle;
  const showTrailingZeros = readLegacyBoolean(LEGACY_KEYS[4]) ?? defaultShowTrailingZeros;
  const migrated = {
    batterFields: ensureMinVisible(batterFields, MIN_VISIBLE_FIELDS),
    pitcherFields: ensureMinVisible(pitcherFields, MIN_VISIBLE_FIELDS),
    decimalPlaces,
    rateDisplayStyle,
    showTrailingZeros
  };
  writeFieldSettings(migrated);
  clearLegacyKeys(LEGACY_KEYS);
  return migrated;
}

// —— #211 观赛页「数据项调整」独立设置 ——
// 独立键：不影响门户/管理端比赛详情页的任何设置；新键优先，
// 未设置时回退现有取值链（管理端键 + 旧键迁移）作为初始展示，首次在观赛页保存后才真正独立。
export const LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY = 'bsball.admin.liveWatch.fieldSettings';

export function readWatchFieldSettings(options) {
  const stored = readKeyedFieldSettings(LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY, options);
  if (stored) return stored;
  return readFieldSettings(options);
}

export function writeWatchFieldSettings(state) {
  writeKeyedFieldSettings(LIVE_WATCH_FIELD_SETTINGS_STORAGE_KEY, state);
}
