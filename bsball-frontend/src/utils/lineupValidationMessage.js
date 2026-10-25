// 阵容页底部提示只保留「联赛未开放 EP 制」这类画布上看不出来的规则错误；
// 空位 / 投手 / 重复 / 无效位置 / 人数错误由卡槽、红框和打线计数直接呈现。
export function visibleLineupError(msg) {
  const text = String(msg ?? '');
  return text.includes('未开放 EP 制') ? text : '';
}
