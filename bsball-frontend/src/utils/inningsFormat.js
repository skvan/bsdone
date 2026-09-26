// 投球局数（IP）换算与显示 —— 行为移植自编译产物入口 chunk（ta/ni/tl/li 逐字）
// 记法：1 局=3 出局；IP 0.1=1/3 局、0.2=2/3 局

// 局数（IP 显示值）→ 出局数
export function inningsToOuts(innings) {
  if (!Number.isFinite(innings) || innings < 0) return 0;
  const value = innings + 1e-10;
  let whole = Math.floor(value);
  let fraction = value - whole;
  if (fraction < 1e-10) return whole * 3;
  let rem = Math.round(fraction * 10);
  if (rem === 3 && fraction < 0.31) rem = 2;
  for (; rem > 2; ) {
    rem -= 3;
    whole += 1;
  }
  for (; rem < 0; ) {
    rem += 3;
    whole -= 1;
  }
  rem = Math.min(2, Math.max(0, rem));
  return whole * 3 + rem;
}

// 出局数 → 局数（IP 显示值）
export function outsToInnings(outs) {
  return !Number.isFinite(outs) || outs < 0 ? 0 : Math.floor(outs / 3) + (outs % 3) * 0.1;
}

// 多个 IP 值求和 → 总 IP（先转出局求和再回转）
export function totalInningsPitched(list) {
  return outsToInnings(list.reduce((sum, value) => sum + inningsToOuts(value), 0));
}

// IP 显示格式化（出局↔局数换算 + toFixed(1) 去尾零；入口 chunk 导出 li）
export function formatInningsPitched(value) {
  if (value == null || !Number.isFinite(Number(value)) || Number(value) < 0) return '0';
  const outs = inningsToOuts(Number(value));
  return outs <= 0 ? '0' : outsToInnings(outs).toFixed(1).replace(/\.0$/, '');
}
