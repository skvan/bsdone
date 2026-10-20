/**
 * 从 vue-i18n 的返回值中提取可展示文本。
 * 正常是 string；部分场景会返回 { value } / { default } / { message } 等消息对象，
 * 直接 String(对象) 会渲染成 [object Object]。
 * 返回值等于 key 时视为未命中（vue-i18n 缺词会原样返回 key）。
 */
export function pickI18nText(value, key = '', depth = 0) {
  if (typeof value === 'string') {
    return value && value !== key && value.trim() ? value : '';
  }
  if (!value || typeof value !== 'object' || depth > 2) return '';
  for (const field of ['value', 'default', 'message', 'text', 'label']) {
    const text = value[field];
    if (typeof text === 'string') {
      if (text && text !== key && text.trim()) return text;
      continue;
    }
    if (text && typeof text === 'object') {
      const nested = pickI18nText(text, key, depth + 1);
      if (nested) return nested;
    }
  }
  return '';
}

export const BOX_VIEW_LABELS = Object.freeze({
  batting: '进攻',
  pitching: '投球',
  fielding: '防守'
});

/** 比赛详情盒子比分视图 Tab：本地 i18n → 全局 i18n → 视图兜底 → key 末段。 */
export function resolveBoxViewLabel(key, localValue, globalValue) {
  const leaf = String(key).split('.').pop();
  return (
    pickI18nText(localValue, key) ||
    pickI18nText(globalValue, key) ||
    BOX_VIEW_LABELS[leaf] ||
    leaf
  );
}
