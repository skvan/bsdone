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
