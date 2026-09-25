// 日期时间格式化 —— 行为移植自编译产物入口 chunk（jn：YYYY-MM-DD HH:mm:ss，空值/无效值回退 "-"）
export function formatDateTime(value) {
  if (value == null || value === '') return '-';
  const date = typeof value === 'string' || typeof value === 'number' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return '-';
  const pad = (n) => String(n).padStart(2, '0');
  return (
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
  );
}

// 点分日期 + 星期 —— 行为移植自编译产物入口 chunk（Zr + xe：YYYY.MM.DD HH:mm:ss (周X)）
const WEEKDAY_LABELS = ['日', '一', '二', '三', '四', '五', '六'];
export function formatDateDot(value) {
  if (value == null || value === '') return '-';
  const date = typeof value === 'string' || typeof value === 'number' ? new Date(value) : value;
  if (Number.isNaN(date.getTime())) return '-';
  const pad = (n) => String(n).padStart(2, '0');
  return (
    `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())} ` +
    `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())} (${WEEKDAY_LABELS[date.getDay()]})`
  );
}
