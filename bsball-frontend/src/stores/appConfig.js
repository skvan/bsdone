// 站点配置 —— 默认值与缓存键移植自编译产物入口 chunk（appConfig store）
// 说明：footerText 默认值保持与编译产物逐字一致（仍含 "By aDz"）。
// 注意：静态页（webapps/index.html、portal.html）已按 PR #41 移除 aDz 署名——新工程是否同步移除待产品确认。
import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { get } from '../api/request';
import { currentTenantCodeFromUrl, DEFAULT_TENANT_CODE } from '../utils/tenantRoute';

export const APP_CONFIG_CACHE_PREFIX = 'bs-ball-app-config';

export const DEFAULT_APP_CONFIG = {
  siteName: 'BS Ball',
  siteTitle: '赛事与数据展示',
  logoUrl: '/bs-ball-logo.png',
  adminLogoUrl: '/bs-ball-logo.png',
  adminTitle: '棒垒球管理系统',
  faviconIco: '',
  footerTextPortal: '<p>赛事与数据展示</p><p>© 2026 棒垒球管理系统 By aDz.</p>',
  footerTextAdmin: '<p>© 2026 棒垒球管理系统 By aDz.</p>',
  showFooterPortal: true,
  showFooterAdmin: false,
  authCaptchaEnabled: true,
  authCaptchaType: 'random',
  authCaptchaRandomTypes: ['input', 'click', 'drag'],
  publicViewCount: true,
  portalHeaderBg: '',
  portalHeaderText: '',
  portalHeaderMode: 'fixed',
  portalFooterBg: '',
  portalFooterText: '',
  portalLayoutWidthMode: 'boxed',
  portalContentMaxWidth: 1440,
  portalHomeSectionOrder: undefined
};

// 白名单式净化：只接收默认值中已知字段（类型一致的才覆盖）
function sanitizeConfig(raw) {
  if (!raw || typeof raw !== 'object') return null;
  const out = {};
  for (const [key, def] of Object.entries(DEFAULT_APP_CONFIG)) {
    const value = raw[key];
    if (value === undefined || value === null) continue;
    if (Array.isArray(def)) {
      if (Array.isArray(value)) out[key] = value;
    } else {
      out[key] = value;
    }
  }
  return out;
}

function cacheKeyFor(tenantCode) {
  return `${APP_CONFIG_CACHE_PREFIX}::${(tenantCode || DEFAULT_TENANT_CODE).trim().toLowerCase()}`;
}

export const useAppConfigStore = defineStore('appConfig', () => {
  const config = ref(null);

  function apply(partial) {
    config.value = { ...DEFAULT_APP_CONFIG, ...(partial || {}) };
  }

  function loadFromCache(tenantCode) {
    try {
      const key = cacheKeyFor(tenantCode || currentTenantCodeFromUrl());
      const raw = localStorage.getItem(key);
      if (raw) {
        apply(sanitizeConfig(JSON.parse(raw)));
        return true;
      }
    } catch {
      // ignore
    }
    return false;
  }

  async function load(tenantCode) {
    try {
      const body = await get('/api/portal/settings');
      const sanitized = sanitizeConfig(body?.data);
      apply(sanitized);
      try {
        localStorage.setItem(cacheKeyFor(tenantCode || currentTenantCodeFromUrl()), JSON.stringify(sanitized || {}));
      } catch {
        // ignore
      }
    } catch {
      // 接口失败时保留默认/缓存值
    }
  }

  function init() {
    loadFromCache();
    load();
  }

  // 指定租户初始化（路由守卫调用：标题/门户壳依赖站点名）
  function initForTenant(tenantCode) {
    loadFromCache(tenantCode);
    load(tenantCode);
  }

  // 站点名称（标题拼接用；无配置时回退默认）
  const siteName = computed(() => config.value?.siteName || DEFAULT_APP_CONFIG.siteName);

  // 管理台标题（adminTitle 优先，回退默认值；对应编译产物 getAdminTitle）
  function getAdminTitle() {
    return config.value?.adminTitle || DEFAULT_APP_CONFIG.adminTitle;
  }

  // 站点资源 URL 解析（相对路径补 /bs-ball/ 前缀；对应编译产物 resolveAssetUrl）
  function resolveAssetUrl(url) {
    if (!url || url.startsWith('data:') || url.startsWith('http')) return url;
    return '/bs-ball/'.replace(/\/+$/, '') + '/' + (url.startsWith('/') ? url.slice(1) : url);
  }

  return { config, siteName, apply, loadFromCache, load, init, initForTenant, getAdminTitle, resolveAssetUrl };
});
