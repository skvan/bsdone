// 应用国际化 —— 行为移植自编译产物入口 chunk（vue-i18n 实例 + 语言探测/持久化）
// 依据：createI18n({legacy:false, locale:<storage|探测>, fallbackLocale:"zh-CN", messages:{zh-CN,zh-TW,en,ko,ja}})
// 词典为 recon-extract-locales4.mjs 从编译产物提取（zh-CN 30 命名空间；其余 24 命名空间 + zh-CN 兜底）
import { createI18n } from 'vue-i18n';
import zhCN from '../locales/zh-CN.json';
import zhTW from '../locales/zh-TW.json';
import en from '../locales/en.json';
import ko from '../locales/ko.json';
import ja from '../locales/ja.json';

export const LOCALE_STORAGE_KEY = 'bs-ball-locale';
export const SUPPORTED_LOCALES = ['zh-CN', 'zh-TW', 'en', 'ko', 'ja'];

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

// 语言来源：localStorage 优先，其次浏览器探测
export function storedLocale() {
  if (typeof localStorage === 'undefined') return detectLocale();
  try {
    const stored = localStorage.getItem(LOCALE_STORAGE_KEY);
    if (stored && isSupportedLocale(stored)) return stored;
  } catch {
    // ignore
  }
  return detectLocale();
}

export const i18n = createI18n({
  legacy: false,
  locale: storedLocale(),
  fallbackLocale: 'zh-CN',
  messages: { 'zh-CN': zhCN, 'zh-TW': zhTW, en, ko, ja }
});

// 取词（与编译产物 t 包装一致：字符串化返回；支持 {param} 插值）
export function t(key, params) {
  return params ? String(i18n.global.t(key, params)) : String(i18n.global.t(key));
}

export function currentLocale() {
  return i18n.global.locale.value;
}

// 切换语言并持久化（编译产物：写入 bs-ball-locale）
export function setLocale(next) {
  if (!isSupportedLocale(next)) return;
  i18n.global.locale.value = next;
  try {
    localStorage.setItem(LOCALE_STORAGE_KEY, next);
  } catch {
    // ignore
  }
}

// 应用语言码 → Element Plus / dayjs 语言包码（编译产物 wr 映射）
export const VENDOR_LOCALE_CODES = { 'zh-CN': 'zh-cn', 'zh-TW': 'zh-tw', en: 'en', ko: 'ko', ja: 'ja' };

// 当前语言对应的第三方语言包码（编译产物 Tr()：非 zh-TW/en/ko/ja 一律回退 zh-cn）
export function vendorLocaleCode() {
  const current = currentLocale();
  return current === 'zh-TW' || current === 'en' || current === 'ko' || current === 'ja'
    ? VENDOR_LOCALE_CODES[current]
    : VENDOR_LOCALE_CODES['zh-CN'];
}
