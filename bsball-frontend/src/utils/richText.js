// 富文本内容判定 —— 行为移植自编译产物入口 chunk（En：去除标签/占位符后是否有可见文本）
export function hasMeaningfulText(html) {
  if (!html?.trim()) return false;
  return (
    html
      .replace(/<[^>]*>/g, ' ')
      .replace(/&nbsp;/gi, ' ')
      .replace(/\s+/g, ' ')
      .trim().length > 0
  );
}
