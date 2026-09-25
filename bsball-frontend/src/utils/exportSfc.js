// SFC 导出助手 —— 与 @vitejs/plugin-vue 的 _export_sfc 等价
// 用途：移植期组件按编译产物方式挂载 __scopeId（保证与逐字拷贝的样式选择器匹配）
export function exportSfc(component, options) {
  for (const [key, value] of options) component[key] = value;
  return component;
}
