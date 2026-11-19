// html2canvas 序列化内联 SVG 时会丢失外部 scoped CSS 与 CSS 变量。
// 截图前把 computed style 写成 presentation attribute，截图后恢复。
const SVG_PRESENTATION_PROPS = [
  'fill',
  'fill-opacity',
  'stroke',
  'stroke-width',
  'stroke-opacity',
  'stroke-dasharray',
  'stroke-linecap',
  'stroke-linejoin',
  'opacity',
  'stop-color',
  'stop-opacity',
  'color',
  'font-size',
  'font-family',
  'font-weight',
  'text-anchor',
  'dominant-baseline',
  'shape-rendering'
];

/**
 * 把 root 内所有 SVG 子元素的 computed presentation 样式写成属性。
 * @returns 恢复函数；调用后移除/还原本次写入的属性。
 */
export function inlineSvgPresentationStyles(root) {
  if (!root || typeof window === 'undefined') return () => {};
  const restores = [];
  root.querySelectorAll('svg *').forEach((node) => {
    const computed = window.getComputedStyle(node);
    const previous = {};
    for (const prop of SVG_PRESENTATION_PROPS) {
      const value = computed.getPropertyValue(prop);
      if (!value) continue;
      previous[prop] = node.getAttribute(prop);
      node.setAttribute(prop, value.trim());
    }
    restores.push(() => {
      Object.entries(previous).forEach(([prop, value]) => {
        if (value == null) node.removeAttribute(prop);
        else node.setAttribute(prop, value);
      });
    });
  });
  return () => restores.forEach((restore) => restore());
}
