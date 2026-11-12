// 批量添加替补：候选项勾选状态与提交值整理（纯函数，供 LineupPoolPanel 使用）
// 候选项 = 名册 − 已在打线者；已选集合可能包含已不在候选中的旧 id，提交前统一剔除。

/** 候选项勾选状态：只统计当前候选项，忽略候选外的旧 id。 */
export function benchSelectionState(options, selectedIds) {
  const list = Array.isArray(options) ? options : [];
  const selected = new Set((Array.isArray(selectedIds) ? selectedIds : []).map(String));
  const selectedCount = list.filter((player) => selected.has(String(player?.id))).length;
  return {
    selectedCount,
    allSelected: list.length > 0 && selectedCount === list.length,
    indeterminate: selectedCount > 0 && selectedCount < list.length,
  };
}

/**
 * 整理提交值：只保留当前候选项，保持原有选择顺序并去重。
 * 返回的 id 取候选项自身类型（避免 checkbox 字符串/数字混用导致下游匹配失败）。
 */
export function sanitizeBenchIds(options, selectedIds) {
  const list = Array.isArray(options) ? options : [];
  const idByKey = new Map(list.map((player) => [String(player?.id), player?.id]));
  const out = [];
  const seen = new Set();
  for (const raw of Array.isArray(selectedIds) ? selectedIds : []) {
    const key = String(raw);
    if (!idByKey.has(key) || seen.has(key)) continue;
    seen.add(key);
    out.push(idByKey.get(key));
  }
  return out;
}
