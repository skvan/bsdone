// 比赛详情字段设置存储——行为保真移植自编译产物 chunk gameDetailFieldSettingsStorage-DkSFgXpY.js
// 逐字对应：H=默认投手字段、m=存储键、F=版本、c=旧版键组、u=合并、d=保底可见、g/A/I/E=旧版读取、
//   w=旧版清理、T=解析、v/S=校验、L=构建、R=保存 detailPage、Z=保存 valueControl、B=写入、_=读取+迁移
import { readDisplaySettings, saveDisplaySettings } from './statsDisplayStorage';
import { isPortalGuideDismissed } from './portalGuide';

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
      result.push({ ...known, visible: item.key === 'pitchPa' || item.key === 'pitchBf' ? known.visible : item.visible ?? known.visible });
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
  try {
    if (typeof window === 'undefined') return;
    const payload = { v: FIELD_SETTINGS_VERSION, ...state };
    window.localStorage.setItem(FIELD_SETTINGS_STORAGE_KEY, JSON.stringify(payload));
  } catch {}
}

export function readFieldSettings(options) {
  const { defaultBatterFields, defaultPitcherFields, defaultDecimalPlaces, defaultRateDisplayStyle, defaultShowTrailingZeros } = options;
  try {
    const raw = typeof window !== 'undefined' ? window.localStorage.getItem(FIELD_SETTINGS_STORAGE_KEY) : null;
    if (raw) {
      const stored = parseStored(raw);
      if (stored?.batterFields && stored?.pitcherFields) {
        clearLegacyKeys(LEGACY_KEYS);
        return {
          batterFields: ensureMinVisible(mergeFields(stored.batterFields, defaultBatterFields), MIN_VISIBLE_FIELDS),
          pitcherFields: ensureMinVisible(mergeFields(stored.pitcherFields, defaultPitcherFields), MIN_VISIBLE_FIELDS),
          decimalPlaces: normalizeDecimalPlaces(stored.decimalPlaces, defaultDecimalPlaces),
          rateDisplayStyle: normalizeRateDisplayStyle(stored.rateDisplayStyle, defaultRateDisplayStyle),
          showTrailingZeros: typeof stored.showTrailingZeros === 'boolean' ? stored.showTrailingZeros : defaultShowTrailingZeros
        };
      }
    }
  } catch {}
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
