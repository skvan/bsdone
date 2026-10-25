// 实时记分台账：一次进攻（打席）内可发生多个独立动作（盗本垒、暴投、击球结果等）。
// 三出局撤销只能回滚「当前动作」倒入的分数，不能连带抹掉同一打席先前动作已得的分数。
export function createRunLedger() {
  return { entries: [], boundary: 0 };
}

export function beginPlay(ledger) {
  ledger.boundary = ledger.entries.length;
  return ledger.boundary;
}

export function addRun(ledger, entry) {
  if (!entry || !(Number(entry.runs) > 0)) return null;
  ledger.entries.push(entry);
  return entry;
}

export function voidCurrentPlayRuns(ledger) {
  if (!ledger.entries.length) return [];
  const start = Math.min(Math.max(0, ledger.boundary), ledger.entries.length);
  const voided = ledger.entries.splice(start);
  ledger.boundary = ledger.entries.length;
  return voided;
}

// 打席落定：取走本打席全部得分（用于组装 playEvent），台账与边界归零。
export function takeAllRuns(ledger) {
  const entries = ledger.entries;
  ledger.entries = [];
  ledger.boundary = 0;
  return entries;
}

// 恢复/重置：直接丢弃台账，边界归零。
export function clearRuns(ledger) {
  ledger.entries.length = 0;
  ledger.boundary = 0;
}

// 推进方式只决定「这一分怎么回来的」；引擎据此判定暴投/捕逸/失误来源。
export function advanceOrigin(reasonId) {
  if (reasonId === 'safe_wp') return 'WILD_PITCH';
  if (reasonId === 'safe_pb') return 'PASSED_BALL';
  if (reasonId === 'safe_te' || reasonId === 'safe_fe' || reasonId === 'safe_pk_err'
    || reasonId === 'safe_same_err' || reasonId === 'safe_diff_err') return 'ERROR';
  return 'NORMAL';
}

// 跑者上垒即失误/妨碍/突破僵局时，其得分来源优先按上垒方式判定（自责分口径）。
export function runOrigin(runner, scoringCause) {
  const reached = runner?.origin;
  if (reached === 'ERROR' || reached === 'TIE_BREAK' || reached === 'INTERFERENCE') {
    return reached;
  }
  return scoringCause ?? 'NORMAL';
}
