// 界面设置 —— 默认值与持久化键移植自编译产物入口 chunk（settings store）
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

// 读取本地设置（与默认值合并；retainOpenTabs 保留显式 false 语义）
export function loadSettings() {
  try {
    const raw = localStorage.getItem(SETTINGS_STORAGE_KEY);
    if (raw) {
      const stored = JSON.parse(raw);
      const merged = { ...DEFAULT_SETTINGS, ...stored };
      if (typeof stored.retainOpenTabs === 'boolean') merged.retainOpenTabs = stored.retainOpenTabs;
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

export const useSettingsStore = defineStore('settings', () => {
  const initial = loadSettings();
  const state = {};
  for (const key of Object.keys(DEFAULT_SETTINGS)) state[key] = ref(initial[key]);

  function snapshot() {
    const out = {};
    for (const key of Object.keys(DEFAULT_SETTINGS)) out[key] = state[key].value;
    return out;
  }

  function save() {
    persistSettings(snapshot());
  }

  function reset() {
    for (const key of Object.keys(DEFAULT_SETTINGS)) state[key].value = DEFAULT_SETTINGS[key];
    save();
  }

  return { ...state, snapshot, save, reset };
});
