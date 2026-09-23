// 门户跨标签存储桥 —— 行为移植自编译产物入口 chunk（di 分发器 + ui/wn 引用计数 + sl 生命周期）
// 作用：全局 storage 事件分发（语言/门户主题/展示设置/引导标记/pageSize/站点配置），组件内以引用计数挂载
import { onMounted, onUnmounted } from 'vue';
import { i18n } from '../i18n';
import { usePortalThemeStore } from '../stores/portalTheme';
import { useAppConfigStore } from '../stores/appConfig';

export const PORTAL_STATS_PAGE_SIZE_SYNC_EVENT = 'bsball-portal-stats-page-size-sync';
export const PORTAL_FIELD_SETTINGS_GUIDE_SYNC_EVENT = 'bsball-portal-field-settings-guide-sync';
export const PORTAL_FEEDBACK_GUIDE_SYNC_EVENT = 'bsball-portal-feedback-guide-sync';
const APP_CONFIG_KEY = 'bs-ball-app-config';

function dispatch(eventName, detail) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(detail === undefined ? new CustomEvent(eventName) : new CustomEvent(eventName, { detail }));
}

function onStorageChange(event) {
  if (!event.key) return;
  if (event.key === 'bs-ball-locale') {
    const next = event.newValue;
    if (next && ['zh-CN', 'zh-TW', 'en', 'ko', 'ja'].includes(next)) i18n.global.locale.value = next;
    return;
  }
  if (event.key === 'bs-ball-portal-theme') {
    usePortalThemeStore().reloadFromStorage();
    return;
  }
  if (event.key === 'bsball.portal.fieldSettingsGuide.v1') {
    dispatch(PORTAL_FIELD_SETTINGS_GUIDE_SYNC_EVENT);
    return;
  }
  if (event.key === 'bsball.portal.feedbackGuide.v1') {
    dispatch(PORTAL_FEEDBACK_GUIDE_SYNC_EVENT);
    return;
  }
  if (event.key === 'bsball.portal.stats.pageSize') {
    dispatch(PORTAL_STATS_PAGE_SIZE_SYNC_EVENT, event.newValue);
    return;
  }
  // 展示设置（bsball.portal.display.settings）分发留待 B4 统计模块接入后补全
  if (event.key === APP_CONFIG_KEY || event.key.startsWith(`${APP_CONFIG_KEY}::`)) {
    useAppConfigStore().fetchPortalSettings();
  }
}

let refCount = 0;

export function addPortalStorageListener() {
  if (typeof window === 'undefined') return;
  if (refCount === 0) window.addEventListener('storage', onStorageChange);
  refCount += 1;
}

export function removePortalStorageListener() {
  if (typeof window === 'undefined') return;
  if (refCount <= 0) return;
  refCount -= 1;
  if (refCount === 0) window.removeEventListener('storage', onStorageChange);
}

// 组件内使用：挂载时 +1，卸载时 -1（对应编译产物 sl）
export function usePortalStorageBridge() {
  onMounted(() => addPortalStorageListener());
  onUnmounted(() => removePortalStorageListener());
}
