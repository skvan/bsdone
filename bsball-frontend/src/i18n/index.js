// 轻量 i18n —— 语言列表/探测/存储键移植自编译产物入口 chunk
// 说明：当前仅内置 zh-CN 词典（自 i18n-dictionary.json 生成）；zh-TW/en/ko/ja 词典待运行态抓取补齐（B1-b 前完成）。
import { ref, inject } from 'vue';
import zhCN from '../locales/zh-CN.json';

export const LOCALE_STORAGE_KEY = 'bs-ball-locale';
export const SUPPORTED_LOCALES = ['zh-CN', 'zh-TW', 'en', 'ko', 'ja'];

const DICTS = {
  'zh-CN': zhCN
};

export function isSupportedLocale(locale) {
  return SUPPORTED_LOCALES.includes(locale);
}

// 浏览器语言探测（与编译产物一致的判定顺序）
export function detectLocale() {
  if (typeof navigator === 'undefined') return 'zh-CN';
  const lang = (navigator.language || (navigator.languages && navigator.languages[0]) || '').toLowerCase();
  if (lang.startsWith('zh')) {
    return lang.includes('tw') || lang.includes('hk') || lang.includes('hant') ? 'zh-TW' : 'zh-CN';
  }
  if (lang.startsWith('ko')) return 'ko';
  if (lang.startsWith('ja')) return 'ja';
  if (lang.startsWith('en')) return 'en';
  return 'zh-CN';
}

export function initialLocale() {
  if (typeof localStorage === 'undefined') return detectLocale();
  const stored = localStorage.getItem(LOCALE_STORAGE_KEY);
  return stored && isSupportedLocale(stored) ? stored : detectLocale();
}

function getByPath(dict, key) {
  const parts = key.split('.');
  let node = dict;
  for (const part of parts) {
    if (node == null) return undefined;
    node = node[part];
  }
  return node;
}

// 取词：支持 {param} 插值；缺失键回退 zh-CN，再回退键名
export function createI18n() {
  const locale = ref(initialLocale());
  const messages = ref(DICTS);

  function t(key, params) {
    let value = getByPath(messages.value[locale.value], key);
    if (value === undefined) value = getByPath(messages.value['zh-CN'], key);
    if (value === undefined || typeof value !== 'string') return key;
    if (!params) return value;
    return value.replace(/\{(\w+)\}/g, (match, name) => (params[name] != null ? String(params[name]) : match));
  }

  function setLocale(next) {
    if (!isSupportedLocale(next)) return;
    locale.value = next;
    try {
      localStorage.setItem(LOCALE_STORAGE_KEY, next);
    } catch {
      // ignore
    }
  }

  // 运行态注入词典（如从旧版抓取的 zh-TW/en/ko/ja）
  function mergeMessages(code, dict) {
    messages.value = { ...messages.value, [code]: dict };
  }

  return { locale, t, setLocale, mergeMessages };
}

const I18N_KEY = Symbol('bsball-i18n');

export function installI18n(app) {
  const i18n = createI18n();
  app.provide(I18N_KEY, i18n);
  app.config.globalProperties.$t = i18n.t;
  return i18n;
}

export function useI18n() {
  const i18n = inject(I18N_KEY, null);
  if (!i18n) {
    throw new Error('i18n 未安装：请在 main.js 中调用 installI18n(app)');
  }
  return i18n;
}
