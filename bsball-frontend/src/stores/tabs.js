// 管理端多标签页 store —— 行为移植自编译产物 tabs chunk（pinia "tabs"）
// 职责：打开页签的增删/关闭/固定/排序、按租户持久化（open + pinned）、LiveGame 家族页签合并
import { computed, ref, watch } from 'vue';
import { defineStore } from 'pinia';
import { useSettingsStore } from './settings';
import {
  DEFAULT_TENANT_CODE,
  firstPathSegment,
  isReservedTenantCode,
  normalizeAdminPath
} from '../utils/tenantRoute';

const OPEN_TABS_STORAGE_KEY = 'bs-ball-admin-tabs';
const PINNED_TABS_STORAGE_KEY = 'bs-ball-admin-tabs-pinned';

// ---------- 持久化（固定页签：{byTenant:{tenantCode:[{path,title,name}]}}） ----------
function readPinnedStorage() {
  try {
    const raw = localStorage.getItem(PINNED_TABS_STORAGE_KEY);
    if (!raw) return { byTenant: {} };
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' && parsed.byTenant && typeof parsed.byTenant === 'object'
      ? parsed
      : { byTenant: {} };
  } catch {
    return { byTenant: {} };
  }
}

function writePinnedStorage(value) {
  try {
    localStorage.setItem(PINNED_TABS_STORAGE_KEY, JSON.stringify(value));
  } catch {
    // ignore
  }
}

function readPinnedForTenant(tenantCode) {
  return (readPinnedStorage().byTenant[tenantCode] ?? []).map((item) => ({
    ...item,
    path: withTenantPrefix(item.path)
  }));
}

function writePinnedForTenant(tenantCode, items) {
  const storage = readPinnedStorage();
  if (items.length === 0) delete storage.byTenant[tenantCode];
  else storage.byTenant[tenantCode] = items;
  writePinnedStorage(storage);
}

// 把当前页签中的固定项按租户写入持久化
function persistPinnedFromTabs(tabs) {
  if (!tabs.length) return;
  const tenant = tenantCodeOfPath(tabs[0].path);
  writePinnedForTenant(
    tenant,
    tabs
      .filter((t) => tenantCodeOfPath(t.path) === tenant && !isDashboardPath(t.path) && t.pinned)
      .map((t) => ({ path: normalizePath(t.path), title: t.title, name: t.name }))
  );
}

// ---------- 路径工具（与编译产物逐字对齐） ----------
function normalizePath(path) {
  if (!path) return '/';
  const trimmed = path.replace(/\/+$/, '');
  return trimmed === '' ? '/' : trimmed;
}

function isDashboardPath(path) {
  const normalized = normalizePath(path);
  return /\/admin\/dashboard$/.test(normalized) || /^\/[^/]+\/admin$/.test(normalized);
}

function samePath(a, b) {
  return normalizePath(a) === normalizePath(b);
}

// 补租户前缀：/admin/... → /{默认租户}/admin/...
function withTenantPrefix(path) {
  let result = path;
  if (/^\/admin(\/|$)/.test(result) && !/^\/[^/]+\/admin/.test(result)) {
    result = `/${DEFAULT_TENANT_CODE}${result}`;
  }
  return normalizePath(result);
}

function tenantCodeOfPath(path) {
  const seg = firstPathSegment(path);
  return seg && !isReservedTenantCode(seg) ? seg : DEFAULT_TENANT_CODE;
}

// 同路径合并（pinned 取或，title/name 取先到非空）
function dedupeByPath(tabs) {
  const result = [];
  const indexByPath = new Map();
  for (const tab of tabs) {
    const path = normalizePath(tab.path);
    const entry = { ...tab, path };
    const existing = indexByPath.get(path);
    if (existing === undefined) {
      indexByPath.set(path, result.length);
      result.push(entry);
    } else {
      const prev = result[existing];
      result[existing] = {
        ...prev,
        ...entry,
        pinned: !!(prev.pinned || entry.pinned),
        title: prev.title || entry.title,
        name: prev.name || entry.name
      };
    }
  }
  return result;
}

// 重建租户路径：/{tenant}{normalizeAdminPath(path)}
function withTenantAndAdmin(path, tenantCode) {
  return normalizePath(`/${tenantCode}${normalizeAdminPath(normalizePath(path))}`);
}

function tenantFromRoute(route, fallbackPath) {
  const fromParams = route.params.tenantCode;
  if (typeof fromParams === 'string' && fromParams.trim()) return fromParams.trim();
  return firstPathSegment(fallbackPath) || DEFAULT_TENANT_CODE;
}

// ---------- 排序规则 ----------
// 确保存在工作台且唯一（保持在前）
function ensureDashboard(tabs, tenantCode) {
  const tenant = tenantCode || firstPathSegment(tabs[0]?.path || '') || DEFAULT_TENANT_CODE;
  let list = [...tabs];
  const dashboards = list.filter((t) => isDashboardPath(t.path));
  if (dashboards.length > 1) {
    const preferred = `/${tenant}/admin/dashboard`;
    const keep =
      dashboards.find((t) => samePath(t.path, preferred)) ||
      dashboards.find((t) => tenantCodeOfPath(t.path) === tenant) ||
      dashboards[0];
    list = list.filter((t) => !isDashboardPath(t.path));
    list = [keep, ...list];
  }
  if (!list.some((t) => isDashboardPath(t.path))) {
    return [
      { path: `/${tenant}/admin/dashboard`, title: '工作台', name: 'AdminDashboard' },
      ...list
    ];
  }
  return list;
}

function moveDashboardToFront(tabs, tenantCode) {
  const list = ensureDashboard(tabs, tenantCode);
  const index = list.findIndex((t) => isDashboardPath(t.path));
  if (index <= 0) return list;
  const [dashboard] = list.splice(index, 1);
  return [dashboard, ...list];
}

// 工作台 → 固定页签 → 其余
function orderTabs(tabs, tenantCode) {
  const list = moveDashboardToFront(ensureDashboard([...tabs], tenantCode), tenantCode);
  const index = list.findIndex((t) => isDashboardPath(t.path));
  if (index < 0) return list;
  const dashboard = list[index];
  const rest = [...list.slice(0, index), ...list.slice(index + 1)];
  const pinned = rest.filter((t) => t.pinned);
  const normal = rest.filter((t) => !t.pinned);
  return [dashboard, ...pinned, ...normal];
}

// 固定页签持久化并入后的排序（持久化固定优先）
function orderTabsWithPinned(tabs, pinnedItems, tenantCode) {
  let list = tabs.map((t) => ({ ...t, path: normalizePath(t.path) }));
  list = dedupeByPath(list);
  list = moveDashboardToFront(ensureDashboard(list, tenantCode), tenantCode);
  const dashboardIndex = list.findIndex((t) => isDashboardPath(t.path));
  if (dashboardIndex < 0) return orderTabs(tabs, tenantCode);
  const dashboard = list[dashboardIndex];
  const rest = [...list.slice(0, dashboardIndex), ...list.slice(dashboardIndex + 1)];
  const byPath = new Map(rest.map((t) => [t.path, t]));
  const pinnedOrder = [];
  const seen = new Set();
  for (const item of pinnedItems) {
    const path = normalizePath(item.path);
    if (isDashboardPath(path) || tenantCodeOfPath(path) !== tenantCode) continue;
    const found = byPath.get(path);
    if (found?.pinned) {
      pinnedOrder.push(found);
      seen.add(found.path);
    }
  }
  for (const item of rest) {
    if (!item.pinned || seen.has(item.path)) continue;
    pinnedOrder.push(item);
    seen.add(item.path);
  }
  const others = rest.filter((t) => !seen.has(t.path));
  return [dashboard, ...pinnedOrder, ...others];
}

// 合并持久化固定页签（不存在的补建占位）
function mergeWithPinned(tabs, tenantCode) {
  let list = tabs.map((t) => ({ ...t, path: normalizePath(t.path) }));
  list = dedupeByPath(list);
  const pinnedItems = readPinnedForTenant(tenantCode);
  if (!pinnedItems.length) return orderTabs(list, tenantCode);
  const byPath = new Map(list.map((t) => [t.path, t]));
  for (const item of pinnedItems) {
    const path = normalizePath(item.path);
    if (isDashboardPath(path) || tenantCodeOfPath(path) !== tenantCode) continue;
    const found = byPath.get(path);
    if (found) {
      found.pinned = true;
    } else {
      const created = { path, title: item.title || path, name: item.name, pinned: true };
      list.push(created);
      byPath.set(path, created);
    }
  }
  list = dedupeByPath(list);
  return orderTabsWithPinned(list, pinnedItems, tenantCode);
}

// 启动时从 localStorage 恢复（仅当"保留打开的页签"开启）
function loadPersistedTabs() {
  try {
    const raw = localStorage.getItem(OPEN_TABS_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed) || parsed.length === 0) return [];
    const tabs = parsed
      .map((t) => ({ ...t, path: withTenantPrefix(t.path) }))
      .filter((t) => t && typeof t.path === 'string');
    if (tabs.length === 0) return [];
    const deduped = dedupeByPath(tabs.map((t) => ({ ...t, path: normalizePath(t.path) })));
    const tenant = tenantCodeOfPath(deduped[0].path);
    const scoped = deduped.filter((t) => tenantCodeOfPath(t.path) === tenant);
    const pinnedItems = readPinnedForTenant(tenant);
    return pinnedItems.length ? orderTabsWithPinned(scoped, pinnedItems, tenant) : orderTabs(scoped, tenant);
  } catch {
    return [];
  }
}

function persistOpenTabs(tabs) {
  try {
    localStorage.setItem(OPEN_TABS_STORAGE_KEY, JSON.stringify(tabs));
  } catch {
    // ignore
  }
}

function initialTabs() {
  return useSettingsStore().retainOpenTabs ? loadPersistedTabs() : [];
}

// ---------- LiveGame 家族页签合并 ----------
const LIVE_TAB_NAMES = new Set(['AdminGameLiveLineup', 'AdminGameLiveLineupGame', 'AdminGameLiveResume', 'AdminGameLiveWatch']);

export function isLiveTabRoute(route) {
  if (route.name && LIVE_TAB_NAMES.has(String(route.name))) return true;
  const path = normalizePath(route.path);
  return (
    /\/admin\/events\/[^/]+\/games\/(?:(\d+)\/)?live$/.test(path) ||
    /\/admin\/events\/[^/]+\/games\/\d+\/lineup$/.test(path)
  );
}

const LIVE_LISTEN_RE = /\/admin\/events\/(\d+)\/games\/live$/;
const LIVE_RESUME_RE = /\/admin\/events\/(\d+)\/games\/(\d+)\/live$/;
const LIVE_LINEUP_RE = /\/admin\/events\/(\d+)\/games\/(\d+)\/lineup$/;

function parseLiveListenPath(path) {
  const m = LIVE_LISTEN_RE.exec(normalizePath(path));
  return m ? { eventId: m[1] } : null;
}

function parseLiveResumePath(path) {
  const m = LIVE_RESUME_RE.exec(normalizePath(path));
  return m ? { eventId: m[1], gameId: m[2] } : null;
}

function parseLiveLineupPath(path) {
  const m = LIVE_LINEUP_RE.exec(normalizePath(path));
  return m ? { eventId: m[1], gameId: m[2] } : null;
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref(initialTabs());
  const state = computed(() => tabs.value);
  const leaveConfirmHandler = ref(null);

  function setLeaveConfirmHandler(handler) {
    leaveConfirmHandler.value = handler;
  }

  watch(
    tabs,
    (value) => {
      persistPinnedFromTabs(value);
      if (useSettingsStore().retainOpenTabs) persistOpenTabs(value);
    },
    { deep: true }
  );

  function persistNow() {
    if (useSettingsStore().retainOpenTabs) persistOpenTabs(tabs.value);
  }

  function clearPersistedStorage() {
    try {
      localStorage.removeItem(OPEN_TABS_STORAGE_KEY);
    } catch {
      // ignore
    }
  }

  function clearAllTabsAndPins() {
    const tenant = tenantCodeOfPath(tabs.value[0]?.path || '') || DEFAULT_TENANT_CODE;
    tabs.value = [{ path: `/${tenant}/admin/dashboard`, title: '工作台', name: 'AdminDashboard' }];
    writePinnedForTenant(tenant, []);
  }

  // 打开/激活页签（含 LiveGame 家族合并）
  function addTab(route) {
    let path = normalizePath(route.path);
    if (/^\/[^/]+\/admin$/.test(path)) path = `${path}/dashboard`;
    const title = route.meta?.title || route.name || path;
    const tenant = tenantFromRoute(route, path);
    let list = tabs.value.map((t) => ({ ...t, path: withTenantAndAdmin(t.path, tenant) }));
    list = dedupeByPath(list);
    const existingIndex = list.findIndex((t) => samePath(t.path, path));

    let mergeResumeIndex = -1;
    if (existingIndex < 0 && route.name === 'AdminGameLiveResume') {
      const ids = parseLiveResumePath(path);
      if (ids) {
        mergeResumeIndex = list.findIndex((t) => {
          const listen = parseLiveListenPath(t.path);
          if (listen != null && listen.eventId === ids.eventId) return true;
          const lineup = parseLiveLineupPath(t.path);
          return lineup != null && lineup.eventId === ids.eventId && lineup.gameId === ids.gameId;
        });
      }
    }

    let mergeLineupIndex = -1;
    if (existingIndex < 0 && mergeResumeIndex < 0 && route.name === 'AdminGameLiveLineup') {
      const ids = parseLiveListenPath(path);
      if (ids) {
        mergeLineupIndex = list.findIndex((t) => {
          const resume = parseLiveResumePath(t.path);
          return resume != null && resume.eventId === ids.eventId;
        });
      }
    }

    let next;
    if (mergeResumeIndex >= 0) {
      const copy = [...list];
      copy[mergeResumeIndex] = { ...copy[mergeResumeIndex], path, title, name: route.name };
      next = orderTabs(ensureDashboard(copy, tenant), tenant);
    } else if (mergeLineupIndex >= 0) {
      const copy = [...list];
      copy[mergeLineupIndex] = { ...copy[mergeLineupIndex], path, title, name: route.name };
      next = orderTabs(ensureDashboard(copy, tenant), tenant);
    } else if (existingIndex < 0) {
      next = orderTabs(ensureDashboard([...list, { path, title, name: route.name }], tenant), tenant);
    } else {
      const copy = [...list];
      const prev = copy[existingIndex];
      copy[existingIndex] = {
        ...prev,
        path,
        title: route.meta?.title || prev.title,
        name: route.name || prev.name
      };
      next = orderTabs(ensureDashboard(copy, tenant), tenant);
    }
    tabs.value = mergeWithPinned(next, tenant);
  }

  // 关闭页签（工作台与固定页签不可关）
  function removeTab(path) {
    const normalized = normalizePath(path);
    if (isDashboardPath(normalized)) return;
    if (tabs.value.find((t) => samePath(t.path, normalized))?.pinned) return;
    tabs.value = orderTabs(tabs.value.filter((t) => !samePath(t.path, normalized)));
  }

  function closeOther(path) {
    const normalized = normalizePath(path);
    tabs.value = orderTabs(ensureDashboard(tabs.value.filter((t) => isDashboardPath(t.path) || t.pinned || samePath(t.path, normalized))));
  }

  function closeLeft(path) {
    const normalized = normalizePath(path);
    const index = tabs.value.findIndex((t) => samePath(t.path, normalized));
    if (index <= 0) return;
    tabs.value = orderTabs(
      ensureDashboard(tabs.value.filter((t, i) => !!(i >= index || isDashboardPath(t.path) || t.pinned)))
    );
  }

  function closeRight(path) {
    const normalized = normalizePath(path);
    const index = tabs.value.findIndex((t) => samePath(t.path, normalized));
    if (index < 0 || index >= tabs.value.length - 1) return;
    tabs.value = orderTabs(
      ensureDashboard(tabs.value.filter((t, i) => !!(i <= index || t.pinned)))
    );
  }

  // 关闭全部（保留工作台）
  function closeAll() {
    const tenant = tenantCodeOfPath(tabs.value[0]?.path || '') || DEFAULT_TENANT_CODE;
    tabs.value = orderTabs(
      [
        { path: `/${tenant}/admin/dashboard`, title: '工作台', name: 'AdminDashboard' },
        ...tabs.value.filter((t) => !isDashboardPath(t.path) && t.pinned)
      ],
      tenant
    );
  }

  function reorderTabs(from, to) {
    if (from === to || from < 0 || to < 0) return;
    const copy = [...tabs.value];
    const [moved] = copy.splice(from, 1);
    copy.splice(to, 0, moved);
    const next = orderTabs(copy, tenantCodeOfPath(copy[0]?.path || '') || DEFAULT_TENANT_CODE);
    tabs.value = next;
    persistPinnedFromTabs(next);
  }

  // 固定/取消固定：插入到"工作台之后、固定块末尾"
  function insertAfterPinnedBlock(list, item) {
    const index = [...list].findIndex((t) => isDashboardPath(t.path));
    const pinnedCount = list.filter((t) => !isDashboardPath(t.path) && t.pinned).length;
    const target = index >= 0 ? index + 1 + pinnedCount : pinnedCount;
    const copy = [...list];
    copy.splice(target, 0, item);
    const next = orderTabs(copy);
    tabs.value = next;
    persistPinnedFromTabs(next);
  }

  function pinTab(path) {
    const normalized = normalizePath(path);
    if (isDashboardPath(normalized)) return;
    const copy = [...tabs.value];
    const index = copy.findIndex((t) => samePath(t.path, normalized));
    if (index < 0) return;
    const item = copy[index];
    if (item.pinned) return;
    item.path = normalized;
    copy.splice(index, 1);
    item.pinned = true;
    insertAfterPinnedBlock(copy, item);
  }

  function unpinTab(path) {
    const normalized = normalizePath(path);
    if (isDashboardPath(normalized)) return;
    const copy = [...tabs.value];
    const index = copy.findIndex((t) => samePath(t.path, normalized));
    if (index < 0) return;
    const [item] = copy.splice(index, 1);
    item.path = normalized;
    item.pinned = false;
    insertAfterPinnedBlock(copy, item);
  }

  return {
    tabs: state,
    addTab,
    removeTab,
    closeOther,
    closeLeft,
    closeRight,
    closeAll,
    reorderTabs,
    pinTab,
    unpinTab,
    persistNow,
    clearPersistedStorage,
    clearAllTabsAndPins,
    leaveConfirmHandler,
    setLeaveConfirmHandler
  };
});

export { samePath, isDashboardPath };
