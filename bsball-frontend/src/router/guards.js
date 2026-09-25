// 路由守卫 —— 行为移植自编译产物入口 chunk（beforeEach/afterEach + 文档标题）
// 语义：
//  1) 每次导航：解析租户（404/异常页清空；/docs 固定默认租户）→ 设活动租户 → 水合登录态 → 组装标题
//  2) 管理端受保护路由无 token → 跳 AdminLogin（带 redirect 回跳）
//  3) 已登录非超管跨租户访问 → 重定向到本人租户同路径
//  4) canAccess 不通过 → 提示并拦截；/admin、/admin/dashboard 与首次导航豁免
//  5) afterEach：门户安全路径记录访问埋点；语言切换时刷新文档标题
import { watch } from 'vue';
import { ElMessage } from 'element-plus';
import { i18n } from '../i18n';
import { useAuthStore } from '../stores/auth';
import { useAppConfigStore } from '../stores/appConfig';
import {
  DEFAULT_TENANT_CODE,
  isReservedTenantCode,
  resolveTenantCodeFromRoute,
  resolveTenantCodeFromUser,
  currentTenantCodeFromUrl,
  firstPathSegment,
  normalizeAdminPath,
  isSafeRedirect,
  isSafeRedirectRoute
} from '../utils/tenantRoute';
import { resolveToken, ensureUserStoredForTenant, setActiveTenant } from '../api/tokenStorage';
import { recordPortalVisit } from '../api/request';

// 管理端受保护路由（剔除 /admin/login；对应编译产物 Io）
function isAdminProtectedRoute(path) {
  const normalized = path.replace(/^\/+/, '/');
  if (normalized.includes('/admin/login')) return false;
  return /\/admin(\/|$)/.test(normalized);
}

// 管理端登录页（对应编译产物 Ra）
function isAdminLoginPath(path) {
  return path.replace(/\/+$/, '').endsWith('/admin/login');
}

// 文档标题：`{页面标题} - {站点名称}`；无页面标题时仅站点名称
function buildDocumentTitle(route) {
  let siteName = 'BS Ball';
  let tenant = null;
  if (route.name !== 'NotFound' && route.name !== 'ServiceUnavailable') {
    tenant = typeof window !== 'undefined' ? currentTenantCodeFromUrl() : null;
    if (!tenant) {
      const fromParams = typeof route.params.tenantCode === 'string' ? route.params.tenantCode.trim() : '';
      const fromPath = firstPathSegment(route.path);
      tenant = fromParams && !isReservedTenantCode(fromParams)
        ? fromParams
        : fromPath && !isReservedTenantCode(fromPath)
          ? fromPath
          : null;
    }
  }
  if (tenant && !isReservedTenantCode(tenant)) {
    const appConfig = useAppConfigStore();
    appConfig.initForTenant(tenant);
    if (appConfig.siteName) siteName = appConfig.siteName;
  }
  const titleKey = route.meta?.titleKey;
  const title = titleKey ? i18n.global.t(titleKey) : route.meta?.title;
  document.title = title ? `${title} - ${siteName}` : siteName;
}

export function installRouterGuards(router) {
  router.beforeEach((to, from) => {
    let tenant = null;
    if (to.name === 'NotFound' || to.name === 'ServiceUnavailable') {
      setActiveTenant(null);
    } else if (to.path === '/docs' || to.path.startsWith('/docs/')) {
      tenant = DEFAULT_TENANT_CODE;
      setActiveTenant(tenant);
    } else {
      tenant = resolveTenantCodeFromRoute(to);
      setActiveTenant(tenant);
    }

    const active = tenant && !isReservedTenantCode(tenant) ? tenant : DEFAULT_TENANT_CODE;
    const needAuth = isAdminProtectedRoute(to.path);

    if (typeof localStorage !== 'undefined') ensureUserStoredForTenant(active);
    const auth = useAuthStore();
    auth.hydrateForTenant(active);
    buildDocumentTitle(to);

    const hasToken = typeof localStorage !== 'undefined' && !!resolveToken(active);
    if (needAuth && !hasToken) {
      return {
        name: 'AdminLogin',
        params: { tenantCode: firstPathSegment(to.path) || DEFAULT_TENANT_CODE },
        query: { redirect: to.fullPath }
      };
    }
    if (needAuth && hasToken && !isAdminLoginPath(to.path)) {
      const current = auth.user;
      if (current && current.superAdmin !== true) {
        const ownTenant = current.tenantCode?.trim() || resolveTenantCodeFromUser(current);
        const routeTenant = typeof to.params.tenantCode === 'string' ? to.params.tenantCode : '';
        if (ownTenant && routeTenant && ownTenant.toLowerCase() !== routeTenant.toLowerCase()) {
          return {
            path: `/${ownTenant}${normalizeAdminPath(to.path)}`,
            query: to.query,
            hash: to.hash,
            replace: true
          };
        }
      }
      const normalizedPath = (to.path || '/').replace(/\/+$/, '') || '/';
      const isAdminRootOrDashboard = /\/admin\/?$/.test(normalizedPath) || normalizedPath.endsWith('/admin/dashboard');
      const isInitialNavigation = from.matched.length === 0;
      if (!isAdminRootOrDashboard && !isInitialNavigation && !auth.canAccess(to.path)) {
        ElMessage.warning('无权访问该页面');
        return false;
      }
    }
    return true;
  });

  router.afterEach((to) => {
    if (isAdminLoginPath(to.path) || !isSafeRedirectRoute(to)) return;
    if (isSafeRedirect(to.path)) recordPortalVisit(to.path);
  });

  // 语言切换 → 刷新文档标题
  watch(() => i18n.global.locale.value, () => {
    buildDocumentTitle(router.currentRoute.value);
  });
}
