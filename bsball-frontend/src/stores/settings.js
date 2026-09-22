// 管理端界面设置（theme store）—— 行为移植自编译产物入口 chunk（settings store）
// 存储键 bs-ball-admin-settings；22 项设置 + 主色/明暗应用 + 跨标签同步 + 快照恢复
import { ref } from 'vue';
import { defineStore } from 'pinia';

export const SETTINGS_STORAGE_KEY = 'bs-ball-admin-settings';

export const DEFAULT_SETTINGS = {
  theme: 'auto',
  primaryColor: '#409EFF',
  menuMode: 'mix',
  showTabs: true,
  retainOpenTabs: false,
  showBreadcrumb: true,
  listRenderMode: 'plain',
  listStripe: false,
  pageTransition: 'none',
  modalCloseOnClickMask: false,
  showWatermark: false,
  watermarkText: 'BS Ball',
  greyMode: false,
  colorWeakMode: false,
  sidebarWidth: 220,
  constrainContentWidth: false,
  contentMaxWidth: 1360,
  contentAlign: 'center',
  fillPageHeight: true,
  menuUniqueOpened: true,
  fixedOperationColumn: false,
  mobileFixedHeader: false
};

// 主色派生调色板使用的 CSS 变量（applyToDOM 前先清除旧值，与编译产物一致）
const HEADER_PALETTE_VARS = [
  '--admin-header-fg',
  '--admin-header-fg-muted',
  '--admin-header-border-bottom',
  '--admin-header-icon-bg',
  '--admin-header-icon-border',
  '--admin-header-menu-text',
  '--admin-header-menu-hover-text',
  '--admin-header-menu-active'
];
const SIDEBAR_PALETTE_VARS = [
  '--admin-sidebar-text',
  '--admin-sidebar-active',
  '--admin-sidebar-header-border',
  '--admin-sidebar-active-separator',
  '--admin-sidebar-hover-bg',
  '--admin-sidebar-menu-active-bg',
  '--admin-sidebar-scrollbar-thumb',
  '--admin-sidebar-scrollbar-thumb-hover'
];

// 读取本地设置（与默认值合并；retainOpenTabs 支持 cacheOpenTabs 旧键别名）
export function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_STORAGE_KEY);
    if (raw) {
      const stored = JSON.parse(raw);
      const merged = { ...DEFAULT_SETTINGS, ...stored };
      if (typeof stored.retainOpenTabs === 'boolean') merged.retainOpenTabs = stored.retainOpenTabs;
      else if (typeof stored.cacheOpenTabs === 'boolean') merged.retainOpenTabs = stored.cacheOpenTabs;
      return merged;
    }
  } catch {
    // ignore
  }
  return { ...DEFAULT_SETTINGS };
}

export function persistSettings(value) {
  try {
    localStorage.setItem(SETTINGS_STORAGE_KEY, JSON.stringify(value));
  } catch {
    // ignore
  }
}

// 有效列表渲染模式（仅 bordered/plain 生效）
function normalizeListRenderMode(value) {
  return value === 'bordered' || value === 'plain' ? value : DEFAULT_SETTINGS.listRenderMode;
}

export const useSettingsStore = defineStore('settings', () => {
  const theme = ref(DEFAULT_SETTINGS.theme);
  const primaryColor = ref(DEFAULT_SETTINGS.primaryColor);
  const menuMode = ref(DEFAULT_SETTINGS.menuMode);
  const showTabs = ref(DEFAULT_SETTINGS.showTabs);
  const retainOpenTabs = ref(DEFAULT_SETTINGS.retainOpenTabs);
  const showBreadcrumb = ref(DEFAULT_SETTINGS.showBreadcrumb);
  const listRenderMode = ref(DEFAULT_SETTINGS.listRenderMode);
  const listStripe = ref(DEFAULT_SETTINGS.listStripe);
  const pageTransition = ref(DEFAULT_SETTINGS.pageTransition);
  const modalCloseOnClickMask = ref(DEFAULT_SETTINGS.modalCloseOnClickMask);
  const showWatermark = ref(DEFAULT_SETTINGS.showWatermark);
  const watermarkText = ref(DEFAULT_SETTINGS.watermarkText);
  const greyMode = ref(DEFAULT_SETTINGS.greyMode);
  const colorWeakMode = ref(DEFAULT_SETTINGS.colorWeakMode);
  const sidebarWidth = ref(DEFAULT_SETTINGS.sidebarWidth);
  const constrainContentWidth = ref(DEFAULT_SETTINGS.constrainContentWidth);
  const contentMaxWidth = ref(DEFAULT_SETTINGS.contentMaxWidth);
  const contentAlign = ref(DEFAULT_SETTINGS.contentAlign);
  const fillPageHeight = ref(DEFAULT_SETTINGS.fillPageHeight);
  const menuUniqueOpened = ref(DEFAULT_SETTINGS.menuUniqueOpened);
  const fixedOperationColumn = ref(DEFAULT_SETTINGS.fixedOperationColumn);
  const mobileFixedHeader = ref(DEFAULT_SETTINGS.mobileFixedHeader);

  let storageHandler = null;

  // 应用设置到 DOM（清除旧调色板变量 → 重建 → 明暗与主题标记）
  function applyToDOM() {
    const el = document.documentElement;
    HEADER_PALETTE_VARS.forEach((name) => el.style.removeProperty(name));
    SIDEBAR_PALETTE_VARS.forEach((name) => el.style.removeProperty(name));
    el.style.removeProperty('--admin-header-bg');
    el.style.removeProperty('--admin-sidebar-bg');
    el.style.setProperty('--admin-sidebar-width', `${sidebarWidth.value}px`);
    el.style.setProperty('--admin-content-max-width', `${contentMaxWidth.value}px`);
    el.style.setProperty('--admin-content-align', contentAlign.value);
    el.style.setProperty('--admin-primary', primaryColor.value);
    el.style.setProperty('--el-color-primary', primaryColor.value);
    const effective = theme.value === 'auto'
      ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      : theme.value;
    el.setAttribute('data-admin-theme', effective);
    el.classList.toggle('dark', effective === 'dark');
  }

  function getSnapshot() {
    return {
      theme: theme.value,
      primaryColor: primaryColor.value,
      menuMode: menuMode.value,
      showTabs: showTabs.value,
      retainOpenTabs: retainOpenTabs.value,
      showBreadcrumb: showBreadcrumb.value,
      listRenderMode: listRenderMode.value,
      listStripe: listStripe.value,
      pageTransition: pageTransition.value,
      modalCloseOnClickMask: modalCloseOnClickMask.value,
      showWatermark: showWatermark.value,
      watermarkText: watermarkText.value,
      greyMode: greyMode.value,
      colorWeakMode: colorWeakMode.value,
      sidebarWidth: sidebarWidth.value,
      constrainContentWidth: constrainContentWidth.value,
      contentMaxWidth: contentMaxWidth.value,
      contentAlign: contentAlign.value,
      fillPageHeight: fillPageHeight.value,
      menuUniqueOpened: menuUniqueOpened.value,
      fixedOperationColumn: fixedOperationColumn.value,
      mobileFixedHeader: mobileFixedHeader.value
    };
  }

  // 持久化全部设置并应用
  function apply() {
    persistSettings(getSnapshot());
    applyToDOM();
  }

  // 从 localStorage 载入（不应用 DOM）
  function init() {
    const stored = loadSettings();
    theme.value = stored.theme;
    primaryColor.value = stored.primaryColor;
    menuMode.value = stored.menuMode;
    showTabs.value = stored.showTabs;
    retainOpenTabs.value = typeof stored.retainOpenTabs === 'boolean' ? stored.retainOpenTabs : DEFAULT_SETTINGS.retainOpenTabs;
    showBreadcrumb.value = stored.showBreadcrumb ?? DEFAULT_SETTINGS.showBreadcrumb;
    listRenderMode.value = normalizeListRenderMode(stored.listRenderMode ?? DEFAULT_SETTINGS.listRenderMode);
    listStripe.value = stored.listStripe ?? DEFAULT_SETTINGS.listStripe;
    pageTransition.value = stored.pageTransition;
    modalCloseOnClickMask.value = stored.modalCloseOnClickMask ?? DEFAULT_SETTINGS.modalCloseOnClickMask;
    showWatermark.value = stored.showWatermark ?? DEFAULT_SETTINGS.showWatermark;
    watermarkText.value = stored.watermarkText ?? DEFAULT_SETTINGS.watermarkText;
    greyMode.value = stored.greyMode ?? DEFAULT_SETTINGS.greyMode;
    colorWeakMode.value = stored.colorWeakMode ?? DEFAULT_SETTINGS.colorWeakMode;
    sidebarWidth.value = stored.sidebarWidth ?? DEFAULT_SETTINGS.sidebarWidth;
    constrainContentWidth.value = stored.constrainContentWidth ?? DEFAULT_SETTINGS.constrainContentWidth;
    contentMaxWidth.value = stored.contentMaxWidth ?? DEFAULT_SETTINGS.contentMaxWidth;
    contentAlign.value = stored.contentAlign ?? DEFAULT_SETTINGS.contentAlign;
    fillPageHeight.value = stored.fillPageHeight ?? DEFAULT_SETTINGS.fillPageHeight;
    menuUniqueOpened.value = typeof stored.menuUniqueOpened === 'boolean' ? stored.menuUniqueOpened : DEFAULT_SETTINGS.menuUniqueOpened;
    fixedOperationColumn.value = typeof stored.fixedOperationColumn === 'boolean' ? stored.fixedOperationColumn : DEFAULT_SETTINGS.fixedOperationColumn;
    mobileFixedHeader.value = typeof stored.mobileFixedHeader === 'boolean' ? stored.mobileFixedHeader : DEFAULT_SETTINGS.mobileFixedHeader;
  }

  // 从快照恢复（仅覆盖显式提供的字段；不落盘、不应用 DOM）
  function restoreFromSnapshot(data) {
    if (!data || typeof data !== 'object') return;
    if (data.theme != null) theme.value = data.theme;
    if (data.primaryColor != null) primaryColor.value = data.primaryColor;
    if (data.menuMode != null) menuMode.value = data.menuMode;
    if (data.showTabs != null) showTabs.value = data.showTabs;
    if (typeof data.retainOpenTabs === 'boolean') retainOpenTabs.value = data.retainOpenTabs;
    else if (typeof data.cacheOpenTabs === 'boolean') retainOpenTabs.value = data.cacheOpenTabs;
    if (data.showBreadcrumb != null) showBreadcrumb.value = data.showBreadcrumb;
    if (data.listRenderMode != null) listRenderMode.value = data.listRenderMode;
    if (data.listStripe != null) listStripe.value = data.listStripe;
    if (data.pageTransition != null) pageTransition.value = data.pageTransition;
    if (data.modalCloseOnClickMask != null) modalCloseOnClickMask.value = data.modalCloseOnClickMask;
    if (data.showWatermark != null) showWatermark.value = data.showWatermark;
    if (data.watermarkText != null) watermarkText.value = data.watermarkText;
    if (data.greyMode != null) greyMode.value = data.greyMode;
    if (data.colorWeakMode != null) colorWeakMode.value = data.colorWeakMode;
    if (data.sidebarWidth != null) sidebarWidth.value = data.sidebarWidth;
    if (data.constrainContentWidth != null) constrainContentWidth.value = data.constrainContentWidth;
    if (data.contentMaxWidth != null) contentMaxWidth.value = data.contentMaxWidth;
    if (data.contentAlign != null) contentAlign.value = data.contentAlign;
    if (data.fillPageHeight != null) fillPageHeight.value = data.fillPageHeight;
    if (typeof data.menuUniqueOpened === 'boolean') menuUniqueOpened.value = data.menuUniqueOpened;
    if (typeof data.fixedOperationColumn === 'boolean') fixedOperationColumn.value = data.fixedOperationColumn;
    if (typeof data.mobileFixedHeader === 'boolean') mobileFixedHeader.value = data.mobileFixedHeader;
  }

  // 重置为默认值（落盘 + 应用）
  function resetToDefault() {
    theme.value = DEFAULT_SETTINGS.theme;
    primaryColor.value = DEFAULT_SETTINGS.primaryColor;
    menuMode.value = DEFAULT_SETTINGS.menuMode;
    showTabs.value = DEFAULT_SETTINGS.showTabs;
    retainOpenTabs.value = DEFAULT_SETTINGS.retainOpenTabs;
    showBreadcrumb.value = DEFAULT_SETTINGS.showBreadcrumb;
    listRenderMode.value = DEFAULT_SETTINGS.listRenderMode;
    listStripe.value = DEFAULT_SETTINGS.listStripe;
    pageTransition.value = DEFAULT_SETTINGS.pageTransition;
    modalCloseOnClickMask.value = DEFAULT_SETTINGS.modalCloseOnClickMask;
    showWatermark.value = DEFAULT_SETTINGS.showWatermark;
    watermarkText.value = DEFAULT_SETTINGS.watermarkText;
    greyMode.value = DEFAULT_SETTINGS.greyMode;
    colorWeakMode.value = DEFAULT_SETTINGS.colorWeakMode;
    sidebarWidth.value = DEFAULT_SETTINGS.sidebarWidth;
    constrainContentWidth.value = DEFAULT_SETTINGS.constrainContentWidth;
    contentMaxWidth.value = DEFAULT_SETTINGS.contentMaxWidth;
    contentAlign.value = DEFAULT_SETTINGS.contentAlign;
    fillPageHeight.value = DEFAULT_SETTINGS.fillPageHeight;
    menuUniqueOpened.value = DEFAULT_SETTINGS.menuUniqueOpened;
    fixedOperationColumn.value = DEFAULT_SETTINGS.fixedOperationColumn;
    mobileFixedHeader.value = DEFAULT_SETTINGS.mobileFixedHeader;
    apply();
  }

  // 跨标签页同步（同键变更 → 重载 + 应用）
  function bindCrossTabSync() {
    if (typeof window === 'undefined' || storageHandler) return;
    storageHandler = (event) => {
      if (event.storageArea === localStorage && event.key === SETTINGS_STORAGE_KEY) {
        init();
        applyToDOM();
      }
    };
    window.addEventListener('storage', storageHandler);
  }

  function unbindCrossTabSync() {
    if (typeof window === 'undefined' || !storageHandler) return;
    window.removeEventListener('storage', storageHandler);
    storageHandler = null;
  }

  // 单项 setter（仅改内存值；落盘/应用由 apply() 统一触发）
  const setTheme = (value) => { theme.value = value; };
  const setPrimaryColor = (value) => { primaryColor.value = value; };
  const setMenuMode = (value) => { menuMode.value = value; };
  const setShowTabs = (value) => { showTabs.value = value; };
  const setRetainOpenTabs = (value) => { retainOpenTabs.value = value; };
  const setShowBreadcrumb = (value) => { showBreadcrumb.value = value; };
  const setListRenderMode = (value) => { listRenderMode.value = value; };
  const setListStripe = (value) => { listStripe.value = value; };
  const setPageTransition = (value) => { pageTransition.value = value; };
  const setModalCloseOnClickMask = (value) => { modalCloseOnClickMask.value = value; };
  const setShowWatermark = (value) => { showWatermark.value = value; };
  const setWatermarkText = (value) => { watermarkText.value = value; };
  const setGreyMode = (value) => { greyMode.value = value; };
  const setColorWeakMode = (value) => { colorWeakMode.value = value; };
  const setSidebarWidth = (value) => { sidebarWidth.value = value; };
  const setConstrainContentWidth = (value) => { constrainContentWidth.value = value; };
  const setContentMaxWidth = (value) => { contentMaxWidth.value = value; };
  const setContentAlign = (value) => { contentAlign.value = value; };
  const setFillPageHeight = (value) => { fillPageHeight.value = value; };
  const setMenuUniqueOpened = (value) => { menuUniqueOpened.value = value; };
  const setFixedOperationColumn = (value) => { fixedOperationColumn.value = value; };
  const setMobileFixedHeader = (value) => { mobileFixedHeader.value = value; };

  init();

  return {
    theme,
    primaryColor,
    menuMode,
    showTabs,
    retainOpenTabs,
    showBreadcrumb,
    listRenderMode,
    listStripe,
    pageTransition,
    modalCloseOnClickMask,
    showWatermark,
    watermarkText,
    greyMode,
    colorWeakMode,
    sidebarWidth,
    constrainContentWidth,
    contentMaxWidth,
    contentAlign,
    fillPageHeight,
    menuUniqueOpened,
    fixedOperationColumn,
    mobileFixedHeader,
    setTheme,
    setPrimaryColor,
    setMenuMode,
    setShowTabs,
    setRetainOpenTabs,
    setShowBreadcrumb,
    setListRenderMode,
    setListStripe,
    setPageTransition,
    setModalCloseOnClickMask,
    setShowWatermark,
    setWatermarkText,
    setGreyMode,
    setColorWeakMode,
    setSidebarWidth,
    setConstrainContentWidth,
    setContentMaxWidth,
    setContentAlign,
    setFillPageHeight,
    setMenuUniqueOpened,
    setFixedOperationColumn,
    setMobileFixedHeader,
    resetToDefault,
    apply,
    applyToDOM,
    restoreFromSnapshot,
    getSnapshot,
    init,
    bindCrossTabSync,
    unbindCrossTabSync
  };
});
