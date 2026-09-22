// 登录态令牌存储 —— 行为移植自编译产物 request chunk（多租户键位与迁移逻辑）
// 键位约定：旧版全局键 admin_token / admin_user；多租户键 admin_token::<tenant> / admin_user::<tenant>
import { DEFAULT_TENANT_CODE, tenantCodeFromUser } from '../utils/tenantRoute';

const LEGACY_TOKEN_KEY = 'admin_token';
const LEGACY_USER_KEY = 'admin_user';
const TOKEN_PREFIX = 'admin_token::';
const USER_PREFIX = 'admin_user::';

// 显式租户码（由入口/布局注入，优先于 URL 解析）
let explicitTenantCode = null;

export function normalizeTenantCode(code) {
  return (code || '').trim().toLowerCase() || DEFAULT_TENANT_CODE.toLowerCase();
}

export function setExplicitTenantCode(code) {
  explicitTenantCode = code && code.length > 0 ? code : null;
}

export function getExplicitTenantCode() {
  return explicitTenantCode;
}

export function tenantTokenKey(code) {
  return TOKEN_PREFIX + normalizeTenantCode(code);
}

export function tenantUserKey(code) {
  return USER_PREFIX + normalizeTenantCode(code);
}

// 读取指定租户的 token（含旧键迁移）
export function getTenantToken(code) {
  if (typeof localStorage === 'undefined') return null;
  const tenant = normalizeTenantCode(code);
  const token = localStorage.getItem(tenantTokenKey(tenant));
  if (token) return token;
  if (!localStorage.getItem(LEGACY_TOKEN_KEY)) return null;
  const storedTenant = readStoredUserTenantCode();
  if (storedTenant && storedTenant === tenant) {
    migrateLegacyForTenant(tenant);
    return localStorage.getItem(tenantTokenKey(tenant));
  }
  return null;
}

export function setTenantAuth(code, token, user) {
  if (typeof localStorage === 'undefined') return;
  const tenant = normalizeTenantCode(code);
  localStorage.setItem(tenantTokenKey(tenant), token);
  localStorage.setItem(tenantUserKey(tenant), JSON.stringify(user));
}

export function removeTenantAuth(code) {
  if (typeof localStorage === 'undefined') return;
  const tenant = normalizeTenantCode(code);
  localStorage.removeItem(tenantTokenKey(tenant));
  localStorage.removeItem(tenantUserKey(tenant));
}

// 旧版全局键迁移到租户键
export function migrateLegacyForTenant(code) {
  if (typeof localStorage === 'undefined') return;
  const legacyToken = localStorage.getItem(LEGACY_TOKEN_KEY);
  const legacyUser = localStorage.getItem(LEGACY_USER_KEY);
  if (legacyToken) localStorage.setItem(tenantTokenKey(code), legacyToken);
  if (legacyUser) localStorage.setItem(tenantUserKey(code), legacyUser);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
  localStorage.removeItem(LEGACY_USER_KEY);
}

// 清空全部租户令牌（含旧键）
export function clearAllTenantAuth() {
  if (typeof localStorage === 'undefined') return;
  const keys = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (key && (key.startsWith(TOKEN_PREFIX) || key.startsWith(USER_PREFIX))) keys.push(key);
  }
  for (const key of keys) localStorage.removeItem(key);
  localStorage.removeItem(LEGACY_TOKEN_KEY);
  localStorage.removeItem(LEGACY_USER_KEY);
}

// 已登录的租户码列表
export function listTenantCodesWithToken() {
  if (typeof localStorage === 'undefined') return [];
  const codes = [];
  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);
    if (!key?.startsWith(TOKEN_PREFIX)) continue;
    const code = key.slice(TOKEN_PREFIX.length).trim().toLowerCase();
    if (code && localStorage.getItem(key)) codes.push(code);
  }
  return codes;
}

// 读取 admin_user 中记录的租户码（tenantCode 优先，其次按 tenantId 查找）
export function readStoredUserTenantCode() {
  if (typeof localStorage === 'undefined') return null;
  try {
    const raw = localStorage.getItem(LEGACY_USER_KEY);
    if (!raw) return null;
    const user = JSON.parse(raw);
    const code = user.tenantCode?.trim().toLowerCase();
    return code || tenantCodeFromUser(user)?.toLowerCase() || null;
  } catch {
    return null;
  }
}

// 解析当前可用 token：指定租户 → 用户记录租户 → 唯一已登录租户
export function resolveToken(code) {
  const tenant = normalizeTenantCode(code || currentTenantCodeForStorage());
  const direct = getTenantToken(tenant);
  if (direct) return direct;
  if (typeof localStorage === 'undefined') return null;
  try {
    const rawUser = localStorage.getItem(tenantUserKey(tenant));
    if (rawUser) {
      const user = JSON.parse(rawUser);
      const userTenant = user.tenantCode?.trim().toLowerCase() || null;
      if (userTenant) {
        const token = localStorage.getItem(tenantTokenKey(userTenant));
        if (token) return token;
      }
    }
  } catch {
    // ignore
  }
  if (typeof localStorage !== 'undefined') {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key?.startsWith(USER_PREFIX)) {
        try {
          const raw = localStorage.getItem(key);
          if (!raw) continue;
          const user = JSON.parse(raw);
          const userTenant = user.tenantCode?.trim().toLowerCase();
          if (!userTenant) continue;
          const token = localStorage.getItem(tenantTokenKey(userTenant));
          if (token) return token;
        } catch {
          // ignore
        }
      }
    }
  }
  const codes = listTenantCodesWithToken();
  return codes.length === 1 ? localStorage.getItem(tenantTokenKey(codes[0])) : null;
}

// 将 admin_user 归档到对应租户键（登录态修复）
export function ensureUserStoredForTenant(code) {
  if (typeof localStorage === 'undefined') return;
  const tenant = normalizeTenantCode(code);
  const token = getTenantToken(tenant) || resolveToken(tenant);
  if (!token) return;
  let raw = localStorage.getItem(tenantUserKey(tenant));
  if (!raw) {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (!key?.startsWith(USER_PREFIX)) continue;
      const value = localStorage.getItem(key);
      if (value) {
        raw = value;
        break;
      }
    }
  }
  if (raw) {
    try {
      setTenantAuth(tenant, token, JSON.parse(raw));
    } catch {
      // ignore
    }
  }
}

// 供 tokenStorage 内部使用：当前租户码（延迟导入避免循环依赖）
function currentTenantCodeForStorage() {
  if (typeof window === 'undefined') return DEFAULT_TENANT_CODE.toLowerCase();
  const path = (window.location.pathname || '/').replace(/^\/+/, '/');
  const m = path.match(/^\/([^/]+)/);
  const seg = m ? m[1].trim().toLowerCase() : '';
  return seg || DEFAULT_TENANT_CODE.toLowerCase();
}
