// 门户主题 —— 行为移植自编译产物入口 chunk（portalTheme store）
// 存储键 bs-ball-portal-theme；取值 light|dark|auto（非法值回退 auto）
import { ref } from 'vue';
import { defineStore } from 'pinia';

export const PORTAL_THEME_STORAGE_KEY = 'bs-ball-portal-theme';

function readStoredTheme() {
  try {
    const value = localStorage.getItem(PORTAL_THEME_STORAGE_KEY);
    if (value === 'light' || value === 'dark' || value === 'auto') return value;
  } catch {
    // ignore
  }
  return 'auto';
}

export const usePortalThemeStore = defineStore('portalTheme', () => {
  const theme = ref(readStoredTheme());

  function persist() {
    try {
      localStorage.setItem(PORTAL_THEME_STORAGE_KEY, theme.value);
    } catch {
      // ignore
    }
  }

  function setTheme(next) {
    theme.value = next;
  }

  // auto → 跟随系统
  function getEffective() {
    if (theme.value === 'auto') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    return theme.value;
  }

  function applyToDOM() {
    const effective = getEffective();
    document.documentElement.classList.toggle('dark', effective === 'dark');
    document.documentElement.setAttribute('data-portal-theme', effective);
  }

  // 持久化 + 应用
  function apply() {
    persist();
    applyToDOM();
  }

  // 从 localStorage 重载并应用
  function reloadFromStorage() {
    theme.value = readStoredTheme();
    applyToDOM();
  }

  return { theme, setTheme, getEffective, applyToDOM, apply, reloadFromStorage };
});
