// 实时动态打席头部垒包解析（#399）：
// 有打席摘要时优先用摘要记录的 basesBefore，避免当前打席内实时 runners 变化导致头部垒包跳动。
export function resolveAtBatGroupBases({ isCurrent, liveBases, summary, activeBases }) {
  const before = isCurrent ? (activeBases ?? summary?.basesBefore) : summary?.basesBefore;
  if (before && typeof before === 'object') {
    return {
      1: !!before[1],
      2: !!before[2],
      3: !!before[3]
    };
  }
  if (isCurrent) {
    return {
      1: !!(liveBases?.[1]),
      2: !!(liveBases?.[2]),
      3: !!(liveBases?.[3])
    };
  }
  return { 1: false, 2: false, 3: false };
}
