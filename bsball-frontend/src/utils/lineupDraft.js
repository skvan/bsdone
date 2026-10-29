// 实时录入「确认先发阵容」中途保存（草稿）工具（#207）
// 快照结构沿用页面既有 v:1 payload（setupForm + 两队列/替补/投手/DH 池/缺阵标记），
// setupForm 新增 gameTime/gameday；gameStarted:false 表示仅保存阵容、比赛尚未开始。

/** 本地时间 → 「YYYY-MM-DD HH:mm:ss」（与 el-date-picker value-format 一致） */
export function toLocalGameTime(date = new Date()) {
  const p = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ${p(
    date.getHours()
  )}:${p(date.getMinutes())}:${p(date.getSeconds())}`;
}

/** 「YYYY-MM-DD[ HH:mm[:ss]]」→ { gameTime, gameday }；非法/空值原样返回且 gameday 为空 */
export function splitGameTime(value) {
  const s = typeof value === 'string' ? value.trim() : '';
  const m = /^(\d{4}-\d{2}-\d{2})(?:[ T](\d{2}:\d{2})(:\d{2})?)?$/.exec(s);
  if (!m) return { gameTime: s, gameday: '' };
  const gameday = m[1];
  const time = m[2] ? `${m[2]}${m[3] ?? ':00'}` : '00:00:00';
  return { gameTime: `${gameday} ${time}`, gameday };
}

/** 组「中途保存」草稿快照（gameStarted:false，不含比赛状态/比分） */
export function buildLineupDraft(input) {
  return {
    v: 1,
    savedAt: Date.now(),
    gameId: input.gameId,
    gameMode: input.gameMode,
    gameStarted: false,
    setupForm: {
      homeTeamId: input.homeTeamId,
      awayTeamId: input.awayTeamId,
      venue: input.venue,
      gameTime: input.gameTime,
      gameday: input.gameday
    },
    homeLineup: input.homeLineup,
    awayLineup: input.awayLineup,
    homeBench: input.homeBench,
    awayBench: input.awayBench,
    awayFieldingPitcherId: input.awayFieldingPitcherId,
    homeFieldingPitcherId: input.homeFieldingPitcherId,
    awayLastDhAddedFromPoolId: input.awayLastDhAddedFromPoolId,
    homeLastDhAddedFromPoolId: input.homeLastDhAddedFromPoolId,
    awayUnavailablePlayerIds: input.awayUnavailablePlayerIds,
    homeUnavailablePlayerIds: input.homeUnavailablePlayerIds
  };
}

/** 解析远端快照字符串：非 v:1 或非法 JSON 一律返回 null（不抛错） */
export function parseLineupDraft(raw) {
  if (typeof raw !== 'string' || !raw.trim()) return null;
  let data;
  try {
    data = JSON.parse(raw);
  } catch {
    return null;
  }
  return data && typeof data === 'object' && data.v === 1 ? data : null;
}

/**
 * 恢复阵容页初始值：快照 setupForm/gameMode 优先，缺失字段回落比赛记录。
 * 空字符串视为「已明确清空」，仅 undefined/null 才回落。
 */
export function resolveLineupResumeInit(game, snapshot) {
  const base = game || {};
  const setup = (snapshot && snapshot.setupForm) || {};
  const pick = (snapVal, gameVal) => (snapVal === undefined || snapVal === null ? gameVal ?? null : snapVal);
  return {
    homeTeamId: pick(setup.homeTeamId, base.homeTeamId),
    awayTeamId: pick(setup.awayTeamId, base.awayTeamId),
    venue: pick(setup.venue, base.venue),
    gameTime: pick(setup.gameTime, base.gameTime),
    gameMode: (snapshot && snapshot.gameMode) || base.gameMode || ''
  };
}
