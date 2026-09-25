// 验证码类型归一化 —— 行为移植自编译产物入口 chunk（po：未知值一律回退 'input'）
export function normalizeCaptchaType(value) {
  const type = typeof value === 'string' ? value.trim().toLowerCase() : '';
  if (type === 'click') return 'click';
  if (type === 'drag') return 'drag';
  return 'input';
}
