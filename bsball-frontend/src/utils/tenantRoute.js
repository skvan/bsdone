// 租户路径工具 —— 行为移植自编译产物 tenantRoute chunk（B0 提取契约）
// 职责：租户码解析/保留字判定/门户与管理端路径构造/重定向安全检查
export const DEFAULT_TENANT_CODE = 'bs-ball';

// 保留字：不可作为租户码的首段路径（与编译产物逐字一致）
const RESERVED_CODES = new Set([
  'admin', 'user', 'users', 'default', 'api', 'assets', 'src', 'files', 'www', 'static',
  'public', 'server', 'health', 'sys', 'system', 'favicon.ico', 'root', 'null', 'undefined',
  'portal', 'docs', 'login', 'logout', 'auth', 'oauth', 'index', '404', 'service-unavailable'
]);

const URL_SEGMENT_MAX = 512;
const BASE_URL = (import.meta.env && import.meta.env.BASE_URL) || '/bs-ball/';

export function isReservedTenantCode(code) {
  return RESERVED_CODES.has(String(code || '').toLowerCase());
}

// 从用户对象解析租户码：tenantCode 优先，其次按 tenantId 在 tenants 中查找
export function tenantCodeFromUser(user) {
  if (user == null || user.tenantId == null || !user.tenants?.length) return null;
  return user.tenants.find((t) => t.id === user.tenantId)?.code ?? null;
}

export function resolveTenantCodeFromUser(user) {
  if (user == null) return null;
  const code = user.tenantCode?.trim();
  return code || tenantCodeFromUser(user);
}

// 用户是否有权访问指定租户码（default 与 bs-ball 互为等价）
export function canUserAccessTenantCode(code, user) {
  const normalized = String(code || '').trim().toLowerCase();
  if (!normalized || isReservedTenantCode(normalized) || !user) return false;
  if (user.superAdmin === true) return true;
  const own = resolveTenantCodeFromUser(user)?.trim().toLowerCase() ?? '';
  if (!own) return false;
  if (own === normalized) return true;
  const fallback = DEFAULT_TENANT_CODE.trim().toLowerCase();
  return own === 'default' && normalized === fallback;
}

export function firstPathSegment(pathStr) {
  const m = pathStr.replace(/^\/+/, '/').match(/^\/([^/]+)/);
  return m ? m[1] : null;
}

// 当前路径（已剥离部署 base，如 /bs-ball）
export function currentRoutePath() {
  if (typeof window === 'undefined') return '';
  const base = BASE_URL.replace(/\/+$/, '') || '';
  let pathname = window.location.pathname || '/';
  if (base && base !== '/' && pathname.startsWith(base)) pathname = pathname.slice(base.length) || '/';
  return pathname.startsWith('/') ? pathname : '/' + pathname;
}

// 门户路由判定（/docs、404 页面以及一切非 /admin 路径）
export function isPortalRoute() {
  const path = currentRoutePath();
  if (path === '/docs' || path.startsWith('/docs/') || path === '/404' || path === '/service-unavailable') return true;
  return !/\/admin(\/|$)/.test(path);
}

// 从当前 URL 解析租户码（/docs 固定使用默认租户）
export function currentTenantCodeFromUrl() {
  if (typeof window === 'undefined') return null;
  const path = currentRoutePath();
  if (path === '/docs' || path.startsWith('/docs/')) return DEFAULT_TENANT_CODE;
  let code = firstPathSegment(path);
  if (!code || isReservedTenantCode(code)) {
    const raw = (window.location.pathname || '/').replace(/^\/+/, '/').match(/^\/([^/]+)/);
    if (raw && !isReservedTenantCode(raw[1])) code = raw[1];
  }
  if ((!code || isReservedTenantCode(code)) && (path === '/' || path === '')) {
    const base = BASE_URL.replace(/\/+$/, '') || '';
    if (base && base !== '/') {
      const seg = base.replace(/^\/+|\/+$/g, '').split('/').filter(Boolean)[0];
      if (seg && !isReservedTenantCode(seg)) code = seg;
    }
  }
  return code && !isReservedTenantCode(code) ? code : null;
}

// 从路由对象解析租户码：params 优先，其次路径首段，再次当前 URL
export function resolveTenantCodeFromRoute(route) {
  const fromParams = typeof route.params?.tenantCode === 'string' ? route.params.tenantCode.trim() : '';
  if (fromParams && !isReservedTenantCode(fromParams)) return fromParams;
  const fromPath = firstPathSegment(route.path || '');
  if (fromPath && !isReservedTenantCode(fromPath)) return fromPath;
  if (typeof window !== 'undefined') {
    const current = currentTenantCodeFromUrl();
    if (current && !isReservedTenantCode(current)) return current;
  }
  return null;
}

// 将 `/x/admin/...` 归一化为 `/admin/...`
export function normalizeAdminPath(pathStr) {
  const m = pathStr.match(/^\/[^/]+(\/admin.*)$/);
  return m ? m[1].replace(/\/+$/, '') || '/admin' : pathStr;
}

// 构造管理端路径：/{tenant}/admin/...
export function buildAdminPath(tenantCode, subPath) {
  let p = subPath.startsWith('/') ? subPath : '/' + subPath;
  if (p === '/admin' || p === '/admin/') p = '/admin/dashboard';
  if (!p.startsWith('/admin')) p = '/admin' + p;
  return `/${tenantCode}${p}`;
}

// 构造门户路径：/{tenant}/ 或 /{tenant}/{sub}
export function buildPortalPath(tenantCode, subPath) {
  if (!subPath || subPath === '/') return `/${tenantCode}/`;
  return `/${tenantCode}/${subPath.startsWith('/') ? subPath.slice(1) : subPath}`;
}

// 剥离 query/hash 并限长（默认 '/'）
export function stripQueryHash(pathStr) {
  if (pathStr == null || pathStr === '') return '';
  let t = String(pathStr).trim();
  if (!t) return '';
  const hashIdx = t.indexOf('#');
  if (hashIdx >= 0) t = t.slice(0, hashIdx).trim();
  const queryIdx = t.indexOf('?');
  if (queryIdx >= 0) t = t.slice(0, queryIdx).trim();
  if (!t) return '/';
  return t.length > URL_SEGMENT_MAX ? t.slice(0, URL_SEGMENT_MAX) : t;
}

// 重定向目标是否安全（不可指向管理端登录/管理区域/异常页）
export function isSafeRedirect(pathStr) {
  if (!pathStr?.trim()) return false;
  const t = pathStr.replace(/^\/+/, '/');
  if (t.includes('/admin/login') || /\/admin(\/|$|\?|#)/.test(t)) return false;
  const clean = stripQueryHash(t).replace(/\/+$/, '') || '/';
  return !(clean === '/404' || clean === '/service-unavailable');
}

export function isSafeRedirectRoute(route) {
  if (route.name === 'NotFound' || route.name === 'ServiceUnavailable') return false;
  const path = (route.path || '').replace(/\/+$/, '') || '/';
  return !(path === '/404' || path === '/service-unavailable');
}
