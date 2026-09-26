// 统计展示设置存储（门户展示服务端配置的本地缓存通道）—— 逐字移植自编译产物入口 chunk 存储链
// 通道：localStorage["bsball.portal.display.settings"] = { v:3, valueControl, statPage, detailPage }
// 说明：入口 chunk 中 valueControl/statPage 键集合命中"同步键"（Ja=Jt∪Xt），读写走该配置对象；
//       未命中键走普通 localStorage JSON 读写（G/ol 语义）。
export const DISPLAY_SETTINGS_STORAGE_KEY = 'bsball.portal.display.settings';
export const DISPLAY_SETTINGS_VERSION = 3;

const VALUE_CONTROL_KEYS = new Set(['rateDisplayStyle', 'showTrailingZeros', 'decimalPlaces']);
const STAT_PAGE_KEYS = new Set([
  'battingOrder', 'battingVisibility',
  'pitchingOrder', 'pitchingVisibility',
  'fieldingOrder', 'fieldingVisibility',
  'teamBattingOrder', 'teamBattingVisibility',
  'teamPitchingOrder', 'teamPitchingVisibility',
  'teamFieldingOrder', 'teamFieldingVisibility'
]);
const SYNCED_KEYS = new Set([...VALUE_CONTROL_KEYS, ...STAT_PAGE_KEYS]);

function isPlainObject(value) {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

function defaultValueControl() {
  return { rateDisplayStyle: 'leadingZero', showTrailingZeros: true, decimalPlaces: 2 };
}

function defaultStatPage() {
  return {
    battingOrder: [], battingVisibility: null,
    pitchingOrder: [], pitchingVisibility: null,
    fieldingOrder: [], fieldingVisibility: null,
    teamBattingOrder: [], teamBattingVisibility: null,
    teamPitchingOrder: [], teamPitchingVisibility: null,
    teamFieldingOrder: [], teamFieldingVisibility: null,
    columnSettingsGuideDismissed: false
  };
}

function defaultDetailPage() {
  return { batterFields: [], pitcherFields: [], guideDismissed: false };
}

export function defaultDisplaySettings() {
  return { v: DISPLAY_SETTINGS_VERSION, valueControl: defaultValueControl(), statPage: defaultStatPage(), detailPage: defaultDetailPage() };
}

function isValidDisplaySettings(value) {
  return (
    isPlainObject(value) &&
    value.v === DISPLAY_SETTINGS_VERSION &&
    isPlainObject(value.valueControl) &&
    isPlainObject(value.statPage) &&
    isPlainObject(value.detailPage)
  );
}

function normalizeRateDisplayStyle(value, fallback) {
  return value === 'dot' || value === 'leadingZero' ? value : fallback;
}

function normalizeDecimalPlaces(value, fallback) {
  return value === 2 || value === 3 ? value : fallback;
}

// 行级设置过滤器（入口 chunk Sa：{key,label,visible} 对象数组）
function sanitizeColumnSettingRows(list) {
  if (!Array.isArray(list)) return [];
  const out = [];
  for (const item of list) {
    if (!item || typeof item !== 'object') continue;
    if (typeof item.key !== 'string' || typeof item.label !== 'string' || typeof item.visible !== 'boolean') continue;
    out.push({ key: item.key, label: item.label, visible: item.visible });
  }
  return out;
}

// 归一化合并（入口 chunk Xa）：以默认值为底，仅接受合法字段
export function mergeDisplaySettings(raw) {
  const merged = defaultDisplaySettings();
  if (isPlainObject(raw.valueControl)) {
    const src = raw.valueControl;
    merged.valueControl.rateDisplayStyle = normalizeRateDisplayStyle(src.rateDisplayStyle, merged.valueControl.rateDisplayStyle);
    merged.valueControl.showTrailingZeros = typeof src.showTrailingZeros === 'boolean' ? src.showTrailingZeros : merged.valueControl.showTrailingZeros;
    merged.valueControl.decimalPlaces = normalizeDecimalPlaces(src.decimalPlaces, merged.valueControl.decimalPlaces);
  }
  if (isPlainObject(raw.statPage)) {
    const src = raw.statPage;
    const takeOrder = (key, value) => {
      if (Array.isArray(value) && value.every((k) => typeof k === 'string')) merged.statPage[key] = value;
    };
    takeOrder('battingOrder', src.battingOrder);
    takeOrder('pitchingOrder', src.pitchingOrder);
    takeOrder('fieldingOrder', src.fieldingOrder);
    takeOrder('teamBattingOrder', src.teamBattingOrder);
    takeOrder('teamPitchingOrder', src.teamPitchingOrder);
    takeOrder('teamFieldingOrder', src.teamFieldingOrder);
    const takeVisibility = (key, value) => {
      if (value === null || (Array.isArray(value) && value.every((k) => typeof k === 'string'))) merged.statPage[key] = value;
    };
    takeVisibility('battingVisibility', src.battingVisibility);
    takeVisibility('pitchingVisibility', src.pitchingVisibility);
    takeVisibility('fieldingVisibility', src.fieldingVisibility);
    takeVisibility('teamBattingVisibility', src.teamBattingVisibility);
    takeVisibility('teamPitchingVisibility', src.teamPitchingVisibility);
    takeVisibility('teamFieldingVisibility', src.teamFieldingVisibility);
    if (typeof src.columnSettingsGuideDismissed === 'boolean') merged.statPage.columnSettingsGuideDismissed = src.columnSettingsGuideDismissed;
  }
  if (isPlainObject(raw.detailPage)) {
    const src = raw.detailPage;
    merged.detailPage.batterFields = sanitizeColumnSettingRows(src.batterFields);
    merged.detailPage.pitcherFields = sanitizeColumnSettingRows(src.pitcherFields);
    if (typeof src.guideDismissed === 'boolean') merged.detailPage.guideDismissed = src.guideDismissed;
  }
  return merged;
}

// 读配置对象（入口 chunk ea）：无窗/无值/损坏 → 默认
export function readDisplaySettings() {
  const fallback = defaultDisplaySettings();
  if (typeof window === 'undefined') return fallback;
  try {
    const raw = window.localStorage.getItem(DISPLAY_SETTINGS_STORAGE_KEY);
    if (!raw) return fallback;
    const parsed = JSON.parse(raw);
    if (isValidDisplaySettings(parsed)) return mergeDisplaySettings(parsed);
    window.localStorage.removeItem(DISPLAY_SETTINGS_STORAGE_KEY);
    return fallback;
  } catch {
    try {
      window.localStorage.removeItem(DISPLAY_SETTINGS_STORAGE_KEY);
    } catch {
      /* 忽略 */
    }
    return fallback;
  }
}

// 存配置对象（入口 chunk Ta——写入前经 Xa 归一化）
export function saveDisplaySettings(next) {
  const normalized = mergeDisplaySettings({ v: DISPLAY_SETTINGS_VERSION, valueControl: next.valueControl, statPage: next.statPage, detailPage: next.detailPage });
  const payload = {
    v: DISPLAY_SETTINGS_VERSION,
    valueControl: normalized.valueControl,
    statPage: normalized.statPage,
    detailPage: normalized.detailPage
  };
  try {
    if (typeof window !== 'undefined') window.localStorage.setItem(DISPLAY_SETTINGS_STORAGE_KEY, JSON.stringify(payload));
  } catch {
    /* 忽略 */
  }
}

// 读统计键（入口 chunk xr：同步键从配置对象取值）
export function readStatKey(key, fallback) {
  const settings = readDisplaySettings();
  if (VALUE_CONTROL_KEYS.has(key)) {
    const value = settings.valueControl[key];
    return value !== undefined ? value : fallback;
  }
  if (STAT_PAGE_KEYS.has(key)) {
    const value = settings.statPage[key];
    return value !== undefined ? value : fallback;
  }
  return fallback;
}

// 写统计键（入口 chunk Wr → Ta）
export function writeStatKey(key, value) {
  const settings = readDisplaySettings();
  if (VALUE_CONTROL_KEYS.has(key)) {
    settings.valueControl[key] = value;
    saveDisplaySettings(settings);
    return;
  }
  if (STAT_PAGE_KEYS.has(key)) {
    settings.statPage[key] = value;
    saveDisplaySettings(settings);
  }
}

// 通用 JSON 读写（入口 chunk G/ol：同步键走配置对象，其余走普通 localStorage）
export function readLocalJson(key, fallback) {
  try {
    if (typeof window === 'undefined') return fallback;
    if (SYNCED_KEYS.has(key)) return readStatKey(key, fallback);
    const raw = window.localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

export function writeLocalJson(key, value) {
  try {
    if (typeof window === 'undefined') return;
    if (SYNCED_KEYS.has(key)) {
      writeStatKey(key, value);
      return;
    }
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    /* 忽略 */
  }
}
