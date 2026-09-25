// 鉴权与权限 store —— 行为移植自编译产物 request chunk（auth store）
// 菜单 ID 制权限模型：菜单表（路径↔ID）、hasMenu/hasPerm/canAccess、登录/登出跳转、跨标签页同步
import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { useAdminConfigTenantStore } from './adminConfigTenant';
import {
  normalizeTenantCode,
  tenantTokenKey,
  tenantUserKey,
  setTenantAuth,
  removeTenantAuth
} from '../api/tokenStorage';
import { normalizeAdminPath, isSafeRedirect, DEFAULT_TENANT_CODE } from '../utils/tenantRoute';

const NO_TENANT_LIMIT_KEY = 'admin_no_tenant_limit';
const SUPER_BASE_TENANT_KEY = 'admin_super_base_tenant_id';

// 菜单路径 → 菜单 ID（与编译产物逐字一致，34 条）
export const MENU_PATH_IDS = {
  '/admin/dashboard': 1,
  '/admin/users': 9,
  '/admin/roles': 10,
  '/admin/menus': 11,
  '/admin/apis': 18,
  '/admin/dict': 12,
  '/admin/articles': 29,
  '/admin/announcements': 13,
  '/admin/config': 14,
  '/admin/resources': 15,
  '/admin/media-icons': 30,
  '/admin/media-gallery': 31,
  '/admin/ip-location-cache': 32,
  '/admin/monitor/portal-devtools-report': 33,
  '/admin/monitor/portal-visit-hit': 34,
  '/admin/monitor/portal-feedback': 35,
  '/admin/monitor/ip-access-policy': 36,
  '/admin/highlight-moments': 37,
  '/admin/lineup-templates': 38,
  '/admin/login-logs': 16,
  '/admin/operation-logs': 17,
  '/admin/tenants': 27,
  '/admin/monitor/data': 5,
  '/admin/monitor/server': 6,
  '/admin/monitor/cache': 7,
  '/admin/monitor/cache-list': 8,
  '/admin/leagues': 19,
  '/admin/stadiums': 26,
  '/admin/teams': 20,
  '/admin/coaches': 21,
  '/admin/players': 22,
  '/admin/history-records': 23,
  '/admin/events': 24,
  '/admin/content': 25
};

// 菜单 ID → 父级菜单 ID（权限放行时同时匹配父级）
export const MENU_PARENT_IDS = {
  9: 2, 10: 2, 11: 2, 18: 2, 12: 2, 29: 2, 13: 2, 14: 2, 15: 2, 16: 2, 17: 2, 27: 2,
  30: 2, 31: 2, 32: 2,
  5: 3, 6: 3, 7: 3, 8: 3, 33: 3, 34: 3, 35: 3, 36: 3,
  19: 4, 26: 4, 20: 4, 21: 4, 22: 4, 23: 4, 24: 4, 25: 4, 37: 4, 38: 4
};

// 用户对象规范化：ID 类字段统一转为数字
function normalizeUser(user) {
  if (!user) return null;
  const tenantIdRaw = user.tenantId;
  const tenantIdNum = tenantIdRaw == null ? undefined : Number(tenantIdRaw);
  return {
    ...user,
    tenantId: Number.isFinite(tenantIdNum) ? tenantIdNum : undefined,
    roleIds: Array.isArray(user.roleIds)
      ? user.roleIds.map((v) => Number(v)).filter((v) => Number.isFinite(v))
      : user.roleIds,
    menuIds: Array.isArray(user.menuIds)
      ? user.menuIds.map((v) => Number(v)).filter((v) => Number.isFinite(v))
      : user.menuIds,
    tenants: Array.isArray(user.tenants)
      ? user.tenants
          .map((t) => ({ ...t, id: Number(t.id) }))
          .filter((t) => Number.isFinite(t.id))
      : user.tenants
  };
}

export const useAuthStore = defineStore('auth', () => {
  const activeTenantStorageCode = ref(normalizeTenantCode(null));
  const user = ref(null);
  const noTenantLimit = ref(false);
  const superAdminBaseTenantId = ref(null);

  // 按租户水合登录态（从 localStorage 恢复）
  function hydrateForTenant(tenantCode) {
    const tenant = normalizeTenantCode(tenantCode);
    activeTenantStorageCode.value = tenant;
    try {
      const raw = typeof localStorage !== 'undefined' ? localStorage.getItem(tenantUserKey(tenant)) : null;
      user.value = raw ? normalizeUser(JSON.parse(raw)) : null;
      noTenantLimit.value =
        typeof localStorage !== 'undefined' && localStorage.getItem(NO_TENANT_LIMIT_KEY) === '1';
      if (!noTenantLimit.value && user.value?.superAdmin === true && Number(user.value.tenantId) === 0) {
        noTenantLimit.value = true;
      }
      const baseRaw = typeof localStorage !== 'undefined' ? localStorage.getItem(SUPER_BASE_TENANT_KEY) : null;
      const baseNum = baseRaw != null ? Number(baseRaw) : NaN;
      superAdminBaseTenantId.value = Number.isFinite(baseNum) ? baseNum : null;
    } catch {
      user.value = null;
      noTenantLimit.value = false;
      superAdminBaseTenantId.value = null;
    }
  }

  // 登录完成：写入租户键并设置超管标记
  function completeLogin(tenantCode, token, userData) {
    const tenant = normalizeTenantCode(tenantCode);
    activeTenantStorageCode.value = tenant;
    const normalized = normalizeUser(userData);
    if (normalized) {
      setTenantAuth(tenant, token, normalized);
      user.value = normalized;
      noTenantLimit.value = normalized.superAdmin === true && Number(normalized.tenantId) === 0;
      if (typeof localStorage !== 'undefined') {
        if (noTenantLimit.value) localStorage.setItem(NO_TENANT_LIMIT_KEY, '1');
        else localStorage.removeItem(NO_TENANT_LIMIT_KEY);
        if (normalized.superAdmin === true && normalized.tenantId != null && normalized.tenantId > 0) {
          localStorage.setItem(SUPER_BASE_TENANT_KEY, String(normalized.tenantId));
          superAdminBaseTenantId.value = normalized.tenantId;
        } else {
          const baseRaw = localStorage.getItem(SUPER_BASE_TENANT_KEY);
          const baseNum = baseRaw != null ? Number(baseRaw) : NaN;
          superAdminBaseTenantId.value = Number.isFinite(baseNum) && baseNum > 0 ? baseNum : null;
        }
      }
    }
  }

  function setNoTenantLimit(flag) {
    noTenantLimit.value = !!flag;
    if (typeof localStorage === 'undefined') return;
    if (noTenantLimit.value) localStorage.setItem(NO_TENANT_LIMIT_KEY, '1');
    else localStorage.removeItem(NO_TENANT_LIMIT_KEY);
  }

  const displayName = computed(() => {
    const u = user.value;
    return u?.nickname || u?.username || '';
  });

  const menuIds = computed(() => new Set(user.value?.menuIds ?? []));
  const perms = computed(() => new Set(user.value?.perms ?? []));
  const menuPaths = computed(() => {
    const paths = user.value?.menuPaths;
    return paths && paths.length ? new Set(paths) : null;
  });

  // 菜单路径是否可访问（菜单 ID 制 + 父级兜底 + 未知路径放行）
  function canAccess(path) {
    if (user.value?.superAdmin === true) return true;
    const base = getBasePath(path);
    const paths = menuPaths.value;
    const ids = menuIds.value;
    if (paths != null && paths.size > 0) {
      if (paths.has(base)) return true;
      const id = MENU_PATH_IDS[base];
      if (id == null) return true;
      return ids.size > 0 ? ids.has(id) || ids.has(MENU_PARENT_IDS[id] ?? 0) : false;
    }
    if (ids.size === 0) return true;
    const id = MENU_PATH_IDS[base];
    if (id == null) return true;
    return ids.has(id) || ids.has(MENU_PARENT_IDS[id] ?? 0);
  }

  // 归一化到已知管理端基础路径（用于权限比对）
  function getBasePath(path) {
    const normalized = normalizeAdminPath(path);
    const m = normalized.match(
      /^\/admin\/(dashboard|users|roles|menus|apis|dict|articles|announcements|config|resources|media-icons|media-gallery|login-logs|operation-logs|tenants|ip-location-cache|monitor\/(?:data|server|cache|cache-list|portal-devtools-report|portal-visit-hit|portal-feedback|ip-access-policy)|leagues|stadiums|teams|coaches|players|events|history-records|highlight-moments|lineup-templates|content)/
    );
    return m ? m[0] : normalized;
  }

  function hasMenu(menuId) {
    const ids = menuIds.value;
    return ids.size === 0 ? menuId === 1 : ids.has(menuId);
  }

  function hasPerm(code) {
    if (!code) return false;
    if (user.value?.superAdmin) return true;
    return perms.value.has(code);
  }

  const canEnterAdmin = computed(() => {
    const u = user.value;
    if (!u) return false;
    if (u.superAdmin === true || u.tenantAdmin === true || (u.menuPaths ?? []).some((p) => typeof p === 'string' && p.startsWith('/admin'))) return true;
    const ids = u.menuIds ?? [];
    return ids.length > 0 && (ids.includes(1) || ids.some((id) => id > 1));
  });

  const systemFirstPath = computed(() => {
    const map = {
      9: '/admin/users', 10: '/admin/roles', 11: '/admin/menus', 18: '/admin/apis', 12: '/admin/dict',
      29: '/admin/articles', 13: '/admin/announcements', 14: '/admin/config', 15: '/admin/resources',
      16: '/admin/login-logs', 17: '/admin/operation-logs', 27: '/admin/tenants'
    };
    for (const id of [9, 10, 11, 18, 12, 29, 13, 14, 15, 16, 17, 27]) if (hasMenu(id)) return map[id] ?? '/admin/users';
    return '/admin/users';
  });

  const monitorFirstPath = computed(() => {
    const map = { 5: '/admin/monitor/data', 6: '/admin/monitor/server', 7: '/admin/monitor/cache', 8: '/admin/monitor/cache-list' };
    for (const id of [5, 6, 7, 8]) if (hasMenu(id)) return map[id] ?? '/admin/monitor/data';
    return '/admin/monitor/data';
  });

  const businessFirstPath = computed(() => {
    const paths = menuPaths.value;
    if (paths != null && paths.size > 0) {
      for (const p of ['/admin/leagues', '/admin/stadiums', '/admin/teams', '/admin/coaches', '/admin/players', '/admin/history-records', '/admin/events', '/admin/content']) {
        if (paths.has(p)) return p;
      }
    }
    const map = {
      19: '/admin/leagues', 26: '/admin/stadiums', 20: '/admin/teams', 21: '/admin/coaches',
      22: '/admin/players', 23: '/admin/history-records', 24: '/admin/events', 25: '/admin/content'
    };
    for (const id of [19, 26, 20, 21, 22, 23, 24, 25]) if (hasMenu(id)) return map[id] ?? '/admin/leagues';
    return '/admin/leagues';
  });

  // 更新用户对象（可选切换租户）
  function setUser(userData, tenantCode) {
    const tenant = normalizeTenantCode(tenantCode ?? activeTenantStorageCode.value);
    activeTenantStorageCode.value = tenant;
    const normalized = normalizeUser(userData);
    user.value = normalized;
    if (normalized?.superAdmin === true && Number(normalized.tenantId) === 0) setNoTenantLimit(true);
    if (typeof localStorage !== 'undefined') {
      if (normalized) localStorage.setItem(tenantUserKey(tenant), JSON.stringify(normalized));
      else localStorage.removeItem(tenantUserKey(tenant));
    }
  }

  // 清空登录态
  function clear(tenantCode) {
    try {
      useAdminConfigTenantStore().$reset();
    } catch {
      // ignore
    }
    const tenant = normalizeTenantCode(tenantCode ?? activeTenantStorageCode.value);
    user.value = null;
    noTenantLimit.value = false;
    superAdminBaseTenantId.value = null;
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem(NO_TENANT_LIMIT_KEY);
      localStorage.removeItem(SUPER_BASE_TENANT_KEY);
    }
    removeTenantAuth(tenant);
  }

  // 管理端未登录跳转
  function redirectToAdminLogin() {
    if (typeof window === 'undefined') return;
    const path = window.location.pathname.replace(/\/+$/, '') || '/';
    if (!/\/admin(\/|$)/.test(path) || /\/admin\/login$/.test(path)) return;
    const tenant = normalizeTenantCode(activeTenantStorageCode.value);
    const current = `${window.location.pathname}${window.location.search}${window.location.hash}`;
    window.location.assign(`/${tenant}/admin/login?redirect=${encodeURIComponent(current)}`);
  }

  // 登录后跳转（redirect 参数受白名单约束）
  function redirectAfterLogin() {
    if (typeof window === 'undefined') return;
    const path = window.location.pathname.replace(/\/+$/, '') || '/';
    if (!/\/admin\/login$/.test(path)) return;
    const tenant = normalizeTenantCode(activeTenantStorageCode.value);
    const redirect = (new URLSearchParams(window.location.search).get('redirect') || '').trim();
    if (redirect && isSafeRedirect(redirect)) {
      window.location.assign(redirect);
      return;
    }
    window.location.assign(`/${tenant}/admin/dashboard`);
  }

  // 跨标签页同步
  let storageHandler = null;
  function bindCrossTabSync() {
    if (typeof window === 'undefined' || storageHandler) return;
    storageHandler = (event) => {
      if (!event.key || event.storageArea !== localStorage) return;
      const tenant = activeTenantStorageCode.value;
      const userKey = tenantUserKey(tenant);
      const tokenKey = tenantTokenKey(tenant);
      if (event.key === userKey) {
        if (!event.newValue) {
          user.value = null;
          redirectToAdminLogin();
          return;
        }
        try {
          user.value = JSON.parse(event.newValue);
          redirectAfterLogin();
        } catch {
          user.value = null;
        }
        return;
      }
      if (event.key === tokenKey) {
        if (event.newValue) redirectAfterLogin();
        else {
          user.value = null;
          redirectToAdminLogin();
        }
        return;
      }
      if (event.key === NO_TENANT_LIMIT_KEY) noTenantLimit.value = event.newValue === '1';
    };
    window.addEventListener('storage', storageHandler);
  }

  function unbindCrossTabSync() {
    if (typeof window === 'undefined' || !storageHandler) return;
    window.removeEventListener('storage', storageHandler);
    storageHandler = null;
  }

  return {
    activeTenantStorageCode,
    user,
    noTenantLimit,
    superAdminBaseTenantId,
    displayName,
    menuIds,
    perms,
    menuPaths,
    canAccess,
    getBasePath,
    hasMenu,
    hasPerm,
    canEnterAdmin,
    systemFirstPath,
    monitorFirstPath,
    businessFirstPath,
    hydrateForTenant,
    completeLogin,
    setNoTenantLimit,
    setUser,
    clear,
    redirectToAdminLogin,
    redirectAfterLogin,
    bindCrossTabSync,
    unbindCrossTabSync
  };
});
