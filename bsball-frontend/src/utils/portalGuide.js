// 门户反馈引导已读标记 —— 行为移植自编译产物入口 chunk（Un/Yn + ri/Ur，键 bsball.portal.feedbackGuide.v1）
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
