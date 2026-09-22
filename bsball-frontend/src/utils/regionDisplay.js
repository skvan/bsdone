// 地区名显示 —— 行为移植自编译产物 regionDisplay chunk（去除末尾「省」）
export function stripProvinceSuffix(text) {
  const value = (text ?? '').trim();
  if (!value) return '';
  return value.endsWith('省') ? value.slice(0, -1) : value;
}
