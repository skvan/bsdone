// 球员选项与格式化 —— 行为移植自编译产物 player chunk（逐字）
export const PLAYER_STATUS_OPTIONS = { active: '在役', retired: '退役', retired_illness: '因病退役', retired_injury: '因伤退役' };

const HAND_LABELS = { L: '左', R: '右', B: '左右' };

export const POSITION_LABELS = {
  P: '投手', C: '捕手', '1B': '一垒手', '2B': '二垒手', '3B': '三垒手',
  SS: '游击手', LF: '左外野手', CF: '中外野手', RF: '右外野手',
  DH: '指定打击', PH: '代打', PR: '代跑', OF: '外野手', IF: '内野手',
  XF: '自由人', XF1: '自由人①', XF2: '自由人②'
};

// 打/投侧显示：如 L (左)
export function formatHand(value) {
  const key = value;
  return key && HAND_LABELS[key] ? `${key} (${HAND_LABELS[key]})` : String(value);
}

// 打/投组合显示：如 R/L、R/-、-/L、-
export function formatBatsThrows(player) {
  const bat = player.batHand ?? player.hand;
  const thr = player.throwHand ?? player.hand;
  return bat && thr ? `${bat}/${thr}` : bat ? `${bat}/-` : thr ? `-/${thr}` : '-';
}

// 守备位置显示：如 SS (游击手)
export function formatPosition(value) {
  return `${value} (${POSITION_LABELS[value]})`;
}
