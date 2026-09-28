// 站点配置 —— 默认值与缓存键移植自编译产物入口 chunk（appConfig store）
// 说明：footer 默认值原与编译产物逐字一致（含 "By aDz"，见 H4）；按产品确认已移除 aDz 署名，
//       改用品牌文案（对齐静态页 PR #41/#146），门户页脚附构建版本号（VITE_APP_VERSION）。
import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import { get } from '../api/request';
import { currentTenantCodeFromUrl, DEFAULT_TENANT_CODE } from '../utils/tenantRoute';

export const APP_CONFIG_CACHE_PREFIX = 'bs-ball-app-config';

const APP_VERSION = import.meta.env.VITE_APP_VERSION || '';
const FOOTER_BRAND = '© 2026 广州康胜体育棒球 · BSD · Baseball & Softball Database';

export const DEFAULT_APP_CONFIG = {
  siteName: 'BS Ball',
  siteTitle: '赛事与数据展示',
  logoUrl: '/bs-ball-logo.png',
  adminLogoUrl: '/bs-ball-logo.png',
  adminTitle: '棒垒球管理系统',
  faviconIco: '',
  footerTextPortal: `<p>${FOOTER_BRAND}${APP_VERSION ? ' · v' + APP_VERSION : ''}</p>`,
  footerTextAdmin: `<p>${FOOTER_BRAND}</p>`,
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
  portalHomeSectionOrder: ['carousel', 'nav', 'promo', 'events', 'teams', 'news'],
  portalHomeSectionHidden: [],
  portalPromoAdImageUrl: '',
  portalPromoTicketImageUrl: '',
  portalPromoAdSlides: [],
  portalPromoTicketSlides: [],
  portalDevtoolsGuard: true,
  portalDevtoolsGuardOverlay: true,
  portalDevtoolsGuardDebuggerTrap: true,
  portalDevtoolsGuardCopyrightNotice: true
};

// HTML 转义（编译产物 sa）
function escapeHtml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// footer 文本合并（编译产物 at：footerText + copyright → 合并后的 footerTextPortal）
function mergeFooterText(footer, copyright, fallback) {
  const a = (footer ?? '').trim();
  const b = (copyright ?? '').trim();
  if (!a && !b) return fallback;
  if (b) return a ? (a.includes(b) ? a : `${a}<p>${escapeHtml(b)}</p>`) : `<p>${escapeHtml(b)}</p>`;
  return a;
}

// 白名单式净化：只接收默认值中已知字段（类型一致的才覆盖；footer/copyright 先合并）
function sanitizeConfig(raw) {
  if (!raw || typeof raw !== 'object') return null;
  const merged = { ...raw };
  merged.footerTextPortal = mergeFooterText(
    raw.footerTextPortal ?? raw.footerText,
    raw.copyrightPortal ?? raw.copyright,
    DEFAULT_APP_CONFIG.footerTextPortal
  );
  merged.footerTextAdmin = mergeFooterText(raw.footerTextAdmin, raw.copyrightAdmin, DEFAULT_APP_CONFIG.footerTextAdmin);
  const out = {};
  for (const [key, def] of Object.entries(DEFAULT_APP_CONFIG)) {
    const value = merged[key];
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
  // 初始即为默认配置（对齐编译产物：各字段 ref 初始化为默认值，未加载时也可直接读取）
  const config = ref({ ...DEFAULT_APP_CONFIG });

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

  // 兼容编译产物命名：重新拉取门户站点配置
  function fetchPortalSettings() {
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

  // 应用站点图标（faviconIco 优先，否则默认 logo；对应编译产物 applyFavicon）
  function applyFavicon() {
    const href = resolveAssetUrl(config.value?.faviconIco || '/bs-ball-logo.png');
    let link = document.querySelector('link[rel="icon"]');
    if (!link) {
      link = document.createElement('link');
      link.rel = 'icon';
      document.head.appendChild(link);
    }
    link.href = href;
  }

  // 门户首页区块排序/隐藏（编译产物顶层透传，消费者：Home/AppConfig；默认值对齐产物 ge(void 0) / ve(void 0)）
  const portalHomeSectionOrder = computed(() => config.value?.portalHomeSectionOrder ?? []);
  const portalHomeSectionHidden = computed(() => config.value?.portalHomeSectionHidden ?? []);

  // 与编译产物 store 对齐的直通字段（AppConfig 等消费者的配置读取；带默认值兜底）
  const PASS_THROUGH_KEYS = [
    'adminLogoUrl', 'adminTitle', 'authCaptchaEnabled', 'authCaptchaType', 'authCaptchaRandomTypes', 'faviconIco',
    'footerTextAdmin', 'footerTextPortal', 'logoUrl', 'portalContentMaxWidth',
    'portalDevtoolsGuard', 'portalDevtoolsGuardCopyrightNotice', 'portalDevtoolsGuardDebuggerTrap', 'portalDevtoolsGuardOverlay',
    'portalFooterBg', 'portalFooterText', 'portalHeaderBg', 'portalHeaderMode', 'portalHeaderText', 'portalLayoutWidthMode',
    'portalPromoAdSlides', 'portalPromoTicketSlides', 'publicViewCount', 'showFooterAdmin', 'showFooterPortal', 'siteTitle'
  ];
  const passThrough = {};
  for (const key of PASS_THROUGH_KEYS) {
    passThrough[key] = computed(() => config.value?.[key] ?? DEFAULT_APP_CONFIG[key]);
  }

  // 恢复默认（编译产物 resetToDefault）
  function resetToDefault() {
    config.value = { ...DEFAULT_APP_CONFIG };
  }

  // 合并服务端设置（编译产物 mergePortalSettingsFromServer：白名单净化后浅合并）
  function mergePortalSettingsFromServer(raw) {
    config.value = { ...config.value, ...(sanitizeConfig(raw) || {}) };
  }

  // 局部更新（编译产物 update：保存后回写 store）
  function update(partial) {
    config.value = { ...config.value, ...(partial || {}) };
  }

  return { config, siteName, portalHomeSectionOrder, portalHomeSectionHidden, ...passThrough, apply, loadFromCache, load, init, initForTenant, fetchPortalSettings, getAdminTitle, resolveAssetUrl, applyFavicon, resetToDefault, mergePortalSettingsFromServer, update };
});
