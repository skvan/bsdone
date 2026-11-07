// 实时录入文案：ja/ko 等非中文语言统一回退英文（全局 fallbackLocale 为 zh-CN）
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';

export function useLiveGameText() {
  const { t, locale } = useI18n();
  const isEnglish = computed(() => !String(locale.value || '').startsWith('zh'));
  const text = (key, params) => t(key, params ?? {}, { locale: isEnglish.value ? 'en' : locale.value });
  return { isEnglish, text };
}
