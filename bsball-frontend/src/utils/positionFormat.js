// 位置显示清洗——行为保真移植自编译产物入口 chunk index-ByOnov1B.js（rl）
export function formatPositionDisplay(row) {
  const raw = row.positionDisplay ?? row.position ?? '-';
  const text = typeof raw === 'string' ? raw : String(raw);
  if (text === '-') return text;
  return text.replace(/\s*、\s*/g, ', ').replace(/\s*,\s*/g, ', ').replace(/,\s*,/g, ', ').trim();
}
