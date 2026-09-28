// 门户引导状态（field settings guide）——行为保真移植自编译产物入口 chunk index-ByOnov1B.js
// 逐字对应：ei=存储键、$e=默认结构、ti=读取、ai=写入、ii=旧版迁移、xn=判断、Wn=标记
import { readDisplaySettings } from './statsDisplayStorage';

export const FIELD_SETTINGS_GUIDE_KEY = 'bsball.portal.fieldSettingsGuide.v1';

export function defaultGuideState() {
  return { v: 1, byTenant: {} };
}

export function readGuideState() {
  if (typeof window === 'undefined') return defaultGuideState();
  try {
    const raw = window.localStorage.getItem(FIELD_SETTINGS_GUIDE_KEY);
    if (!raw) return defaultGuideState();
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || parsed.v !== 1) return defaultGuideState();
    const state = parsed;
    if (!state.byTenant || typeof state.byTenant !== 'object') state.byTenant = {};
    return state;
  } catch {
    return defaultGuideState();
  }
}

export function writeGuideState(state) {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(FIELD_SETTINGS_GUIDE_KEY, JSON.stringify(state));
  } catch {}
}

export function migrateLegacyGuide(state) {
  if (state.legacyPortalMigrated) return;
  state.legacyPortalMigrated = true;
  const legacy = readDisplaySettings();
  if (legacy.detailPage.guideDismissed || legacy.statPage.columnSettingsGuideDismissed) state.legacyGlobalDismissed = true;
  writeGuideState(state);
}

export function isPortalGuideDismissed(tenantCode) {
  const code = tenantCode?.trim();
  if (!code) return false;
  const state = readGuideState();
  migrateLegacyGuide(state);
  if (state.legacyGlobalDismissed) return true;
  return !!state.byTenant[code];
}

export function markPortalGuideSeen(tenantCode) {
  const code = tenantCode?.trim();
  if (!code) return;
  const state = readGuideState();
  migrateLegacyGuide(state);
  state.byTenant[code] = true;
  writeGuideState(state);
}

// ---- 门户反馈引导（feedbackGuide，键 bsball.portal.feedbackGuide.v1）：保留既有实现 ----
const GUIDE_STORAGE_KEY = 'bsball.portal.feedbackGuide.v1';

function emptyStore() {
  return { v: 1, byTenant: {} };
}

function readStore() {
  if (typeof window === 'undefined') return emptyStore();
  try {
    const raw = window.localStorage.getItem(GUIDE_STORAGE_KEY);
    if (!raw) return emptyStore();
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || parsed.v !== 1) return emptyStore();
    if (!parsed.byTenant || typeof parsed.byTenant !== 'object') parsed.byTenant = {};
    return parsed;
  } catch {
    return emptyStore();
  }
}

function writeStore(store) {
  try {
    window.localStorage.setItem(GUIDE_STORAGE_KEY, JSON.stringify(store));
  } catch {
    // ignore
  }
}

// 该租户反馈引导是否已读（无租户码返回 false）
export function isPortalFeedbackGuideSeen(tenantCode) {
  const code = tenantCode?.trim();
  if (!code) return false;
  return !!readStore().byTenant[code];
}

// 标记该租户反馈引导已读
export function markPortalFeedbackGuideSeen(tenantCode) {
  const code = tenantCode?.trim();
  if (!code) return;
  const store = readStore();
  store.byTenant[code] = true;
  writeStore(store);
}
