// 文本截断——行为保真移植自编译产物入口 chunk index-ByOnov1B.js（il）
export function truncateString(value, maxLength) {
  const text = (value ?? '').trim();
  if (text === '') return '';
  const chars = [...text];
  return chars.length <= maxLength ? text : chars.slice(0, maxLength).join('') + '…';
}

// 空值保护字符串（入口 chunk el）——null/空串返回空串，其余 String 化
export function toSafeString(value) {
  return value == null || value === '' ? '' : String(value);
}
