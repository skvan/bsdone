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
