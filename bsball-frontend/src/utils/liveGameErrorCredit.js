// 失误记账判定（#236）：本 play 已记过失误时，后续“同一失误推进”不再重复计 E；
// 原因列表正常情况下会在未记失误时隐藏该选项，这里做兜底。
export function shouldCreditError(reasonId, playErrorCredited) {
  return reasonId !== 'safe_same_err' || !playErrorCredited;
}

// 本 play 尚未记过失误时，不提供“同一play内失误推进”选项
export function filterErrorReasons(reasons, playErrorCredited) {
  if (!Array.isArray(reasons) || playErrorCredited) return reasons;
  return reasons.filter((reason) => reason?.id !== 'safe_same_err');
}
