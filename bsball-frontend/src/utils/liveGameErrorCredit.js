// 失误记账判定（#236/#437）：同一 play 内推进承接前一次失误/暴投/捕逸，不再重复计 E。
// （WP/PB 的计数在各自入口跳过，不在此函数处理。）
export function shouldCreditError(reasonId) {
  return reasonId !== 'safe_same_err';
}

// 本 play 已记过失误、暴投或捕逸时，提供“同一play内推进”选项；否则隐藏。
export function filterErrorReasons(reasons, playErrorCredited, playWpCredited = false, playPbCredited = false) {
  if (!Array.isArray(reasons) || playErrorCredited || playWpCredited || playPbCredited) return reasons;
  return reasons.filter((reason) => reason?.id !== 'safe_same_err');
}

// E 录入默认单选失误责任者；显式开启多失误模式后追加，再点已选守备员则移除
export function selectErrorFielder(steps, pos, multiple = false) {
  const list = Array.isArray(steps) ? steps : [];
  if (!multiple) return [pos];
  return list.includes(pos) ? list.filter((item) => item !== pos) : [...list, pos];
}

// 旧快照/撤销栈的 E flow 没有 multiError；已有多名责任者时按多选恢复，避免下一次点选清空旧选择
export function normalizeErrorFlow(flow) {
  if (flow?.type === 'E' && flow.multiError == null && Array.isArray(flow.steps) && flow.steps.length > 1) {
    flow.multiError = true;
  }
  return flow;
}
