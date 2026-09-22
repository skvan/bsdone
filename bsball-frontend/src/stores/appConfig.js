// 站点配置 —— 默认值与缓存键移植自编译产物入口 chunk（appConfig store）
// 说明：footerText 默认值保持与编译产物逐字一致（仍含 "By aDz"）。
// 注意：静态页（webapps/index.html、portal.html）已按 PR #41 移除 aDz 署名——新工程是否同步移除待产品确认。
import { ref } from 'vue';
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

  function loadFromCache() {
    try {
      const key = cacheKeyFor(currentTenantCodeFromUrl());
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

  async function load() {
    try {
      const body = await get('/api/portal/settings');
      const sanitized = sanitizeConfig(body?.data);
      apply(sanitized);
      try {
        localStorage.setItem(cacheKeyFor(currentTenantCodeFromUrl()), JSON.stringify(sanitized || {}));
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

  return { config, apply, loadFromCache, load, init };
});
