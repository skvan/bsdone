// 门户页头模式归一化 —— 行为移植自编译产物入口 chunk（Le）
// natural/scroll/static → natural；reveal/autohide/hide-on-scroll → reveal；其余 → fixed
export function normalizePortalHeaderMode(mode) {
  const value = typeof mode === 'string' ? mode.trim().toLowerCase() : '';
  if (value === 'natural' || value === 'scroll' || value === 'static') return 'natural';
  if (value === 'reveal' || value === 'autohide' || value === 'hide-on-scroll') return 'reveal';
  return 'fixed';
}
