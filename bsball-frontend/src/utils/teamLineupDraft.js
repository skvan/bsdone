// 赛前阵容提交（球队端）：只承载单队数据，和办赛方整场快照分开。
// v:1 结构：lineup/bench 使用阵容页现有行对象，fieldingPitcherId 为仅守备投手。

export function buildTeamLineupDraft(input = {}) {
  return {
    v: 1,
    gameId: input.gameId ?? null,
    teamId: input.teamId ?? null,
    gameMode: input.gameMode ?? 'BASEBALL',
    complete: input.complete === true,
    lineup: Array.isArray(input.lineup) ? input.lineup : [],
    bench: Array.isArray(input.bench) ? input.bench : [],
    fieldingPitcherId: input.fieldingPitcherId ?? null,
    lastDhAddedFromPoolId: input.lastDhAddedFromPoolId ?? null
  };
}

/** 解析远端提交：非 v:1 / 非法 JSON 一律 null（不抛错）。 */
export function parseTeamLineupDraft(raw) {
  if (typeof raw !== 'string' || !raw.trim()) return null;
  let data;
  try {
    data = JSON.parse(raw);
  } catch {
    return null;
  }
  return data && typeof data === 'object' && data.v === 1 ? data : null;
}

/** 是否有可展示的阵容数据（占位行 id<=0 不算）。 */
export function hasTeamLineupData(draft) {
  if (!draft || typeof draft !== 'object') return false;
  const hasRows = (list) => Array.isArray(list) && list.some((row) => Number(row?.id) > 0);
  return hasRows(draft.lineup) || hasRows(draft.bench) || Number(draft.fieldingPitcherId) > 0;
}

/** 转成阵容板 loadDraft 入参。 */
export function toBoardDraft(draft) {
  if (!draft || typeof draft !== 'object') return null;
  return {
    rows: Array.isArray(draft.lineup) ? draft.lineup : [],
    bench: Array.isArray(draft.bench) ? draft.bench : [],
    fieldingPitcherId: draft.fieldingPitcherId ?? null,
    lastDhAddedFromPoolId: draft.lastDhAddedFromPoolId ?? null
  };
}

/** 办赛方快照某一侧是否已有实际阵容数据。 */
export function snapshotSideHasData(snapshot, side) {
  if (!snapshot || (side !== 'home' && side !== 'away')) return false;
  const rows = Array.isArray(snapshot[`${side}Lineup`]) ? snapshot[`${side}Lineup`] : [];
  const bench = Array.isArray(snapshot[`${side}Bench`]) ? snapshot[`${side}Bench`] : [];
  return hasTeamLineupData({
    lineup: rows,
    bench,
    fieldingPitcherId: snapshot[`${side}FieldingPitcherId`]
  });
}

/** 提交时间是否晚于快照记录的“已载入版本”（任一为空按需要提示处理）。 */
export function submissionIsNewer(submissionUpdatedAt, loadedAt) {
  if (!submissionUpdatedAt) return false;
  if (!loadedAt) return true;
  const a = Date.parse(submissionUpdatedAt);
  const b = Date.parse(loadedAt);
  if (Number.isNaN(a) || Number.isNaN(b)) return true;
  return a > b;
}

/** 后端 LocalDateTime / ISO 字符串 → 「YYYY-MM-DD HH:mm」；空值返回 emptyText。 */
export function formatLineupDateTime(value, emptyText = '时间待定') {
  const text = String(value ?? '').trim();
  return text ? text.replace('T', ' ').slice(0, 16) : emptyText;
}
